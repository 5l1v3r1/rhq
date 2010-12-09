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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * Table showing group plugin configuration history.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupPluginConfigurationTable extends Table {
    private final ResourceGroup group;
    private final ResourcePermission groupPerms;

    public HistoryGroupPluginConfigurationTable(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId, MSG.view_group_pluginConfig_table_title());
        this.group = groupComposite.getResourceGroup();
        this.groupPerms = groupComposite.getResourcePermission();

        setDataSource(new DataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField fieldId = new ListGridField("id", MSG.common_title_version());
        ListGridField fieldDateCreated = new ListGridField("dateCreated", MSG.common_title_dateCreated());
        ListGridField fieldLastUpdated = new ListGridField("lastUpdated", MSG.common_title_lastUpdated());
        ListGridField fieldUser = new ListGridField("user", MSG.common_title_user());
        ListGridField fieldStatus = new ListGridField("status", MSG.common_title_status());

        fieldId.setWidth("10%");
        fieldDateCreated.setWidth("35%");
        fieldLastUpdated.setWidth("35%");
        fieldUser.setWidth("*");
        fieldStatus.setWidth("10%");

        fieldStatus.setType(ListGridFieldType.ICON);
        HashMap<String, String> statusIcons = new HashMap<String, String>(4);
        statusIcons.put(ConfigurationUpdateStatus.SUCCESS.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.SUCCESS));
        statusIcons.put(ConfigurationUpdateStatus.FAILURE.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.FAILURE));
        statusIcons.put(ConfigurationUpdateStatus.INPROGRESS.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.INPROGRESS));
        statusIcons.put(ConfigurationUpdateStatus.NOCHANGE.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.NOCHANGE));
        fieldStatus.setValueIcons(statusIcons);
        fieldStatus.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                final Window winModal = new LocatableWindow(HistoryGroupPluginConfigurationTable.this
                    .extendLocatorId("statusDetailsWin"));
                winModal.setTitle(MSG.view_group_pluginConfig_table_statusDetails());
                winModal.setOverflow(Overflow.VISIBLE);
                winModal.setShowMinimizeButton(false);
                winModal.setShowMaximizeButton(true);
                winModal.setIsModal(true);
                winModal.setShowModalMask(true);
                winModal.setAutoSize(true);
                winModal.setAutoCenter(true);
                winModal.setShowResizer(true);
                winModal.setCanDragResize(true);
                winModal.centerInPage();
                winModal.addCloseClickHandler(new CloseClickHandler() {
                    @Override
                    public void onCloseClick(CloseClientEvent event) {
                        winModal.markForDestroy();
                    }
                });

                LocatableHTMLPane htmlPane = new LocatableHTMLPane(HistoryGroupPluginConfigurationTable.this
                    .extendLocatorId("statusDetailsPane"));
                htmlPane.setMargin(10);
                htmlPane.setDefaultWidth(500);
                htmlPane.setDefaultHeight(400);
                htmlPane.setContents(getStatusHtmlString(event.getRecord()));
                winModal.addItem(htmlPane);
                winModal.show();
            }
        });
        fieldStatus.setShowHover(true);
        fieldStatus.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String html = getStatusHtmlString(record);
                return html;
            }
        });

        ListGrid listGrid = getListGrid();
        listGrid.setFields(fieldId, fieldDateCreated, fieldLastUpdated, fieldUser, fieldStatus);

        addTableAction(extendLocatorId("deleteAction"), MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(this.groupPerms.isInventory() ? TableActionEnablement.ANY
                : TableActionEnablement.NEVER) {

                @Override
                public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                    if (selection == null || selection.length == 0) {
                        return;
                    }

                    ConfigurationGWTServiceAsync service = GWTServiceLookup.getConfigurationService();
                    Integer groupId = HistoryGroupPluginConfigurationTable.this.group.getId();
                    Integer[] updateIds = new Integer[selection.length];
                    int i = 0;
                    for (ListGridRecord record : selection) {
                        updateIds[i++] = record.getAttributeAsInt("id");
                    }

                    service.deleteGroupPluginConfigurationUpdate(groupId, updateIds, new AsyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            refresh();
                            Message message = new Message(MSG.view_group_pluginConfig_table_deleteSuccessful(String
                                .valueOf(selection.length)), Message.Severity.Info, EnumSet.of(
                                Message.Option.Transient, Message.Option.Sticky));
                            CoreGUI.getMessageCenter().notify(message);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_group_pluginConfig_table_deleteFailure(),
                                caught);
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("viewSettingsAction"), MSG.view_group_pluginConfig_table_viewSettings(),
            new AbstractTableAction(TableActionEnablement.SINGLE) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    CoreGUI.goToView(LinkManager
                        .getGroupPluginConfigurationUpdateHistoryLink(HistoryGroupPluginConfigurationTable.this.group
                            .getId())
                        + "/" + selection[0].getAttribute("id") + "/Settings");
                }
            });

        addTableAction(extendLocatorId("viewMemberHistoryAction"), MSG
            .view_group_pluginConfig_table_viewMemberHistory(), new AbstractTableAction(TableActionEnablement.SINGLE) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                CoreGUI.goToView(LinkManager
                    .getGroupPluginConfigurationUpdateHistoryLink(HistoryGroupPluginConfigurationTable.this.group
                        .getId())
                    + "/" + selection[0].getAttribute("id") + "/Members");
            }
        });

    }

    private String getStatusHtmlString(Record record) {
        String html = null;
        AbstractConfigurationUpdate obj = (AbstractConfigurationUpdate) record.getAttributeAsObject("object");
        switch (obj.getStatus()) {
        case SUCCESS: {
            html = MSG.view_group_pluginConfig_table_statusSuccess();
            break;
        }
        case INPROGRESS: {
            html = "<p>" + MSG.view_group_pluginConfig_table_statusInprogress() + "</p><p>"
                + MSG.view_group_pluginConfig_table_msg1() + "</p>";
            break;
        }
        case NOCHANGE: {
            html = MSG.view_group_pluginConfig_table_statusNochange();
            break;
        }
        case FAILURE: {
            html = obj.getErrorMessage();
            if (html == null) {
                html = "<p>" + MSG.view_group_pluginConfig_table_statusFailure() + "</p><p>"
                    + MSG.view_group_pluginConfig_table_msg1() + "</p>";
            } else {
                if (html.length() > 80) {
                    // this was probably an error stack trace, snip it so the tooltip isn't too big
                    html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                        + MSG.view_group_pluginConfig_table_clickStatusIcon() + "</p>";
                } else {
                    html = "<pre>" + html + "</pre>";
                }
                html = html + "<p>" + MSG.view_group_pluginConfig_table_msg1() + "</p>";
            }
            break;
        }
        }
        return html;
    }

    private class DataSource extends RPCDataSource<GroupPluginConfigurationUpdate> {

        @Override
        public GroupPluginConfigurationUpdate copyValues(Record from) {
            return (GroupPluginConfigurationUpdate) from.getAttributeAsObject("object");
        }

        @Override
        public ListGridRecord copyValues(GroupPluginConfigurationUpdate from) {
            ListGridRecord record = new ListGridRecord();

            record.setAttribute("id", from.getId());
            record.setAttribute("dateCreated", new Date(from.getCreatedTime()));
            record.setAttribute("lastUpdated", new Date(from.getModifiedTime()));
            record.setAttribute("user", from.getSubjectName());
            record.setAttribute("status", from.getStatus().name());

            record.setAttribute("object", from);

            return record;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

            GroupPluginConfigurationUpdateCriteria criteria = new GroupPluginConfigurationUpdateCriteria();
            ArrayList<Integer> groupList = new ArrayList<Integer>(1);
            groupList.add(HistoryGroupPluginConfigurationTable.this.group.getId());
            criteria.addFilterResourceGroupIds(groupList);

            configurationService.findGroupPluginConfigurationUpdatesByCriteria(criteria,
                new AsyncCallback<PageList<GroupPluginConfigurationUpdate>>() {

                    @Override
                    public void onSuccess(PageList<GroupPluginConfigurationUpdate> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_group_pluginConfig_table_failFetch(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }
    }
}
