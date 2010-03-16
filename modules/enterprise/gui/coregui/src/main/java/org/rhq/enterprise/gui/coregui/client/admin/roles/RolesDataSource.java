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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
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
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class RolesDataSource extends RPCDataSource<Role> {

    private RoleGWTServiceAsync roleService = GWTServiceLookup.getRoleService();

    private static RolesDataSource INSTANCE;

    public static RolesDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RolesDataSource();
        }
        return INSTANCE;
    }

    private RolesDataSource() {
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
            criteria.addFilterSubjectId(subjectId);
        }

        criteria.fetchResourceGroups(true);
        criteria.fetchPermissions(true);
        criteria.fetchSubjects(true);

        roleService.findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch Roles Data", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Role> result) {

                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                response.setData(buildRecords(result));
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected void executeAdd(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        Role newRole = copyValues(rec);

        roleService.createRole(newRole, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("name", "A role with name already exists.");
                response.setErrors(errors);
                //                CoreGUI.getErrorHandler().handleError("Failed to create role",caught);
                response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(Role result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Role [" + result.getName() + "] added", Message.Severity.Info));
                ListGridRecord record = new ListGridRecord();
                response.setData(new Record[] { copyValues(result) });
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @Override
    protected void executeUpdate(final DSRequest request, final DSResponse response) {
        final ListGridRecord record = getEditedRecord(request);
        System.out.println("Updating record: " + record);
        Role updatedRole = copyValues(record);
        roleService.updateRole(updatedRole, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to update role", caught);
            }

            public void onSuccess(Role result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Role [" + result.getName() + "] updated", Message.Severity.Info));
                System.out.println("Role Updated");
                response.setData(new Record[] { copyValues(result) });
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        final Role newRole = copyValues(rec);

        roleService.removeRoles(new Integer[] { newRole.getId() }, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete role", caught);
            }

            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Role [" + newRole.getName() + "] removed", Message.Severity.Info));
                System.out.println("Role deleted");
                response.setData(new Record[] { rec });
                processResponse(request.getRequestId(), response);
            }
        });

    }

    public Role copyValues(ListGridRecord from) {
        Role to = new Role();
        to.setId(from.getAttributeAsInt("id"));
        to.setName(from.getAttributeAsString("name"));
        //        to.setDate (from.getAttributeAsDate ("date"));
        //        from.getAttributeAsIntArray("resourceGroups");
        to.setResourceGroups((Set<ResourceGroup>) from.getAttributeAsObject("resourceGroups"));
        to.setPermissions((Set<Permission>) from.getAttributeAsObject("permissions"));
        to.setSubjects((Set<Subject>) from.getAttributeAsObject("subjects"));
        return to;
    }

    public ListGridRecord copyValues(Role from) {
        ListGridRecord to = new ListGridRecord();
        to.setAttribute("id", from.getId());
        to.setAttribute("name", from.getName());
        to.setAttribute("resourceGroups", from.getResourceGroups());
        to.setAttribute("permissions", from.getPermissions());
        to.setAttribute("subjects", from.getSubjects());

        to.setAttribute("entity", from);
        return to;
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

    public ListGridRecord[] buildRecords(Collection<Role> roles) {
        ListGridRecord[] roleRecords = new ListGridRecord[roles.size()];
        int i = 0;
        for (Role role : roles) {
            ListGridRecord record = copyValues(role);
            roleRecords[i++] = record;
        }
        return roleRecords;
    }
}
