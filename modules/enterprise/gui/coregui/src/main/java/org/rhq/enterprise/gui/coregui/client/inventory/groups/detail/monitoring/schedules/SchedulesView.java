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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules;

import com.smartgwt.client.data.Criteria;

import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;

/**
 * The group Monitoring>Schedules subtab.
 *
 * @author Ian Springer
 */
public class SchedulesView extends AbstractMeasurementScheduleListView {

    private static final String TITLE = MSG.view_group_meas_schedules_title();
    private static final String[] EXCLUDED_FIELD_NAMES = new String[] { MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID };

    private ResourceGroupComposite resourceGroupComposite;

    public SchedulesView(String locatorId, ResourceGroupComposite resourceGroupComposite) {
        super(locatorId, TITLE, new SchedulesDataSource(resourceGroupComposite.getResourceGroup().getId()),
                createCriteria(resourceGroupComposite.getResourceGroup().getId()), EXCLUDED_FIELD_NAMES);

        this.resourceGroupComposite = resourceGroupComposite;
    }

    public boolean hasManageMeasurementsPermission() {
        return this.resourceGroupComposite.getResourcePermission().isMeasure();
    }

    private static Criteria createCriteria(int resourceGroupId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID, resourceGroupId);
        return criteria;
    }

}
