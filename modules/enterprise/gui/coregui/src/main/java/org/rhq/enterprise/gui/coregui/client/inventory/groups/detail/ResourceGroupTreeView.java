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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.core.domain.resource.group.composite.ClusterKeyFlyweight;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupTreeView extends LocatableVLayout implements BookmarkableView {

    private TreeGrid treeGrid;

    private ViewId currentViewId;
    private int rootGroupId;
    private int selectedGroupId;
    private String selectedNodeId;

    private ResourceGroupTreeContextMenu contextMenu;

    private ResourceGroup rootResourceGroup;
    private Map<Integer, ResourceType> typeMap;
    private ResourceGroup selectedGroup;

    public ResourceGroupTreeView(String locatorId) {
        super(locatorId);

        setWidth(250);
        setHeight100();

        setShowResizeBar(true);
    }

    @Override
    protected void onInit() {
        super.onInit();

        treeGrid = new TreeGrid();
        treeGrid.setWidth100();
        treeGrid.setHeight100();
        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setSortField(EnhancedTreeNode.Attributes.NAME);
        treeGrid.setSortFoldersBeforeLeaves(true);
        treeGrid.setSeparateFolders(true);
        treeGrid.setShowHeader(false);

        addMember(this.treeGrid);

        contextMenu = new ResourceGroupTreeContextMenu(extendLocatorId("contextMenu"));
        treeGrid.setContextMenu(contextMenu);

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (!selectionEvent.isRightButtonDown() && selectionEvent.getState()) {
                    ResourceGroupEnhancedTreeNode selectedNode = (ResourceGroupEnhancedTreeNode) selectionEvent
                        .getRecord();
                    selectedNodeId = selectedNode.getID();
                    com.allen_sauer.gwt.log.client.Log.info("Node selected in tree: " + selectedNode);
                    ResourceType type = selectedNode.getResourceType();
                    if (type != null) {
                        // It's a cluster group node, not a subcategory node or an autoTypeGroup node.
                        ClusterKey key = selectedNode.getClusterKey();
                        if (key == null) {
                            // The root group was selected.
                            String groupId = selectedNode.getID();
                            com.allen_sauer.gwt.log.client.Log.debug("Selecting group [" + groupId + "]...");
                            String viewPath = ResourceGroupTopView.VIEW_ID + "/" + groupId;
                            String currentViewPath = History.getToken();
                            if (!currentViewPath.startsWith(viewPath)) {
                                CoreGUI.goToView(viewPath);
                            }
                        } else {
                            com.allen_sauer.gwt.log.client.Log.debug("Selecting cluster group [" + key + "]...");
                            selectClusterGroup(key);
                        }
                    }
                }
            }
        });

        treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent event) {
                // stop the browser right-click menu
                event.cancel();

                // don't select the node on a right click, since we're not navigating to it
                treeGrid.deselectRecord(event.getNode());
                if (null != selectedNodeId) {
                    treeGrid.selectRecord(treeGrid.getTree().findById(SeleniumUtility.getSafeId(selectedNodeId)));
                }

                contextMenu.showContextMenu(event.getNode());
            }
        });

    }

    public void setSelectedGroup(final int groupId, boolean isAutoCluster) {
        this.selectedGroupId = groupId;

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.addFilterVisible(Boolean.valueOf(!isAutoCluster));
        criteria.fetchResourceType(true);

        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroup>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_tree_common_loadFailed_group(String.valueOf(groupId)), caught);
                }

                @Override
                public void onSuccess(PageList<ResourceGroup> result) {
                    ResourceGroup group = result.get(0);
                    ResourceGroupTreeView.this.selectedGroup = group;

                    GroupCategory groupCategory = group.getGroupCategory();
                    switch (groupCategory) {
                    case MIXED:
                        // For mixed groups, there will only ever be one item in the tree, even if the group is recursive.
                        // This is because mixed groups don't normally have clustered/identical resources across members
                        // so there is no attempt here to build auto cluster nodes.
                        ResourceGroupTreeView.this.rootResourceGroup = group;
                        ResourceGroupTreeView.this.rootGroupId = rootResourceGroup.getId();
                        ResourceGroupEnhancedTreeNode fakeRoot = new ResourceGroupEnhancedTreeNode("fakeRootNode");
                        ResourceGroupEnhancedTreeNode rootNode = new ResourceGroupEnhancedTreeNode(group.getName());
                        String icon = ImageManager.getGroupIcon(GroupCategory.MIXED);
                        rootNode.setIcon(icon);
                        rootNode.setID(String.valueOf(rootResourceGroup.getId()));
                        fakeRoot.setID("__fakeRoot__");
                        rootNode.setParentID(fakeRoot.getID());
                        fakeRoot.setChildren(new ResourceGroupEnhancedTreeNode[] { rootNode });
                        Tree tree = new Tree();
                        tree.setRoot(fakeRoot);
                        treeGrid.setData(tree);
                        treeGrid.markForRedraw();
                        break;
                    case COMPATIBLE:
                        if (group.getClusterResourceGroup() == null) {
                            // This is a straight up compatible group.
                            ResourceGroupTreeView.this.rootResourceGroup = group;
                        } else {
                            // This is a cluster group beneath a real recursive compatible group.
                            ResourceGroupTreeView.this.rootResourceGroup = group.getClusterResourceGroup();
                        }
                        loadGroup(ResourceGroupTreeView.this.rootResourceGroup.getId());
                        break;
                    }
                }
            });
    }

    private void loadGroup(int groupId) {
        if (groupId == this.rootGroupId) {
            // Still looking at the same compat-recursive tree

            ResourceGroupEnhancedTreeNode selectedNode;
            if (this.selectedGroup.getClusterKey() != null) {
                selectedNode = (ResourceGroupEnhancedTreeNode) treeGrid.getTree().find(
                    ResourceGroupEnhancedTreeNode.CLUSTER_KEY, this.selectedGroup.getClusterKey());
            } else {
                // TODO not sure when this else would happen, why would a group's cluster key be null?
                selectedNode = (ResourceGroupEnhancedTreeNode) treeGrid.getTree().findById(
                    SeleniumUtility.getSafeId(String.valueOf(this.selectedGroup.getId())));
            }

            // TODO reselect tree to selected node
            if (selectedNode != null) {
                TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                treeGrid.getTree().openFolders(parents);
                treeGrid.getTree().openFolder(selectedNode);
                treeGrid.selectRecord(selectedNode);
            }
        } else {
            this.rootGroupId = groupId;
            GWTServiceLookup.getClusterService().getClusterTree(groupId, new AsyncCallback<ClusterFlyweight>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_groupTree(), caught);
                }

                public void onSuccess(ClusterFlyweight result) {
                    loadTreeTypes(result);
                }
            });
        }
    }

    private void loadTreeTypes(final ClusterFlyweight root) {
        Set<Integer> typeIds = new HashSet<Integer>();
        typeIds.add(this.rootResourceGroup.getResourceType().getId());
        getTreeTypes(root, typeIds);

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(typeIds.toArray(new Integer[typeIds.size()]),
            EnumSet.of(ResourceTypeRepository.MetadataType.subCategory),
            new ResourceTypeRepository.TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    ResourceGroupTreeView.this.typeMap = types;
                    loadTree(root);
                }
            });
    }

    private void selectClusterGroup(ClusterKey key) {
        GWTServiceLookup.getClusterService().createAutoClusterBackingGroup(key, true,
            new AsyncCallback<ResourceGroup>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_createFailed_autoCluster(), caught);
                }

                @Override
                public void onSuccess(ResourceGroup result) {
                    renderAutoCluster(result);
                }
            });
    }

    private void renderAutoCluster(ResourceGroup backingGroup) {
        String viewPath = ResourceGroupDetailView.AUTO_CLUSTER_VIEW_PATH + "/" + backingGroup.getId();
        String currentViewPath = History.getToken();
        if (!currentViewPath.startsWith(viewPath)) {
            CoreGUI.goToView(viewPath);
        }
    }

    private void loadTree(ClusterFlyweight root) {
        ClusterKey rootKey = new ClusterKey(root.getGroupId());
        ResourceGroupEnhancedTreeNode fakeRoot = new ResourceGroupEnhancedTreeNode("fakeRootNode");
        fakeRoot.setID("__fakeRoot__");

        ResourceGroupEnhancedTreeNode rootNode = new ResourceGroupEnhancedTreeNode(rootResourceGroup.getName());
        rootNode.setID(rootKey.getKey());
        rootNode.setParentID(fakeRoot.getID());

        ResourceType rootResourceType = typeMap.get(rootResourceGroup.getResourceType().getId());
        rootNode.setResourceType(rootResourceType);

        String icon = ImageManager.getClusteredResourceIcon(rootResourceType.getCategory());
        rootNode.setIcon(icon);

        fakeRoot.setChildren(new ResourceGroupEnhancedTreeNode[] { rootNode });

        loadTree(rootNode, root, rootKey);

        Tree tree = new Tree();

        tree.setRoot(fakeRoot);
        org.rhq.enterprise.gui.coregui.client.util.TreeUtility.printTree(tree);

        treeGrid.setData(tree);
        treeGrid.getTree().openFolder(rootNode);
        treeGrid.markForRedraw();
    }

    public void loadTree(ResourceGroupEnhancedTreeNode parentNode, ClusterFlyweight parentClusterGroup,
        ClusterKey parentKey) {
        if (!parentClusterGroup.getChildren().isEmpty()) {
            // First pass - group the children by type.
            Map<ResourceType, List<ClusterFlyweight>> childrenByType = new HashMap<ResourceType, List<ClusterFlyweight>>();
            for (ClusterFlyweight child : parentClusterGroup.getChildren()) {
                ClusterKeyFlyweight keyFlyweight = child.getClusterKey();

                ResourceType type = this.typeMap.get(keyFlyweight.getResourceTypeId());
                List<ClusterFlyweight> children = childrenByType.get(type);
                if (children == null) {
                    children = new ArrayList<ClusterFlyweight>();
                    childrenByType.put(type, children);
                }
                children.add(child);
            }

            // Second pass - process each of the sets of like-typed children created in the first pass.
            List<ResourceGroupEnhancedTreeNode> childNodes = new ArrayList<ResourceGroupEnhancedTreeNode>();
            Map<String, ResourceGroupEnhancedTreeNode> subCategoryNodesByName = new HashMap<String, ResourceGroupEnhancedTreeNode>();
            Map<String, List<ResourceGroupEnhancedTreeNode>> subCategoryChildrenByName = new HashMap<String, List<ResourceGroupEnhancedTreeNode>>();
            for (ResourceType childType : childrenByType.keySet()) {
                List<ClusterFlyweight> children = childrenByType.get(childType);
                List<ResourceGroupEnhancedTreeNode> nodesByType = new ArrayList<ResourceGroupEnhancedTreeNode>();
                for (ClusterFlyweight child : children) {
                    ResourceGroupEnhancedTreeNode node = createClusterGroupNode(parentKey, childType, child);
                    nodesByType.add(node);

                    if (!child.getChildren().isEmpty()) {
                        ClusterKey key = node.getClusterKey();
                        loadTree(node, child, key); // recurse
                    }
                }

                // Insert an autoTypeGroup node if the type is not a singleton.
                if (!childType.isSingleton()) {
                    // This will override the parent IDs of all nodesByType nodes with the auto group node ID that is being created
                    ResourceGroupEnhancedTreeNode autoTypeGroupNode = createAutoTypeGroupNode(parentKey, childType,
                        nodesByType);
                    nodesByType.clear();
                    nodesByType.add(autoTypeGroupNode);
                }

                // Insert subcategory node(s) if the type has a subcategory.
                ResourceSubCategory subcategory = childType.getSubCategory();
                if (subcategory != null) {
                    ResourceGroupEnhancedTreeNode lastSubcategoryNode = null;

                    ResourceSubCategory currentSubCategory = subcategory;
                    boolean currentSubcategoryNodeCreated = false;
                    do {
                        ResourceGroupEnhancedTreeNode currentSubcategoryNode = subCategoryNodesByName
                            .get(currentSubCategory.getName());
                        if (currentSubcategoryNode == null) {
                            currentSubcategoryNode = new ResourceGroupEnhancedTreeNode(currentSubCategory.getName());
                            // Note, subcategory names are typically already plural, so there's no need to pluralize them.
                            currentSubcategoryNode.setTitle(currentSubCategory.getDisplayName());
                            currentSubcategoryNode.setIsFolder(true);
                            currentSubcategoryNode.setID("cat" + currentSubCategory.getName());
                            currentSubcategoryNode.setParentID(parentKey.getKey());
                            subCategoryNodesByName.put(currentSubCategory.getName(), currentSubcategoryNode);
                            subCategoryChildrenByName.put(currentSubCategory.getName(),
                                new ArrayList<ResourceGroupEnhancedTreeNode>());

                            if (currentSubCategory.getParentSubCategory() == null) {
                                // It's a root subcategory - add a node for it to the tree.
                                childNodes.add(currentSubcategoryNode);
                            }

                            currentSubcategoryNodeCreated = true;
                        }

                        if (lastSubcategoryNode != null) {
                            List<ResourceGroupEnhancedTreeNode> currentSubcategoryChildren = subCategoryChildrenByName
                                .get(currentSubcategoryNode.getName());
                            // make sure we re-parent the child so it is under the subcategory folder
                            for (ResourceGroupEnhancedTreeNode currentSubcategoryChild : currentSubcategoryChildren) {
                                currentSubcategoryChild.setParentID(currentSubcategoryNode.getID());
                            }
                            currentSubcategoryChildren.add(lastSubcategoryNode);
                        }
                        lastSubcategoryNode = currentSubcategoryNode;
                    } while (currentSubcategoryNodeCreated
                        && (currentSubCategory = currentSubCategory.getParentSubCategory()) != null);

                    List<ResourceGroupEnhancedTreeNode> subcategoryChildren = subCategoryChildrenByName.get(subcategory
                        .getName());
                    subcategoryChildren.addAll(nodesByType);
                } else {
                    childNodes.addAll(nodesByType);
                }
            }

            for (String subcategoryName : subCategoryNodesByName.keySet()) {
                ResourceGroupEnhancedTreeNode subcategoryNode = subCategoryNodesByName.get(subcategoryName);
                List<ResourceGroupEnhancedTreeNode> subcategoryChildren = subCategoryChildrenByName
                    .get(subcategoryName);
                // make sure the parent for the subcat children are referring to the parent subcat node
                for (ResourceGroupEnhancedTreeNode subcatChild : subcategoryChildren) {
                    subcatChild.setParentID(subcategoryNode.getID());
                }
                subcategoryNode.setChildren(subcategoryChildren
                    .toArray(new ResourceGroupEnhancedTreeNode[subcategoryChildren.size()]));
            }

            parentNode.setChildren(childNodes.toArray(new ResourceGroupEnhancedTreeNode[childNodes.size()]));
        }
    }

    private ResourceGroupEnhancedTreeNode createClusterGroupNode(ClusterKey parentKey, ResourceType type,
        ClusterFlyweight child) {

        ResourceGroupEnhancedTreeNode node = new ResourceGroupEnhancedTreeNode(child.getName());

        ClusterKeyFlyweight keyFlyweight = child.getClusterKey();
        ClusterKey key = new ClusterKey(parentKey, keyFlyweight.getResourceTypeId(), keyFlyweight.getResourceKey());
        String icon = ImageManager.getClusteredResourceIcon(type.getCategory());
        String id = key.getKey();
        String parentId = parentKey.getKey();
        node.setID(id);
        node.setParentID(parentId);
        node.setClusterKey(key);
        node.setResourceType(type);
        node.setIsFolder(!child.getChildren().isEmpty());
        node.setIcon(icon);
        return node;
    }

    private ResourceGroupEnhancedTreeNode createAutoTypeGroupNode(ClusterKey parentKey, ResourceType type,
        List<ResourceGroupEnhancedTreeNode> memberNodes) {
        String name = StringUtility.pluralize(type.getName());
        ResourceGroupEnhancedTreeNode autoTypeGroupNode = new ResourceGroupEnhancedTreeNode(name);
        String parentId = parentKey.getKey();
        String autoTypeGroupNodeId = "rt" + String.valueOf(type.getId());
        autoTypeGroupNode.setID(autoTypeGroupNodeId);
        autoTypeGroupNode.setParentID(parentId);
        autoTypeGroupNode.setIsFolder(true);
        for (ResourceGroupEnhancedTreeNode memberNode : memberNodes) {
            memberNode.setParentID(autoTypeGroupNodeId);
        }
        autoTypeGroupNode.setChildren(memberNodes.toArray(new ResourceGroupEnhancedTreeNode[memberNodes.size()]));
        return autoTypeGroupNode;
    }

    public void renderView(ViewPath viewPath) {
        currentViewId = viewPath.getCurrent();
        String currentViewIdPath = currentViewId.getPath();
        if (this.currentViewId != null) {
            if ("AutoCluster".equals(currentViewIdPath)) {
                // Move the currentViewId to the ID portion to play better with other code
                currentViewId = viewPath.getNext();
                String clusterGroupIdString = currentViewId.getPath();
                Integer clusterGroupId = Integer.parseInt(clusterGroupIdString);
                setSelectedGroup(clusterGroupId, true);
            } else {
                String groupIdString = currentViewId.getPath();
                int groupId = Integer.parseInt(groupIdString);
                setSelectedGroup(groupId, false);
            }
        }
    }

    private void getTreeTypes(ClusterFlyweight clusterFlyweight, Set<Integer> typeIds) {
        if (clusterFlyweight.getClusterKey() != null) {
            typeIds.add(clusterFlyweight.getClusterKey().getResourceTypeId());
        }

        for (ClusterFlyweight child : clusterFlyweight.getChildren()) {
            getTreeTypes(child, typeIds);
        }
    }

    private class ResourceGroupEnhancedTreeNode extends EnhancedTreeNode {
        private static final String CLUSTER_KEY = "key";
        private static final String RESOURCE_TYPE = "resourceType";

        public ResourceGroupEnhancedTreeNode(String name) {
            super(name);
        }

        public ClusterKey getClusterKey() {
            return (ClusterKey) getAttributeAsObject(CLUSTER_KEY);
        }

        public void setClusterKey(ClusterKey key) {
            setAttribute(CLUSTER_KEY, key);
        }

        @Override
        public void setID(String id) {
            super.setID(SeleniumUtility.getSafeId(id));
        }

        @Override
        public void setParentID(String parentID) {
            super.setParentID(SeleniumUtility.getSafeId(parentID));
        }

        public ResourceType getResourceType() {
            return (ResourceType) getAttributeAsObject(RESOURCE_TYPE);
        }

        public void setResourceType(ResourceType rt) {
            setAttribute(RESOURCE_TYPE, rt);
        }

    }
}
