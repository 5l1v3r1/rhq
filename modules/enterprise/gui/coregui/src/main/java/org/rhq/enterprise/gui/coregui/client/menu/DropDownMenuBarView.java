/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.menu;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuBar;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class DropDownMenuBarView extends LocatableVLayout {

    private AboutModalWindow aboutModalWindow;

    public DropDownMenuBarView(String locatorId) {
        super(locatorId, 5);

        setHeight(50);
        setWidth100();

    }

    protected void onDraw3() {
        super.onDraw();

        this.aboutModalWindow = new AboutModalWindow();

        HLayout topBar = new HLayout();
        topBar.setHeight(28);
        topBar.setStyleName("topMenuBar");
        //        url("./images/cssButton/button_stretch.png") repeat-x scroll 0 0 #DDDDDD
        //        topBar.setBackgroundImage("[SKIN]/cssButton/button_stretch.png");
        //        topBar.setBackgroundRepeat(BkgndRepeat.REPEAT_X);

        Img logo = new Img("header/rhq_logo_28px.png", 80, 28);
        logo.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                DropDownMenuBarView.this.aboutModalWindow.show();
            }
        });

        topBar.addMember(logo);

        // Create a menu bar
        final MenuBar menu = new MenuBar();
        menu.setHeight(28);

        Menu subsystemsMenu = new Menu();

        subsystemsMenu.setTitle("Subsystems");
        subsystemsMenu.addItem(new EMenuItem("Configuration Changes", LinkManager.getSubsystemConfigurationLink(),
            "subsystems/configure/Configure_16.png"));//, new LinkCommand("#config"));
        subsystemsMenu.addItem(new EMenuItem("Suspect Metrics", LinkManager.getSubsystemSuspectMetricsLink(),
            "subsystems/monitor/Monitor_failed_16.png"));//, new LinkCommand("#config"));
        subsystemsMenu.addItem(new EMenuItem("Operations", LinkManager.getSubsystemOperationHistoryLink(),
            "subsystems/control/Operation_16.png"));//, new LinkCommand("#config"));
        subsystemsMenu.addItem(new EMenuItem("Alerts", LinkManager.getSubsystemAlertHistoryLink(),
            "subsystems/alert/Alert_HIGH_16.png"));//, new LinkCommand("#config"));
        subsystemsMenu.addItem(new EMenuItem("Alert Definitions", LinkManager.getSubsystemAlertDefsLink(),
            "subsystems/alert/Alerts_16.png"));//, new LinkCommand("#config"));

        final Menu overviewMenu = new Menu();
        overviewMenu.setTitle("Overview");
        overviewMenu.setStyleName("SimpleMenuBarButton");
        overviewMenu.setBorder("none");
        com.smartgwt.client.widgets.menu.MenuItem subsystemsSubMenuItem = new com.smartgwt.client.widgets.menu.MenuItem(
            "Subsystems");
        subsystemsSubMenuItem.setSubmenu(subsystemsMenu);
        overviewMenu.addItem(subsystemsSubMenuItem);
        overviewMenu.addItem(new EMenuItem("AutoDiscovery Queue", LinkManager.getAutodiscoveryQueueLink()));//,new LinkCommand("#cofng"));
        overviewMenu.addItem(new EMenuItem("Dashboard", LinkManager.getDashboardLink()));//,new LinkCommand("#cofng"));

        Menu resourcesMenu = new Menu();
        resourcesMenu.setTitle("Resources");
        resourcesMenu.setStyleName("SimpleMenuBarButton");
        resourcesMenu.setBorder("none");
        resourcesMenu.setStyleName("menuBarMenuButton");
        resourcesMenu.addItem(new EMenuItem("All Resources", LinkManager.getHubAllResourcesLink()));//, new LinkCommand("#fsdf"));
        resourcesMenu
            .addItem(new EMenuItem("Platforms", LinkManager.getHubPlatformsLink(), "types/Platform_up_16.png"));//, new LinkCommand("#fsdf"));
        resourcesMenu.addItem(new EMenuItem("Servers", LinkManager.getHubServerssLink(), "types/Server_up_16.png"));//, new LinkCommand("#fsdf"));
        resourcesMenu.addItem(new EMenuItem("Services", LinkManager.getHubServicesLink(), "types/Service_up_16.png"));//, new LinkCommand("#fsdf"));
        resourcesMenu.addItem(new MenuItemSeparator());

        final FavoritesMenu favoritesMenu = new FavoritesMenu();
        resourcesMenu.addItem(favoritesMenu);
        menu.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                com.allen_sauer.gwt.log.client.Log.info("over here");
                favoritesMenu.refresh();
            }
        });

        Menu groupsMenu = new Menu();
        groupsMenu.setStyleName("menuBarMenuButton");
        groupsMenu.setTitle("Groups");
        groupsMenu.addItem(new EMenuItem("All Groups", LinkManager.getHubAllGroupsLink())); //, new LinkCommand("#sf"));
        groupsMenu.addItem(new EMenuItem("Compatible Groups", LinkManager.getHubCompatibleGroupsLink())); //, new LinkCommand("#sf"));
        groupsMenu.addItem(new EMenuItem("Mixed Groups", LinkManager.getHubMixedGroupsLink())); //, new LinkCommand("#sf"));
        groupsMenu.addItem(new EMenuItem("Group Definitions", LinkManager.getHubGroupDefinitionsLink())); //, new LinkCommand("#sf"));
        groupsMenu.addItem(new MenuItemSeparator());
        groupsMenu.addItem(new EMenuItem("New Group", LinkManager.getHubNewGroupLink())); //, new LinkCommand("#sf"));
        groupsMenu.addItem(new EMenuItem("New Group Definition", LinkManager.getHubNewGroupDefLink())); //, new LinkCommand("#sf"));
        groupsMenu.addItem(new MenuItemSeparator());
        //        groupsMenu.addItem(new MenuItem("Favorites")); //, new LinkCommand("#sf"));

        Menu securityMenu = new Menu();
        securityMenu.setTitle("Security");
        securityMenu.addItem(new EMenuItem("Users", LinkManager.getAdminUsersLink()));
        securityMenu.addItem(new EMenuItem("Roles", LinkManager.getAdminRolesLink()));
        MenuItem securitySubMenuItem = new MenuItem("Security");
        securitySubMenuItem.setSubmenu(securityMenu);

        Menu sysConfigMenu = new Menu();
        sysConfigMenu.setTitle("System Configuration");
        sysConfigMenu.addItem(new EMenuItem("Settings", LinkManager.getAdminSysConfigLink()));
        sysConfigMenu.addItem(new EMenuItem("Plugins", LinkManager.getAdminPluginsLink()));
        sysConfigMenu.addItem(new EMenuItem("Templates", LinkManager.getAdminTemplatesLink()));
        MenuItem sysConfigSubMenuItem = new MenuItem("System Configuration");
        sysConfigSubMenuItem.setSubmenu(sysConfigMenu);

        Menu contentMenu = new Menu();
        contentMenu.setTitle("Content");
        contentMenu.addItem(new EMenuItem("Bundles", "#Bundles"));
        contentMenu.addItem(new EMenuItem("Content Providers", LinkManager.getAdminContentProvidersLink()));
        contentMenu.addItem(new EMenuItem("Content Repositories", LinkManager.getAdminContentReposLink()));
        MenuItem contentSubMenuItem = new MenuItem("Content");
        contentSubMenuItem.setSubmenu(contentMenu);

        Menu haMenu = new Menu();
        haMenu.setTitle("High Availability");
        haMenu.addItem(new EMenuItem("Servers", LinkManager.getHAServersLink()));
        haMenu.addItem(new EMenuItem("Agents", LinkManager.getHAAgentsLink()));
        haMenu.addItem(new EMenuItem("Affinity Groups", LinkManager.getHAAffinityGroupsLink()));
        haMenu.addItem(new EMenuItem("Partition Events", LinkManager.getHAEventsLink()));
        MenuItem haSubMentItem = new MenuItem("High Availability");
        haSubMentItem.setSubmenu(haMenu);

        Menu reportsMenu = new Menu();
        reportsMenu.setTitle("Reports");
        reportsMenu.addItem(new EMenuItem("Resource Version Inventory Report", LinkManager.getReportsInventoryLink()));
        MenuItem reportsSubMenuItem = new MenuItem("Reports");
        reportsSubMenuItem.setSubmenu(reportsMenu);

        Menu adminMenu = new Menu();
        adminMenu.setTitle("Administration");
        adminMenu.addItem(securitySubMenuItem);
        adminMenu.addItem(sysConfigSubMenuItem);
        adminMenu.addItem(contentSubMenuItem);
        adminMenu.addItem(haSubMentItem);
        adminMenu.addItem(reportsSubMenuItem);
        adminMenu.addItem(new EMenuItem("Downloads", LinkManager.getAdminDownloadsLink()));
        adminMenu.addItem(new EMenuItem("License", LinkManager.getAdminLicenseLink()));

        Menu helpMenu = new Menu();
        helpMenu.setStyleName("menuBarMenuButton");
        helpMenu.setTitle("Help");
        helpMenu.addItem(new EMenuItem("Online Documentation", "http://www.rhq-project.org")); //, new LinkCommand("#sdfs"));
        helpMenu.addItem(new EMenuItem("Open a support case", "http://www.rhq-project.org")); //, new LinkCommand("#sdfs"));
        //        helpMenu.addItem(new MenuItem("About")); //, new LinkCommand("#sdfs"));

        menu.setMenus(overviewMenu, resourcesMenu, groupsMenu, adminMenu, helpMenu);

        topBar.addMember(menu);

        addMember(topBar);
        addMember(new SearchBarPane(this.extendLocatorId("Search")));

    }

    public static class EMenuItem extends com.smartgwt.client.widgets.menu.MenuItem {

        String link;

        public EMenuItem(String title, String link) {
            super(title);
            this.link = link;
            init();
        }

        public EMenuItem(String title, String link, String icon) {
            super(title, icon);
            this.link = link;
            init();
        }

        private void init() {
            addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                public void onClick(MenuItemClickEvent menuItemClickEvent) {
                    if (link.startsWith("#")) {
                        History.newItem(link.substring(1));
                    } else {
                        Window.Location.assign(link);
                    }
                }
            });
        }
    }

}
