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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.admin.users.UserEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceView;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourcesView extends HLayout implements Presenter {

    private SectionStack sectionStack;
    private Canvas contentCanvas;


    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();


        contentCanvas = new Canvas();
        contentCanvas.setWidth("*");
        contentCanvas.setHeight100();


        sectionStack = new SectionStack();
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        sectionStack.addSection(buildResourcesSection());
        sectionStack.addSection(buildGroupsSection());

        addMember(sectionStack);
        addMember(contentCanvas);

        setContent(buildResourceSearchView());

        CoreGUI.setBreadCrumb(getPlace());
    }


    private ResourceSearchView buildResourceSearchView() {
        ResourceSearchView searchView = new ResourceSearchView();
        searchView.addResourceSelectedListener(new ResourceSelectListener() {
            public void onResourceSelected(Resource resource) {
                CoreGUI.setContent(new ResourceView(resource));
            }
        });
        return searchView;
    }


    private SectionStackSection buildResourcesSection() {
        final SectionStackSection section = new SectionStackSection("Resources");
        section.setExpanded(true);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode savedSearches = new TreeNode("Saved Searches", new TreeNode("Down JBossAS (4)"));
        final TreeNode manageRolesNode = new TreeNode("Global Saved Searches");
        tree.setRoot(new TreeNode("security",
                savedSearches,
                manageRolesNode));

        treeGrid.setData(tree);


        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getRecord() == savedSearches) {
                    setContent(new ResourceSearchView());
                } else if (selectionEvent.getRecord() == manageRolesNode) {
                    setContent(new ResourceSearchView());
                }
            }
        });


        section.addItem(treeGrid);


        return section;
    }


    private SectionStackSection buildGroupsSection() {
        final SectionStackSection section = new SectionStackSection("Resource Groups");
        section.setExpanded(true);

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setShowHeader(false);

        Tree tree = new Tree();
        final TreeNode manageServersNode = new TreeNode("Groups");
        final TreeNode manageAgentsNode = new TreeNode("DynaGroups");
        final TreeNode manageAffinityGroupsNode = new TreeNode("Special Groups");
        final TreeNode managePartitionEventsNode = new TreeNode("Greg's Groups");

        tree.setRoot(new TreeNode("clustering",
                manageServersNode,
                manageAgentsNode,
                manageAffinityGroupsNode,
                managePartitionEventsNode));

        treeGrid.setData(tree);


        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                HTMLPane pane = new HTMLPane();
                pane.setContentsType(ContentsType.PAGE);
                pane.setWidth100();
                pane.setHeight100();

                String url = null;
                if (selectionEvent.getRecord() == manageServersNode) {
                    url = "/rhq/ha/listServers.xhtml";
                } else if (selectionEvent.getRecord() == manageAgentsNode) {
                    url = "/rhq/ha/listAgents.xhtml";
                } else if (selectionEvent.getRecord() == manageAffinityGroupsNode) {
                    url = "/rhq/ha/listAffinityGroups.xhtml";
                } else if (selectionEvent.getRecord() == managePartitionEventsNode) {
                    url = "/rhq/ha/listPartitionEvents.xhtml";
                }
                pane.setContentsURL(url + "?nomenu=true");
                setContent(pane);

            }
        });

        section.addItem(treeGrid);


        return section;
    }


    public void setContent(Canvas newContent) {
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();
        newContent.setWidth100();
        newContent.setHeight100();
        contentCanvas.addChild(newContent);
        contentCanvas.draw();
    }


    public boolean fireDisplay(Place place, List<Place> children) {
        if (place.equals(getPlace())) {
            if (!children.isEmpty()) {
                if (contentCanvas.getChildren().length > 0) {
                    Canvas element = contentCanvas.getChildren()[0];
                    if (element instanceof Presenter) {
                        ((Presenter) element).fireDisplay(children.get(0), children.subList(1, children.size() - 1));
                    }
                }
            }
            return true;
        }
        return false;
    }


    public Place getPlace() {
        return new Place("Resources", "Resources");
    }
}
