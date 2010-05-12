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
package org.rhq.enterprise.gui.coregui.client.bundle.list;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.bundle.create.BundleCreateWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class BundlesListView extends VLayout {

    public BundlesListView() {
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final Table table = new Table("Bundles");

        table.setDataSource(new BundlesWithLatestVersionDataSource());

        table.getListGrid().getField("id").setWidth("60");
        table.getListGrid().getField("name").setWidth("25%");
        table.getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        table.getListGrid().getField("description").setWidth("25%");
        table.getListGrid().getField("latestVersion").setWidth("25%");
        table.getListGrid().getField("versionsCount").setWidth("*");

        table.getListGrid().setSelectionType(SelectionStyle.SIMPLE);
        table.getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);

        table.addTableAction("New", Table.SelectionEnablement.ALWAYS, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                new BundleCreateWizard().startBundleWizard();

            }
        });

        table.addTableAction("Delete", Table.SelectionEnablement.ANY, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selections) {
                BundlesWithLatestVersionDataSource ds = (BundlesWithLatestVersionDataSource) table.getDataSource();
                for (ListGridRecord selection : selections) {
                    BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                    final BundleWithLatestVersionComposite object = ds.copyValues(selection);
                    bundleManager.deleteBundle(object.getBundleId(), new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                "Failed to delete bundle [" + object.getBundleName() + "]", caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Deleted bundle [" + object.getBundleName() + "]", Severity.Info));
                        }
                    });
                }
            }
        });

        // can change this back to SINGLE selection when we feel like it. currently allowing the wizard to
        // select the bundle.
        table.addTableAction("Deploy", Table.SelectionEnablement.ALWAYS, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                if (selection.length == 0) {
                    new BundleDeployWizard().startBundleWizard();
                    return;
                }

                BundlesWithLatestVersionDataSource ds = (BundlesWithLatestVersionDataSource) table.getDataSource();
                final BundleWithLatestVersionComposite object = ds.copyValues(selection[0]);
                BundleCriteria bc = new BundleCriteria();
                bc.addFilterId(object.getBundleId());
                BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                bundleManager.findBundlesByCriteria(bc, new AsyncCallback<PageList<Bundle>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            "Failed to load bundle to deploy [" + object.getBundleName() + "]", caught);
                    }

                    public void onSuccess(PageList<Bundle> result) {
                        if (result == null || result.size() != 1) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Failed to get single bundle to deploy [" + object.getBundleName() + "]",
                                    Severity.Error));
                            return;
                        }
                        new BundleDeployWizard(result.get(0).getId()).startBundleWizard();
                    }
                });
            }
        });

        addMember(table);

    }
}
