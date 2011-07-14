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
package org.rhq.enterprise.gui.legacy.taglib.display;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.gui.util.UrlUtility;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.server.resource.ResourceFacetsCache;

/**
 * A {@link QuicknavDecorator} for {@link ResourceGroup}s.
 *
 * @author Ian Springer
 */
public class GroupQuicknavDecorator extends QuicknavDecorator {
    private static final String MONITOR_URL = "/rhq/group/monitor/graphs.xhtml";
    private static final String EVENTS_URL = "/rhq/group/events/history.xhtml";
    private static final String INVENTORY_URL = "/rhq/group/inventory/view.xhtml";
    private static final String CONFIGURE_URL = "/rhq/group/configuration/viewCurrent.xhtml";
    private static final String OPERATIONS_URL = "/rhq/group/operation/groupOperationScheduleNew.xhtml";
    private static final String ALERT_URL = "/rhq/group/alert/listGroupAlertDefinitions.xhtml";
    private static final String CONTENT_URL = "/rhq/group/content/view.xhtml?mode=view";

    private ResourceGroupComposite resourceGroupComposite;
    private ResourceFacets resourceFacets;

    @Override
    public String decorate(Object columnValue) throws Exception {
        this.resourceGroupComposite = (ResourceGroupComposite) columnValue;

        ResourceType type = resourceGroupComposite.getResourceGroup().getResourceType();
        if (type == null) {
            this.resourceFacets = ResourceFacets.NONE;
        } else {
            this.resourceFacets = ResourceFacetsCache.getSingleton().getResourceFacets(type.getId());
        }
        return getOutput();
    }

    @Override
    protected String getFullURL(String url) {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
        Map<String, String> params = new HashMap<String, String>();
        if (url.equals(MONITOR_URL) || url.equals(EVENTS_URL) || url.equals(INVENTORY_URL)
            || url.equals(OPERATIONS_URL)) {
            params.put(HubConstants.PARAM_GROUP_CATEGORY, this.resourceGroupComposite.getCategory().name());
        }

        params.put(HubConstants.PARAM_GROUP_ID, String.valueOf(this.resourceGroupComposite.getResourceGroup().getId()));
        String fullURL = request.getContextPath() + UrlUtility.addParametersToQueryString(url, params);
        return fullURL;
    }

    @Override
    protected String getTagName() {
        return "group-quicknav-decorator";
    }

    @Override
    protected boolean isMonitorSupported() {
        return (isCompatibleGroup());
    }

    @Override
    protected boolean isEventsSupported() {
        return (isCompatibleGroup() && this.resourceFacets.isEvent());
    }

    @Override
    protected boolean isInventorySupported() {
        return true;
    }

    @Override
    protected boolean isConfigureSupported() {
        return (isCompatibleGroup() && this.resourceFacets.isConfiguration());
    }

    @Override
    protected boolean isOperationsSupported() {
        return (isCompatibleGroup() && this.resourceFacets.isOperation());
    }

    @Override
    protected boolean isAlertSupported() {
        return (isCompatibleGroup());
    }

    @Override
    protected boolean isContentSupported() {
        return false;
        //return (isCompatibleGroup() && this.resourceGroupComposite.getResourceFacets().isContent());
    }

    private boolean isCompatibleGroup() {
        return this.resourceGroupComposite.getResourceGroup().getGroupCategory() == GroupCategory.COMPATIBLE;
    }

    // TODO: For now, all icons are "allowed", but this may change in the future.
    //       (see http://jira.jboss.com/jira/browse/JBNADM-1616)

    @Override
    protected boolean isMonitorAllowed() {
        return true; // LookupUtil.getSystemManager().isMonitoringEnabled();
    }

    @Override
    protected boolean isEventsAllowed() {
        return true;
    }

    @Override
    protected boolean isInventoryAllowed() {
        return true;
    }

    @Override
    protected boolean isConfigureAllowed() {
        return true;
    }

    @Override
    protected boolean isOperationsAllowed() {
        return true;
    }

    @Override
    protected boolean isAlertAllowed() {
        return true; //LookupUtil.getSystemManager().isMonitoringEnabled();
    }

    @Override
    protected boolean isContentAllowed() {
        return false;
    }

    @Override
    protected String getMonitorURL() {
        return MONITOR_URL;
    }

    @Override
    protected String getEventsURL() {
        return EVENTS_URL;
    }

    @Override
    protected String getInventoryURL() {
        return INVENTORY_URL;
    }

    @Override
    protected String getConfigureURL() {
        return CONFIGURE_URL;
    }

    @Override
    protected String getOperationsURL() {
        return OPERATIONS_URL;
    }

    @Override
    protected String getAlertURL() {
        return ALERT_URL;
    }

    @Override
    protected String getContentURL() {
        return CONTENT_URL;
    }
}