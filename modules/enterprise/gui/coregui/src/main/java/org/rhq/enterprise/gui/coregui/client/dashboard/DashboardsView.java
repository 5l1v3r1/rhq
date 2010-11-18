/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.CloseClickHandler;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.InventorySummaryView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MessagePortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class DashboardsView extends LocatableVLayout implements BookmarkableView {
    public static final ViewName VIEW_ID = new ViewName("Dashboard", MSG.view_dashboards_title());

    private TabSet tabSet;

    private IButton editButton;

    private boolean editMode = false;

    private List<Dashboard> dashboards;
    private Map<String, Dashboard> dashboardsByName;

    private DashboardView selectedDashboardView;
    private Dashboard selectedDashboard;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    private String selectedTab;

    public DashboardsView(String locatorId) {
        super(locatorId);
        setOverflow(Overflow.AUTO);
        setPadding(5);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        dashboardService.findDashboardsForSubject(new AsyncCallback<List<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
            }

            public void onSuccess(List<Dashboard> result) {
                if (result.isEmpty()) {
                    result.add(getDefaultDashboard());
                }
                updateDashboards(result);
            }
        });
    }

    private void updateDashboards(List<Dashboard> dashboards) {
        removeMembers(getMembers());
        this.dashboards = dashboards;
        this.dashboardsByName = new HashMap(dashboards.size());
        for (Dashboard dashboard : dashboards) {
            this.dashboardsByName.put(dashboard.getName(), dashboard);
        }

        tabSet = new LocatableTabSet(getLocatorId());

        tabSet.setWidth100();
        tabSet.setHeight100();

        tabSet.setCanCloseTabs(true);

        editButton = new LocatableIButton(extendLocatorId("Mode"), editMode ? MSG.common_title_view_mode() : MSG
            .common_title_edit_mode());
        editButton.setAutoFit(true);
        editButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                editMode = !editMode;
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                selectedDashboardView.setEditMode(editMode);
            }
        });

        final IButton newDashboardButton = new LocatableIButton(extendLocatorId("New"), MSG
            .common_title_new_dashboard());
        newDashboardButton.setAutoFit(true);
        newDashboardButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addNewDashboard();
            }
        });

        HLayout buttons = new HLayout(5);
        buttons.addMember(editButton);
        buttons.addMember(newDashboardButton);

        tabSet.setTabBarControls(buttons);

        tabSet.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
                History.newItem("Dashboard/" + tabSelectedEvent.getTab().getTitle(), false);
                selectedDashboardView = (DashboardView) tabSelectedEvent.getTab().getPane();
                selectedDashboard = selectedDashboardView.getDashboard();
                selectedDashboardView.setEditMode(editMode);
            }
        });

        for (Dashboard dashboard : dashboards) {
            DashboardView dashboardView = new DashboardView(extendLocatorId(dashboard.getName()), this, dashboard);

            Tab tab = new LocatableTab(dashboard.getName(), dashboard.getName());
            tab.setPane(dashboardView);
            tab.setCanClose(true);

            tabSet.addTab(tab);
            if (dashboard.getName().equals(selectedTab)) {
                tabSet.selectTab(tab);
            }
        }

        tabSet.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(final TabCloseClickEvent tabCloseClickEvent) {
                final DashboardView dashboardView = (DashboardView) tabCloseClickEvent.getTab().getPane();
                SC.ask(MSG.view_dashboards_confirm1() + " [" + tabCloseClickEvent.getTab().getTitle() + "]?",
                    new BooleanCallback() {
                        public void execute(Boolean aBoolean) {
                            if (aBoolean) {
                                dashboardView.delete();
                            } else {
                                tabCloseClickEvent.cancel();
                            }
                        }
                    });
            }
        });

        addMember(tabSet);

    }

    protected Dashboard getDefaultDashboard() {

        Dashboard dashboard = new Dashboard();
        dashboard.setName(MSG.common_title_default());
        dashboard.setColumns(2);
        dashboard.setColumnWidths("32%", "68%");
        dashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, "#F1F2F3"));

        DashboardPortlet summary = new DashboardPortlet(MSG.view_dashboardsManager_inventory_title(),
            InventorySummaryView.KEY, 230);
        dashboard.addPortlet(summary, 0, 0);

        DashboardPortlet tagCloud = new DashboardPortlet(MSG.view_dashboardsManager_tagcloud_title(),
            TagCloudPortlet.KEY, 200);
        dashboard.addPortlet(tagCloud, 0, 1);

        // Experimental
        //        StoredPortlet platformSummary = new StoredPortlet("Platform Summary", PlatformPortletView.KEY, 300);
        //        col2.add(platformSummary);

        DashboardPortlet welcome = new DashboardPortlet(MSG.view_dashboardsManager_message_title(), MessagePortlet.KEY,
            180);
        welcome.getConfiguration().put(
            new PropertySimple("message", MSG.view_dashboardsManager_message_title_details()));
        dashboard.addPortlet(welcome, 1, 0);

        DashboardPortlet news = new DashboardPortlet(MSG.view_dashboardsManager_mashup_title(), MashupPortlet.KEY, 320);
        news.getConfiguration().put(
            new PropertySimple("address", "http://rhq-project.org/display/RHQ/RHQ+News?decorator=popup"));
        dashboard.addPortlet(news, 1, 1);
        //
        DashboardPortlet discoveryQueue = new DashboardPortlet(MSG.view_portlet_autodiscovery_title(),
            AutodiscoveryPortlet.KEY, 250);
        dashboard.addPortlet(discoveryQueue, 1, 2);

        DashboardPortlet recentAlerts = new DashboardPortlet(RecentAlertsPortlet.KEY, RecentAlertsPortlet.KEY, 250);
        dashboard.addPortlet(recentAlerts, 1, 3);

        DashboardPortlet recentlyAdded = new DashboardPortlet(MSG.common_title_recently_added(), RecentlyAddedView.KEY,
            250);
        dashboard.addPortlet(recentlyAdded, 1, 4);

        DashboardPortlet operations = new DashboardPortlet(MSG.common_title_operations(), OperationsPortlet.KEY, 500);
        dashboard.addPortlet(operations, 1, 5);

        DashboardPortlet problemResources = new DashboardPortlet(MSG.view_portlet_problem_resources_title(),
            ProblemResourcesPortlet.KEY, 250);
        //initialize config for the problemResources portlet.
        problemResources.getConfiguration()
            .put(
                new PropertySimple(ProblemResourcesPortlet.PROBLEM_RESOURCE_SHOW_MAX,
                    ProblemResourcesPortlet.defaultValue));
        problemResources.getConfiguration()
            .put(
                new PropertySimple(ProblemResourcesPortlet.PROBLEM_RESOURCE_SHOW_HRS,
                    ProblemResourcesPortlet.defaultValue));
        dashboard.addPortlet(problemResources, 1, 6);

        return dashboard;

    }

    public void addNewDashboard() {
        int i = 0;
        String availableDashboardName = null;
        while (availableDashboardName == null) {
            String candidateDashboardName = MSG.common_title_custom() + i++;
            if (!this.dashboardsByName.containsKey(candidateDashboardName)) {
                availableDashboardName = candidateDashboardName;
            }
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setName(availableDashboardName);

        dashboard.setColumns(2);
        dashboard.setColumnWidths("30%", "70%");

        dashboardService.storeDashboard(dashboard, new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
            }

            public void onSuccess(Dashboard result) {
                DashboardView dashboardView = new DashboardView(extendLocatorId(result.getName()), DashboardsView.this,
                    result);

                Tab tab = new LocatableTab(extendLocatorId(result.getName()), result.getName());
                tab.setPane(dashboardView);
                tab.setCanClose(true);

                tabSet.addTab(tab);

                tabSet.selectTab(tab);
                editMode = true;
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                dashboardView.setEditMode(editMode);

            }
        });
    }

    public void updateNames() {
        for (Tab t : tabSet.getTabs()) {
            DashboardView view = (DashboardView) t.getPane();
            t.setTitle(view.getDashboard().getName());
        }
    }

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            selectedTab = viewPath.getCurrent().getPath();

            //added to avoid NPE in gwt debug window. 
            if (tabSet != null) {
                for (Tab tab : tabSet.getTabs()) {
                    if (tab.getTitle().equals(selectedTab)) {
                        tabSet.selectTab(tab);
                    }
                }
            } else {
                Log.info("WARN: While rendering DashboardsView, tabSet is null.");
            }
        }
    }

    public Dashboard getDashboard() {
        return selectedDashboard;
    }
}
