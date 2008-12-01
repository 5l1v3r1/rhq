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
package org.rhq.enterprise.gui.legacy.portlet.recentlyApproved;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class ModifyAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(ModifyAction.class.getName());

        PropertiesForm pForm = (PropertiesForm) form;
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        WebUserPreferences preferences = user.getPreferences();
        String range = pForm.getRange().toString();

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        if (!pForm.isDisplayOnDash()) {
            DashboardUtils.removePortlet(user, pForm.getPortletName());
        }

        preferences.setPreference(".dashContent.recentlyApproved.range", range);

        LogFactory.getLog("user.preferences").trace(
            "Invoking setUserPrefs" + " in recentlyApproved/ModifyAction " + " for " + user.getId() + " at "
                + System.currentTimeMillis() + " user.prefs = " + user.getPreferences());

        preferences.persistPreferences();
        session.removeAttribute(Constants.USERS_SES_PORTAL);

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }
}