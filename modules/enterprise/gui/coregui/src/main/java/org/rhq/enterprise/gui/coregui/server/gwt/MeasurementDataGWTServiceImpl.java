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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;
import java.util.Set;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class MeasurementDataGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementDataGWTService {

    private static final long serialVersionUID = 1L;

    private MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
    private CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();
    private MeasurementOOBManagerLocal measurementOOBManager = LookupUtil.getOOBManager();

    private MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
    private MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();

    public List<MeasurementDataTrait> findCurrentTraitsForResource(int resourceId, DisplayType displayType) {
        try {
            return SerialUtility.prepare(dataManager.findCurrentTraitsForResource(getSessionSubject(), resourceId,
                displayType), "MeasurementDataService.findCurrentTraitsForResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Set<MeasurementData> findLiveData(int resourceId, int[] definitionIds) {
        try {
            return SerialUtility.prepare(dataManager.findLiveData(getSessionSubject(), resourceId, definitionIds),
                "MeasurementDataService.findLiveData");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(int resourceId, int[] definitionIds,
        long beginTime, long endTime, int numPoints) {
        try {
            return SerialUtility.prepare(dataManager.findDataForResource(getSessionSubject(), resourceId,
                definitionIds, beginTime, endTime, numPoints), "MeasurementDataService.findDataForResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForResource(int scheduleId, long start, long end,
        PageControl pageControl) {
        try {
            return SerialUtility.prepare(callTimeDataManager.findCallTimeDataForResource(getSessionSubject(),
                scheduleId, start, end, pageControl), "MeasurementDataService.findCallTimeDataForResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(MeasurementDefinitionCriteria criteria) {
        try {
            return SerialUtility.prepare(definitionManager.findMeasurementDefinitionsByCriteria(getSessionSubject(),
                criteria), "MeasurementDataService.findMeasurementDefinintionsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<MeasurementSchedule> findMeasurementSchedulesByCriteria(MeasurementScheduleCriteria criteria) {
        try {
            return SerialUtility.prepare(scheduleManager.findSchedulesByCriteria(getSessionSubject(), criteria),
                "MeasurementDataService.findMeasurementSchedulesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesByContext(EntityContext context) {
        try {
            return SerialUtility.prepare(scheduleManager.getMeasurementScheduleCompositesByContext(getSessionSubject(),
                context, PageControl.getUnlimitedInstance()),
                "MeasurementDataService.getMeasurementScheduleCompositesByContext");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<MeasurementOOBComposite> getSchedulesWithOOBs(String metricNameFilter, String resourceNameFilter,
        String parentNameFilter, PageControl pc) {
        try {
            return SerialUtility.prepare(measurementOOBManager.getSchedulesWithOOBs(getSessionSubject(),
                metricNameFilter, resourceNameFilter, parentNameFilter, pc),
                "MeasurementDataService.getSchedulesWithOOBs");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<MeasurementOOBComposite> getHighestNOOBsForResource(int resourceId, int n) {
        try {
            return SerialUtility.prepare(measurementOOBManager.getHighestNOOBsForResource(getSessionSubject(),
                resourceId, n), "MeasurementDataService.getHighestNOOBsForResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void enableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) {
        try {
            scheduleManager.enableSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void disableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) {
        try {
            scheduleManager.disableSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateSchedulesForResource(int resourceId, int[] measurementDefinitionIds, long collectionInterval) {
        try {
            scheduleManager.updateSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds,
                collectionInterval);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void enableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds) {
        try {
            scheduleManager.enableSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void disableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds) {
        try {
            scheduleManager.disableSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds,
        long collectionInterval) {
        try {
            scheduleManager.updateSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds, collectionInterval);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void enableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules) {
        try {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(getSessionSubject(),
                measurementDefinitionIds, 0, updateExistingSchedules);
        } catch (RuntimeException e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void disableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules) {
        try {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(getSessionSubject(),
                measurementDefinitionIds, -1, updateExistingSchedules);
        } catch (RuntimeException e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateSchedulesForResourceType(int[] measurementDefinitionIds, long collectionInterval,
                                           boolean updateExistingSchedules) {
        try {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(getSessionSubject(),
                measurementDefinitionIds, collectionInterval, updateExistingSchedules);
        } catch (RuntimeException e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<MeasurementDataTrait> findTraitsByCriteria(MeasurementDataTraitCriteria criteria) {
        try {
            return SerialUtility.prepare(dataManager.findTraitsByCriteria(getSessionSubject(), criteria),
                "MeasurementDataService.findTraitsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}
