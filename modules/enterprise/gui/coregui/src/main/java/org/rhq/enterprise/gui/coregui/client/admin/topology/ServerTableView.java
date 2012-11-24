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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
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
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Jiri Kremser
 */
public class ServerTableView extends TableSection<ServerNodeDatasource> {

    public static final ViewName VIEW_ID = new ViewName("Servers(GWT)", MSG.view_adminTopology_servers() + "(GWT)",
        IconEnum.SERVERS);
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

    public ServerTableView(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
        setHeight100();
        setWidth100();
        setDataSource(new ServerNodeDatasource());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);

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
                                GWTServiceLookup.getCloudService().updateServerMode(selectedIds, Server.OperationMode.NORMAL, 
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
                                GWTServiceLookup.getCloudService().updateServerMode(selectedIds, Server.OperationMode.MAINTENANCE, 
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

        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerDetailView(extendLocatorId("detailsView"), id);
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

    private ArrayList<String> getSelectedNames(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        ArrayList<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_NAME));
        }
        return ids;
    }

}
