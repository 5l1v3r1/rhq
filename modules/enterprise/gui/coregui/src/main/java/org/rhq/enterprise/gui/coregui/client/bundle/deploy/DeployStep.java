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
package org.rhq.enterprise.gui.coregui.client.bundle.deploy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Jay Shaughnessy
 *
 */
public class DeployStep implements WizardStep {

    private VLayout canvas;
    private final BundleDeployWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public DeployStep(BundleDeployWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public String getName() {
        return "Deploy Bundle to Destination Platforms";
    }

    public Canvas getCanvas() {
        if (canvas == null) {
            canvas = new VLayout();
            canvas.setWidth100();
            canvas.setHeight100();
            canvas.setAlign(Alignment.CENTER);

            final Img deployingImage = new Img("/images/status-bar.gif");
            deployingImage.setLayoutAlign(Alignment.CENTER);
            deployingImage.setWidth(50);
            deployingImage.setHeight(15);

            final Label deployingMessage = new Label("Deploying...");
            deployingMessage.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

            canvas.addMember(deployingImage);
            canvas.addMember(deployingMessage);

            bundleServer.createBundleDeployment(wizard.getBundleVersion().getId(), wizard.getBundleDestination()
                .getId(), wizard.getName(), wizard.getDescription(), wizard.getConfig(), false, -1, false, //
                new AsyncCallback<BundleDeployment>() {
                    public void onSuccess(BundleDeployment result) {
                        deployingImage.setSrc("/images/status_complete.gif");
                        deployingMessage.setText("Created Deployment...");
                        CoreGUI.getMessageCenter().notify(
                            new Message("Created deployment [" + result.getName() + "] description ["
                                + result.getDescription(), Severity.Info));
                        wizard.setBundleDeployment(result);

                        bundleServer.scheduleBundleDeployment(wizard.getBundleDeployment().getId(), //
                            new AsyncCallback<BundleDeployment>() {
                                public void onSuccess(BundleDeployment result) {
                                    deployingImage.setSrc("/images/status_complete.gif");
                                    deployingMessage.setText("Bundle Group Deployment Scheduled!");
                                    CoreGUI.getMessageCenter().notify(
                                        new Message("Schedule bundle group deployment ["
                                            + wizard.getBundleDeployment().getName() + "] resource group ["
                                            + result.getDestination().getGroup() + "]", Severity.Info));
                                    wizard.setBundleDeployment(result);
                                }

                                public void onFailure(Throwable caught) {
                                    deployingImage.setSrc("/images/status_error.gif");
                                    deployingMessage.setText("Failed to Schedule Group Deployment!");
                                    CoreGUI.getErrorHandler().handleError(
                                        "Failed to schedule group deployment: " + caught.getMessage(), caught);
                                    wizard.setBundleDeployment(null);
                                }
                            });
                    }

                    public void onFailure(Throwable caught) {
                        deployingImage.setSrc("/images/status_error.gif");
                        deployingMessage.setText("Failed to create deployment!");
                        CoreGUI.getErrorHandler().handleError("Failed to create deployment: " + caught.getMessage(),
                            caught);
                    }
                });
        }

        return canvas;
    }

    public boolean nextPage() {
        return true;
    }
}
