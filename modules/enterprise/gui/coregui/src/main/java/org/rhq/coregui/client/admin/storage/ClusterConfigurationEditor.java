/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.storage;

import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RHQ_REPLICATION_FACTOR;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_SYSTEM_AUTH_REPLICATION_FACTOR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.components.form.StringLengthValidator;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * The component for editing the cluster wide configuration
 *
 * @author Jirka Kremser
 */
public class ClusterConfigurationEditor extends EnhancedVLayout implements RefreshableView {

    private EnhancedDynamicForm schemaForm;
    private EnhancedDynamicForm clusterForm;
    private EnhancedDynamicForm deploymentForm;
    private EnhancedDynamicForm credentialsForm;
    private EnhancedDynamicForm regularSnapshotsForm;
    private EnhancedIButton saveButton;
    private StorageClusterSettings settings;
    private final boolean readOnly;

    private static String FIELD_GOSSIP_PORT = "gossip_port";
    private static String FIELD_AUTOMATIC_DEPLOYMENT = "automatic_deployment";
    private static String FIELD_USERNAME = "username";
    private static String FIELD_PASSWORD = "password";
    private static String FIELD_PASSWORD_VERIFY = "verify_password";
    private static String FIELD_SNAPSHOTS_ENABLED = "snapshots_enabled";
    private static String FIELD_SNAPSHOTS_SCHEDULE = "snapshots_schedule";
    private static String FIELD_SNAPSHOTS_RETENTION = "snapshots_retention";
    private static String FIELD_SNAPSHOTS_COUNT = "snapshots_count";
    private static String FIELD_SNAPSHOTS_DELETION = "snapshots_deletion";
    private static String FIELD_SNAPSHOTS_LOCATION = "snapshots_location";

    public ClusterConfigurationEditor(boolean readOnly) {
        super();
        this.readOnly = readOnly;
    }

    private void fetchClusterSettings() {
        GWTServiceLookup.getStorageService().retrieveClusterSettings(new AsyncCallback<StorageClusterSettings>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_storageNodes_clusterSettings_message_cantLoad() + caught.getMessage(),
                    caught);
            }

