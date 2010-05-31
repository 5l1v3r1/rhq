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
package org.rhq.enterprise.gui.legacy.portlet.resourcehealth;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.resource.composite.ResourceNamesDisambiguationResult;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.tag.FunctionTagLibrary;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.FavoriteResourcePortletPreferences;
import org.rhq.enterprise.gui.legacy.portlet.BaseRSSAction;
import org.rhq.enterprise.gui.legacy.portlet.RSSFeed;
import org.rhq.enterprise.gui.legacy.taglib.display.DisambiguatedResourceLineageTag;
import org.rhq.enterprise.gui.legacy.util.DisambiguatedResourceListUtil;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RSSAction extends BaseRSSAction {
    private static final Log log = LogFactory.getLog(RSSAction.class.getName());

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        RSSFeed feed = getNewRSSFeed(request);
        ResourceManagerLocal manager = LookupUtil.getResourceManager();

        // Set title
        MessageResources res = getResources(request);
        feed.setTitle(res.getMessage("dash.home.ResourceHealth"));

        // Get the resources health
        WebUser user = getWebUser(request);
        if (user != null) {
            WebUserPreferences preferences = user.getWebPreferences();
            FavoriteResourcePortletPreferences favoriteResourcePreferences = preferences
                .getFavoriteResourcePortletPreferences();

            PageList<ResourceHealthComposite> results = manager.findResourceHealth(user.getSubject(),
                favoriteResourcePreferences.asArray(), PageControl.getUnlimitedInstance());

            if ((results != null) && (results.size() > 0)) {
                PageList<DisambiguationReport<ResourceHealthComposite>> list = DisambiguatedResourceListUtil.disambiguate(manager, results, ViewAction.RESOURCE_ID_EXTRACTOR);
                
                for (DisambiguationReport<ResourceHealthComposite> summary : list) {
                    String link = feed.getBaseUrl() + FunctionTagLibrary.getDefaultResourceTabURL() + "?id="
                        + summary.getOriginal().getId();

                    String availText = res.getMessage("dash.home.ResourceHealth.rss.item.availability", summary
                        .getOriginal().getAvailabilityType().toString());
                    String alertsText = res.getMessage("dash.home.ResourceHealth.rss.item.alerts", Long.valueOf(summary
                        .getOriginal().getAlerts()));
                    String typeText = res.getMessage("dash.home.ResourceHealth.rss.item.resourceType", summary
                        .getOriginal().getTypeName());
                    String parentsText = res.getMessage("dash.home.ResourceHealth.rss.item.resourceParents", getLineage(summary));
                    
                    long now = System.currentTimeMillis();

                    StringBuffer desc = new StringBuffer();
                    desc.append("<table><tr><td align=\"left\">").append(typeText).append("</td></tr>");

                    desc.append("<tr><td align=\"left\">").append(parentsText).append("</td></tr>");
                    
                    if (favoriteResourcePreferences.showAvailability) {
                        desc.append("<tr><td align=\"left\">").append(availText).append("</td></tr>");
                    }

                    if (favoriteResourcePreferences.showAlerts) {
                        desc.append("<tr><td align=\"left\">").append(alertsText).append("</td></tr>");
                    }

                    desc.append("</table>");

                    feed.addItem(summary.getOriginal().getName(), link, desc.toString(), now);
                }
            }

            request.setAttribute("rssFeed", feed);

            return mapping.findForward(Constants.RSS_URL);
        } else {
            throw new LoginException("RSS access requires authentication");
        }
    }
    
    private static String getLineage(DisambiguationReport<?> report) {
        StringWriter writer = new StringWriter();
        try {
            DisambiguatedResourceLineageTag.writeParents(writer, report.getParents(), false, false);
            return writer.toString();
        } catch (IOException e) {
            return "";
        }
    }
}