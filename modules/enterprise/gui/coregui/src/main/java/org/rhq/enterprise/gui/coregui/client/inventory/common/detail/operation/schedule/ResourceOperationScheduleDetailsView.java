/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.trigger.JobTriggerEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class ResourceOperationScheduleDetailsView extends AbstractRecordEditor {

    private static final String FIELD_OPERATION_DESCRIPTION = "operationDescription";
    private static final String FIELD_OPERATION_PARAMETERS = "operationParameters";

    private ResourceComposite resourceComposite;
    private Map<String, String> operationNameToDescriptionMap = new HashMap<String, String>();
    private Map<String, ConfigurationDefinition> operationNameToParametersDefinitionMap =
        new HashMap<String, ConfigurationDefinition>();
    private StaticTextItem operationDescriptionItem;
    private StaticTextItem operationParametersItem;
    private LocatableHLayout operationParametersConfigurationHolder;
    private JobTriggerEditor triggerEditor;
    private Configuration parameters;
    private EnhancedDynamicForm notesForm;

    public ResourceOperationScheduleDetailsView(String locatorId, ResourceComposite resourceComposite, int scheduleId) {
        super(locatorId, new ResourceOperationScheduleDataSource(resourceComposite), scheduleId, "Scheduled Operation", null);

        this.resourceComposite = resourceComposite;
        ResourceType resourceType = this.resourceComposite.getResource().getResourceType();
        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            this.operationNameToDescriptionMap.put(operationDefinition.getName(), operationDefinition.getDescription());
            this.operationNameToParametersDefinitionMap.put(operationDefinition.getName(),
                operationDefinition.getParametersConfigurationDefinition());
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        super.renderView(viewPath);

        // Existing schedules are not editable. This may change in the future.
        boolean isReadOnly = (getRecordId() != 0);
        init(isReadOnly);
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        SelectItem operationNameItem = new SelectItem(ResourceOperationScheduleDataSource.Field.OPERATION_NAME);
        items.add(operationNameItem);
        operationNameItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                refreshOperationDescriptionItem();
                refreshOperationParametersItem();
            }
        });

        this.operationDescriptionItem = new StaticTextItem(FIELD_OPERATION_DESCRIPTION, "Operation Description");
        items.add(this.operationDescriptionItem);

        this.operationParametersItem = new StaticTextItem(FIELD_OPERATION_PARAMETERS, "Operation Parameters");
        items.add(this.operationParametersItem);

        return items;
    }

    @Override
    protected LocatableVLayout buildContentPane() {
        LocatableVLayout contentPane = super.buildContentPane();

        this.operationParametersConfigurationHolder = new LocatableHLayout(extendLocatorId("ConfigHolder"));
        this.operationParametersConfigurationHolder.setVisible(false);
        contentPane.addMember(this.operationParametersConfigurationHolder);

        HTMLFlow hr = new HTMLFlow("<p/><hr/><p/>");
        contentPane.addMember(hr);

        if (isNewRecord()) {
            this.triggerEditor = new JobTriggerEditor(extendLocatorId("TriggerEditor"));
            contentPane.addMember(this.triggerEditor);
            hr = new HTMLFlow("<p/><hr/><p/>");
            contentPane.addMember(hr);
        }

        this.notesForm = new EnhancedDynamicForm(extendLocatorId("NotesForm"), isReadOnly(),
            isNewRecord());
        this.notesForm.setWidth100();
        TextAreaItem notesItem = new TextAreaItem(ResourceOperationScheduleDataSource.Field.DESCRIPTION, "Notes");
        notesItem.setWidth(450);
        notesItem.setHeight(150);
        this.notesForm.setFields(notesItem);
        contentPane.addMember(this.notesForm);

        return contentPane;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        refreshOperationDescriptionItem();
        refreshOperationParametersItem();
    }

    @Override
    protected String getTitleFieldName() {
        return ResourceOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME;
    }

    @Override
    protected Record createNewRecord() {
        Record record = super.createNewRecord();
        Subject sessionSubject = UserSessionManager.getSessionSubject();
        record.setAttribute(ResourceOperationScheduleDataSource.Field.SUBJECT, sessionSubject);
        return record;
    }

    @Override
    protected void editRecord(Record record) {
        refreshOperationDescriptionItem();
        refreshOperationParametersItem();

        FormItem notesItem = this.notesForm.getField(ResourceOperationScheduleDataSource.Field.DESCRIPTION);
        notesItem.setValue(getForm().getValue(ResourceOperationScheduleDataSource.Field.DESCRIPTION));

        super.editRecord(record);
    }

    @Override
    protected void save(DSRequest requestProperties) {
        requestProperties.setAttribute("parameters", this.parameters);

        if (!this.triggerEditor.validate()) {
            // TODO: print error Message
            return;
        }
        EnhancedDynamicForm form = getForm();

        Record jobTriggerRecord = new ListGridRecord();

        Date startTime = this.triggerEditor.getStartTime();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.START_TIME, startTime);

        Date endTime = this.triggerEditor.getEndTime();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.END_TIME, endTime);

        Integer repeatCount = this.triggerEditor.getRepeatCount();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.REPEAT_COUNT, repeatCount);

        Long repeatInterval = this.triggerEditor.getRepeatInterval();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.REPEAT_INTERVAL, repeatInterval);

        String cronExpression = this.triggerEditor.getCronExpression();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.CRON_EXPRESSION, cronExpression);

        form.setValue("jobTrigger", jobTriggerRecord);

        FormItem notesItem = this.notesForm.getField(ResourceOperationScheduleDataSource.Field.DESCRIPTION);
        form.setValue(ResourceOperationScheduleDataSource.Field.DESCRIPTION, (String)notesItem.getValue());

        super.save(requestProperties);
    }

    private void refreshOperationDescriptionItem() {
        String operationName = getSelectedOperationName();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation.</i>";
        } else {
            value = this.operationNameToDescriptionMap.get(operationName);
        }
        this.operationDescriptionItem.setValue(value);
    }

    private void refreshOperationParametersItem() {
        String operationName = getSelectedOperationName();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation.</i>";
        } else {
            ConfigurationDefinition parametersDefinition = this.operationNameToParametersDefinitionMap.get(operationName);
            if (parametersDefinition == null || parametersDefinition.getPropertyDefinitions().isEmpty()) {
                value = "<i>" + MSG.view_operationCreateWizard_parametersStep_noParameters() + "</i>";

                for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                    child.destroy();
                }
                this.operationParametersConfigurationHolder.hide();
            } else {
                value = isNewRecord() ? "<i>Enter parameters below...</i>" : "";

                for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                    child.destroy();
                }
                Configuration defaultConfiguration = (parametersDefinition.getDefaultTemplate() != null) ?
                    parametersDefinition.getDefaultTemplate().createConfiguration() : new Configuration();
                ConfigurationEditor configurationEditor = new ConfigurationEditor("ParametersEditor", parametersDefinition,
                    defaultConfiguration);
                configurationEditor.setReadOnly(isReadOnly());
                this.parameters = configurationEditor.getConfiguration();
                this.operationParametersConfigurationHolder.addMember(configurationEditor);
                this.operationParametersConfigurationHolder.show();
            }
        }
        this.operationParametersItem.setValue(value);
    }

    private String getSelectedOperationName() {
        FormItem operationNameItem = getForm().getField(ResourceOperationScheduleDataSource.Field.OPERATION_NAME);
        return (String)operationNameItem.getValue();
    }

}
