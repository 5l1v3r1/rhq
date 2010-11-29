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
package org.rhq.enterprise.gui.coregui.client.components.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.ImgProperties;
import com.smartgwt.client.widgets.TransferImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.events.KeyPressEvent;
import com.smartgwt.client.widgets.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VStack;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTransferImgButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class AbstractSelector<T> extends LocatableVLayout {

    private static String SELECTOR_KEY = "id";

    Set<AssignedItemsChangedHandler> assignedItemsChangedHandlers = new HashSet<AssignedItemsChangedHandler>();

    protected ListGridRecord[] initialSelection;
    protected List<Record> availableRecords;
    protected DynamicForm availableFilterForm;
    protected HLayout hlayout;
    protected LocatableListGrid availableGrid;
    protected LocatableListGrid assignedGrid;
    protected RPCDataSource<T> datasource;

    protected TransferImgButton addButton;
    protected TransferImgButton removeButton;
    protected TransferImgButton addAllButton;
    protected TransferImgButton removeAllButton;

    protected Criteria latestCriteria;

    private boolean isReadOnly;

    public AbstractSelector(String locatorId) {
        this(locatorId, false);
    }

    public AbstractSelector(String locatorId, boolean isReadOnly) {
        super(locatorId);

        this.isReadOnly = isReadOnly;

        setWidth100();
        setMargin(7);

        this.hlayout = new HLayout();
        this.assignedGrid = new LocatableListGrid(extendLocatorId("assignedGrid"));

        if (this.isReadOnly) {
            this.assignedGrid.setSelectionType(SelectionStyle.NONE);
        } else {
            this.availableGrid = new LocatableListGrid(extendLocatorId("availableGrid"));
        }
    }

    public void setAssigned(ListGridRecord[] assignedRecords) {
        initialSelection = assignedRecords;
    }

    /**
     * Returns the set of currently selected {@link Record record}s.
     *
     * @return the set of currently selected {@link Record record}s
     */
    public ListGridRecord[] getSelectedRecords() {
        return this.assignedGrid.getRecords();
    }

    /**
     * Returns the set of currently selected {@link T item}s.
     *
     * @return the set of currently selected {@link T item}s
     */
    public Set<T> getSelectedItems() {
        ListGridRecord[] selectedRecords = this.assignedGrid.getRecords();
        return getDataSource().buildDataObjects(selectedRecords);
    }

    /**
     * Returns the IDs of the currently selected items
     * 
     * @return the IDs of the currently selected items
     */
    public Set<Integer> getSelection() {
        ListGridRecord[] selectedRecords = this.assignedGrid.getRecords();
        Set<Integer> ids = new HashSet<Integer>(selectedRecords.length);
        for (ListGridRecord selectedRecord : selectedRecords) {
            Integer id = selectedRecord.getAttributeAsInt(getSelectorKey());
            ids.add(id);
        }
        return ids;
    }

    protected abstract DynamicForm getAvailableFilterForm();

    protected abstract RPCDataSource<T> getDataSource();

    protected abstract Criteria getLatestCriteria(DynamicForm availableFilterForm);

    /**
     * Subclasses can override this if they want an icon displayed next to each item in the list grids.
     *
     * @return the icon to be displayed, or null if no icon should be displayed
     */
    protected String getItemIcon() {
        return null;
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.hlayout.setAlign(Alignment.LEFT);

        if (!this.isReadOnly) {
            // LEFT SIDE
            this.availableFilterForm = getAvailableFilterForm();
            if (this.availableFilterForm != null) {
                addMember(this.availableFilterForm);
            }

            SectionStack availableItemsStack = buildAvailableItemsStack();
            hlayout.addMember(availableItemsStack);

            // CENTER BUTTONS
            VStack moveButtonStack = buildButtonStack();
            hlayout.addMember(moveButtonStack);
        }

        // RIGHT SIDE
        SectionStack assignedItemsStack = buildAssignedItemsStack();
        this.hlayout.addMember(assignedItemsStack);

        addMember(this.hlayout);
    }

    private SectionStack buildAvailableItemsStack() {
        SectionStack availableSectionStack = new SectionStack();
        availableSectionStack.setWidth(300);
        availableSectionStack.setHeight(250);

        SectionStackSection availableSection = new SectionStackSection(getAvailableItemsGridTitle());
        availableSection.setCanCollapse(false);
        availableSection.setExpanded(true);

        availableGrid.setCanDragRecordsOut(true);
        availableGrid.setCanAcceptDroppedRecords(true);
        if (getItemIcon() != null) {
            availableGrid.setDragTrackerMode(DragTrackerMode.ICON);
            availableGrid.setTrackerImage(new ImgProperties(getItemIcon(), 16, 16));
        }
        availableGrid.setDragDataAction(DragDataAction.MOVE);
        this.availableGrid.setLoadingMessage(MSG.common_msg_loading());
        this.availableGrid.setEmptyMessage(MSG.common_msg_noItemsToShow());

        List<ListGridField> availableFields = new ArrayList<ListGridField>();
        String itemIcon = getItemIcon();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            availableFields.add(iconField);
        }
        ListGridField nameField = new ListGridField(getItemName(), capitalize(getItemTitle()));
        availableFields.add(nameField);
        availableGrid.setFields(availableFields.toArray(new ListGridField[availableFields.size()]));

        availableSection.setItems(availableGrid);
        availableSectionStack.addSection(availableSection);

        // Load data.
        datasource = getDataSource();
        populateAvailableGrid(new Criteria());

        if (availableFilterForm != null) {
            availableFilterForm.addItemChangedHandler(new ItemChangedHandler() {
                public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                    latestCriteria = getLatestCriteria(availableFilterForm);

                    Timer timer = new Timer() {
                        @Override
                        public void run() {
                            if (latestCriteria != null) {
                                Criteria criteria = latestCriteria;
                                latestCriteria = null;
                                populateAvailableGrid(criteria);
                            }
                        }
                    };
                    timer.schedule(500);
                }
            });
        }

        // Add event handlers.
        availableGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent event) {
                updateButtonEnablement();
            }
        });

        availableGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                updateButtonEnablement();
            }
        });

        availableGrid.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                addSelectedRows();
            }
        });

        availableGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                removeSelectedRows();
            }
        });

        return availableSectionStack;
    }

    private void populateAvailableGrid(Criteria criteria) {
        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        datasource.invalidateCache();
        datasource.fetchData(criteria, new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                availableRecords = new ArrayList<Record>();
                Record[] allRecords = response.getData();
                if (initialSelection != null) {
                    Set<Integer> selectedRecordIds = new HashSet<Integer>(initialSelection.length);
                    for (Record record : initialSelection) {
                        Integer id = record.getAttributeAsInt(getSelectorKey());
                        selectedRecordIds.add(id);
                    }
                    for (Record record : allRecords) {
                        int id = record.getAttributeAsInt(getSelectorKey());
                        if (!selectedRecordIds.contains(id)) {
                            availableRecords.add(record);
                        }
                    }
                } else {
                    availableRecords.addAll(Arrays.asList(allRecords));
                }
                availableGrid.setData(availableRecords.toArray(new Record[availableRecords.size()]));
            }
        });
    }

    private VStack buildButtonStack() {
        VStack moveButtonStack = new VStack(6);
        moveButtonStack.setHeight100();
        moveButtonStack.setAlign(VerticalAlignment.CENTER);
        moveButtonStack.setWidth(42);

        addButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT);
        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addSelectedRows();
            }
        });
        moveButtonStack.addMember(addButton);

        removeButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                removeSelectedRows();

            }
        });
        moveButtonStack.addMember(removeButton);

        addAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT_ALL);
        addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                availableGrid.selectAllRecords();
                addSelectedRows();
            }
        });
        moveButtonStack.addMember(addAllButton);

        removeAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT_ALL);
        removeAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.selectAllRecords();
                removeSelectedRows();
            }
        });
        moveButtonStack.addMember(removeAllButton);

        return moveButtonStack;
    }

    private SectionStack buildAssignedItemsStack() {
        SectionStack assignedSectionStack = new SectionStack();
        assignedSectionStack.setWidth(300);
        assignedSectionStack.setHeight(250);
        assignedSectionStack.setAlign(Alignment.LEFT);

        SectionStackSection assignedSection = new SectionStackSection(getAssignedItemsGridTitle());
        assignedSection.setCanCollapse(false);
        assignedSection.setExpanded(true);

        assignedGrid.setCanReorderRecords(true);
        assignedGrid.setCanDragRecordsOut(true);
        if (getItemIcon() != null) {
            assignedGrid.setDragTrackerMode(DragTrackerMode.ICON);
            assignedGrid.setTrackerImage(new ImgProperties(getItemIcon(), 16, 16));
        }
        assignedGrid.setCanAcceptDroppedRecords(true);
        this.assignedGrid.setLoadingMessage(MSG.common_msg_loading());
        this.assignedGrid.setEmptyMessage(MSG.common_msg_noItemsToShow());

        List<ListGridField> assignedFields = new ArrayList<ListGridField>();
        String itemIcon = getItemIcon();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            assignedFields.add(iconField);
        }
        ListGridField nameField = new ListGridField(getItemName(), capitalize(getItemTitle()));
        assignedFields.add(nameField);
        assignedGrid.setFields(assignedFields.toArray(new ListGridField[assignedFields.size()]));

        assignedSection.setItems(assignedGrid);
        assignedSectionStack.addSection(assignedSection);

        // Load data.
        if (initialSelection != null) {
            assignedGrid.setData(initialSelection);
        }

        if (!this.isReadOnly) {
            // Add event handlers.
            assignedGrid.addDoubleClickHandler(new DoubleClickHandler() {
                public void onDoubleClick(DoubleClickEvent event) {
                    removeSelectedRows();
                }
            });

            assignedGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    updateButtonEnablement();
                }
            });

            assignedGrid.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ("Delete".equals(event.getKeyName())) {
                        removeSelectedRows();
                    }
                }
            });

            assignedGrid.addRecordDropHandler(new RecordDropHandler() {
                public void onRecordDrop(RecordDropEvent recordDropEvent) {
                    addSelectedRows();
                }
            });
        }

        return assignedSectionStack;
    }

    private void notifyAssignedItemsChangedHandlers() {
        for (AssignedItemsChangedHandler handler : assignedItemsChangedHandlers) {
            handler.onSelectionChanged(new AssignedItemsChangedEvent(assignedGrid.getSelection()));
        }
    }

    public void reset() {
        this.assignedGrid.setData(this.initialSelection);
        populateAvailableGrid(getLatestCriteria(getAvailableFilterForm()));
    }

    public HandlerRegistration addAssignedItemsChangedHandler(final AssignedItemsChangedHandler handler) {
        this.assignedItemsChangedHandlers.add(handler);
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                assignedItemsChangedHandlers.remove(handler);
            }
        };
    }

    /**
     * Moves the rows selected in the assigned grid to the available grid.
     */
    public void removeSelectedRows() {
        moveSelectedData(this.assignedGrid, this.availableGrid);
        notifyAssignedItemsChangedHandlers();
        updateButtonEnablement();
    }

    /**
     * Moves the rows selected in the available grid to the assigned grid.
     */
    public void addSelectedRows() {
        moveSelectedData(this.availableGrid, this.assignedGrid);
        notifyAssignedItemsChangedHandlers();
        updateButtonEnablement();
    }

    private void moveSelectedData(ListGrid sourceGrid, ListGrid targetGrid) {
        targetGrid.transferSelectedData(sourceGrid);
        sourceGrid.removeSelectedData();
    }

    protected String getItemName() {
        return "name";
    }

    protected abstract String getItemTitle();

    protected String getAvailableItemsGridTitle() {
        String itemTitle = getItemTitle();
        return MSG.view_selector_available(capitalize(itemTitle));
    }

    protected String getAssignedItemsGridTitle() {
        String itemTitle = getItemTitle();
        return MSG.view_selector_assigned(capitalize(itemTitle));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        assignedGrid.destroy();

        if (!isReadOnly) {
            if (availableFilterForm != null) {
                availableFilterForm.destroy();
            }

            availableGrid.destroy();

            addButton.destroy();
            removeButton.destroy();
            addAllButton.destroy();
            removeAllButton.destroy();
        }
    }

    protected void updateButtonEnablement() {
        addButton.setDisabled(!availableGrid.anySelected() || availableGrid.getDataAsRecordList().isEmpty());
        removeButton.setDisabled(!assignedGrid.anySelected() || assignedGrid.getDataAsRecordList().isEmpty());
        addAllButton.setDisabled(availableGrid.getDataAsRecordList().isEmpty());
        removeAllButton.setDisabled(assignedGrid.getDataAsRecordList().isEmpty());
    }

    @Deprecated
    protected void select(ListGridRecord[] records) {
    }

    @Deprecated
    public LocatableListGrid getAvailableGrid() {
        return availableGrid;
    }

    @Deprecated
    public LocatableListGrid getAssignedGrid() {
        return assignedGrid;
    }

    protected String getSelectorKey() {
        return SELECTOR_KEY;
    }

    private static String capitalize(String itemTitle) {
        return Character.toUpperCase(itemTitle.charAt(0)) + itemTitle.substring(1);
    }
}
