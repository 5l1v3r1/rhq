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
package org.rhq.enterprise.gui.legacy.portlet.criticalalerts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class ModifyAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        PropertiesForm pForm = (PropertiesForm) form;
        WebUser user = SessionUtils.getWebUser(request.getSession());
        String forwardStr = RetCodeConstants.SUCCESS_URL;

        if (pForm.isRemoveClicked()) {
            DashboardUtils.removeResources(pForm.getIds(), ".dashContent.criticalalerts.resources", user);
            forwardStr = "review";
        }

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        if (!pForm.isDisplayOnDash()) {
            DashboardUtils.removePortlet(user, pForm.getPortletName());
        }

        Integer numberOfAlerts = pForm.getNumberOfAlerts();
        String past = pForm.getPast();
        String prioritity = pForm.getPriority();
        String selectedOrAll = pForm.getSelectedOrAll();

        user.setPreference(".dashContent.criticalalerts.numberOfAlerts", numberOfAlerts.toString());
        user.setPreference(".dashContent.criticalalerts.past", past);
        user.setPreference(".dashContent.criticalalerts.priority", prioritity);
        user.setPreference(".dashContent.criticalalerts.selectedOrAll", selectedOrAll);
        user.persistPreferences();

        request.getSession().removeAttribute(Constants.USERS_SES_PORTAL); // clean-up

        return mapping.findForward(forwardStr);
    }
}