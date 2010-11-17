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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 *
 * @deprecated {@link ResourceTreeView} is used instead
 */
@Deprecated
public class NewResourceTreeView extends LocatableVLayout {
    private Resource selectedResource;
    private Resource rootResource;

    private TreeGrid treeGrid;
    private Menu contextMenu;

    private ViewId currentViewId;

    private ArrayList<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    private boolean initialSelect = false;

    public NewResourceTreeView(String locatorId) {
        super(locatorId);

        setWidth("250");
        setHeight100();

        setShowResizeBar(true);
    }

    public void onInit() {

    }

    private void buildTree() {

        treeGrid = new CustomResourceTreeGrid(getLocatorId());

        treeGrid.setOpenerImage("resources/dir.png");
        treeGrid.setOpenerIconSize(16);

        //        treeGrid.setAutoFetchData(true);
        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setSortField("name");
        treeGrid.setShowHeader(false);

        treeGrid.setLeaveScrollbarGap(false);

        contextMenu = new Menu();
        MenuItem item = new MenuItem("Expand node");

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (!selectionEvent.isRightButtonDown() && selectionEvent.getState()) {
                    if (treeGrid.getSelectedRecord() instanceof ResourceTreeDatasource.ResourceTreeNode) {
                        ResourceTreeDatasource.ResourceTreeNode node = (ResourceTreeDatasource.ResourceTreeNode) treeGrid
                            .getSelectedRecord();
                        Log.info("Resource selected in tree: " + node.getResource());

                        String newToken = "Resource/" + node.getResource().getId();
                        String currentToken = History.getToken();
                        if (!currentToken.startsWith(newToken)) {

                            String ending = currentToken.replaceFirst("^[^\\/]*\\/[^\\/]*", "");

                            History.newItem("Resource/" + node.getResource().getId() + ending);

                        }
                    }

                }
            }
        });

        // This constructs the context menu for the resource at the time of the click.
        setContextMenu(contextMenu);

        treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent event) {
                event.getNode();
                event.cancel();

                if (event.getNode() instanceof ResourceTreeDatasource.AutoGroupTreeNode) {
                    showContextMenu((ResourceTreeDatasource.AutoGroupTreeNode) event.getNode());
                } else if (event.getNode() instanceof ResourceTreeDatasource.ResourceTreeNode) {
                    showContextMenu((ResourceTreeDatasource.ResourceTreeNode) event.getNode());
                }
            }
        });
    }

    private void showContextMenu(ResourceTreeDatasource.AutoGroupTreeNode node) {

        contextMenu.setItems(new MenuItem(node.getName()));
        contextMenu.showContextMenu();

    }

    private void showContextMenu(final ResourceTreeDatasource.ResourceTreeNode node) {
        ResourceType type = node.getResource().getResourceType();
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            type.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.subCategory,
                ResourceTypeRepository.MetadataType.pluginConfigurationDefinition,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    buildResourceContextMenu(node.getResource(), type);
                    contextMenu.showContextMenu();
                }
            });
    }

    private void buildResourceContextMenu(final Resource resource, final ResourceType resourceType) {
        contextMenu.setItems(new MenuItem(resource.getName()));

        contextMenu.addItem(new MenuItem(MSG.view_tree_common_contextMenu_type_name_label(resourceType.getName())));

        MenuItem editPluginConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_pluginConfiguration());
        editPluginConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                int resourceId = resource.getId();
                int resourceTypeId = resourceType.getId();

                Window configEditor = new Window();
                configEditor.setTitle(MSG.view_tree_common_contextMenu_editPluginConfiguration(resource.getName()));
                configEditor.setWidth(800);
                configEditor.setHeight(800);
                configEditor.setIsModal(true);
                configEditor.setShowModalMask(true);
                configEditor.setCanDragResize(true);
                configEditor.centerInPage();
                configEditor.addItem(new ConfigurationEditor("PluginConfig-" + resource.getName(), resourceId,
                    resourceTypeId, ConfigurationEditor.ConfigType.plugin));
                configEditor.show();

            }
        });
        editPluginConfiguration.setEnabled(resourceType.getPluginConfigurationDefinition() != null);
        contextMenu.addItem(editPluginConfiguration);

        MenuItem editResourceConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_resourceConfiguration());
        editResourceConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                int resourceId = resource.getId();
                int resourceTypeId = resourceType.getId();

                final Window configEditor = new Window();
                configEditor.setTitle(MSG.view_tree_common_contextMenu_editResourceConfiguration(resource.getName()));
                configEditor.setWidth(800);
                configEditor.setHeight(800);
                configEditor.setIsModal(true);
                configEditor.setShowModalMask(true);
                configEditor.setCanDragResize(true);
                configEditor.setShowResizer(true);
                configEditor.centerInPage();
                configEditor.addCloseClickHandler(new CloseClickHandler() {
                    public void onCloseClick(CloseClientEvent closeClientEvent) {
                        configEditor.destroy();
                    }
                });
                configEditor.addItem(new ConfigurationEditor("ResourceConfig-" + resource.getName(), resourceId,
                    resourceTypeId, ConfigurationEditor.ConfigType.resource));
                configEditor.show();

            }
        });
        editResourceConfiguration.setEnabled(resourceType.getResourceConfigurationDefinition() != null);
        contextMenu.addItem(editResourceConfiguration);

        contextMenu.addItem(new MenuItemSeparator());

        // Operations Menu
        MenuItem operations = new MenuItem(MSG.view_tree_common_contextMenu_operations());
        Menu opSubMenu = new Menu();
        for (final OperationDefinition operationDefinition : resourceType.getOperationDefinitions()) {
            MenuItem operationItem = new MenuItem(operationDefinition.getDisplayName());
            operationItem.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    new OperationCreateWizard(selectedResource, operationDefinition).startOperationWizard();
                }
            });
            opSubMenu.addItem(operationItem);
            // todo action
        }
        operations.setEnabled(!resourceType.getOperationDefinitions().isEmpty());
        operations.setSubmenu(opSubMenu);
        contextMenu.addItem(operations);

        contextMenu.addItem(buildMetricsMenu(resourceType));

        // Create Menu
        MenuItem createChildMenu = new MenuItem(MSG.common_button_create_child());
        Menu createChildSubMenu = new Menu();
        for (final ResourceType childType : resourceType.getChildResourceTypes()) {
            if (childType.isCreatable()) {
                MenuItem createItem = new MenuItem(childType.getName());
                createChildSubMenu.addItem(createItem);
                createItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        ResourceFactoryCreateWizard.showCreateWizard(resource, childType);
                    }
                });

            }
        }
        createChildMenu.setSubmenu(createChildSubMenu);
        createChildMenu.setEnabled(createChildSubMenu.getItems().length > 0);
        contextMenu.addItem(createChildMenu);

        // Manually Add Menu
        MenuItem importChildMenu = new MenuItem(MSG.common_button_import());
        Menu importChildSubMenu = new Menu();
        for (ResourceType childType : resourceType.getChildResourceTypes()) {
            if (childType.isSupportsManualAdd()) {
                importChildSubMenu.addItem(new MenuItem(childType.getName()));
                //todo action
            }
        }
        if (resourceType.getCategory() == ResourceCategory.PLATFORM) {
            loadManuallyAddServersToPlatforms(importChildSubMenu);
        }
        importChildMenu.setSubmenu(importChildSubMenu);
        importChildMenu.setEnabled(importChildSubMenu.getItems().length > 0);
        contextMenu.addItem(importChildMenu);
    }

    private void loadManuallyAddServersToPlatforms(final Menu manuallyAddMenu) {
        ResourceTypeGWTServiceAsync rts = GWTServiceLookup.getResourceTypeGWTService();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterSupportsManualAdd(true);
        criteria.fetchParentResourceTypes(true);
        rts.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_manualAddChildren(),
                    caught);
            }

            public void onSuccess(PageList<ResourceType> result) {
                for (ResourceType type : result) {
                    if (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty()) {
                        MenuItem item = new MenuItem(type.getName());
                        manuallyAddMenu.addItem(item);
                    }
                }
            }
        });
    }

    private MenuItem buildMetricsMenu(final ResourceType type) {
        MenuItem measurements = new MenuItem("Measurements");
        final Menu measurementsSubMenu = new Menu();

        GWTServiceLookup.getDashboardService().findDashboardsForSubject(new AsyncCallback<List<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_dashboard(), caught);
            }

            public void onSuccess(List<Dashboard> result) {

                for (final MeasurementDefinition def : type.getMetricDefinitions()) {

                    MenuItem defItem = new MenuItem(def.getDisplayName());
                    measurementsSubMenu.addItem(defItem);
                    Menu defSubItem = new Menu();
                    defItem.setSubmenu(defSubItem);

                    for (final Dashboard d : result) {
                        MenuItem addToDBItem = new MenuItem(MSG.view_tree_common_contextMenu_addChartToDashboard(d
                            .getName()));
                        defSubItem.addItem(addToDBItem);

                        addToDBItem.addClickHandler(new ClickHandler() {
                            public void onClick(MenuItemClickEvent menuItemClickEvent) {

                                DashboardPortlet p = new DashboardPortlet(def.getDisplayName() + " Chart",
                                    GraphPortlet.KEY, 250);
                                p.getConfiguration().put(
                                    new PropertySimple(GraphPortlet.CFG_RESOURCE_ID, selectedResource.getId()));
                                p.getConfiguration().put(
                                    new PropertySimple(GraphPortlet.CFG_DEFINITION_ID, def.getId()));

                                d.addPortlet(p, 0, 0);

                                GWTServiceLookup.getDashboardService().storeDashboard(d,
                                    new AsyncCallback<Dashboard>() {
                                        public void onFailure(Throwable caught) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(), caught);
                                        }

                                        public void onSuccess(Dashboard result) {
                                            CoreGUI.getMessageCenter().notify(
                                                new Message(MSG
                                                    .view_tree_common_contextMenu_saveChartToDashboardSuccessful(result
                                                        .getName()), Message.Severity.Info));
                                        }
                                    });

                            }
                        });

                    }

                }

            }
        });
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }

    Resource getResource(int resourceId) {
        if (this.treeGrid != null && this.treeGrid.getTree() != null) {
            ResourceTreeDatasource.ResourceTreeNode treeNode = (ResourceTreeDatasource.ResourceTreeNode) this.treeGrid
                .getTree().findById(String.valueOf(resourceId));
            if (treeNode != null) {
                return treeNode.getResource();
            }
        }
        return null;
    }

    private void setRootResource(Resource rootResource) {
        this.rootResource = rootResource;
    }

    public void setSelectedResource(final Resource selectedResource, final ViewId viewId) {
        this.selectedResource = selectedResource;

        TreeNode node = null;
        if (treeGrid != null && treeGrid.getTree() != null
            && (node = treeGrid.getTree().findById(String.valueOf(selectedResource.getId()))) != null) {

            // This is the case where the tree was previously loaded and we get fired to look at a different
            // node in the same tree and just have to switch the selection

            TreeNode[] parents = treeGrid.getTree().getParents(node);
            treeGrid.getTree().openFolders(parents);
            treeGrid.getTree().openFolder(node);

            treeGrid.deselectAllRecords();
            treeGrid.selectRecord(node);

            // Update breadcrumbs
            viewId.getBreadcrumbs().clear();
            for (int i = parents.length - 1; i >= 0; i--) {
                TreeNode n = parents[i];
                adjustBreadcrumb(n, viewId);
            }
            adjustBreadcrumb(node, viewId);

            CoreGUI.refreshBreadCrumbTrail();

        } else {

            // Data not yet loaded or a node in a different tree

            final ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

            resourceService.getPlatformForResource(selectedResource.getId(), new AsyncCallback<Resource>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_root(), caught);
                }

                public void onSuccess(Resource result) {
                    rootResource = result;

                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterRootResourceId(rootResource.getId());
                    criteria.setPageControl(PageControl.getUnlimitedInstance());

                    resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler()
                                .handleError(MSG.view_tree_common_loadFailed_descendants(), caught);
                        }

                        public void onSuccess(PageList<Resource> result) {

                            loadTree(rootResource.getId(), result);
                        }
                    });
                }
            });

        }
    }

    private void loadTree(int rootId, PageList<Resource> result) {
        if (this.treeGrid != null) {
            this.treeGrid.destroy();
        }
        this.buildTree();

        HashMap<Integer, Resource> data = new HashMap<Integer, Resource>();
        for (Resource res : result) {
            data.put(res.getId(), res);
        }

        Tree tree = new Tree();

        tree.setRoot(NewResourceTreeDataSource.build(rootId, data));
        treeGrid.setData(tree);
        addMember(this.treeGrid);
    }

    private void adjustBreadcrumb(TreeNode node, ViewId viewId) {
        if (node instanceof ResourceTreeDatasource.ResourceTreeNode) {

            Resource nr = ((ResourceTreeDatasource.ResourceTreeNode) node).getResource();
            String display = node.getName() + " <span class=\"subtitle\">" + nr.getResourceType().getName() + "</span>";
            String icon = "types/" + nr.getResourceType().getCategory().getDisplayName() + "_up_16.png";

            viewId.getBreadcrumbs().add(new Breadcrumb(node.getAttribute("id"), display, icon, true));

        } else {

            //            if (node.getName() != null) {
            //                viewId.getBreadcrumbs().add(new Breadcrumb(node.getAttribute("id"), node.getName(), null, true));
            //            }
        }
    }

    public void addResourceSelectListener(ResourceSelectListener listener) {
        this.selectListeners.add(listener);
    }

    public void renderView(ViewPath viewPath) {
        currentViewId = viewPath.getCurrent();
    }
}