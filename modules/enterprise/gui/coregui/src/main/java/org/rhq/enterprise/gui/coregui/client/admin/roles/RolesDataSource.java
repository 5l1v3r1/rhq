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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RoleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class RolesDataSource extends RPCDataSource {

    private static RolesDataSource INSTANCE;

    private RoleGWTServiceAsync roleService = GWTServiceLookup.getRoleService();

    public static RolesDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RolesDataSource();
        }
        return INSTANCE;
    }

    protected RolesDataSource() {
        super("Roles");

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);

        DataSourceTextField nameField = new DataSourceTextField("name", "Name");


        setFields(idDataField, nameField);
    }


    public void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        RoleCriteria criteria = new RoleCriteria();
        criteria.setPageControl(getPageControl(request));

        Integer subjectId = request.getCriteria().getAttributeAsInt("subjectId");
        if (subjectId != null) {
            criteria.setFilterSubjectId(subjectId);
        }

        criteria.setFetchResourceGroups(true);
        criteria.setFetchPermissions(true);
        criteria.setFetchSubjects(true);

        roleService.findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch Resource Data");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Role> result) {

                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int x = 0; x < result.size(); x++) {
                    Role role = result.get(x);
                    ListGridRecord record = new ListGridRecord();

                    copyValues(role, record);

                    records[x] = record;
                }

                response.setData(records);
                response.setTotalRows(result.getTotalSize());    // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }


    @Override
    protected void executeAdd(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        Role newRole = new Role("newName"); // TODO GH: Make default constructor public
        copyValues(rec, newRole);

        roleService.createRole(newRole, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                Map<String,String> errors = new HashMap<String, String>();
                errors.put("name", "A role with name already exists.");
                response.setErrors(errors);
//                CoreGUI.getErrorHandler().handleError("Failed to create role",caught);
                response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(Role result) {
                ListGridRecord record = new ListGridRecord();
                copyValues(result,record);
                response.setData(new Record[] {record});
                processResponse(request.getRequestId(),response);
            }
        });

    }

    @Override
    protected void executeUpdate(final DSRequest request, final DSResponse response) {
        final ListGridRecord record = getEditedRecord(request);
        System.out.println("Updating record: " + record);
        Role updatedRole = new Role("foo");
        copyValues(record, updatedRole);
        roleService.updateRole(updatedRole, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to update role",caught);
            }

            public void onSuccess(Role result) {
                System.out.println("Role Updated");
                copyValues(result,record);
                response.setData(new Record[] {record});
                processResponse(request.getRequestId(),response);
            }
        });
    }

    @Override
    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        Role newRole = new Role("newName"); // TODO GH: Make default constructor public
        copyValues(rec, newRole);

        roleService.removeRoles(new Integer[]{newRole.getId()}, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete role",caught);
            }

            public void onSuccess(Void result) {
                System.out.println("Role deleted");
                response.setData(new Record[]{rec});
                processResponse(request.getRequestId(), response);
            }
        });

    }

    private static void copyValues(ListGridRecord from, Role to) {
        to.setId(from.getAttributeAsInt("id"));
        to.setName(from.getAttributeAsString("name"));
//        to.setDate (from.getAttributeAsDate ("date"));
//        from.getAttributeAsIntArray("resourceGroups");
        to.setResourceGroups((Set<ResourceGroup>) from.getAttributeAsObject("resourceGroups"));
        to.setPermissions((Set<Permission>) from.getAttributeAsObject("permissions"));
        to.setSubjects((Set<Subject>) from.getAttributeAsObject("subjects"));
    }

    static void copyValues(Role from, ListGridRecord to) {
        to.setAttribute("id", from.getId());
        to.setAttribute("name", from.getName());
        to.setAttribute("resourceGroups", from.getResourceGroups());
        to.setAttribute("permissions", from.getPermissions());
        to.setAttribute("subjects", from.getSubjects());

        to.setAttribute("entity", from);
    }


    private ListGridRecord getEditedRecord(DSRequest request) {
        // Retrieving values before edit
        JavaScriptObject oldValues = request.getAttributeAsJavaScriptObject("oldValues");
        // Creating new record for combining old values with changes
        ListGridRecord newRecord = new ListGridRecord();
        // Copying properties from old record
        JSOHelper.apply(oldValues, newRecord.getJsObj());
        // Retrieving changed values
        JavaScriptObject data = request.getData();
        // Apply changes
        JSOHelper.apply(data, newRecord.getJsObj());
        return newRecord;
    }

    public static ListGridRecord[] buildRecords(Collection<Role> roles) {
        ListGridRecord[] roleRecords = new ListGridRecord[roles.size()];
        int i = 0;
        for (Role role : roles) {
            ListGridRecord record = new ListGridRecord();
            copyValues(role, record);
            roleRecords[i++] = record;
        }
        return roleRecords;
    }
}
