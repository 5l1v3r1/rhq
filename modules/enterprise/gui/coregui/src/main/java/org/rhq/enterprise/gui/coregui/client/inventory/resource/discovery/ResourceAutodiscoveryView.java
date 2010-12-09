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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceAutodiscoveryView extends LocatableVLayout {
    private static final String TITLE = MSG.view_autoDiscoveryQ_title();
    private static final String HEADER_ICON = "global/Recent_24.png";

    private boolean simple;
    private TreeGrid treeGrid;
    private ToolStrip footer;
    private DataSource dataSource;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceAutodiscoveryView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();
    }

    public ResourceAutodiscoveryView(String locatorId, boolean simple) {
        this(locatorId);
        this.simple = simple;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!simple) {
            Img img = new Img(HEADER_ICON, 24, 24);
            img.setPadding(4);

            HTMLFlow title = new HTMLFlow();
            title.setWidth100();
            title.setHeight(35);
            title.setContents(TITLE);
            title.setPadding(4);
            title.setStyleName("HeaderLabel");

            DynamicForm form = new LocatableDynamicForm(this.extendLocatorId("Status"));
            final SelectItem statusSelectItem = new SelectItem("status", MSG.common_title_status());
            statusSelectItem.setValueMap(AutodiscoveryQueueDataSource.NEW, AutodiscoveryQueueDataSource.IGNORED,
                AutodiscoveryQueueDataSource.NEW_AND_IGNORED);
            statusSelectItem.setValue(AutodiscoveryQueueDataSource.NEW);
            form.setItems(statusSelectItem);

            statusSelectItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent changedEvent) {
                    treeGrid.fetchData(new Criteria("status", (String) statusSelectItem.getValue()));
                }
            });

            HLayout titleLayout = new HLayout();
            titleLayout.setAutoHeight();
            titleLayout.setAlign(VerticalAlignment.BOTTOM);

            titleLayout.addMember(img);
            titleLayout.addMember(title);
            titleLayout.addMember(new LayoutSpacer());
            titleLayout.addMember(form);

            addMember(titleLayout);
        }

        treeGrid = new LocatableTreeGrid(this.getLocatorId());

        treeGrid.setHeight100();

        dataSource = new AutodiscoveryQueueDataSource(treeGrid);
        treeGrid.setDataSource(dataSource);
        treeGrid.setAutoFetchData(true);
        treeGrid.setResizeFieldsInRealTime(true);
        treeGrid.setAutoFitData(Autofit.HORIZONTAL);
        treeGrid.setWrapCells(true);
        treeGrid.setFixedRecordHeights(false);

        final TreeGridField name, key, type, description, status, ctime;
        name = new TreeGridField("name");
        key = new TreeGridField("resourceKey");
        type = new TreeGridField("typeName");
        description = new TreeGridField("description");
        status = new TreeGridField("statusLabel");
        ctime = new TreeGridField("ctime");

        if (!simple) {
            treeGrid.setFields(name, key, type, description, status, ctime);
        } else {
            treeGrid.setFields(name, key, type, status);
        }

        treeGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);
        treeGrid.setShowSelectedStyle(false);
        treeGrid.setShowPartialSelection(true);
        treeGrid.setCascadeSelection(true);

        // Do this last since it causes the TreeGrid to be initialized.
        treeGrid.deselectAllRecords();

        addMember(treeGrid);

        footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        addMember(footer);

        final IButton importButton = new LocatableIButton(this.extendLocatorId("Import"), MSG
            .view_autoDiscoveryQ_import());
        final IButton ignoreButton = new LocatableIButton(this.extendLocatorId("Ignore"), MSG
            .view_autoDiscoveryQ_ignore());
        final IButton unignoreButton = new LocatableIButton(this.extendLocatorId("Unignore"), MSG
            .view_autoDiscoveryQ_unignore());

        footer.addMember(importButton);
        footer.addMember(ignoreButton);
        footer.addMember(unignoreButton);

        importButton.setDisabled(true);
        ignoreButton.setDisabled(true);
        unignoreButton.setDisabled(true);

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                boolean noneSelected = treeGrid.getSelection().length == 0;
                importButton.setDisabled(noneSelected);
                ignoreButton.setDisabled(noneSelected);
                unignoreButton.setDisabled(noneSelected);
            }
        });

        importButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                resourceService.importResources(getSelectedIds(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_importFailure(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_autoDiscoveryQ_importSuccessful(), Message.Severity.Info));
                        treeGrid.invalidateCache();
                        treeGrid.markForRedraw();
                    }
                });
            }
        });

        ignoreButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                resourceService.ignoreResources(getSelectedIds(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_ignoreFailure(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_autoDiscoveryQ_ignoreSuccessful(), Message.Severity.Info));
                        treeGrid.invalidateCache();
                        treeGrid.markForRedraw();
                    }
                });
            }
        });

        unignoreButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                resourceService.unignoreResources(getSelectedIds(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_unignoreFailure(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_autoDiscoveryQ_unignoreSuccessful(), Message.Severity.Info));
                        treeGrid.invalidateCache();
                        treeGrid.markForRedraw();
                    }
                });
            }
        });

    }

    private int[] getSelectedIds() {
        List<Integer> selected = new ArrayList<Integer>();
        for (ListGridRecord node : treeGrid.getSelection()) {
            if (!InventoryStatus.COMMITTED.name().equals(node.getAttributeAsString("status"))) {
                selected.add(Integer.parseInt(node.getAttributeAsString("id")));
            }
        }

        return TableUtility.getIds(selected);
    }

    /** Custom refresh operation as we cannot directly extend Table because it
     * contains a TreeGrid which is not a Table.
     */
    @Override
    public void redraw() {
        super.redraw();
        //now reload the table data
        this.treeGrid.invalidateCache();
        this.treeGrid.markForRedraw();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public TreeGrid getTreeGrid() {
        return treeGrid;
    }
}
