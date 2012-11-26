/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.topology;

import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_ADDRESS;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_AGENT_COUNT;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_ID;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_MTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_NAME;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerWithAgentCountDatasourceField.FIELD_SECURE_PORT;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jirka Kremser
 *
 */
public class ServerWithAgentCountDatasource extends ServerDatasource<ServerWithAgentCountComposite, Criteria> {

    public ServerWithAgentCountDatasource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();
        DataSourceField idField = new DataSourceIntegerField(FIELD_ID.propertyName(), FIELD_ID.title(), 50);
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);
        return fields;
    }

    public List<ListGridField> getListGridFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = FIELD_ID.getListGridField();
        idField.setHidden(true);
        fields.add(idField);

        fields.add(FIELD_NAME.getListGridField("*"));
        fields.add(FIELD_OPERATION_MODE.getListGridField("90"));
        fields.add(FIELD_ADDRESS.getListGridField("110"));
        fields.add(FIELD_PORT.getListGridField("90"));
        fields.add(FIELD_SECURE_PORT.getListGridField("75"));

        ListGridField lastUpdateTimeField = FIELD_MTIME.getListGridField("110");
        TimestampCellFormatter.prepareDateField(lastUpdateTimeField);
        fields.add(lastUpdateTimeField);

        fields.add(FIELD_AFFINITY_GROUP.getListGridField("80"));
        fields.add(FIELD_AGENT_COUNT.getListGridField("75"));

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
        final PageControl pc = getPageControl(request);

        GWTServiceLookup.getCloudService().getServers(pc, new AsyncCallback<List<ServerWithAgentCountComposite>>() {
            public void onSuccess(List<ServerWithAgentCountComposite> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.size());
                processResponse(request.getRequestId(), response);
            }

            @Override
            public void onFailure(Throwable t) {
                //todo: CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), t);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.         
        PageControl pageControl = new PageControl(0, getDataPageSize());

        // Initialize sorting.
        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");
            for (String sort : sorts) {
                PageOrdering ordering = (sort.startsWith("-")) ? PageOrdering.DESC : PageOrdering.ASC;
                String columnName = (ordering == PageOrdering.DESC) ? sort.substring(1) : sort;
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }

        return pageControl;
    }

    @Override
    public ServerWithAgentCountComposite copyValues(Record from) {
        throw new UnsupportedOperationException("ServerTableView.CloudDataSourcepublic Server copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(ServerWithAgentCountComposite from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_ID.propertyName(), from.getServer().getId());
        record.setAttribute(FIELD_NAME.propertyName(), from.getServer().getName());
        record.setAttribute(FIELD_OPERATION_MODE.propertyName(), from.getServer().getOperationMode());
        record.setAttribute(FIELD_ADDRESS.propertyName(), from.getServer().getAddress());
        record.setAttribute(FIELD_PORT.propertyName(), from.getServer().getPort());
        record.setAttribute(FIELD_SECURE_PORT.propertyName(), from.getServer().getSecurePort());
        record.setAttribute(FIELD_MTIME.propertyName(), from.getServer().getMtime());
        record.setAttribute(FIELD_AFFINITY_GROUP.propertyName(), from.getServer().getAffinityGroup() == null ? ""
            : from.getServer().getAffinityGroup().getName());
        record.setAttribute(FIELD_AGENT_COUNT.propertyName(), from.getAgentCount());
        return record;
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criteria for this datasource, just return null
        return null;
    }
}
