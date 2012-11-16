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
package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.ServerTableView.CloudDataSource;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.upload.PluginFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;

/**
 * @author Jiri Kremser
 */
public class ServerTableView extends TableSection<CloudDataSource> {

    public static final ViewName VIEW_ID = new ViewName("Servers", MSG.view_adminTopology_servers(), IconEnum.SERVERS);
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MODE = "mode";
    private static final String FIELD_ENDPOINT_ADDRESS = "endpointAddress";
    private static final String FIELD_NONSECURE_PORT = "nonsecurePort";
    private static final String FIELD_SECURE_PORT = "securePort";
    private static final String FIELD_LAST_UPDATE_TIME = "lastUpdateTime";
    private static final String FIELD_AFFINITY_GROUP = "affinityGroup";
    private static final String FIELD_AGENT_COUNT = "agentCount";

    private boolean showUndeployed = false;

    public ServerTableView(String locatorId) {
        super(locatorId, null);
        setHeight100();
        setWidth100();
        setDataSource(new CloudDataSource());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);

        addTableAction(extendLocatorId("enable"), MSG.common_button_enable(), MSG.common_msg_areYouSure(),
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    int[] selectedIds = getSelectedIds(selections);
                    GWTServiceLookup.getPluginService().enableServerPlugins(selectedIds,
                        new AsyncCallback<ArrayList<String>>() {
                            @Override
                            public void onSuccess(ArrayList<String> result) {
                                Message msg = new Message(
                                    MSG.view_admin_plugins_enabledServerPlugins(result.toString()), Severity.Info);
                                CoreGUI.getMessageCenter().notify(msg);
                                refresh();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_admin_plugins_enabledServerPluginsFailure() + " " + caught.getMessage(),
                                    caught);
                                refreshTableInfo();
                            }
                        });
                }
            });

        addTableAction(extendLocatorId("disable"), MSG.common_button_disable(), new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                ArrayList<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_admin_plugins_serverDisableConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            GWTServiceLookup.getPluginService().disableServerPlugins(selectedIds,
                                new AsyncCallback<ArrayList<String>>() {
                                    @Override
                                    public void onSuccess(ArrayList<String> result) {
                                        Message msg = new Message(MSG.view_admin_plugins_disabledServerPlugins(result
                                            .toString()), Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
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

        addTableAction(extendLocatorId("undeploy"), MSG.view_admin_plugins_undeploy(), new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                ArrayList<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_admin_plugins_serverUndeployConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            GWTServiceLookup.getPluginService().undeployServerPlugins(selectedIds,
                                new AsyncCallback<ArrayList<String>>() {
                                    @Override
                                    public void onSuccess(ArrayList<String> result) {
                                        Message msg = new Message(MSG.view_admin_plugins_undeployedServerPlugins(result
                                            .toString()), Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_admin_plugins_undeployedServerPluginsFailure() + " "
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

        addTableAction(extendLocatorId("purge"), MSG.common_button_purge(), MSG.common_msg_areYouSure(),
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public boolean isEnabled(ListGridRecord[] selection) {
                    if (showUndeployed) {
                        return super.isEnabled(selection);
                    } else {
                        return false; // we aren't showing undeployed plugins, so there is no plugin shown that can be purged anyway
                    }
                }

                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    int[] selectedIds = getSelectedIds(selections);
                    GWTServiceLookup.getPluginService().purgeServerPlugins(selectedIds,
                        new AsyncCallback<ArrayList<String>>() {
                            @Override
                            public void onSuccess(ArrayList<String> result) {
                                Message msg = new Message(
                                    MSG.view_admin_plugins_purgedServerPlugins(result.toString()), Severity.Info);
                                CoreGUI.getMessageCenter().notify(msg);
                                refresh();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_admin_plugins_purgedServerPluginsFailure() + " " + caught.getMessage(),
                                    caught);
                                refreshTableInfo();
                            }
                        });
                }
            });

        IButton scanForUpdatesButton = new LocatableIButton(extendLocatorId("scanButton"),
            MSG.view_admin_plugins_scan());
        scanForUpdatesButton.setAutoFit(true);
        scanForUpdatesButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                GWTServiceLookup.getPluginService().scanAndRegister(new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Message msg = new Message(MSG.view_admin_plugins_scanComplete(), Severity.Info);
                        CoreGUI.getMessageCenter().notify(msg);
                        refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_admin_plugins_scanFailure() + " " + caught.getMessage(), caught);
                        refreshTableInfo();
                    }
                });
            }
        });

        IButton restartMasterPCButton = new LocatableIButton(extendLocatorId("restartMasterPCButton"),
            MSG.view_admin_plugins_restartMasterPC());
        restartMasterPCButton.setAutoFit(true);
        restartMasterPCButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Message msg = new Message(MSG.view_admin_plugins_restartMasterPCStarted(), Severity.Info);
                CoreGUI.getMessageCenter().notify(msg);

                GWTServiceLookup.getPluginService().restartMasterPluginContainer(new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Message msg = new Message(MSG.view_admin_plugins_restartMasterPCComplete(), Severity.Info);
                        CoreGUI.getMessageCenter().notify(msg);
                        refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_admin_plugins_restartMasterPCFailure() + " " + caught.getMessage(), caught);
                        refreshTableInfo();
                    }
                });
            }
        });


        PluginFileUploadForm pluginUploadForm = new PluginFileUploadForm(extendLocatorId("upload"),
            MSG.view_admin_plugins_upload(), true);

        addExtraWidget(scanForUpdatesButton, true);
        addExtraWidget(restartMasterPCButton, true);
        addExtraWidget(pluginUploadForm, true);

        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerPluginDetailView(extendLocatorId("detailsView"), id);
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

    public class CloudDataSource extends RPCDataSource<Server, Criteria> {

        public CloudDataSource() {
            super();
            List<DataSourceField> fields = addDataSourceFields();
            addFields(fields);
        }

        @Override
        protected List<DataSourceField> addDataSourceFields() {
            List<DataSourceField> fields = super.addDataSourceFields();
            DataSourceField idField = new DataSourceIntegerField(FIELD_ID, MSG.common_title_id(), 50);
            idField.setPrimaryKey(true);
            idField.setHidden(true);
            fields.add(idField);
            return fields;
        }

        public List<ListGridField> getListGridFields() {
            List<ListGridField> fields = new ArrayList<ListGridField>();

            ListGridField idField = new ListGridField(FIELD_ID, MSG.common_title_id());
            idField.setHidden(true);
            fields.add(idField);

            ListGridField nameField = new ListGridField(FIELD_NAME, MSG.common_title_name());
            fields.add(nameField);

            ListGridField modeField = new ListGridField(FIELD_MODE, MSG.view_adminTopology_server_mode());
            fields.add(modeField);

            ListGridField endpointAddressField = new ListGridField(FIELD_ENDPOINT_ADDRESS, MSG.view_adminTopology_server_endpointAddress());
//            TimestampCellFormatter.prepareDateField(endpointAddressField);
            fields.add(endpointAddressField);

            ListGridField nonsecurePortField = new ListGridField(FIELD_NONSECURE_PORT, MSG.view_adminTopology_server_nonSecurePort());
//            enabledField.setType(ListGridFieldType.IMAGE);
//            enabledField.setAlign(Alignment.CENTER);
            fields.add(nonsecurePortField);

            ListGridField securedPortField = new ListGridField(FIELD_SECURE_PORT, MSG.view_adminTopology_server_securePort());
//            deployedField.setType(ListGridFieldType.IMAGE);
//            deployedField.setAlign(Alignment.CENTER);
//            deployedField.setHidden(true);
            fields.add(securedPortField);

            ListGridField lastUpdateTimeField = new ListGridField(FIELD_LAST_UPDATE_TIME, MSG.view_adminTopology_server_lastUpdateTime());
            TimestampCellFormatter.prepareDateField(endpointAddressField);
            fields.add(lastUpdateTimeField);
            
            ListGridField affinityGroupField = new ListGridField(FIELD_AFFINITY_GROUP, MSG.view_adminTopology_server_affinityGroup());
            fields.add(affinityGroupField);
            
            ListGridField agentCountField = new ListGridField(FIELD_AGENT_COUNT, MSG.view_adminTopology_server_agentCount());
            fields.add(agentCountField);

            idField.setWidth(100);
            nameField.setWidth("30%");
            modeField.setWidth("*");
            endpointAddressField.setWidth("20%");
            nonsecurePortField.setWidth(65);
            securedPortField.setWidth(75);
            lastUpdateTimeField.setWidth(100);

            return fields;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
//            GWTServiceLookup.getPluginService().getServerPlugins(showUndeployed,
//                new AsyncCallback<ArrayList<Server>>() {
//                    public void onSuccess(ArrayList<Server> result) {
//                        response.setData(buildRecords(result));
//                        response.setTotalRows(result.size());
//                        processResponse(request.getRequestId(), response);
//                    }
//
//                    @Override
//                    public void onFailure(Throwable t) {
//                        CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), t);
//                        response.setStatus(DSResponse.STATUS_FAILURE);
//                        processResponse(request.getRequestId(), response);
//                    }
//                });
        }

        @Override
        public Server copyValues(Record from) {
            throw new UnsupportedOperationException("ServerTableView.CloudDataSourcepublic Server copyValues(Record from)");
        }

        @Override
        public ListGridRecord copyValues(Server from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_ID, from.getId());
            record.setAttribute(FIELD_NAME, from.getName());
            record.setAttribute(FIELD_MODE, from.getOperationMode());
            record.setAttribute(FIELD_ENDPOINT_ADDRESS, from.getAddress());
            record.setAttribute(FIELD_NONSECURE_PORT, from.getPort());
            record.setAttribute(FIELD_SECURE_PORT, from.getSecurePort());
            record.setAttribute(FIELD_LAST_UPDATE_TIME, from.getMtime());
            record.setAttribute(FIELD_AFFINITY_GROUP, from.getAffinityGroup());
            record.setAttribute(FIELD_AGENT_COUNT, from.getAgentCount());
            return record;
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            // we don't use criteria for this datasource, just return null
            return null;
        }
    }

}
