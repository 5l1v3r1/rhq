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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.LogFactory;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.FavoriteResourcePortletPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

public class QuickFavoritesUtil {

    /**
     * This not only returns the flag to indicate if the resource found in the request is a favorite, but it also adds
     * an attribute "isFavorite" to the request so others can know the status later on down the request chain.
     *
     * @param  request
     *
     * @return <code>true</code> if the resource in the request is a user's favorite
     */
    public static Boolean determineIfFavoriteResource(HttpServletRequest request) {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        int id = WebUtility.getResourceId(request);
        Boolean isFavorite = isFavorite(user, id);
        request.setAttribute("isFavorite", isFavorite);
        return isFavorite;
    }

    public static Boolean determineIfFavoriteResource(int resourceId) {
        HttpServletRequest request = FacesContextUtility.getRequest();
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Boolean isFavorite = isFavorite(user, resourceId);
        request.setAttribute("isFavorite", isFavorite);
        return isFavorite;
    }

    public static Boolean determineIfFavoriteGroup(HttpServletRequest request, int groupId) {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Boolean isFavorite = isFavoriteGroup(user, groupId);
        request.setAttribute("isFavorite", isFavorite);
        return isFavorite;
    }

    public static Boolean isFavorite(WebUser user, int id) {
        try {
            FavoriteResourcePortletPreferences favoriteResources = user.getWebPreferences()
                .getFavoriteResourcePortletPreferences();
            return favoriteResources.isFavorite(id);
        } catch (Exception e) {
            LogFactory.getLog(QuickFavoritesUtil.class).error("Cannot determine if [" + id + "] is a favorite", e);
            return false;
        }
    }

    public static Boolean isFavoriteGroup(WebUser user, int groupId) {
        try {
            WebUserPreferences.FavoriteGroupPortletPreferences favoriteGroups = user.getWebPreferences()
                .getFavoriteGroupPortletPreferences();
            return favoriteGroups.isFavorite(groupId);
        } catch (Exception e) {
            LogFactory.getLog(QuickFavoritesUtil.class).error("Cannot determine if group [" + groupId + "] is a favorite", e);
            return false;
        }
    }


}