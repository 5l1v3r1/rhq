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

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.tree.TreeNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

/**
 * This doesn't extend RPCDataSource because it is tree oriented and
 * behaves different from normal list data sources in some places.
 * @author Greg Hinkle
 */
public class ResourceTreeDatasource extends DataSource {


    private boolean initialized = false;
    int rootId;
    List<Resource> initialData;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceTreeDatasource(List<Resource> initialData) {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        this.initialData = initialData;

        this.rootId = rootId;

        DataSourceField idDataField = new DataSourceTextField("id", "ID");
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name");
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceImageField availabilityDataField = new DataSourceImageField("currentAvailability", "Availability");
        descriptionDataField.setCanEdit(false);

        DataSourceTextField parentIdField = new DataSourceTextField("parentId", "Parent ID");
        parentIdField.setForeignKey("id");
//        parentIdField.setRootValue(rootId);

//        DataSourceTextField parentKeyField = new DataSourceTextField("parentKey", "Parent KEY");
//        parentKeyField.setForeignKey("id");
//        parentKeyField.setRootValue(rootId);


//        nameDataField.setType(FieldType.);

        setDropExtraFields(false);

        setFields(idDataField, nameDataField, descriptionDataField, availabilityDataField);
    }


    @Override
    protected Object transformRequest(DSRequest request) {
        String requestId = request.getRequestId();
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Asume success
        response.setStatus(0);
        switch (request.getOperationType()) {
            case ADD:
                //executeAdd(lstRec, true);
                break;
            case FETCH:
                executeFetch(requestId, request, response);
                break;
            case REMOVE:
                //executeRemove(lstRec);
                break;
            case UPDATE:
                //executeAdd(lstRec, false);
                break;

            default:
                break;
        }

        return request.getData();
    }

    public void executeFetch(final String requestId, final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();


        String p = request.getCriteria().getAttribute("parentId");
//        System.out.println("All attributes: " + Arrays.toString(request.getCriteria().getAttributes()));

        ResourceCriteria criteria = new ResourceCriteria();

        if (p == null) {
            System.out.println("DataSourceTree: Loading initial data");

//            criteria.addFilterId(rootId);

            processIncomingData(initialData, response, requestId);
            return;

        } else {
            System.out.println("DataSourceTree: Loading " + p);

            criteria.addFilterParentResourceId(Integer.parseInt(p));
        }

        // The server is already eager fetch resource type
        // * criteria.fetchResourceType(true);


        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch resources");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(requestId, response);
            }

            public void onSuccess(PageList<Resource> result) {
                processIncomingData(result, response, requestId);
            }
        });
    }


    private void processIncomingData(List<Resource> result, final DSResponse response, final String requestId) {

        ResourceTypeRepository.Cache.getInstance().loadResourceTypes(result,
                EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children),
                new ResourceTypeRepository.ResourceTypeLoadedCallback() {
                    public void onResourceTypeLoaded(List<Resource> result) {
                        response.setData(build(result));
                        processResponse(requestId, response);

                    }
                });
    }


    /**
     * Construct a set of TreeNodes from a list of resources
     *
     * @param resources
     * @return
     */
    public static TreeNode[] build(List<Resource> resources) {
        ResourceTreeNode[] records = new ResourceTreeNode[resources.size()];
        for (int x = 0; x < resources.size(); x++) {
            Resource res = resources.get(x);
            ResourceTreeNode record = new ResourceTreeNode(res);
            records[x] = record;
        }

        return introduceTypeFolders(records);
    }


    private static TreeNode[] introduceTypeFolders(ResourceTreeNode[] nodes) {
        ArrayList<TreeNode> built = new ArrayList<TreeNode>();
        HashMap<ResourceType, TypeTreeNode> types = new HashMap<ResourceType, TypeTreeNode>();


        for (ResourceTreeNode node : nodes) {
            built.add(node);

            if (!types.containsKey(node.getResourceType()) && node.getResourceType().getCategory() != ResourceCategory.PLATFORM) {
                TypeTreeNode typeNode = new TypeTreeNode(String.valueOf(node.getResource().getParentResource().getId()), node.getResourceType().getName());
                built.add(typeNode);
                types.put(node.getResourceType(), typeNode);
            }
        }


        return built.toArray(new TreeNode[built.size()]);

    }


    private static boolean sameTypes(ResourceTreeNode[] nodes) {
        ResourceType first = nodes[0].getResourceType();
        for (ResourceTreeNode node : nodes) {
            if (!first.equals(node)) {
                return false;
            }
        }
        return true;
    }


    public static class TypeTreeNode extends TreeNode {


        private TypeTreeNode(String parentId, String type) {
            setID(parentId + "_" + type);
            setParentID(parentId);

            setAttribute("id", parentId + "_" + type);
            setAttribute("parentId", parentId);
//            setAttribute("parentKey", parentId);
            setAttribute("name", type);
        }

    }


    public static class ResourceTreeNode extends TreeNode {

        private Resource resource;

        private ResourceTreeNode(Resource resource) {
            this.resource = resource;

            String id = String.valueOf(resource.getId());
            String parentId = resource.getParentResource() == null ? null : (resource.getParentResource().getId() + "_" + resource.getResourceType().getName());

//            System.out.println(id + " / " + parentId);

            setID(id);
            setParentID(parentId);

            setAttribute("id", id);
            setAttribute("parentId", parentId);

//            setAttribute("parentKey", resource.getParentResource() == null ? 0 : (resource.getParentResource().getId() + resource.getResourceType().getName()));

            setAttribute("name", resource.getName());
            setAttribute("description", resource.getDescription());
            setAttribute("currentAvailability",
                    resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP
                            ? "/images/icons/availability_green_16.png"
                            : "/images/icons/availability_red_16.png");

            setIsFolder((resource.getResourceType().getChildResourceTypes() != null &&
                    !resource.getResourceType().getChildResourceTypes().isEmpty()));
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
