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
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.upload.BundleFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

public class BundleUploadDataStep implements WizardStep {

    private final AbstractBundleCreateWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private ArrayList<BundleFileUploadForm> uploadForms;
    private Boolean noFilesNeedToBeUploaded = null; // will be non-null when we know the answer

    public BundleUploadDataStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        final VLayout mainLayout = new VLayout();
        mainLayout.setMargin(Integer.valueOf(20));
        mainLayout.setWidth100();
        mainLayout.setHeight(10);

        bundleServer.getAllBundleVersionFilenames(this.wizard.getBundleVersion().getId(),
            new AsyncCallback<HashMap<String, Boolean>>() {

                public void onSuccess(HashMap<String, Boolean> result) {
                    wizard.setAllBundleFilesStatus(result);
                    prepareForm(mainLayout);
                    if (noFilesNeedToBeUploaded) {
                        wizard.getView().incrementStep();
                    }
                }

                public void onFailure(Throwable caught) {
                    wizard.setAllBundleFilesStatus(null);
                    CoreGUI.getErrorHandler().handleError("Cannot obtain bundle file information from server", caught);
                }
            });

        return mainLayout;
    }

    public boolean nextPage() {
        return isFinished();
    }

    public String getName() {
        return "Upload Bundle Files";
    }

    private boolean isFinished() {
        if (noFilesNeedToBeUploaded != null && noFilesNeedToBeUploaded.booleanValue()) {
            return true;
        }

        if (wizard.getAllBundleFilesStatus() == null) {
            return false;
        }

        boolean needToUpload = false;
        for (BundleFileUploadForm uploadForm : this.uploadForms) {
            if (uploadForm.getUploadResults() == null) {
                uploadForm.submitForm();
                needToUpload = true;
            }
        }
        if (needToUpload) {
            return false;
        }

        if (wizard.getAllBundleFilesStatus().containsValue(Boolean.FALSE)) {
            return false;
        }
        return true;
    }

    private void prepareForm(VLayout mainLayout) {
        // if there are no files to upload, immediately skip this step       
        final HashMap<String, Boolean> allFilesStatus = wizard.getAllBundleFilesStatus();
        noFilesNeedToBeUploaded = Boolean.TRUE;

        if (null == allFilesStatus || allFilesStatus.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : allFilesStatus.entrySet()) {
            if (!entry.getValue()) {
                noFilesNeedToBeUploaded = Boolean.FALSE;
                break;
            }
        }
        if (noFilesNeedToBeUploaded) {
            return;
        }

        if (allFilesStatus != null && allFilesStatus.size() == 0) {
            HeaderLabel label = new HeaderLabel("No files need to be uploaded for this bundle");
            label.setWidth100();
            mainLayout.addMember(label);
            uploadForms = null;
            noFilesNeedToBeUploaded = Boolean.TRUE;
            return;
        }

        noFilesNeedToBeUploaded = Boolean.FALSE;
        uploadForms = new ArrayList<BundleFileUploadForm>();

        for (Map.Entry<String, Boolean> entry : allFilesStatus.entrySet()) {
            String fileToBeUploaded = entry.getKey();
            Boolean isAlreadyUploaded = entry.getValue();

            HLayout indivLayout = new HLayout();
            indivLayout.setWidth100();
            indivLayout.setAutoHeight();

            Label nameLabel = new Label(fileToBeUploaded + ": ");
            nameLabel.setWidth("*");
            nameLabel.setAlign(Alignment.RIGHT);
            nameLabel.setLayoutAlign(VerticalAlignment.CENTER);
            indivLayout.addMember(nameLabel);

            final BundleFileUploadForm uploadForm = new BundleFileUploadForm(this.wizard.getBundleVersion(),
                fileToBeUploaded, false, (isAlreadyUploaded) ? Boolean.TRUE : null);
            uploadForm.setWidth("75%");
            indivLayout.addMember(uploadForm);

            uploadForm.addFormHandler(new DynamicFormHandler() {
                public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                    String results = event.getResults();
                    if (!results.contains("Failed to upload bundle file")) {
                        allFilesStatus.put(uploadForm.getName(), Boolean.TRUE);
                    } else {
                        allFilesStatus.put(uploadForm.getName(), Boolean.FALSE);
                        CoreGUI.getMessageCenter().notify(
                            new Message("Failed to upload bundle file", results, Message.Severity.Error));
                    }
                }
            });
            uploadForm.addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
                public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                    allFilesStatus.put(uploadForm.getName(), Boolean.FALSE);
                    CoreGUI.getMessageCenter().notify(
                        new Message("Failed to upload file", null, Message.Severity.Error));
                }
            });

            uploadForms.add(uploadForm);

            mainLayout.addMember(indivLayout);
        }

        return;
    }
}