            @Override
            public void onSuccess(StorageClusterSettings settings) {
                ClusterConfigurationEditor.this.settings = settings;
                prepareForms();
            }
        });
    }

    private void save() {
        updateSettings();
        GWTServiceLookup.getStorageService().updateClusterSettings(settings, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                Message msg = new Message(MSG.view_adminTopology_storageNodes_clusterSettings_message_updateSuccess(),
                    Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(msg);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_storageNodes_clusterSettings_message_updateFail(), caught);
            }
        });
    }

    private List<FormItem> buildHeaderItems() {
        List<FormItem> fields = new ArrayList<FormItem>();
        fields.add(createHeaderTextItem(MSG.view_configEdit_property()));
        fields.add(createHeaderTextItem(MSG.common_title_value()));
        fields.add(createHeaderTextItem(MSG.common_title_description()));
        return fields;
    }

    private StaticTextItem createHeaderTextItem(String value) {
        StaticTextItem unsetHeader = new StaticTextItem();
        unsetHeader.setValue(value);
        unsetHeader.setShowTitle(false);
        unsetHeader.setCellStyle("configurationEditorHeaderCell");
        return unsetHeader;
    }

    private EnhancedDynamicForm buildForm(String groupTitle) {
        EnhancedDynamicForm form = new EnhancedDynamicForm();
        form.setHiliteRequiredFields(true);
        form.setNumCols(3);
        form.setCellPadding(5);
        form.setColWidths(190, 220, "*");
        form.setIsGroup(true);
        form.setGroupTitle(groupTitle);
        form.setWidth100();
        form.setOverflow(Overflow.VISIBLE);
        form.setExtraSpace(15);
        return form;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        refresh();
    }

    private void prepareForms() {
        setWidth100();
        schemaForm = buildForm("<div align='left' class='storageConfig'><div>" +
            MSG.view_adminTopology_storageNodes_clusterSettings_replication_settings() + "</div>" + "<div>" +
            MSG.view_adminTopology_storageNodes_clusterSettings_replication_settings_desc() + "</div>");
        FormItemBuilder builder = new FormItemBuilder();
        List<FormItem> rhqSchemaItems = builder.withName(FIELD_RHQ_REPLICATION_FACTOR.propertyName())
            .withTitle(FIELD_RHQ_REPLICATION_FACTOR.title())
            .withValue(Integer.toString(settings.getRhqReplicationFactor()))
            .withDescription(MSG.view_adminTopology_storageNodes_field_rhq_replication_factor_desc())
            .withReadOnlySetTo(true)
            .build();
        List<FormItem> systemAuthSchemaItems = builder.withName(FIELD_SYSTEM_AUTH_REPLICATION_FACTOR.propertyName())
            .withTitle(FIELD_SYSTEM_AUTH_REPLICATION_FACTOR.title())
            .withValue(Integer.toString(settings.getSystemAuthReplicationFactor()))
            .withDescription(MSG.view_adminTopology_storageNodes_field_system_auth_replication_factor_desc())
            .withReadOnlySetTo(true)
            .build();

        List<FormItem> items = buildHeaderItems();
        items.addAll(rhqSchemaItems);
        items.addAll(systemAuthSchemaItems);
        schemaForm.setFields(items.toArray(new FormItem[items.size()]));

        clusterForm = buildForm("<div align='left' class='storageConfig'><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_clusterSettings() + "</div><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_clusterSettings_desc() + "</div>");

        items = buildHeaderItems();
        Validator validator = new IsIntegerValidator();

        // cql port field
        builder = new FormItemBuilder();
        List<FormItem> cqlPortItems = builder.withName(FIELD_CQL_PORT.propertyName()).withTitle(FIELD_CQL_PORT.title())
            .withValue(String.valueOf(settings.getCqlPort()))
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_clusterSettings_cqlPort())
            .withValidators(validator).build();
        items.addAll(cqlPortItems);

        // gossip port field
        builder = new FormItemBuilder();
        List<FormItem> gossipPortItems = builder.withName(FIELD_GOSSIP_PORT)
            .withTitle(MSG.view_adminTopology_storageNodes_field_gossipPort())
            .withValue(String.valueOf(settings.getGossipPort()))
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_clusterSettings_gossipPort())
            .withValidators(validator).build();
        items.addAll(gossipPortItems);
        clusterForm.setFields(items.toArray(new FormItem[items.size()]));

        deploymentForm = buildForm("<div align='left' class='storageConfig'><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_deployments() + "</div><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_deployments_desc() + "</div>");
        FormItemBuilder.resetOddRow();
        items = buildHeaderItems();

        // automatic deployment field
        builder = new FormItemBuilder();
        List<FormItem> automaticDeploymentItems = builder.withName(FIELD_AUTOMATIC_DEPLOYMENT)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_deployments_autoDeploy_title())
            .withValue(Boolean.toString(settings.getAutomaticDeployment()))
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_deployments_autoDeploy())
            .withReadOnlySetTo(readOnly).build((FormItem) GWT.create(RadioGroupItem.class));
        RadioGroupItem autoDeployRadio = (RadioGroupItem) automaticDeploymentItems.get(1);
        autoDeployRadio.setVertical(false);
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>(2);
        values.put("true", "On");
        values.put("false", "Off");
        autoDeployRadio.setValueMap(values);
        autoDeployRadio.setValue(settings.getAutomaticDeployment());
        items.addAll(automaticDeploymentItems);
        deploymentForm.setFields(items.toArray(new FormItem[items.size()]));

        credentialsForm = buildForm("<div align='left' class='storageConfig'><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_credentials() + "</div><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_credentials_desc() + "</div>");
        FormItemBuilder.resetOddRow();
        items = buildHeaderItems();

        // username field
        StringLengthValidator usernameValidator = new StringLengthValidator(4, 100, false);
        builder = new FormItemBuilder();
        List<FormItem> usernameItems = builder.withName(FIELD_USERNAME).withTitle(MSG.common_title_username())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_username())
            .withValue(settings.getUsername()).withReadOnlySetTo(true).withValidators(usernameValidator).build();
        items.addAll(usernameItems);

        // password field
        StringLengthValidator passwordValidator1 = new StringLengthValidator(6, 100, false);
        passwordValidator1.setErrorMessage(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_err1());
        // due to SmartGWT bug that changes focus after each input (https://code.google.com/p/smartgwt/issues/detail?id=309)
        passwordValidator1.setValidateOnChange(false);
        builder = new FormItemBuilder();
        List<FormItem> passwordItems = builder.withName(FIELD_PASSWORD).withTitle(MSG.common_title_password())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_password())
            .withValue(settings.getPasswordHash()).withReadOnlySetTo(readOnly).withValidators(passwordValidator1)
            .withAttribute("autocomplete", "off").build((FormItem) GWT.create(PasswordItem.class));
        items.addAll(passwordItems);

        // password_verify field
        builder = new FormItemBuilder();
        passwordValidator1 = new StringLengthValidator(6, 100, false);
        passwordValidator1.setErrorMessage(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_err1());
        MatchesFieldValidator passwordValidator2 = new MatchesFieldValidator();
        passwordValidator2.setOtherField(FIELD_PASSWORD);
        passwordValidator2.setErrorMessage(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_err2());
        // due to same bug in SmartGWT as above
        passwordValidator1.setValidateOnChange(false);
        passwordValidator2.setValidateOnChange(false);
        List<FormItem> passwordVerifyItems = builder.withName(FIELD_PASSWORD_VERIFY)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_verify_title())
            .withValue(settings.getPasswordHash())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_credentials_verify())
            .withReadOnlySetTo(readOnly).withValidators(passwordValidator1, passwordValidator2)
            .withAttribute("autocomplete", "off").build((FormItem) GWT.create(PasswordItem.class));

        items.addAll(passwordVerifyItems);
        credentialsForm.setFields(items.toArray(new FormItem[items.size()]));

        validator = new IntegerRangeValidator();
        ((IntegerRangeValidator) validator).setMin(0);

        regularSnapshotsForm = buildForm("<div align='left' class='storageConfig'><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement() + "</div><div>"
            + MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_desc() + "</div>");
        FormItemBuilder.resetOddRow();
        items = buildHeaderItems();

        builder = new FormItemBuilder();
        List<FormItem> snapshotsEnabledFormItems = builder.withName(FIELD_SNAPSHOTS_ENABLED)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_enabled_title())
            .withValue(Boolean.toString(settings.getRegularSnapshots().getEnabled()))
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_enabled_desc())
            .build((FormItem) GWT.create(RadioGroupItem.class));
        RadioGroupItem snapshotsEnabledRadio = (RadioGroupItem) snapshotsEnabledFormItems.get(1);
        snapshotsEnabledRadio.setVertical(false);
        values = new LinkedHashMap<String, String>(2);
        values.put("true", "On");
        values.put("false", "Off");
        snapshotsEnabledRadio.setValueMap(values);
        snapshotsEnabledRadio.setValue(settings.getRegularSnapshots().getEnabled());
        items.addAll(snapshotsEnabledFormItems);

        List<FormItem> snapshotsScheduleFormItems = builder.withName(FIELD_SNAPSHOTS_SCHEDULE)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_schedule_title())
            .withValue(settings.getRegularSnapshots().getSchedule())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_schedule_desc())
            .build();
        items.addAll(snapshotsScheduleFormItems);

        List<FormItem> snapshotsRetentionFormItems = new FormItemBuilder().withName(FIELD_SNAPSHOTS_RETENTION)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_retention_title())
            .withValue(settings.getRegularSnapshots().getRetention())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_retention_desc())
            .build(new SelectItem());
        SelectItem snapshotsRetentionSelect = (SelectItem) snapshotsRetentionFormItems.get(1);
        snapshotsRetentionSelect.setValueMap("Keep All", "Keep Last N", "Delete Older Than N days");
        items.addAll(snapshotsRetentionFormItems);

        List<FormItem> snapshotsCountFormItems = new FormItemBuilder().withName(FIELD_SNAPSHOTS_COUNT)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_count_title())
            .withValue(String.valueOf(settings.getRegularSnapshots().getCount()))
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_count_desc())
            .withReadOnlySetTo("Keep All".equals(settings.getRegularSnapshots().getRetention()))
            .withValidators(validator).build(new IntegerItem());
        items.addAll(snapshotsCountFormItems);

        final FormItem snapshotsCountNumber = snapshotsCountFormItems.get(1);

        snapshotsRetentionSelect.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                snapshotsCountNumber.setDisabled("Keep All".equals(event.getValue()));
            }
        });

        List<FormItem> snapshotsDeletionFormItems = new FormItemBuilder().withName(FIELD_SNAPSHOTS_DELETION)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_deletion_title())
            .withValue(settings.getRegularSnapshots().getDeletion())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_deletion_desc())
            .build(new SelectItem());
        SelectItem snapshotsDeletionSelect = (SelectItem) snapshotsDeletionFormItems.get(1);
        snapshotsDeletionSelect.setValueMap("Delete", "Move");
        items.addAll(snapshotsDeletionFormItems);

        List<FormItem> snapshotsLocationFormItems = new FormItemBuilder().withName(FIELD_SNAPSHOTS_LOCATION)
            .withTitle(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_location_title())
            .withValue(settings.getRegularSnapshots().getLocation())
            .withDescription(MSG.view_adminTopology_storageNodes_clusterSettings_snapshotManagement_location_desc())
            .withRequiredSetTo(false)
            .withReadOnlySetTo("Delete".equals(settings.getRegularSnapshots().getDeletion()))
            .build();
        items.addAll(snapshotsLocationFormItems);
        final FormItem snapshotsLocationText = snapshotsLocationFormItems.get(1);

        snapshotsDeletionSelect.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                snapshotsLocationText.setDisabled("Delete".equals(event.getValue()));
            }
        });

        regularSnapshotsForm.setFields(items.toArray(new FormItem[items.size()]));

        LayoutSpacer spacer = new LayoutSpacer();
        spacer.setWidth100();

        ToolStrip toolStrip = buildToolStrip();
        setMembers(schemaForm, clusterForm, deploymentForm, credentialsForm, regularSnapshotsForm, spacer, toolStrip);
        clusterForm.validate();
        deploymentForm.validate();
        credentialsForm.validate();
        regularSnapshotsForm.validate();
        markForRedraw();
    }

    @Override
    public void refresh() {
        fetchClusterSettings();
    }

    private EnhancedToolStrip buildToolStrip() {
        saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (clusterForm.validate() && deploymentForm.validate() && credentialsForm.validate()
                    && regularSnapshotsForm.validate()) {
                    SC.ask(MSG.view_adminTopology_storageNodes_clusterSettings_message_confirmation(),
                        new BooleanCallback() {
                            @Override
                            public void execute(Boolean value) {
                                if (value) {
                                    save();
                                }
                            }
                        });
                }
            }
        });
        saveButton.setDisabled(readOnly);
        EnhancedToolStrip toolStrip = new EnhancedToolStrip();
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);
        toolStrip.addMember(saveButton);

        return toolStrip;
    }

    private StorageClusterSettings updateSettings() {
        settings.setCqlPort(Integer.parseInt(clusterForm.getValueAsString(FIELD_CQL_PORT.propertyName())));
        settings.setGossipPort(Integer.parseInt(clusterForm.getValueAsString(FIELD_GOSSIP_PORT)));

        settings.setAutomaticDeployment(Boolean.parseBoolean(deploymentForm
            .getValueAsString(FIELD_AUTOMATIC_DEPLOYMENT)));

        // set the credentials only if there was a change
        String wannabeUsername = credentialsForm.getValueAsString(FIELD_USERNAME);
        settings.setUsername(wannabeUsername.equals(settings.getUsername()) ? null : wannabeUsername);
        String wannabePassword = credentialsForm.getValueAsString(FIELD_PASSWORD);
        settings.setPasswordHash(wannabePassword.equals(settings.getPasswordHash()) ? null : wannabePassword);

        settings.getRegularSnapshots().setEnabled(
            Boolean.parseBoolean(regularSnapshotsForm.getValueAsString(FIELD_SNAPSHOTS_ENABLED)));
        settings.getRegularSnapshots().setSchedule(regularSnapshotsForm.getValueAsString(FIELD_SNAPSHOTS_SCHEDULE));
        settings.getRegularSnapshots().setRetention(regularSnapshotsForm.getValueAsString(FIELD_SNAPSHOTS_RETENTION));
        settings.getRegularSnapshots().setCount(
            Integer.parseInt(regularSnapshotsForm.getValueAsString(FIELD_SNAPSHOTS_COUNT)));
        settings.getRegularSnapshots().setDeletion(regularSnapshotsForm.getValueAsString(FIELD_SNAPSHOTS_DELETION));
        settings.getRegularSnapshots().setLocation(regularSnapshotsForm.getValueAsString(FIELD_SNAPSHOTS_LOCATION));
        return settings;
    }

    private static class FormItemBuilder {
        private String name;
        private String title;
        private String value;
        private String description;
        private Validator[] validators;
        private boolean readOnly;
        private boolean required = true;
        private Map<String, Object> attributes = new HashMap<String, Object>();

        private static boolean oddRow = true;

        public static void resetOddRow() {
            oddRow = true;
        }

        public FormItemBuilder withAttribute(String name, Object value) {
            attributes.put(name, value);
            return this;
        }

        public FormItemBuilder withRequiredSetTo(boolean required) {
            this.required = required;
            return this;
        }

        public FormItemBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public FormItemBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public FormItemBuilder withValue(String value) {
            this.value = value;
            return this;
        }

        public FormItemBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public FormItemBuilder withValidators(Validator... validators) {
            this.validators = validators;
            return this;
        }

        public FormItemBuilder withReadOnlySetTo(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public List<FormItem> build() {
            return build(new TextItem());
        }

        // GWT doesn't support reflection by default, therefore this "hack"
        public List<FormItem> build(FormItem valueItem) {
            List<FormItem> fields = new ArrayList<FormItem>();
            StaticTextItem nameItem = new StaticTextItem();
            nameItem.setStartRow(true);
            nameItem.setValue("<b>" + title + "</b>");
            nameItem.setShowTitle(false);
            nameItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(nameItem);

            valueItem.setName(name);
            valueItem.setValue(value);
            valueItem.setWidth(220);
            if (validators != null && validators.length > 0) {
                valueItem.setValidators(validators);
            }
            valueItem.setValidateOnChange(true);
            valueItem.setAlign(Alignment.CENTER);
            valueItem.setShowTitle(false);
            valueItem.setRequired(required);
            valueItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            valueItem.setDisabled(readOnly);
            fields.add(valueItem);

            StaticTextItem descriptionItem = new StaticTextItem();
            descriptionItem.setValue(description);
            descriptionItem.setShowTitle(false);
            descriptionItem.setEndRow(true);
            descriptionItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(descriptionItem);

            oddRow = !oddRow;
            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                valueItem.setAttribute(e.getKey(), e.getValue());
            }
            return fields;
        }

    }
}
