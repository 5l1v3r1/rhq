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
package org.rhq.enterprise.gui.ha;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewServerUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewServerUIBean";

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();
    private CloudManagerLocal cloudManager = LookupUtil.getCloudManager();
    private Server server;

    public ViewServerUIBean() {
        hasPermission();
        int serverId = FacesContextUtility.getRequiredRequestParameter("serverId", Integer.class);
        server = cloudManager.getServerById(serverId);
    }

    @Override
    public DataModel getDataModel() {
        if (null == dataModel) {
            dataModel = new ViewServerDataModel(PageControlView.ServerConnectedAgentsView, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public Server getServer() {
        return server;
    }

    public String edit() {
        return "edit";
    }

    public String save() {
        try {
            cloudManager.updateServer(getSubject(), getServer());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "This server's public address and ports have been updated.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + e.getMessage());
            return "edit"; // stay in edit mode on failure
        }

        return "success";
    }

    public String cancel() {
        return "success";
    }

    private class ViewServerDataModel extends PagedListDataModel<Agent> {
        public ViewServerDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<Agent> fetchPage(PageControl pc) {
            int serverId = getServer().getId();
            PageList<Agent> results = agentManager.getAgentsByServer(getSubject(), serverId, pc);
            return results;
        }
    }

    /**
     * Throws a permission exception if the user is not allowed to access this functionality. 
     */
    private void hasPermission() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_INVENTORY)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have the proper permissions to view or manage servers");
        }
    }
}