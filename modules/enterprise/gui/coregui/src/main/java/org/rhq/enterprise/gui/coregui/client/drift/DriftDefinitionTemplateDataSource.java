/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.DriftDefinition.BaseDirectory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.templates.DriftDefinitionTemplateTypeView;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class DriftDefinitionTemplateDataSource extends
    RPCDataSource<DriftDefinitionTemplate, DriftDefinitionTemplateCriteria> {

    // columns
    public static final String ATTR_BASE_DIR_STRING = "baseDirString";
    public static final String ATTR_DEFINED_BY = "definedBy";
    public static final String ATTR_DRIFT_HANDLING_MODE = "driftHandlingMode";
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_EDIT = "edit";
    public static final String ATTR_INTERVAL = "interval";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_NUM_DEFINITIONS = "numDefinitions";
    public static final String ATTR_PINNED = "pinned";

    // non-columns
    // public static final String ATTR_ENTITY = "object";
    public static final String ATTR_ID = "id";
    public static final String ATTR_IS_PINNED = "isPinned";
    public static final String ATTR_IS_USER_DEFINED = "isUserDefined";

    public static final String DRIFT_HANDLING_MODE_NORMAL = MSG.view_drift_table_driftHandlingMode_normal();
    public static final String DRIFT_HANDLING_MODE_PLANNED = MSG.view_drift_table_driftHandlingMode_plannedChanges();

    private int resourceTypeId;
    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

    public DriftDefinitionTemplateDataSource(int resourceTypeId) {
        this.resourceTypeId = resourceTypeId;

        addDataSourceFields();
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField nameField = new ListGridField(ATTR_NAME, MSG.common_title_name());
        fields.add(nameField);

        ListGridField definedByField = new ListGridField(ATTR_DEFINED_BY, MSG.view_adminTemplates_definedBy());
        fields.add(definedByField);

        ListGridField numDefinitionsField = new ListGridField(ATTR_NUM_DEFINITIONS, MSG.common_title_definitions());
        fields.add(numDefinitionsField);

        ListGridField pinnedField = new ListGridField(ATTR_PINNED, MSG.view_drift_table_pinned());
        pinnedField.setType(ListGridFieldType.IMAGE);
        pinnedField.setAlign(Alignment.CENTER);
        pinnedField.addRecordClickHandler(new RecordClickHandler() {

            public void onRecordClick(RecordClickEvent event) {

                if (event.getRecord().getAttributeAsBoolean(ATTR_IS_PINNED)) {
                    CoreGUI.goToView(LinkManager.getAdminTemplatesEditLink(DriftDefinitionTemplateTypeView.VIEW_ID
                        .getName(), String.valueOf(resourceTypeId))
                        + "/" + event.getRecord().getAttribute(ATTR_ID));
                }
            }
        });
        fields.add(pinnedField);

        ListGridField enabledField = new ListGridField(ATTR_ENABLED, MSG.common_title_enabled());
        enabledField.setType(ListGridFieldType.IMAGE);
        enabledField.setAlign(Alignment.CENTER);
        fields.add(enabledField);

        ListGridField driftHandlingModeField = new ListGridField(ATTR_DRIFT_HANDLING_MODE, MSG
            .view_drift_table_driftHandlingMode());
        fields.add(driftHandlingModeField);

        ListGridField intervalField = new ListGridField(ATTR_INTERVAL, MSG.common_title_interval());
        fields.add(intervalField);

        ListGridField baseDirField = new ListGridField(ATTR_BASE_DIR_STRING, MSG.view_drift_table_baseDir());
        // can't sort on this because it's not an entity field, it's derived from the config only
        baseDirField.setCanSort(false);
        fields.add(baseDirField);

        ListGridField editField = new ListGridField(ATTR_EDIT, MSG.common_title_edit());
        editField.setType(ListGridFieldType.IMAGE);
        editField.setAlign(Alignment.CENTER);
        editField.addRecordClickHandler(new RecordClickHandler() {

            public void onRecordClick(RecordClickEvent event) {
                CoreGUI.goToView(LinkManager.getAdminTemplatesEditLink(DriftDefinitionTemplateTypeView.VIEW_ID
                    .getName(), String.valueOf(resourceTypeId))
                    + "/" + event.getRecord().getAttribute(ATTR_ID) + "/Edit");
            }
        });
        fields.add(editField);

        nameField.setWidth("20%");
        definedByField.setWidth("10%");
        numDefinitionsField.setWidth(70);
        pinnedField.setWidth(70);
        enabledField.setWidth(60);
        driftHandlingModeField.setWidth("10%");
        intervalField.setWidth(70);
        baseDirField.setWidth("*");
        editField.setWidth(70);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final DriftDefinitionTemplateCriteria criteria) {

        driftService.findDriftDefinitionTemplatesByCriteria(criteria,
            new AsyncCallback<PageList<DriftDefinitionTemplate>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<DriftDefinitionTemplate> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<DriftDefinitionTemplate> result, final DSResponse response,
        final DSRequest request) {

        response.setData(buildRecords(result));
        response.setTotalRows(result.getTotalSize());
        processResponse(request.getRequestId(), response);
    }

    /**
     * Sub-classes can override this to add fine-grained control over the result set size. By default the
     * total rows are set to the total result set for the query, allowing proper paging.  But some views (portlets)
     * may want to limit results to a small set (like most recent).  
     * @param result
     * @param response
     * @param request
     * 
     * @return should not exceed result.size(). 
     */
    protected int getTotalRows(final Collection<DriftDefinitionTemplate> result, final DSResponse response,
        final DSRequest request) {
        return result.size();
    }

    @Override
    protected DriftDefinitionTemplateCriteria getFetchCriteria(DSRequest request) {

        DriftDefinitionTemplateCriteria criteria = new DriftDefinitionTemplateCriteria();
        criteria.addFilterResourceTypeId(resourceTypeId);

        // I'm fetching these only to get the count of defs for the template. If it ends up being too slow
        // then we'll probably need to create a criteria method that returns a composite.
        criteria.fetchDriftDefinitions(true);
        criteria.setPageControl(getPageControl(request));

        return criteria;
    }

    @Override
    public DriftDefinitionTemplate copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(DriftDefinitionTemplate from) {
        return convert(from);
    }

    public static ListGridRecord convert(DriftDefinitionTemplate from) {
        ListGridRecord record = new ListGridRecord();
        DriftDefinition templateDef = from.getTemplateDefinition();

        // column attrs

        record.setAttribute(ATTR_NAME, from.getName());
        record
            .setAttribute(ATTR_DEFINED_BY, from.isUserDefined() ? MSG.common_title_user() : MSG.common_title_plugin());
        record.setAttribute(ATTR_NUM_DEFINITIONS, String.valueOf(from.getDriftDefinitions().size()));
        record.setAttribute(ATTR_PINNED, templateDef.isPinned() ? ImageManager.getPinnedIcon() : ImageManager
            .getUnpinnedIcon());
        record.setAttribute(ATTR_ENABLED, ImageManager.getAvailabilityIcon(templateDef.isEnabled()));
        record.setAttribute(ATTR_DRIFT_HANDLING_MODE, getDriftHandlingModeDisplayName(templateDef
            .getDriftHandlingMode()));
        record.setAttribute(ATTR_INTERVAL, String.valueOf(templateDef.getInterval()));
        record.setAttribute(ATTR_BASE_DIR_STRING, getBaseDirString(templateDef.getBasedir()));
        // fixed value, just the edit icon
        record.setAttribute(ATTR_EDIT, ImageManager.getEditIcon());

        // non-column attrs used in processing

        // don't store the entity unless necessary, it's just overhead
        // record.setAttribute(ATTR_ENTITY, from);
        record.setAttribute(ATTR_ID, from.getId());
        record.setAttribute(ATTR_IS_PINNED, templateDef.isPinned());
        record.setAttribute(ATTR_IS_USER_DEFINED, from.isUserDefined());

        return record;
    }

    public static String getDriftHandlingModeDisplayName(DriftHandlingMode driftHandlingMode) {
        switch (driftHandlingMode) {
        case plannedChanges:
            return DRIFT_HANDLING_MODE_PLANNED;

        default:
            return DRIFT_HANDLING_MODE_NORMAL;
        }
    }

    private static String getBaseDirString(BaseDirectory basedir) {
        return basedir.getValueContext() + ":" + basedir.getValueName();
    }

}