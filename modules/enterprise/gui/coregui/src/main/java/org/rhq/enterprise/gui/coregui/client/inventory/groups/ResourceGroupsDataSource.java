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
package org.rhq.enterprise.gui.coregui.client.inventory.groups;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupsDataSource extends RPCDataSource<ResourceGroup> {

    private String query;

    private ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

    private static ResourceGroupsDataSource INSTANCE;

    public static ResourceGroupsDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceGroupsDataSource();
        }
        return INSTANCE;
    }

    public ResourceGroupsDataSource() {
//        super("ResourceGroups");

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID", 20);
        idDataField.setPrimaryKey(true);

        DataSourceTextField nameDataField = new DataSourceTextField("name", "Name", 200);
        nameDataField.setCanEdit(false);

        DataSourceTextField descriptionDataField = new DataSourceTextField("description", "Description");
        descriptionDataField.setCanEdit(false);

        DataSourceTextField typeNameDataField = new DataSourceTextField("typeName", "Type");
        DataSourceTextField pluginNameDataField = new DataSourceTextField("pluginName", "Plugin");
        DataSourceTextField categoryDataField = new DataSourceTextField("category", "Category");

        setFields(idDataField, nameDataField, descriptionDataField, typeNameDataField, pluginNameDataField,
            categoryDataField);
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.setPageControl(getPageControl(request));
        criteria.addFilterName(query);

        if (request.getCriteria().getValues().get("category") != null) {
            criteria.addFilterGroupCategory(GroupCategory.valueOf(((String) request.getCriteria().getValues().get(
                "category")).toUpperCase()));
        }

        if (request.getCriteria().getValues().get("downMemberCount") != null) {
            criteria.addFilterDownMemberCount(Integer.parseInt((String) request.getCriteria().getValues().get(
                "downMemberCount")));
        }

        groupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load groups", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<ResourceGroup> result) {
                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    public ResourceGroup copyValues(ListGridRecord from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(ResourceGroup from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("group", from);
        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("groupCategory", from.getGroupCategory());

        if (from.getResourceType() != null) {
            record.setAttribute("resourceType", from.getResourceType());
            record.setAttribute("typeName", from.getResourceType().getName());
            record.setAttribute("pluginName", from.getResourceType().getPlugin());
        }

        return record;
    }

}