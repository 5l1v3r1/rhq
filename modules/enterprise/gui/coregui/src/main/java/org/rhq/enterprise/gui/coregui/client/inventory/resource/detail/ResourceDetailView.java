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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.UnknownViewException;
import org.rhq.enterprise.gui.coregui.client.View;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewRenderer;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.SimpleCollapsiblePanel;
import org.rhq.enterprise.gui.coregui.client.components.SubTabLayout;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

import com.google.gwt.user.client.History;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import java.util.EnumSet;

/**
 * Right panel of the resource view.
 *
 * @author Greg Hinkle
 */
public class ResourceDetailView extends VLayout implements ViewRenderer, ResourceSelectListener, TwoLevelTabSelectedHandler {

    private Resource resource;

    private SimpleCollapsiblePanel summaryPanel;
    private ResourceSummaryView summaryView;

    private TwoLevelTab summaryTab;
    private TwoLevelTab monitoringTab;
    private TwoLevelTab inventoryTab;
    private TwoLevelTab operationsTab;
    private TwoLevelTab alertsTab;
    private TwoLevelTab configurationTab;
    private TwoLevelTab eventsTab;
    private TwoLevelTab contentTab;

    private TwoLevelTabSet topTabSet;

    private ResourceTitleBar titleBar;

    private ViewId currentView;
    private ViewId baseViewId;

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setHeight100();

        // The header section
        summaryView = new ResourceSummaryView();
        summaryPanel = new SimpleCollapsiblePanel("Summary", summaryView);

        // The Tabs section

        topTabSet = new TwoLevelTabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth100();
        topTabSet.setHeight100();
        topTabSet.setEdgeMarginSize(0);
        topTabSet.setEdgeSize(0);

        summaryTab = new TwoLevelTab("Summary", "/images/icons/Service_up_16.png");
        summaryTab.registerSubTabs("Overview", "Timeline");

        monitoringTab = new TwoLevelTab("Monitoring", "/images/icons/Monitor_grey_16.png");
        monitoringTab.registerSubTabs("Graphs", "Tables", "Traits", "Availability", "Schedules");

        inventoryTab = new TwoLevelTab("Inventory", "/images/icons/Inventory_grey_16.png");
        inventoryTab.registerSubTabs("Children", "Connection Settings");

        operationsTab = new TwoLevelTab("Operations", "/images/icons/Operation_grey_16.png");
        operationsTab.registerSubTabs("History", "Scheduled");

        alertsTab = new TwoLevelTab("Alerts", "/images/icons/Alert_grey_16.png");
        alertsTab.registerSubTabs("Alert History", "Alert Definitions");

        configurationTab = new TwoLevelTab("Configuration", "/images/icons/Configure_grey_16.png");
        configurationTab.registerSubTabs("Current", "History");

        eventsTab = new TwoLevelTab("Events", "/images/icons/Events_grey_16.png");

        contentTab = new TwoLevelTab("Content", "/images/icons/Content_grey_16.png");

        topTabSet.setTabs(summaryTab, monitoringTab, inventoryTab, operationsTab, alertsTab, configurationTab, eventsTab, contentTab);


        topTabSet.addTwoLevelTabSelectedHandler(this);

        titleBar = new ResourceTitleBar();
        addMember(titleBar);

        addMember(summaryPanel);

