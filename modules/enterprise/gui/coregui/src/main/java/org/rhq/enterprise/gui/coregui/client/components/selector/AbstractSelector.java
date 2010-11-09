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
import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.types.ListGridFieldType;
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
 */
public abstract class AbstractSelector<T> extends LocatableVLayout {
    private static String SELECTOR_KEY = "id";

    protected Set<Integer> selection = new HashSet<Integer>();
    protected Set<String> selectionAlternateIds = new HashSet<String>();

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

    public AbstractSelector(String locatorId) {
        super(locatorId);
        setPadding(7);
        hlayout = new HLayout();
        availableGrid = new LocatableListGrid(extendLocatorId("availableGrid"));
        assignedGrid = new LocatableListGrid(extendLocatorId("assignedGrid"));
    }

    public void setAssigned(ListGridRecord[] assignedRecords) {
        initialSelection = assignedRecords;
    }

    /**
     * Returns the list of IDs for the records being transferred.
     * 
     * @return the list of IDs for the records being transferred
     */
    public Set<Integer> getSelection() {
        return selection;
    }

    public Set<String> getSelectionAlternateIds() {
        return selectionAlternateIds;
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

        availableFilterForm = getAvailableFilterForm();
        if (availableFilterForm != null) {
            addMember(availableFilterForm);
        }

        hlayout.setAlign(VerticalAlignment.BOTTOM);

        // LEFT SIDE
        SectionStack availableSectionStack = new SectionStack();
        //availableSectionStack.setWidth(300);
        availableSectionStack.setHeight(300);

        SectionStackSection availableSection = new SectionStackSection(getAvailableItemsGridTitle());
        availableSection.setCanCollapse(false);
        availableSection.setExpanded(true);
        
        availableGrid.setCanDragRecordsOut(true);
        availableGrid.setCanAcceptDroppedRecords(true);
        availableGrid.setDragTrackerMode(DragTrackerMode.ICON);
        availableGrid.setTrackerImage(new ImgProperties("types/Service_up_16.png", 16, 16));
        availableGrid.setDragDataAction(DragDataAction.MOVE);

        datasource = getDataSource();
        datasource.fetchData(new Criteria(), new DSCallback() {
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                availableRecords = new ArrayList<Record>();
                Record[] allRecords = response.getData();
                if (selection != null) {
                    for (Record record : allRecords) {
                        int id = record.getAttributeAsInt("id");
                        if (!selection.contains(id)) {
                            availableRecords.add(record);
                        }
                    }
                } else {
                    availableRecords.addAll(Arrays.asList(allRecords));
                }
                availableGrid.setData(availableRecords.toArray(new Record[availableRecords.size()]));
            }
        });

        //availableGrid.setDataSource(datasource);
        //availableGrid.setFetchDelay(700);
        //availableGrid.setAutoFetchData(true);

