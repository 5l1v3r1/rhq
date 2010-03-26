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

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class BundleFileUploadForm extends DynamicCallbackForm {

    private UploadItem bundleUploadItem;
    private ButtonItem uploadButton;
    private StaticTextItem icon;

    private Boolean uploadResults;

    private final BundleVersion bundleVersion;
    private final String name;
    private final boolean showNameLabel;
    private final FormItemIcon iconLoading;
    private final FormItemIcon iconGreen;
    private final FormItemIcon iconRed;
    private final FormItemIcon iconGrey;

    public BundleFileUploadForm(BundleVersion bundleVersion, String name, boolean showNameLabel,
        Boolean isAlreadyUploaded) {

        super(name);
        this.bundleVersion = bundleVersion;
        this.name = name;
        this.showNameLabel = showNameLabel;
        this.uploadResults = isAlreadyUploaded; // null if unknown, false if error during previous upload attempt, true if already uploaded before

        setEncoding(Encoding.MULTIPART);
        setAction(GWT.getModuleBaseURL() + "/BundleFileUploadServlet");

        iconLoading = new FormItemIcon();
        iconLoading.setSrc("ajax-loader.gif");
        iconLoading.setWidth(16);
        iconLoading.setHeight(16);

        iconGreen = new FormItemIcon();
        iconGreen.setSrc("/images/icons/availability_green_16.png");
        iconGreen.setWidth(16);
        iconGreen.setHeight(16);

        iconRed = new FormItemIcon();
        iconRed.setSrc("/images/icons/availability_red_16.png");
        iconRed.setWidth(16);
        iconRed.setHeight(16);

        iconGrey = new FormItemIcon();
        iconGrey.setSrc("/images/icons/availability_grey_16.png");
        iconGrey.setWidth(16);
        iconGrey.setHeight(16);
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    /**
     * The name of the file that is to be uploaded to the server. The actual client file can
     * be named whatever, but the server will use this name as the name of the bundle file.
     * @return bundle file name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if the file was successfully uploaded, false if an error occurred.
     * Returns null if this upload form has not be submitted yet (see {@link #submitForm()}).
     * @return status of the upload request
     */
    public Boolean getUploadResults() {
        return uploadResults;
    }

    @Override
    public void submitForm() {
        Object value = bundleUploadItem.getValue();
        if (value == null || value.toString().length() == 0) {
            icon.setIcons(iconRed);
            icon.setTooltip("Please select a file to upload");
        } else {
            icon.setIcons(iconLoading);
            icon.setTooltip("Loading...");
            super.submitForm();
        }
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        HiddenItem sessionIdField = new HiddenItem("sessionid");
        sessionIdField.setValue(CoreGUI.getSessionSubject().getSessionId().intValue());

        HiddenItem bundleVersionIdField = new HiddenItem("bundleVersionId");
        bundleVersionIdField.setValue(this.bundleVersion.getId());

        HiddenItem nameField = new HiddenItem("name");
        nameField.setValue(this.name);

        HiddenItem versionField = new HiddenItem("version");
        versionField.setValue("1.0");

        setNumCols(7);

        bundleUploadItem = new UploadItem("bundleFileUploadItem", name);
        bundleUploadItem.setEndRow(false);
        bundleUploadItem.setShowTitle(showNameLabel);

        uploadButton = new ButtonItem("Upload");
        uploadButton.setStartRow(false);
        uploadButton.setEndRow(false);
        uploadButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                submitForm();
            }
        });

        icon = new StaticTextItem("icon");
        icon.setStartRow(false);
        icon.setShowTitle(false);
        if (uploadResults != null) {
            if (uploadResults.booleanValue()) {
                icon.setIcons(iconGreen);
                icon.setTooltip("Bundle file has already been uploaded");
            } else {
                icon.setIcons(iconRed);
                icon.setTooltip("Bundle file upload has previously failed");
            }
        } else {
            icon.setIcons(iconGrey);
            icon.setTooltip("Select a file to upload, then click the 'Upload' button or 'Next'");
        }
        icon.setShowIcons(true);

        setItems(sessionIdField, bundleVersionIdField, nameField, versionField, bundleUploadItem, uploadButton, icon);

        addFormHandler(new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                String results = event.getResults();
                if (!results.contains("Failed to upload bundle file")) {
                    uploadResults = Boolean.TRUE;
                    icon.setIcons(iconGreen);
                    icon.setTooltip("Uploaded bundle file successfully");
                    CoreGUI.getMessageCenter().notify(
                        new Message("Uploaded bundle file successfully", results, Severity.Info));
                    icon.hide();
                    icon.show();
                } else {
                    uploadResults = Boolean.FALSE;
                    icon.setIcons(iconRed);
                    icon.setTooltip("Bundle file upload failed");
                    CoreGUI.getMessageCenter()
                        .notify(new Message("Bundle file upload failed", results, Severity.Error));
                    icon.hide();
                    icon.show();
                }
            }
        });

        addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                uploadResults = Boolean.FALSE;
                icon.setIcons(iconRed);
                icon.setTooltip("Bundle file upload failed");
                CoreGUI.getMessageCenter().notify(new Message("Bundle file upload request failed", Severity.Error));
                icon.hide();
                icon.show();
            }
        });
    }
}
