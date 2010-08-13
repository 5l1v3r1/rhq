/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * A view that displays a non-paginated table of {@link org.rhq.core.domain.measurement.MeasurementSchedule measurement
 * schedule}s, along with the ability to sort, enable, disable, or update the collection interval on those schedules.
 *
 * @author Ian Springer
 */
// TODO: Actually, this class probably does not need to be abstract...
public abstract class AbstractMeasurementScheduleListView extends Table {
    private static final String TITLE = "Metric Collection Schedules";

    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[]{
        new SortSpecifier(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME, SortDirection.ASCENDING)
    };

    public AbstractMeasurementScheduleListView(AbstractMeasurementScheduleDataSource dataSource) {
        this(dataSource, null, null);
    }

    public AbstractMeasurementScheduleListView(AbstractMeasurementScheduleDataSource dataSource, Criteria criteria, String[] excludedFieldNames) {
        super(TITLE, criteria, SORT_SPECIFIERS, excludedFieldNames);
        setDataSource(dataSource);
    }

    @Override
    public AbstractMeasurementScheduleDataSource getDataSource() {
        return (AbstractMeasurementScheduleDataSource) super.getDataSource();
    }

    @Override
    protected void onInit() {
        super.onInit();

        ListGrid listGrid = getListGrid();
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_DISPLAY_NAME).setWidth("20%");
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_DESCRIPTION).setWidth("30%");
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_DATA_TYPE).setWidth("20%");
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_ENABLED).setWidth("10%");
        listGrid.getField(MeasurementScheduleCriteria.SORT_FIELD_INTERVAL).setWidth("20%");

        addTableAction("Enable", Table.SelectionEnablement.ANY, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                getDataSource().enableSchedules(AbstractMeasurementScheduleListView.this);
            }
        });
        addTableAction("Disable", Table.SelectionEnablement.ANY, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                getDataSource().disableSchedules(AbstractMeasurementScheduleListView.this);
            }
        });
        // TODO: Add controls to set collection interval - base class is going to need massaging...
    }
}