        List<ListGridField> availableFields = new ArrayList<ListGridField>();
        String itemIcon = getItemIcon();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            availableFields.add(iconField);
        }
        ListGridField nameField = new ListGridField("name", "Role");
        availableFields.add(nameField);
        availableGrid.setFields(availableFields.toArray(new ListGridField[availableFields.size()]));

        availableSection.setItems(availableGrid);
        availableSectionStack.addSection(availableSection);
        hlayout.addMember(availableSectionStack);

        if (availableFilterForm != null) {
            availableFilterForm.addItemChangedHandler(new ItemChangedHandler() {
                public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                    latestCriteria = getLatestCriteria(availableFilterForm);

                    Timer timer = new Timer() {
                        @Override
                        public void run() {
                            if (latestCriteria != null) {
                                // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
                                availableGrid.invalidateCache();

                                Criteria criteria = latestCriteria;
                                latestCriteria = null;
                                availableGrid.fetchData(criteria);
                            }
                        }
                    };
                    timer.schedule(500);
                }
            });
        }

        // CENTER BUTTONS
        VStack moveButtonStack = new VStack(6);
        moveButtonStack.setAlign(VerticalAlignment.CENTER);
        moveButtonStack.setWidth(40);

        addButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT);
        removeButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT);
        addAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.RIGHT_ALL);
        removeAllButton = new LocatableTransferImgButton(this.getLocatorId(), TransferImgButton.LEFT_ALL);

        moveButtonStack.addMember(addButton);
        moveButtonStack.addMember(removeButton);
        moveButtonStack.addMember(addAllButton);
        moveButtonStack.addMember(removeAllButton);

        hlayout.addMember(moveButtonStack);

        // RIGHT SIDE
        SectionStack assignedSectionStack = new SectionStack();
        //assignedSectionStack.setWidth(300);
        assignedSectionStack.setHeight(300);

        SectionStackSection assignedSection = new SectionStackSection(getAssignedItemsGridTitle());
        assignedSection.setCanCollapse(false);
        assignedSection.setExpanded(true);

        assignedGrid.setCanReorderRecords(true);
        assignedGrid.setCanDragRecordsOut(true);
        assignedGrid.setDragTrackerMode(DragTrackerMode.ICON);
        assignedGrid.setTrackerImage(new ImgProperties("types/Service_up_16.png", 16, 16));
        assignedGrid.setCanAcceptDroppedRecords(true);

        List<ListGridField> assignedFields = new ArrayList<ListGridField>();
        if (itemIcon != null) {
            ListGridField iconField = new ListGridField("icon", 25);
            iconField.setType(ListGridFieldType.ICON);
            iconField.setCellIcon(itemIcon);
            iconField.setShowDefaultContextMenu(false);
            assignedFields.add(iconField);
        }
        nameField = new ListGridField("name", "Role");
        assignedFields.add(nameField);
        assignedGrid.setFields(assignedFields.toArray(new ListGridField[assignedFields.size()]));

        assignedSection.setItems(assignedGrid);
        assignedSectionStack.addSection(assignedSection);
        hlayout.addMember(assignedSectionStack);

        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                addSelectedRows();
            }
        });

        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                removeSelectedRows();

            }
        });
        addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                availableGrid.selectAllRecords();
                addSelectedRows();
            }
        });
        removeAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                assignedGrid.selectAllRecords();
                removeSelectedRows();
            }
        });

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

        if (initialSelection != null) {
            assignedGrid.setData(initialSelection);
            for (ListGridRecord record : initialSelection) {
                if (getSelectorKey().equalsIgnoreCase("id")) {
                    selection.add(record.getAttributeAsInt(getSelectorKey()));
                } else {
                    selectionAlternateIds.add(record.getAttributeAsString(getSelectorKey()));
                }
            }
        }
        
        addMember(hlayout);
    }

    private void notifyAssignedItemsChangedHandlers() {
        for (AssignedItemsChangedHandler handler : assignedItemsChangedHandlers) {
            handler.onSelectionChanged(new AssignedItemsChangedEvent(assignedGrid.getSelection()));
        }
    }

    public void reset() {
        select(initialSelection);
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

    private void removeSelectedRows() {
        availableGrid.transferSelectedData(assignedGrid);
        deselect(assignedGrid.getSelection());
        assignedGrid.removeSelectedData();
        notifyAssignedItemsChangedHandlers();
        updateButtonEnablement();
    }

    private void addSelectedRows() {
        assignedGrid.transferSelectedData(availableGrid);
        select(assignedGrid.getSelection());
        availableGrid.removeSelectedData();
        notifyAssignedItemsChangedHandlers();
        updateButtonEnablement();
    }

    protected abstract String getItemTitle();

    protected String getAvailableItemsGridTitle() {
        String itemTitle = getItemTitle();
        return "Available " + capitalize(itemTitle) + "s";
    }

    protected String getAssignedItemsGridTitle() {
        String itemTitle = getItemTitle();
        return "Assigned " + capitalize(itemTitle) + "s";
    }

    private static String capitalize(String itemTitle) {
        return Character.toUpperCase(itemTitle.charAt(0)) + itemTitle.substring(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        availableGrid.destroy();
        assignedGrid.destroy();
        addButton.destroy();
        removeButton.destroy();
        addAllButton.destroy();
        removeAllButton.destroy();
        if (availableFilterForm != null) {
            availableFilterForm.destroy();
        }
    }

    protected void updateButtonEnablement() {
        addButton.setDisabled(!availableGrid.anySelected() || availableGrid.getDataAsRecordList().isEmpty());
        removeButton.setDisabled(!assignedGrid.anySelected() || assignedGrid.getDataAsRecordList().isEmpty());
        addAllButton.setDisabled(availableGrid.getDataAsRecordList().isEmpty());
        removeAllButton.setDisabled(assignedGrid.getDataAsRecordList().isEmpty());
    }

    protected void select(ListGridRecord[] records) {
        availableGrid.deselectAllRecords();
        for (ListGridRecord record : records) {
            if (getSelectorKey().equalsIgnoreCase("id")) {
                selection.add(record.getAttributeAsInt(getSelectorKey()));
            } else {
                selectionAlternateIds.add(record.getAttributeAsString(getSelectorKey()));
            }
        }
        assignedGrid.markForRedraw();
    }

    protected void deselect(ListGridRecord[] records) {
        Set<Integer> toRemove = new HashSet<Integer>();
        Set<String> toRemoveStringIds = new HashSet<String>();
        if (getSelectorKey().equalsIgnoreCase("id")) {//integer id based
            for (ListGridRecord record : records) {
                toRemove.add(record.getAttributeAsInt(getSelectorKey()));
            }
            selection.removeAll(toRemove);

            for (Integer id : toRemove) {
                Record record = availableGrid.getDataAsRecordList().find(getSelectorKey(), id);
                if (record != null) {
                    ((ListGridRecord) record).setEnabled(true);
                }
            }
        } else {//not using 'id' as selection criteria
            for (ListGridRecord record : records) {
                toRemoveStringIds.add(record.getAttributeAsString(getSelectorKey()));
            }
            selectionAlternateIds.removeAll(toRemoveStringIds);           
        }
        availableGrid.markForRedraw();
    }

    // TODO: Add reset() method.

    public LocatableListGrid getAvailableGrid() {
        return availableGrid;
    }

    public LocatableListGrid getAssignedGrid() {
        return assignedGrid;
    }

    protected String getSelectorKey() {
        return SELECTOR_KEY;
    }
}
