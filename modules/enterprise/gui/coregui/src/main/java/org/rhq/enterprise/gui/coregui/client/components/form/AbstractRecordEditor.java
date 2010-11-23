/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import java.util.EnumSet;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * An editor for a SmartGWT {@link Record} backed by an {@link RPCDataSource}.
 *
 * @author Ian Springer
 */
public abstract class AbstractRecordEditor<DS extends RPCDataSource> extends LocatableVLayout
    implements BookmarkableView, DetailsView {

    private static final Label LOADING_LABEL = new Label(MSG.widget_recordEditor_label_loading());

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";

    private static final int ID_NEW = 0;

    private int recordId;
    private TitleBar titleBar;
    private VLayout editCanvas;
    private EnhancedDynamicForm form;
    private DS dataSource;
    private IButton saveButton;
    private IButton resetButton;
    private boolean isReadOnly;
    private VLayout bottomLayout;
    private String dataTypeName;
    private String listViewPath;
    private boolean wasInvalid;

    public AbstractRecordEditor(String locatorId, DS dataSource, int recordId, String dataTypeName,
                                String headerIcon) {
        super(locatorId);
        this.dataSource = dataSource;
        this.recordId = recordId;
        this.dataTypeName = capitalize(dataTypeName);

        // Set properties for this VLayout.
        setOverflow(Overflow.AUTO);
        setPadding(7);

        // Display a "Loading..." label at the top of the view to keep the user informed.
        addMember(LOADING_LABEL);

        // Add title bar. We'll set the actual title later.
        this.titleBar = new TitleBar(null, headerIcon);
        this.titleBar.hide();
        addMember(this.titleBar);
    }

    @Override
    public void renderView(ViewPath viewPath) {
        String parentViewPath = viewPath.getParentViewPath();
        if (!viewPath.isEnd()) {
            CoreGUI.getErrorHandler().handleError(MSG.widget_recordEditor_error_invalidViewPath(viewPath.toString()));
            CoreGUI.goToView(parentViewPath);
        } else {
            this.listViewPath = parentViewPath; // e.g. Administration/Security/Roles
        }
    }

    /**
     * <b>IMPORTANT:</b> Subclasses are responsible for invoking this method after all asynchronous operations invoked
     * by {@link #renderView(ViewPath)} have completed.
     *
     * @param isReadOnly whether or not the record editor should be in read-only mode
     */
    protected void init(boolean isReadOnly) {
        if (this.recordId == ID_NEW && isReadOnly) {
            Message message =
                new Message("You do not have the permissions required to create a new " + this.dataTypeName + ".",
                    Message.Severity.Error);
            CoreGUI.goToView(getListViewPath(), message);
        } else {
            this.isReadOnly = isReadOnly;
            this.editCanvas = buildEditor();
            this.editCanvas.hide();
            addMember(this.editCanvas);

            if (this.recordId == ID_NEW) {
                editNewRecord();

                // Now that all the widgets have been created and initialized, make everything visible.
                displayForm();
            } else {
                fetchExistingRecord(this.recordId);
            }
        }
    }

    private VLayout buildEditor() {
        VLayout editorVLayout = new VLayout();

        boolean isNewRecord = (this.recordId == ID_NEW);
        this.form = new EnhancedDynamicForm(this.getLocatorId(), this.isReadOnly, isNewRecord);
        this.form.setDataSource(this.dataSource);

        List<FormItem> items = createFormItems(isNewRecord);
        this.form.setItems(items.toArray(new FormItem[items.size()]));

        this.form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent event) {
                AbstractRecordEditor.this.onItemChanged();
            }
        });

        editorVLayout.addMember(this.form);

        this.bottomLayout = new VLayout();
        editorVLayout.addMember(this.bottomLayout);

        if (!this.isReadOnly) {
            VLayout verticalSpacer = new VLayout();
            verticalSpacer.setHeight(12);
            editorVLayout.addMember(verticalSpacer);

            HLayout buttonLayout = createButtons();

            editorVLayout.addMember(buttonLayout);
        }

        return editorVLayout;
    }

    private HLayout createButtons() {
        HLayout buttonLayout = new HLayout(10);

        saveButton = new LocatableIButton(this.extendLocatorId("Save"), MSG.common_button_save());
        saveButton.setDisabled(true);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });

        resetButton = new LocatableIButton(this.extendLocatorId("Reset"), MSG.common_button_reset());
        resetButton.setDisabled(true);
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                reset();
                resetButton.disable();
            }
        });

        IButton cancelButton = new LocatableIButton(this.extendLocatorId("Cancel"), MSG.common_button_cancel());
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                History.back();
            }
        });

        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(resetButton);
        buttonLayout.addMember(cancelButton);
        return buttonLayout;
    }

    public VLayout getBottomLayout() {
        return bottomLayout;
    }

    public EnhancedDynamicForm getForm() {
        return form;
    }

    public DS getDataSource() {
        return dataSource;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public int getRecordId() {
        return recordId;
    }

    public String getListViewPath() {
        return listViewPath;
    }

    protected abstract List<FormItem> createFormItems(boolean newUser);

    /**
     * This method should be called whenever any editable item on the page is changed. It will enable the Reset button
     * and update the Save button's enablement based on whether or not all items on the form are valid.
     */
    public void onItemChanged() {
        // TODO: We also need to validate complex fields - selectors, etc.
        boolean isValid = this.form.valuesAreValid(false);

        // If we're in editable mode, update the button enablement.
        if (!this.isReadOnly) {
            this.saveButton.setDisabled(!isValid);
            this.resetButton.setDisabled(false);
            if (!isValid) {
                this.wasInvalid = true;
                Message message = new Message("One or more fields have invalid values. This " + this.dataTypeName
                    + " cannot be saved until these values are corrected.", Message.Severity.Warning, EnumSet.of(
                    Message.Option.Sticky, Message.Option.Transient));
                CoreGUI.getMessageCenter().notify(message);
            } else {
                if (this.wasInvalid) {
                    Message message = new Message("All fields now have valid values. This " + this.dataTypeName
                        + " can now be saved.", Message.Severity.Info, EnumSet.of(Message.Option.Sticky,
                        Message.Option.Transient));
                    CoreGUI.getMessageCenter().notify(message);
                    this.wasInvalid = false;
                }
            }
        }
    }

    protected void reset() {
        this.form.reset();
    }

    protected void save() {
        this.form.saveData(new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (response.getStatus() == RPCResponse.STATUS_SUCCESS) {
                    Record[] data = response.getData();
                    Record record = data[0];

                    String id = record.getAttribute(FIELD_ID);
                    String name = record.getAttribute(getTitleFieldName());

                    Message message;
                    String conciseMessage;
                    String detailedMessage;
                    DSOperationType operationType = request.getOperationType();
                    if (Log.isDebugEnabled()) {
                        Object dataObject = dataSource.copyValues(record);
                        if (operationType == DSOperationType.ADD) {
                            Log.debug("Created: " + dataObject);
                        } else {
                            Log.debug("Updated: " + dataObject);
                        }
                    }
                    switch (operationType) {
                        case ADD:
                            conciseMessage = MSG.widget_recordEditor_info_recordCreatedConcise(dataTypeName);
                            detailedMessage = MSG.widget_recordEditor_info_recordCreatedDetailed(dataTypeName, name);
                            if (CoreGUI.isDebugMode()) {
                                conciseMessage += " (" + FIELD_ID + "=" + id + ")";
                                detailedMessage += " (" + FIELD_ID + "=" + id + ")";
                            }
                            break;
                        case UPDATE:
                            conciseMessage = MSG.widget_recordEditor_info_recordUpdatedConcise(dataTypeName);
                            detailedMessage = MSG.widget_recordEditor_info_recordUpdatedDetailed(dataTypeName, name);
                            break;
                        default:
                            throw new IllegalStateException(
                                MSG.widget_recordEditor_error_unsupportedOperationType(operationType.name()));
                    }

                    message = new Message(conciseMessage, detailedMessage);
                    CoreGUI.goToView(getListViewPath(), message);
                } else if (response.getStatus() == RPCResponse.STATUS_VALIDATION_ERROR) {
                    Message message = new Message("Operation failed - one or more fields have invalid values.",
                        Message.Severity.Error);
                    CoreGUI.getMessageCenter().notify(message);
                } else {
                    // assume failure
                    Message message = new Message("Operation failed - an error occurred.", Message.Severity.Error);
                    CoreGUI.getMessageCenter().notify(message);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void editRecord(Record record) {
        // Update the view title.
        String recordName = record.getAttribute(getTitleFieldName());
        String title = (this.isReadOnly) ? MSG.widget_recordEditor_title_view(this.dataTypeName, recordName) :
            MSG.widget_recordEditor_title_edit(this.dataTypeName, recordName);
        this.titleBar.setTitle(title);

        // Load the data into the form.
        this.form.editRecord(record);

        // Perform up front validation for existing records.
        // NOTE: We do *not* do this for new records, since we expect most of the required fields to be blank.
        this.form.validate();
    }

    protected void editNewRecord() {
        // Update the view title.
        this.titleBar.setTitle("New " + this.dataTypeName);

        // Clear the form
        this.form.editNewRecord();

        // But make sure the value of the "id" field is set to "0", since a value of null could cause the dataSource's
        // copyValues(Record) impl to choke.
        FormItem idItem = this.form.getItem(FIELD_ID);
        if (idItem != null) {
            idItem.setDefaultValue(ID_NEW);
            idItem.hide();
        }
    }

    private void displayForm() {
        LOADING_LABEL.hide();
        this.titleBar.show();
        this.editCanvas.show();
        markForRedraw();
    }

    protected void fetchExistingRecord(final int recordId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(FIELD_ID, recordId);
        this.form.fetchData(criteria, new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                    Record[] records = response.getData();
                    if (records.length == 0) {
                        throw new IllegalStateException(MSG.widget_recordEditor_error_noRecords());
                    }
                    if (records.length > 1) {
                        throw new IllegalStateException(MSG.widget_recordEditor_error_multipleRecords());
                    }
                    Record record = records[0];
                    editRecord(record);

                    // Now that all the widgets have been created and initialized, make everything visible.
                    displayForm();
                }
            }
        });
    }

    protected String getTitleFieldName() {
        return FIELD_NAME;
    }

    @Override
    public boolean isEditable() {
        return (!this.isReadOnly);
    }

    protected static ListGridRecord[] toListGridRecordArray(Record[] roleRecords) {
        ListGridRecord[] roleListGridRecords = new ListGridRecord[roleRecords.length];
        for (int i = ID_NEW, roleRecordsLength = roleRecords.length; i < roleRecordsLength; i++) {
            Record roleRecord = roleRecords[i];
            roleListGridRecords[i] = (ListGridRecord)roleRecord;
        }
        return roleListGridRecords;
    }
    
    private static String capitalize(String itemTitle) {
        return Character.toUpperCase(itemTitle.charAt(ID_NEW)) + itemTitle.substring(1);
    }
    
}
