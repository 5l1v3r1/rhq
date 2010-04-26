/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.admin.role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.exception.LdapCommunicationException;
import org.rhq.enterprise.server.exception.LdapFilterException;
import org.rhq.enterprise.server.resource.group.LdapGroupManager;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data to facilitate display of the form for adding groups to a role.
 */
public class AddLdapGroupsFormPrepareAction extends TilesAction {
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddLdapGroupsFormPrepareAction.class.getName());

        Subject whoami = RequestUtils.getSubject(request);
        AddLdapGroupsForm addForm = (AddLdapGroupsForm) form;
        Integer roleId = addForm.getR();

        if (roleId == null) {
            roleId = RequestUtils.getRoleId(request);
        }

        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role == null) {
            RequestUtils.setError(request, Constants.ERR_ROLE_NOT_FOUND);
            return null;
        }

        addForm.setR(role.getId());

        PageControl pca = WebUtility.getPageControl(request, "a");
        PageControl pcp = WebUtility.getPageControl(request, "p");

        //BZ-580127 Refactor so that all lists are initialized regardless of ldap server 
        // availability or state of filter params
        List<String> pendingGroupIds = new ArrayList<String>();
        Set<Map<String, String>> allGroups = new HashSet<Map<String, String>>();
        PageList<LdapGroup> assignedList = new PageList<LdapGroup>();
        Set<Map<String, String>> availableGroupsSet = new HashSet<Map<String, String>>();
        Set<Map<String, String>> pendingSet = new HashSet<Map<String, String>>();

        PageList<Map<String, String>> pendingGroups = new PageList<Map<String, String>>(pendingSet, 0, pcp);
        PageList<Map<String, String>> availableGroups = new PageList<Map<String, String>>(availableGroupsSet, 0, pca);
        /* pending groups are those on the right side of the "add
         * to list" widget- awaiting association with the rolewhen the form's "ok" button is clicked. */
        pendingGroupIds = SessionUtils.getListAsListStr(request.getSession(), Constants.PENDING_RESGRPS_SES_ATTR);

        log.trace("getting pending groups for role [" + roleId + ")");
        String name = "foo";

        try { //defend against ldap communication runtime difficulties.
            allGroups = LdapGroupManager.getInstance().findAvailableGroups();
            RoleManagerLocal roleManager = LookupUtil.getRoleManager();

            assignedList = roleManager.findLdapGroupsByRole(role.getId(), PageControl.getUnlimitedInstance());

            allGroups = filterExisting(assignedList, allGroups);
            Set<String> pendingIds = new HashSet<String>(pendingGroupIds);

            pendingSet = findPendingGroups(pendingIds, allGroups);
            pendingGroups = new PageList<Map<String, String>>(pendingSet, pendingSet.size(), pcp);

            /* available groups are all groups in the system that are not
             * associated with the role and are not pending
             */
            log.trace("getting available groups for role [" + roleId + "]");

            availableGroupsSet = findAvailableGroups(pendingIds, allGroups);
            availableGroups = new PageList<Map<String, String>>(availableGroupsSet, availableGroupsSet.size(), pca);
        } catch (LdapFilterException lce) {
            ActionMessages actionMessages = new ActionMessages();
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("admin.role.LdapGroupFilterMessage"));
            saveErrors(request, actionMessages);
        } catch (LdapCommunicationException lce) {
            ActionMessages actionMessages = new ActionMessages();
            SystemManagerLocal manager = LookupUtil.getSystemManager();
            Properties options = manager.getSystemConfiguration();
            String providerUrl = options.getProperty(RHQConstants.LDAPUrl, "(unavailable)");
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("admin.role.LdapCommunicationMessage",
                providerUrl));
            saveErrors(request, actionMessages);
        }

        //place calculated values into session.
        request.setAttribute(Constants.PENDING_RESGRPS_ATTR, pendingGroups);
        request.setAttribute(Constants.NUM_PENDING_RESGRPS_ATTR, new Integer(pendingGroups.getTotalSize()));
        request.setAttribute(Constants.AVAIL_RESGRPS_ATTR, availableGroups);
        request.setAttribute(Constants.NUM_AVAIL_RESGRPS_ATTR, new Integer(availableGroups.getTotalSize()));

        return null;
    }

    private Set<Map<String, String>> findPendingGroups(Set<String> pending, Set<Map<String, String>> allGroups) {
        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        for (Map<String, String> group : allGroups) {
            if (pending.contains(group.get("name"))) {
                ret.add(group);
            }
        }
        return ret;
    }

    private Set<Map<String, String>> findAvailableGroups(Set<String> pending, Set<Map<String, String>> allGroups) {
        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        for (Map<String, String> group : allGroups) {
            if (!pending.contains(group.get("name"))) {
                ret.add(group);
            }
        }
        return ret;
    }

    private Set<Map<String, String>> filterExisting(List<LdapGroup> pendingItems, Set<Map<String, String>> allGroups) {
        Set<String> pending = new HashSet<String>();
        for (LdapGroup group : pendingItems) {
            pending.add(group.getName());
        }

        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        for (Map<String, String> group : allGroups) {
            if (!pending.contains(group.get("name"))) {
                ret.add(group);
            }
        }
        return ret;
    }
}