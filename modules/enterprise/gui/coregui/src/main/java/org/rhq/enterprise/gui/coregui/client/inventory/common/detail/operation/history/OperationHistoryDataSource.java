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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public abstract class OperationHistoryDataSource extends RPCDataSource<ResourceOperationHistory> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String OPERATION_NAME = "operationName";
        public static final String RESOURCE = "resource";
        public static final String STATUS = "status";
        public static final String STARTED_TIME = "startedTime";
        public static final String CREATED_TIME = "createdTime";
        public static final String DURATION = "duration";
        public static final String SUBJECT = "subject";
        public static final String OPERATION_DEFINITION = "operationDefinition";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String PARAMETERS = "parameters";
    }

    protected OperationGWTServiceAsync operationService = GWTServiceLookup.getOperationService();

    public OperationHistoryDataSource() {
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        /*
        DataSourceIntegerField idField = new DataSourceIntegerField(Field.ID);
        idField.setPrimaryKey(true);
        fields.add(idField);
        */

        DataSourceTextField nameField = new DataSourceTextField(Field.OPERATION_NAME);
        fields.add(nameField);

        /*
        DataSourceTextField resourceField = new DataSourceTextField(Field.RESOURCE);
        fields.add(resourceField);

        DataSourceTextField statusField = new DataSourceTextField(Field.STATUS);
        fields.add(statusField);

        DataSourceTextField startedField = new DataSourceTextField(Field.STARTED_TIME);
        startedField.setType(FieldType.DATETIME);
        fields.add(startedField);
        */

        return fields;
    }

    @Override
    public ResourceOperationHistory copyValues(Record from) {
        throw new UnsupportedOperationException("ResourceOperationHistory is read only.");
    }

    @Override
    public ListGridRecord copyValues(ResourceOperationHistory from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(Field.ID, from.getId());
        record.setAttribute(Field.CREATED_TIME, from.getCreatedTime());
        record.setAttribute(Field.STARTED_TIME, new Date(from.getStartedTime()));
        record.setAttribute(Field.DURATION, from.getDuration());
        record.setAttribute(Field.RESOURCE, from.getResource());
        record.setAttribute(Field.SUBJECT, from.getSubjectName());
        record.setAttribute(Field.OPERATION_DEFINITION, from.getOperationDefinition());
        record.setAttribute(Field.OPERATION_NAME, from.getOperationDefinition().getDisplayName());
        record.setAttribute(Field.ERROR_MESSAGE, from.getErrorMessage());
        record.setAttribute(Field.STATUS, from.getStatus().name());
        record.setAttribute(Field.PARAMETERS, from.getParameters());

        return record;
    }

}
