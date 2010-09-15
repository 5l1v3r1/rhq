/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can retribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is tributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.LinkedHashMap;

import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.agent.install.RemoteAgentInstallView;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author Greg Hinkle
 */
public class AdministrationView extends LocatableHLayout implements BookmarkableView {

    public static final String VIEW_PATH = "Administration";

    private ViewId currentSectionViewId;
    private ViewId currentPageViewId;

    private SectionStack sectionStack;

    private Canvas contentCanvas;
    private Canvas currentContent;
    private LinkedHashMap<String, TreeGrid> treeGrids = new LinkedHashMap<String, TreeGrid>();

    public AdministrationView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        contentCanvas = new Canvas();
        contentCanvas.setWidth("*");
        contentCanvas.setHeight100();

        sectionStack = new LocatableSectionStack(this.getLocatorId());
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        treeGrids.put("Security", buildSecuritySection());
        treeGrids.put("Configuration", buildSystemConfigurationSection());
        treeGrids.put("Cluster", buildManagementClusterSection());

        for (final String name : treeGrids.keySet()) {
            TreeGrid grid = treeGrids.get(name);

            grid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    if (selectionEvent.getState()) {
                        CoreGUI.goToView("Administration/" + name + "/" + selectionEvent.getRecord().getAttribute("name"));
                    }
                }
            });

            SectionStackSection section = new SectionStackSection(name);
            section.setExpanded(true);
            section.addItem(grid);

            sectionStack.addSection(section);
        }

        addMember(sectionStack);
        addMember(contentCanvas);

    }

    private HTMLFlow defaultView() {
        String contents = "<h1>Administration</h1>\n"
            + "From this section, the RHQ global settings can be administered. This includes configuring \n"
            + "<a href=\"\">Security</a>, setting up <a href=\"\">Plugins</a> and other stuff.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private TreeGrid buildSecuritySection() {

        final TreeGrid securityTreeGrid = new LocatableTreeGrid("Security");
        securityTreeGrid.setLeaveScrollbarGap(false);
        securityTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageUsersNode = new TreeNode("Manage Users");
        manageUsersNode.setIcon("global/User_16.png");

        final TreeNode manageRolesNode = new TreeNode("Manage Roles");
        manageRolesNode.setIcon("global/Role_16.png");

        final TreeNode remoteAgentInstall = new TreeNode("Remote Agent Install");
        remoteAgentInstall.setIcon("global/Agent_16.png");

        tree.setRoot(new TreeNode("security", manageUsersNode, manageRolesNode, remoteAgentInstall));

        securityTreeGrid.setData(tree);

        return securityTreeGrid;
    }

    private TreeGrid buildManagementClusterSection() {

        final TreeGrid mgmtClusterTreeGrid = new LocatableTreeGrid("Topology");
        mgmtClusterTreeGrid.setLeaveScrollbarGap(false);
        mgmtClusterTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageServersNode = new TreeNode("Servers");
        final TreeNode manageAgentsNode = new TreeNode("Agents");
        final TreeNode manageAffinityGroupsNode = new TreeNode("Affinity Groups");
        final TreeNode managePartitionEventsNode = new TreeNode("Partition Events");

        tree.setRoot(new TreeNode("clustering", manageServersNode, manageAgentsNode, manageAffinityGroupsNode,
            managePartitionEventsNode));

        mgmtClusterTreeGrid.setData(tree);

        return mgmtClusterTreeGrid;
    }

    private TreeGrid buildSystemConfigurationSection() {

        final TreeGrid systemConfigTreeGrid = new LocatableTreeGrid("Config");
        systemConfigTreeGrid.setLeaveScrollbarGap(false);
        systemConfigTreeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageSettings = new TreeNode("System Settings");
        final TreeNode manageTemplates = new TreeNode("Templates");
        final TreeNode manageDownloads = new TreeNode("Downloads");
        final TreeNode manageLicense = new TreeNode("License");
        final TreeNode managePlugins = new TreeNode("Plugins");

        tree.setRoot(new TreeNode("System Configuration", manageSettings, manageTemplates, manageDownloads,
            manageLicense, managePlugins));

        systemConfigTreeGrid.setData(tree);

        return systemConfigTreeGrid;
    }

    public void setContent(Canvas newContent) {

        if (contentCanvas.getChildren().length > 0) {
            for (Canvas child : contentCanvas.getChildren()) {
                child.destroy();
            }
        }

        contentCanvas.addChild(newContent);
        contentCanvas.markForRedraw();
        currentContent = newContent;
    }

    private void renderContentView(ViewPath viewPath) {

        currentSectionViewId = viewPath.getCurrent();
        currentPageViewId = viewPath.getNext();

        String section = currentSectionViewId.getPath();
        String page = currentPageViewId.getPath();

        Canvas content = null;
        if ("Security".equals(section)) {

            if ("Manage Users".equals(page)) {
                content = new UsersView(this.extendLocatorId("Users"));
            } else if ("Manage Roles".equals(page)) {
                content = new RolesView(this.extendLocatorId("Roles"));
            } else if ("Remote Agent Install".equals(page)) {
                content = new RemoteAgentInstallView(this.extendLocatorId("RemoteAgentInstall"));
            }
        } else if ("Configuration".equals(section)) {

            String url = null;
            if ("System Settings".equals(page)) {
                url = "/admin/config/Config.do?mode=edit";
            } else if ("Templates".equals(page)) {
                url = "/admin/config/EditDefaults.do?mode=monitor&viewMode=all";
            } else if ("Downloads".equals(page)) {
                url = "/rhq/admin/downloads-body.xhtml";
            } else if ("License".equals(page)) {
                url = "/admin/license/LicenseAdmin.do?mode=view";
            } else if ("Plugins".equals(page)) {
                url = "/rhq/admin/plugin/plugin-list-plain.xhtml";
            }
            url = addQueryStringParam(url, "nomenu=true");
            content = new FullHTMLPane(url);

        } else if ("Cluster".equals(section)) {
            String url = null;
            if ("Servers".equals(page)) {
                url = "/rhq/ha/listServers-plain.xhtml";
            } else if ("Agents".equals(page)) {
                url = "/rhq/ha/listAgents-plain.xhtml";
            } else if ("Affinity Groups".equals(page)) {
                url = "/rhq/ha/listAffinityGroups-plain.xhtml";
            } else if ("Partition Events".equals(page)) {
                url = "/rhq/ha/listPartitionEvents-plain.xhtml";
            }
            content = new FullHTMLPane(url);
        }

        for (String name : treeGrids.keySet()) {

            TreeGrid treeGrid = treeGrids.get(name);
            if (name.equals(section)) {
                //                treeGrid.setSelectedPaths(page);
            } else {
                treeGrid.deselectAllRecords();
            }
        }

        // ignore clicks on subsection folder nodes
        if (null != content) {
            setContent(content);

            if (content instanceof BookmarkableView) {
                ((BookmarkableView) content).renderView(viewPath.next().next());
            }
        }

    }

    public void renderView(ViewPath viewPath) {

        if (!viewPath.isCurrent(currentSectionViewId) || !viewPath.isNext(currentPageViewId)) {

            if (viewPath.isEnd()) {
                // Display default view
                setContent(defaultView());
            } else {
                renderContentView(viewPath);
            }
        } else {
            if (this.currentContent instanceof BookmarkableView) {
                ((BookmarkableView) this.currentContent).renderView(viewPath.next().next());
            }

        }
    }

    private static String addQueryStringParam(String url, String param) {
        char separatorChar = (url.indexOf('?') == -1) ? '?' : '&';
        return url + separatorChar + param;
    }
}
