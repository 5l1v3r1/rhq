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

import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.enterprise.gui.coregui.client.bundle.create.BundleCreateWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.create.BundleUpdateWizard;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * @author Greg Hinkle
 */
public class BundlesListView extends VLayout {

    @Override
    protected void onDraw() {
        super.onDraw();

        Table table = new Table("Bundles");

        table.setDataSource(new BundlesDataSource());

        table.getListGrid().getField("id").setWidth("60");
        table.getListGrid().getField("name").setWidth("25%");
        table.getListGrid().getField("description").setWidth("*");

        table.getListGrid().setSelectionType(SelectionStyle.SIMPLE);
        table.getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);

        table.addTableAction("Create Bundle", Table.SelectionEnablement.ALWAYS, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                new BundleCreateWizard().startBundleWizard();

            }
        });

        table.addTableAction("Update Bundle", Table.SelectionEnablement.SINGLE, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                Bundle bundle = (Bundle) selection[0].getAttributeAsObject("entity");
                BundleUpdateWizard bundleUpdateWizard = new BundleUpdateWizard(null);
                bundleUpdateWizard.startBundleWizard();
            }
        });

        table.addTableAction("Deploy Selected Bundle", Table.SelectionEnablement.SINGLE, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                new BundleDeployWizard((Bundle) selection[0].getAttributeAsObject("object")).startBundleWizard();
            }
        });

        addMember(table);
    }
}
