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
package org.rhq.enterprise.gui.coregui.client.components.upload;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

/**
 * Upload a single file and use it to create a new PackageVersion. 
 * 
 * @author Jay Shaughnessy
 * @author Lukas Krejci
 */
public class PackageVersionFileUploadForm extends FileUploadForm {

    private int packageTypeId;
    private Integer archId;
    private Integer repoId;
    private int packageVersionId;

    public PackageVersionFileUploadForm(String locatorId, int packageTypeId, String packageName, String version,
        Integer archId, Integer repoId, boolean showNameLabel, boolean showUploadButton, Boolean isAlreadyUploaded) {

        super(locatorId, packageName, version, showNameLabel, showUploadButton, isAlreadyUploaded);

        this.packageTypeId = packageTypeId;
        this.archId = archId;
        this.repoId = repoId;
        
        setAction(GWT.getModuleBaseURL() + "PackageVersionFileUploadServlet");
    }

    /**
     * If this component successfully uploaded a package version file, this will return
     * the new package version's ID. Otherwise, 0 is returned.
     * 
     * @return the new package version ID
     */
    public int getPackageVersionId() {
        return this.packageVersionId;
    }

    public int getPackageTypeId() {
        return packageTypeId;
    }
    
    public void setPackageTypeId(int value) {
        packageTypeId = value;
        if (onDrawCalled()) {
            getItem("packageTypeId").setValue(value);
        }
    }
    
    public Integer getArchitectureId() {
        return archId;
    }
    
    public void setArchitectureId(Integer value) {
        archId = value; 
        FormItem item = getItem("archId");
        if (item != null) {
            if (value == null) {
                removeField("archId");
            } else {
                item.setDefaultValue(value);
            }
        } else if (value != null && onDrawCalled()) {
            HiddenItem archIdField = new HiddenItem("archId");
            archIdField.setDefaultValue(value);
            addField(archIdField);
        }
    }
    
    public Integer getRepoId() {
        return repoId;
    }
    
    public void setRepoId(Integer value) {
        repoId = value;
        FormItem item = getItem("repoId");
        if (item != null) {
            if (value != null) {
                removeField("repoId");
            } else {
                item.setDefaultValue(value);
            }
        } else if (value != null && onDrawCalled()) {
            HiddenItem repoIdField = new HiddenItem("repoId");
            repoIdField.setDefaultValue(value);
            addField(repoIdField);
        }
    }
    
    @Override
    protected List<FormItem> getOnDrawItems() {
        List<FormItem> onDrawItems = super.getOnDrawItems();

        HiddenItem packageTypeIdField = new HiddenItem("packageTypeId");
        packageTypeIdField.setValue(packageTypeId);
        onDrawItems.add(packageTypeIdField);

        if (null != archId) {
            HiddenItem archIdField = new HiddenItem("archId");
            archIdField.setDefaultValue(archId);
            onDrawItems.add(archIdField);
        }

        if (null != repoId) {
            HiddenItem repoIdField = new HiddenItem("repoId");
            repoIdField.setDefaultValue(repoId);
            onDrawItems.add(repoIdField);
        }
        
        return onDrawItems;
    }

    protected boolean processSubmitCompleteResults(String submitCompleteEventResults) {
        packageVersionId = parseIdFromResponse(submitCompleteEventResults);
        return (packageVersionId > 0);
    }

    private int parseIdFromResponse(String results) {
        String successMsgPrefix = "success ["; // the upload servlet will respond with "success [packageVersionId]" on success
        int startSuccessMsgPrefix = results.indexOf(successMsgPrefix);
        if (startSuccessMsgPrefix < 0) {
            return 0; // must mean it wasn't a success - results is probably an error message
        }
        int endSuccessMsgPrefix = startSuccessMsgPrefix + successMsgPrefix.length();
        int startSuccessMsgPostfix = results.indexOf(']', endSuccessMsgPrefix);
        if (startSuccessMsgPostfix < 0) {
            return 0; // this should never happen, if we have "success [" we should always have the ending "]" bracket
        }
        String packageVersionIdString = results.substring(endSuccessMsgPrefix, startSuccessMsgPostfix);
        int id = 0;
        try {
            id = Integer.parseInt(packageVersionIdString);
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_packageVersionFile(), e);
        }
        return id;
    }

    @Override
    public void submitForm() {

        // reset the name value from the package type name to the path of the actual uploaded file. The
        // name portion will be parsed out and used for the package name.
        FormItem nameItem = getItem("name");
        FormItem fileUploadItem = getItem("fileUploadItem");
        String uploadFilePath = (String) fileUploadItem.getValue();
        nameItem.setValue(uploadFilePath);

        super.submitForm();
    }

    private void removeField(String fieldName) {
        FormItem[] items = getFields();
        FormItem[] newItems = new FormItem[items.length - 1];
        int idx = 0;
        for (FormItem i : items) {
            if (!fieldName.equals(i.getName())) {
                newItems[idx] = i;
            }
            ++idx;
        }
        
        setFields(newItems);
    }
    
    private void addField(FormItem item) {
        FormItem[] items = getFields();
        FormItem[] newItems = new FormItem[items.length + 1];
        
        System.arraycopy(items, 0, newItems, 0, items.length);
        newItems[items.length] = item;
    }    
}
