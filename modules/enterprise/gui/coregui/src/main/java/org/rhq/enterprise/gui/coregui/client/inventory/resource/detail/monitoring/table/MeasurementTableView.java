/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.ArrayList;

import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.components.measurement.MeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * Views a resource's measurements in a tabular view.
 * 
 * @author John Mazzitelli
 */
public class MeasurementTableView extends Table<MeasurementTableDataSource> {

    public MeasurementTableView(String locatorId, int resourceId) {
        super(locatorId);
        setDataSource(new MeasurementTableDataSource(resourceId));
    }

    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();
        setListGridFields(fields.toArray(new ListGridField[0]));
        addExtraWidget(new MeasurementRangeEditor(extendLocatorId("range")), true);
    }
}
