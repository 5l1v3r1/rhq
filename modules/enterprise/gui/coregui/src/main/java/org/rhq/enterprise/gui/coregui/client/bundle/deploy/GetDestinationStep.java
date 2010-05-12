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

import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.selection.PlatformResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.AbstractSelector;

public class GetDestinationStep implements WizardStep {

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private final BundleDeployWizard wizard;
    private VLayout form;
    DynamicForm valForm = new DynamicForm();
    private AbstractSelector<ResourceGroup> selector;
    private BundleDestination dest = new BundleDestination();

    public GetDestinationStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return "New Destination";
    }

    public Canvas getCanvas() {
        if (this.form == null) {
            this.form = new VLayout();

            this.valForm.setWidth100();
            this.valForm.setNumCols(2);
            this.valForm.setColWidths("50%", "*");

            final TextItem nameTextItem = new TextItem("name", "Destination Name");
            nameTextItem.setRequired(true);
            nameTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setSubtitle(value.toString());
                    dest.setName(value.toString());
                }
            });

            final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", "Destination Description");
            descriptionTextAreaItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    dest.setDescription(value.toString());
                }
            });

            final TextItem deployDirTextItem = new TextItem("deployDir",
                "Root Deployment Directory (on destination platforms)");
            deployDirTextItem.setRequired(true);
            deployDirTextItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    dest.setDeployDir(value.toString());
                }
            });

            this.valForm.setItems(nameTextItem, descriptionTextAreaItem, deployDirTextItem);
            CanvasItem ci1 = new CanvasItem();
            ci1.setShowTitle(false);
            ci1.setCanvas(valForm);
            ci1.setDisabled(true);

            this.selector = new PlatformResourceGroupSelector();
            CanvasItem ci2 = new CanvasItem();
            ci2.setShowTitle(false);
            ci2.setCanvas(this.selector);
            ci2.setDisabled(true);

            this.form.addMember(this.valForm);
            this.form.addMember(this.selector);
        }

        return this.form;
    }

    public boolean nextPage() {
        HashSet<Integer> selection = this.selector.getSelection();
        if (selection.size() != 1) {
            SC.warn("Select only a single destination group for deployment.");
            return false;
        }

        bundleServer.createBundleDestination(wizard.getBundleId(), this.dest.getName(), this.dest.getDescription(),
            this.dest.getDeployDir(), selection.iterator().next(), //
            new AsyncCallback<BundleDestination>() {
                public void onSuccess(BundleDestination result) {
                    wizard.setBundleDestination(result);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to create destination: " + caught.getMessage(),
                        caught);
                }
            });

        return true;
    }
}
