/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.measurement.tables.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.measurement.tables.MetricsTableUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class ResourceMetricsTableUIBean extends MetricsTableUIBean {

    private List<MetricDisplaySummary> traitSummaries;
    private List<MeasurementDataTrait> traitHistory;

    public ResourceMetricsTableUIBean() {
        super();

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences.MetricRangePreferences range = user.getMeasurementPreferences()
            .getMetricRangePreferences();

        List<MeasurementSchedule> traitSchedules = scheduleManager.findMeasurementSchedulesForResourceAndType(subject,
            resource.getId(), DataType.TRAIT, null, true); //null -> don't filter, we want everything, false -> not only enabled

        int[] traitScheduleIds = new int[traitSchedules.size()];
        int i = 0;
        for (MeasurementSchedule sched : traitSchedules) {
            traitScheduleIds[i++] = sched.getId();
        }

        if (traitScheduleIds != null) {
            traitSummaries = chartManager.getMetricDisplaySummariesForResource(subject, resource.getId(),
                traitScheduleIds, range.begin, range.end);
        }

        HttpServletRequest request = FacesContextUtility.getRequest();
        int definitionId = WebUtility.getOptionalIntRequestParameter(request, "traitDefinitionId", -1);

        if (definitionId != -1) {
            MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
            traitHistory = dataManager.findTraits(subject, resource.getId(), definitionId);
        }
    }

    public List<MetricDisplaySummary> getTraitSummaries() {
        return traitSummaries;
    }

    public List<MeasurementDataTrait> getTraitHistory() {
        return traitHistory;
    }
}
