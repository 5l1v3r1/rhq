/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecentlyAddedResourceDS extends DataSource {


    public RecentlyAddedResourceDS() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);


        DataSourceTextField idField = new DataSourceTextField("id", "ID");
        idField.setPrimaryKey(true);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", "Parent ID");
        parentIdField.setForeignKey("id");

        DataSourceTextField resourceNameField = new DataSourceTextField("name", "Resource Name");
        resourceNameField.setPrimaryKey(true);

        DataSourceTextField timestampField = new DataSourceTextField("timestamp", "Date/Time");

        setFields(idField, parentIdField, resourceNameField, timestampField);
    }

    @Override
    protected Object transformRequest(DSRequest request) {
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Asume success
        response.setStatus(0);
        switch (request.getOperationType()) {
            case FETCH:
                executeFetch(request, response);
                break;
            default:
                break;
        }

        return request.getData();
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {

        ResourceCriteria c = new ResourceCriteria();

        String p = request.getCriteria().getAttribute("parentId");

        if (p == null) {
            c.addFilterResourceCategory(ResourceCategory.PLATFORM);
            c.fetchChildResources(true);
        } else {
            c.addFilterParentResourceId(Integer.parseInt(p));
        }

        // TODO GH: Enhance resourceCriteria query to support itime based filtering for
        // "Recently imported" resources

        GWTServiceLookup.getResourceService().findRecentlyAddedResources(0, 100,
            new AsyncCallback<List<RecentlyAddedResourceComposite>>() {
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError("Failed to load recently added resources", throwable);
                }

                public void onSuccess(List<RecentlyAddedResourceComposite> recentlyAddedList) {
                    List<RecentlyAddedResourceComposite> list = new ArrayList<RecentlyAddedResourceComposite>();

                    for (RecentlyAddedResourceComposite recentlyAdded : recentlyAddedList) {
                        list.add(recentlyAdded);
                        list.addAll(recentlyAdded.getChildren());
                    }

                    response.setData(buildNodes(list));
                    response.setTotalRows(list.size());
                    processResponse(request.getRequestId(), response);
                }
            });
//
//        GWTServiceLookup.getResourceService().findResourcesByCriteria(c, new AsyncCallback<PageList<Resource>>() {
//            public void onFailure(Throwable caught) {
//                CoreGUI.getErrorHandler().handleError("Failed to load recently added resources data",caught);
//                response.setStatus(DSResponse.STATUS_FAILURE);
//                processResponse(request.getRequestId(), response);
//            }
//
//            public void onSuccess(PageList<Resource> result) {
//                PageList<Resource> all = new PageList<Resource>();
//
//                for (Resource root : result) {
//                    all.add(root);
//                    if (root.getChildResources() != null)
//                        all.addAll(root.getChildResources());
//                }
//
//
//                response.setData(buildNodes(all));
//                response.setTotalRows(all.getTotalSize());
//                processResponse(request.getRequestId(), response);
//            }
//        });
    }

//    private TreeNode[] buildNodes(PageList<Resource> list) {
//        TreeNode[] treeNodes = new TreeNode[list.size()];
//        for (int i = 0; i < list.size(); ++i) {
//            treeNodes[i] = new ResourceTreeNode(list.get(i));
//        }
//        return treeNodes;
//    }

    private TreeNode[] buildNodes(List<RecentlyAddedResourceComposite> list) {
        TreeNode[] treeNodes = new TreeNode[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            treeNodes[i] = new RecentlyAddedTreeNode(list.get(i));
        }
        return treeNodes;
    }

    public static class RecentlyAddedTreeNode extends TreeNode {
        private RecentlyAddedResourceComposite recentlyAdded;

        private RecentlyAddedTreeNode(RecentlyAddedResourceComposite c) {
            recentlyAdded = c;
            Date dateAdded = new Date(recentlyAdded.getCtime());

            String id = String.valueOf(recentlyAdded.getId());
            String parentId = recentlyAdded.getParentId() == 0 ? null
                    : String.valueOf((recentlyAdded.getParentId()));

            setID(id);
            setParentID(parentId);

            setAttribute("id", id);
            setAttribute("parentId", parentId);
            setAttribute("name", recentlyAdded.getName());
            setAttribute("timestamp", DateTimeFormat.getMediumDateTimeFormat().format(dateAdded));
            setIsFolder(recentlyAdded.getParentId() == 0);
        }
    }

    public static class ResourceTreeNode extends TreeNode {

        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = String.valueOf(resource.getId());
            String parentId = resource.getParentResource() == null ? null
                    : String.valueOf((resource.getParentResource().getId()));

            setID(id);
            setParentID(parentId);

            setAttribute("id", id);
            setAttribute("parentId", parentId);
            setAttribute("name", resource.getName());
            setAttribute("timestamp", "");//String.valueOf(resource.getItime())); // Seems to be null
            setAttribute("currentAvailability",
                    resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP
                            ? "/images/icons/availability_green_16.png"
                            : "/images/icons/availability_red_16.png");
        }

        public Resource getResource() {
            return resource;
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        public ResourceType getResourceType() {
            return resource.getResourceType();
        }

        public String getParentId() {
            return getAttribute("parentId");
        }
    }
}
