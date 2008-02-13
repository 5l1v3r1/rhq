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
package org.rhq.enterprise.gui.legacy.portlet.autodiscovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Form action for autodiscovery dashboard portlet.
 */
public class ViewAction extends TilesAction {
    public static final Log log = LogFactory.getLog(ViewAction.class.getName());

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        WebUser webUser = SessionUtils.getWebUser(request.getSession());
        Subject user = webUser.getSubject();
        AIQueueForm queueForm = (AIQueueForm) form;
        PageControl pageControl;

        try {
            int size = Integer.parseInt(webUser.getPreference(".dashContent.autoDiscovery.range"));
            if (size < 1) {
                pageControl = PageControl.getUnlimitedInstance();
            } else {
                pageControl = new PageControl(0, size);
            }
        } catch (Exception e) {
            // should never happen but if somehow there is a bogus number, just fallback to the default
            pageControl = new PageControl(0, 2);
        }

        Map<Resource, List<Resource>> queuedResources;

        try {
            queuedResources = discoveryBoss.getQueuedPlatformsAndServers(user, pageControl);

            // If the queue is empty, check to see if there are ANY agents defined in inventory.
            if (queuedResources.isEmpty()) {
                int count = LookupUtil.getAgentManager().getAgentCount();
                request.setAttribute("hasNoAgents", count == 0);
            }
        } catch (PermissionException pe) {
            // user doesn't have permissions to see or import anything, just show an empty list
            queuedResources = new HashMap<Resource, List<Resource>>();
        }

        context.putAttribute("resources", queuedResources);

        // as a convienence, check every box for the user
        List<Integer> platformsToProcess = new ArrayList<Integer>(queuedResources.size());
        List<Integer> serversToProcess = new ArrayList<Integer>();
        for (Resource platform : queuedResources.keySet()) {
            platformsToProcess.add(platform.getId());

            // Add all top-level servers on this platform
            List<Resource> servers1 = queuedResources.get(platform);
            for (Resource server : servers1) {
                serversToProcess.add(server.getId());
            }
        }

        queueForm.setPlatformsToProcess(platformsToProcess.toArray(new Integer[0]));
        queueForm.setServersToProcess(serversToProcess.toArray(new Integer[0]));

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());

        // Check for previous error
        // First, check for ignore error - that is, cannot ignore resources already in inventory
        Object ignoreErr = request.getSession().getAttribute(Constants.IMPORT_IGNORE_ERROR_ATTR);
        if (ignoreErr != null) {
            ActionMessage err = new ActionMessage("dash.autoDiscovery.import.ignore.Error");
            RequestUtils.setError(request, err, ActionMessages.GLOBAL_MESSAGE);

            // Only show the error once
            request.getSession().setAttribute(Constants.IMPORT_IGNORE_ERROR_ATTR, null);
        }

        // Check for exception caused by license restriction not allowing additional resources to be imported
        Exception exc = (Exception) request.getSession().getAttribute(Constants.IMPORT_ERROR_ATTR);
        if (exc != null) {
            request.getSession().removeAttribute(Constants.IMPORT_ERROR_ATTR);
            log.error("Failed to approve AI report", exc);

            //         if ( exc instanceof AIQApprovalException )
            //         {
            //            ActionMessage err = new ActionMessage( "dash.autoDiscovery.import.limit.Error" );
            //            RequestUtils.setError( request, err, ActionMessages.GLOBAL_MESSAGE );
            //         }
            //         else
            //         {
            ActionMessage err = new ActionMessage("dash.autoDiscovery.import.general.Error", ThrowableUtil
                .getAllMessages(exc, true));
            RequestUtils.setError(request, err, ActionMessages.GLOBAL_MESSAGE);
            //         }
        }

        return null;
    }
}