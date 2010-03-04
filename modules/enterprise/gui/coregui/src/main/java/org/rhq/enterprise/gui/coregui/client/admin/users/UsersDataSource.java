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
package org.rhq.enterprise.gui.coregui.client.admin.users;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.FieldValueExtractor;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.MatchesFieldValidator;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class UsersDataSource extends RPCDataSource<Subject> {

    private static UsersDataSource INSTANCE;

    private SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();


    public static UsersDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UsersDataSource();
        }
        return INSTANCE;
    }


    private UsersDataSource() {
        super("Users");

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);

        DataSourceTextField usernameField = new DataSourceTextField("username", "User Name", 100, true);
        usernameField.setRequired(true);

        DataSourceTextField firstName = new DataSourceTextField("firstName", "First Name", 100, true);

        DataSourceTextField lastName = new DataSourceTextField("lastName", "Last Name", 100, true);

        DataSourceTextField password = new DataSourceTextField("password", "Password", 100, false);
        password.setType(FieldType.PASSWORD);

        LengthRangeValidator passwordValdidator = new LengthRangeValidator();
        passwordValdidator.setMin(6);
        passwordValdidator.setErrorMessage("Password must be at least six characters");
        password.setValidators(passwordValdidator);


        DataSourceTextField passwordVerify = new DataSourceTextField("passwordVerify", "Verify", 100, false);
        passwordVerify.setType(FieldType.PASSWORD);

        MatchesFieldValidator passwordsEqualValidator = new MatchesFieldValidator();
        passwordsEqualValidator.setOtherField("password");
        passwordsEqualValidator.setErrorMessage("Passwords do not match");
        passwordVerify.setValidators(passwordsEqualValidator);


        DataSourceTextField email = new DataSourceTextField("email", "Email Address", 100, true);

        DataSourceTextField phone = new DataSourceTextField("phoneNumber", "Phone");

        DataSourceTextField department = new DataSourceTextField("department", "Department");

        DataSourceField roles = new DataSourceField();
        roles.setForeignKey("Roles.id");
        roles.setName("roles");
        roles.setMultiple(true);


        setFields(idDataField, usernameField, firstName, lastName, password, passwordVerify, phone, email, department);
    }


    public void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        SubjectCriteria criteria = new SubjectCriteria();
        criteria.setPageControl(getPageControl(request));

        subjectService.findSubjectsByCriteria(criteria, new AsyncCallback<PageList<Subject>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
                System.err.println("Failed to fetch Resource Data");
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Subject> result) {
                System.out.println("Data retrieved in: " + (System.currentTimeMillis() - start));

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int x = 0; x < result.size(); x++) {
                    Subject subject = result.get(x);

                    records[x] = copyValues(subject);
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
        Subject newSubject = copyValues(rec);

        subjectService.createSubject(newSubject, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                // TODO better exceptions so we can set the right validation errors
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("username", "A user with this name already exists.");
                response.setErrors(errors);
//                CoreGUI.getErrorHandler().handleError("Failed to create role",caught);
                response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(Subject result) {
                response.setData(new Record[]{copyValues(result)});
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @Override
    protected void executeUpdate(final DSRequest request, final DSResponse response) {
        final ListGridRecord record = getEditedRecord(request);
        System.out.println("Updating record: " + record);
        final Subject updatedSubject = copyValues(record);
        subjectService.updateSubject(updatedSubject, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to update subject", caught);
            }

            public void onSuccess(final Subject result) {

                String password = record.getAttributeAsString("password");
                if (password != null) {
                    subjectService.changePassword(updatedSubject.getName(), password, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to update subject's password", caught);
                        }

                        public void onSuccess(Void nothing) {
                            System.out.println("Subject Updated");
                            response.setData(new Record[]{copyValues(result)});
                            processResponse(request.getRequestId(), response);

                        }
                    });
                } else {
                    System.out.println("Subject Updated");
                    response.setData(new Record[]{copyValues(result)});
                    processResponse(request.getRequestId(), response);
                }
            }
        });
    }

    @Override
    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        final ListGridRecord rec = new ListGridRecord(data);
        Subject subjectToDelete = copyValues(rec);

        subjectService.deleteSubjects(new int[]{subjectToDelete.getId()}, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete role", caught);
            }

            public void onSuccess(Void result) {
                System.out.println("Subject deleted");
                response.setData(new Record[]{rec});
                processResponse(request.getRequestId(), response);
            }
        });

    }


    public Subject copyValues(ListGridRecord from) {
        Subject to = new Subject();
        to.setId(from.getAttributeAsInt("id"));
        to.setName(from.getAttributeAsString("username"));
        to.setFirstName(from.getAttributeAsString("firstName"));
        to.setLastName(from.getAttributeAsString("lastName"));
        to.setFactive(from.getAttributeAsBoolean("factive"));
        to.setDepartment(from.getAttributeAsString("department"));
        to.setPhoneNumber(from.getAttributeAsString("phoneNumber"));
        to.setEmailAddress(from.getAttributeAsString("email"));

        to.setRoles((Set<Role>) from.getAttributeAsObject("roles"));
        return to;
    }

    public ListGridRecord copyValues(Subject from) {
        ListGridRecord to = new ListGridRecord();
        to.setAttribute("id", from.getId());
        to.setAttribute("username", from.getName());
        to.setAttribute("firstName", from.getFirstName());
        to.setAttribute("lastName", from.getLastName());
        to.setAttribute("factive", from.getFactive());
        to.setAttribute("department", from.getDepartment());
        to.setAttribute("phoneNumber", from.getPhoneNumber());
        to.setAttribute("email", from.getEmailAddress());

        to.setAttribute("roles", from.getRoles());

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

}
