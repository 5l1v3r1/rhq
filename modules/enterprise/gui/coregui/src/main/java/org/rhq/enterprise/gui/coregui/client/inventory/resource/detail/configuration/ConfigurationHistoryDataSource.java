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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import java.util.Date;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryDataSource extends RPCDataSource<ResourceConfigurationUpdate> {

    private ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();


    public ConfigurationHistoryDataSource() {
        super();

        // id, resource, subject, configuration, createdTime, duration, errorMessage, modifiedTime, status


        DataSourceIntegerField idField = new DataSourceIntegerField("id");
        idField.setPrimaryKey(true);
        addField(idField);

        DataSourceTextField submittedField = new DataSourceTextField("createdTime");
        submittedField.setType(FieldType.DATETIME);
        addField(submittedField);

        DataSourceTextField statusField = new DataSourceTextField("status");
        addField(statusField);

        DataSourceTextField subjectField = new DataSourceTextField("subject");
        addField(subjectField);


    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        int resourceId = Integer.parseInt((String) request.getCriteria().getValues().get("resourceId"));

        configurationService.findResourceConfigurationUpdates(resourceId, new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Unable to load configuration history",caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(),response);
            }

            public void onSuccess(PageList<ResourceConfigurationUpdate> result) {
                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }





    @Override
    public ResourceConfigurationUpdate copyValues(ListGridRecord from) {
       return null;  // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(ResourceConfigurationUpdate from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());
        record.setAttribute("resource",from.getResource());
        record.setAttribute("subject",from.getSubjectName());
        record.setAttribute("configuration", from.getConfiguration());
        record.setAttribute("createdTime",new Date(from.getCreatedTime()));
        record.setAttribute("duration", from.getDuration());
        record.setAttribute("errorMessage", from.getErrorMessage());
        record.setAttribute("modifiedTime", new Date(from.getModifiedTime()));
        record.setAttribute("status",from.getStatus().name());

        record.setAttribute("entity",from);
        return record;
    }
}