        addMember(topTabSet);

//        CoreGUI.addBreadCrumb(getPlace());
    }

    public void onResourceSelected(Resource resource) {

        this.resource = resource;

        this.summaryView.onResourceSelected(resource);

        titleBar.setResource(resource);

        int selectedTab = topTabSet.getSelectedTabNumber();


        FullHTMLPane summaryPane = new FullHTMLPane("/rhq/resource/summary/overview-plain.xhtml?id=" + resource.getId());
        FullHTMLPane timelinePane = new FullHTMLPane("/rhq/resource/summary/timeline-plain.xhtml?id=" + resource.getId());
        summaryTab.updateSubTab("Overview", summaryPane);
        summaryTab.updateSubTab("Timeline", timelinePane);


        monitoringTab.updateSubTab("Graphs", new GraphListView(resource)); // new FullHTMLPane("/rhq/common/monitor/graphs.xhtml?id=" + resource.getId()));
        monitoringTab.updateSubTab("Tables", new FullHTMLPane("/rhq/common/monitor/tables.xhtml?id=" + resource.getId()));
        monitoringTab.updateSubTab("Traits", new FullHTMLPane("/rhq/resource/monitor/traits.xhtml?id=" + resource.getId()));
        monitoringTab.updateSubTab("Availability", new FullHTMLPane("/rhq/resource/monitor/availabilityHistory.xhtml?id=" + resource.getId()));
        monitoringTab.updateSubTab("Schedules", new FullHTMLPane("/rhq/resource/monitor/schedules.xhtml?id=" + resource.getId()));


        inventoryTab.updateSubTab("Children", ResourceSearchView.getChildrenOf(resource.getId()));
        inventoryTab.updateSubTab("Connection Settings", new ConfigurationEditor(resource.getId(), resource.getResourceType().getId(), ConfigurationEditor.ConfigType.plugin));


        configurationTab.updateSubTab("Current", new ConfigurationEditor(resource.getId(), resource.getResourceType().getId()));
        configurationTab.updateSubTab("History", ConfigurationHistoryView.getHistoryOf(resource.getId()));

        topTabSet.setSelectedTab(selectedTab);

        updateTabStatus();

        topTabSet.markForRedraw();
    }


    private void updateTabStatus() {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.events, ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {


                        if (type.getOperationDefinitions() == null || type.getOperationDefinitions().isEmpty()) {
                            topTabSet.disableTab(operationsTab);
                        } else {
                            topTabSet.enableTab(operationsTab);
                        }

                        if (type.getEventDefinitions() == null || type.getEventDefinitions().isEmpty()) {
                            topTabSet.disableTab(eventsTab);
                        } else {
                            topTabSet.enableTab(eventsTab);
                        }

                        if (type.getPackageTypes() == null || type.getPackageTypes().isEmpty()) {
                            topTabSet.disableTab(contentTab);
                        } else {
                            topTabSet.enableTab(contentTab);
                        }

                        if (type.getResourceConfigurationDefinition() == null) {
                            topTabSet.disableTab(configurationTab);
                        } else {
                            topTabSet.enableTab(configurationTab);
                        }

                        if (topTabSet.getSelectedTab().getDisabled()) {
                            topTabSet.setSelectedTab(0);
                        }

                    }
                });

    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        // TODO: Implement this method.

        String tabPath = "/" + tabSelectedEvent.getId() + "/" + tabSelectedEvent.getSubTabId();


//        System.out.println("TAB: " + currentView.getPath() + tabPath);

        if (resource != null) {
            History.newItem("Resource/" + resource.getId() + tabPath, false);
        }
    }

    public View renderView(ViewId viewId, boolean lastNode) throws UnknownViewException {

        if (baseViewId == null) {
            baseViewId = viewId;
        }
        currentView = viewId;

        String[] parts = currentView.getPath().split("/");
        Breadcrumb crumb = null;
        if (parts.length >= 3) {
            for (Tab t : topTabSet.getTabs()) {
                TwoLevelTab tab = (TwoLevelTab) t;

                if (tab.getTitle().equals(parts[2])) {

                    if (parts.length == 3) {
                        crumb = new Breadcrumb(parts[2], parts[2], false);
                    }


                    if (parts.length >= 4) {
                        tab.getLayout().selectTab(parts[3]);

                        // Switch the top tab after the subtab to avoid an extra subtab render
                        topTabSet.selectTab(tab);


                        if (parts.length == 4) {
                            crumb = new Breadcrumb(parts[3], parts[3], false);
                        }
                    }
                }
            }
        }

        if (crumb != null) {
            return new View(viewId, crumb);
        } else {
            return new View(viewId);
        }
    }
}
