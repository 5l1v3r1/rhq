/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.inventory.group;

import javax.faces.application.FacesMessage;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * A POJO Seam component that handles loading and updating of plugin configurations across a compatible Group.
 *
 * @author Ian Springer
 */
//@Name("EditGroupPluginConfigurationUIBean")
//@Scope(ScopeType.CONVERSATION)
public class EditGroupPluginConfigurationUIBean extends AbstractGroupPluginConfigurationUIBean {
    public static final String VIEW_ID = "/rhq/group/inventory/edit-connection.xhtml";

    //    @In(value = "org.jboss.seam.faces.redirect")
    //    private Redirect redirect;

    //    @Create
    //    @Begin
    public void init() {
        loadConfigurations();
        // We can set this once here, since this.redirect is scoped to the same CONVERSATION as this managed bean instance.
        //        this.redirect.setParameter(ParamConstants.GROUP_ID_PARAM, getGroup().getId());
        return;
    }

    /**
     * Asynchronously persist the group member Configurations to the DB as well as push them out to the corresponding
     * Agents. This gets called when user clicks the SAVE button.
     */
    public void updateConfigurations() {
        try {
            // TODO: See if there's some way for the config renderer to handle calling applyGroupConfiguration(),
            //       so the managed bean doesn't have to worry about doing it.
            getConfigurationSet().applyGroupConfiguration();
            getConfigurationManager().scheduleGroupPluginConfigurationUpdate(
                EnterpriseFacesContextUtility.getSubject(), getGroup().getId(), getPluginConfigurations());
        } catch (Exception e) {
            if (e instanceof PermissionException) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, e.getLocalizedMessage());
                // this.redirect.setViewId(ViewGroupPluginConfigurationUIBean.VIEW_ID);
            } else {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to schedule group plugin Configuration update - cause: " + e);
                // this.redirect.setViewId(VIEW_ID);
            }
            // this.redirect.execute();
            return;
        }
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Group plugin Configuration update scheduled.");
        //        Conversation.instance().endBeforeRedirect();
        //        this.redirect.setViewId(ViewGroupConnectionPropertyHistoryUIBean.VIEW_ID);
        //        this.redirect.execute();
        return;
    }

    /**
     * End the convo and redirect back to view-connection.xhtml. This gets called when user clicks the CANCEL button.
     */
    // @End
    public void cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Edit canceled.");
        //        this.redirect.setViewId(ViewGroupPluginConfigurationUIBean.VIEW_ID);
        //        this.redirect.execute();
        return;
    }

    /**
     * End the convo and reload the current page (edit-connection.xhtml). This gets called when user clicks the RESET button.
     */
    // @End
    public void reset() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "All properties reset to original values.");
        //        this.redirect.setViewId(VIEW_ID);
        //        this.redirect.execute();
        return;
    }
}