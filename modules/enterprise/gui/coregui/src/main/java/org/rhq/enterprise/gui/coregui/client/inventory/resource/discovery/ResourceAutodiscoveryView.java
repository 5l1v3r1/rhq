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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
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
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceAutodiscoveryView extends VLayout {

    private boolean simple = false;
    private TreeGrid treeGrid;
    private ToolStrip footer;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceAutodiscoveryView() {
        setWidth100();
        setHeight100();
    }

    public ResourceAutodiscoveryView(boolean simple) {
        this();
        this.simple = simple;
    }

    @Override
    protected void onInit() {
        super.onInit();


        HLayout titleLayout = new HLayout(150);

        HTMLFlow title = new HTMLFlow();
        title.setWidth100();
        title.setHeight(35);
        title.setContents("Auto Discovery Queue");
        title.setPadding(4);
        title.setStyleName("HeaderLabel");
        titleLayout.addMember(title);


        DynamicForm form = new DynamicForm();
        final SelectItem statusSelectItem = new SelectItem("statuses", "Displayed Statuses");
        statusSelectItem.setValueMap("New", "Ignored", "New and Ignored");
        statusSelectItem.setValue("New");
        form.setItems(statusSelectItem);
        if (!simple)
            titleLayout.addMember(form);


        addMember(titleLayout);

        treeGrid = new TreeGrid();

        treeGrid.setHeight100();

        treeGrid.setDataSource(new AutodiscoveryQueueDataSource());
        treeGrid.setAutoFetchData(true);
        treeGrid.setTitle("Auto Discovery Queue");
        treeGrid.setResizeFieldsInRealTime(true);

        final TreeGridField name, key, type, description, status, ctime;
        name = new TreeGridField("name");
        key = new TreeGridField("resourceKey");
        type = new TreeGridField("typeName");
        description = new TreeGridField("description");
        status = new TreeGridField("status");
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


        addMember(treeGrid);

        footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        addMember(footer);


        final IButton importButton = new IButton("Import");
        final IButton ignoreButton = new IButton("Ignore");
        final IButton unignoreButton = new IButton("Unignore");


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


        importButton.addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        resourceService.importResources(getSelectedIds(), new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to import resources", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(new Message("Successfully imported the selected resources", Message.Severity.Info));
                                treeGrid.invalidateCache();
                            }
                        });
                    }
                }
        );


        ignoreButton.addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        resourceService.ignoreResources(getSelectedIds(), new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to ignore resources", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(new Message("Successfully ignored the selected resources", Message.Severity.Info));
                                treeGrid.invalidateCache();
                            }
                        });
                    }
                }
        );

        unignoreButton.addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        resourceService.unignoreResources(getSelectedIds(), new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to unignore resources", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(new Message("Successfully unignored the selected resources", Message.Severity.Info));
                                treeGrid.invalidateCache();
                            }
                        });
                    }
                }
        );

        statusSelectItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                treeGrid.fetchData(new Criteria("statuses", (String) statusSelectItem.getValue()));
            }
        });
    }

    private Integer[] getSelectedIds() {
        ArrayList<Integer> selected = new ArrayList<Integer>();
        for (ListGridRecord node : treeGrid.getSelection()) {
            if (!InventoryStatus.COMMITTED.name().equals(node.getAttributeAsString("status"))) {
                selected.add(Integer.parseInt(node.getAttributeAsString("id")));
            }
        }
        return selected.toArray(new Integer[selected.size()]);
    }
}
