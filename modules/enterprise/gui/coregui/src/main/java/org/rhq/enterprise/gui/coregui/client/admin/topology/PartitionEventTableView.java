/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.topology;

import static org.rhq.enterprise.gui.coregui.client.admin.topology.FailoverListItemDatasourceField.FIELD_ORDINAL;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jiri Kremser
 * 
 */
public class PartitionEventTableView extends TableSection<PartitionEventDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("PartitionEvents(GWT)", "Partition Events(GWT)",
        IconEnum.SERVERS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    private final boolean showActions = true;

    public PartitionEventTableView(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
        setHeight100();
        setWidth100();
        setDataSource(new PartitionEventDatasource());
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        if (showActions) {
            listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);
            showActions();
        } else {
            // sorting by order field
            listGrid.sort(FIELD_ORDINAL.propertyName(), SortDirection.ASCENDING);
        }
        for (ListGridField field : fields) {
            // adding the cell formatter for name field (clickable link)
            if (field.getName() == FIELD_NAME) {
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null) {
                            return "";
                        }
                        String detailsUrl = "#" + VIEW_PATH + "/" + record.getAttributeAsString(FIELD_ID);
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);

                    }
                });
            }
            // TODO: adding the cell formatter for affinity group (clickable link)
        }
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerDetailView(extendLocatorId("detailsView"), id);
    }

    private void showActions() {
        addTableAction(extendLocatorId("setNormal"), MSG.view_adminTopology_server_setNormal(),
            MSG.common_msg_areYouSure(), new AuthorizedTableAction(this, TableActionEnablement.ANY,
                Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    List<String> selectedNames = getSelectedNames(selections);
                    // TODO: msg
                    //                       String message = MSG.view_admin_plugins_serverDisableConfirm(selectedNames.toString());
                    String message = "Really? Normal? For all I've done for you? " + selectedNames;
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                SC.say("setting servers to maintenance mode, ids: " + selectedIds);
                                GWTServiceLookup.getCloudService().updateServerMode(selectedIds,
                                    Server.OperationMode.NORMAL, new AsyncCallback<Void>() {
                                        public void onSuccess(Void arg0) {
                                            // TODO: msg
                                            Message msg = new Message(MSG
                                                .view_admin_plugins_disabledServerPlugins("sdf"), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            // TODO: msg
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
                                                    + caught.getMessage(), caught);
                                            refreshTableInfo();
                                        }

                                    });
                            } else {
                                refreshTableInfo();
                            }
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("setMaintenance"), MSG.view_adminTopology_server_setMaintenance(),
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    List<String> selectedNames = getSelectedNames(selections);
                    // TODO: msg
                    //                String message = MSG.view_admin_plugins_serverDisableConfirm(selectedNames.toString());
                    String message = "Really? Maitenance? For all I've done for you? " + selectedNames;
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                SC.say("setting servers to maintenance mode, ids: " + selectedIds);
                                GWTServiceLookup.getCloudService().updateServerMode(selectedIds,
                                    Server.OperationMode.MAINTENANCE, new AsyncCallback<Void>() {
                                        public void onSuccess(Void arg0) {
                                            // TODO: msg
                                            Message msg = new Message(MSG
                                                .view_admin_plugins_disabledServerPlugins("sdf"), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            // TODO: msg
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
                                                    + caught.getMessage(), caught);
                                            refreshTableInfo();
                                        }

                                    });
                            } else {
                                refreshTableInfo();
                            }
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("removeSelected"), MSG.view_adminTopology_server_removeSelected(),
            MSG.common_msg_areYouSure(), new AuthorizedTableAction(this, TableActionEnablement.ANY,
                Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    List<String> selectedNames = getSelectedNames(selections);
                    String message = "Really? Delete? For all I've done for you? " + selectedNames;
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                SC.say("setting servers to maintenance mode, ids: " + selectedIds);
                                GWTServiceLookup.getCloudService().deleteServers(selectedIds,
                                    new AsyncCallback<Void>() {
                                        public void onSuccess(Void arg0) {
                                            // TODO: msg
                                            Message msg = new Message(MSG
                                                .view_admin_plugins_disabledServerPlugins("sdf"), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            // TODO: msg
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
                                                    + caught.getMessage(), caught);
                                            refreshTableInfo();
                                        }

                                    });
                            } else {
                                refreshTableInfo();
                            }
                        }
                    });
                }
            });
    }

    private int[] getSelectedIds(ListGridRecord[] selections) {
        if (selections == null) {
            return new int[0];
        }
        int[] ids = new int[selections.length];
        int i = 0;
        for (ListGridRecord selection : selections) {
            ids[i++] = selection.getAttributeAsInt(FIELD_ID);
        }
        return ids;
    }

    private List<String> getSelectedNames(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        List<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_NAME));
        }
        return ids;
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
