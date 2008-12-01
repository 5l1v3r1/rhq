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
package org.rhq.enterprise.gui.legacy.action.resource.common;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

public class QuickFavoritesAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();
        int id = WebUtility.getResourceId(request);
        Boolean isFavorite = QuickFavoritesUtil.isFavorite(user, id);
        String mode = request.getParameter("mode");

        HashMap forwardParams = new HashMap(1);
        forwardParams.put(ParamConstants.RESOURCE_ID_PARAM, id);

        if (mode == null) {
            return returnFailure(request, mapping, forwardParams);
        }

        if (mode.equals("add")) {
            if (isFavorite.booleanValue()) {
                // already in the favorites list - should not happen but just return, it's already there
                return returnSuccess(request, mapping, forwardParams, BaseAction.YES_RETURN_PATH);
            }

            // Add to favorites and save
            String favorites;
            favorites = preferences.getPreferences().getSimple(Constants.USERPREF_KEY_FAVORITE_RESOURCES)
                .getStringValue();
            favorites += DashboardUtils.DASHBOARD_DELIMITER + id;
            preferences.setPreference(Constants.USERPREF_KEY_FAVORITE_RESOURCES, favorites);
        } else if (mode.equals("remove")) {
            if (!isFavorite.booleanValue()) {
                // not already a favorite - should not happen but just return, it's already gone
                return returnSuccess(request, mapping, forwardParams, BaseAction.YES_RETURN_PATH);
            }

            // Remove from favorites and save
            DashboardUtils.removeResources(new String[] { String.valueOf(id) },
                Constants.USERPREF_KEY_FAVORITE_RESOURCES, user);
        } else {
            return returnFailure(request, mapping, forwardParams);
        }

        preferences.persistPreferences();

        return returnSuccess(request, mapping, forwardParams, BaseAction.YES_RETURN_PATH);
    }
}