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
package org.rhq.enterprise.server.measurement;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.engine.jms.model.OutOfBoundsConditionMessage;

/**
 * A manager for {@link MeasurementBaseline}s.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Joseph Marques
 */
@Local
public interface MeasurementBaselineManagerLocal {
    /**
     * This is the entry point which calls out to all other *calculateAutoBaselines* methods seen
     * in this interface.  This is the method that should be called on a regular basis by some sort
     * of job scheduler / timer to determine whether baselines even need to be calculated.  This is
     * an inexpensive operation and can be run every minute if needed because it will only calculate
     * baselines for the ones that need it (as configured by the system preferences for how frequently
     * they should be recalculated, and how much data should be used in that recalculation).
     */
    void calculateAutoBaselines();

    /**
     * Calculates baselines for all scheduled measurements. The measurement data that was collected between the given
     * start and end times is used to calculate the baselines.
     *
     * @param  startTime use measurement data that was collected at or after this time (epoch millis)
     * @param  endtime   use measurement data that was collected at or before this time (epoch millis)
     *
     * @return the time that all the baselines were computed; this value must be passed to
     *         {@link #calculateAutoBaselinesPostProcessing(long)}
     */
    long calculateAutoBaselines(long startTime, long endtime);

    int _calculateAutoBaselinesDELETE(long startTime, long endtime) throws Exception;

    int _calculateAutoBaselinesINSERT(long startTime, long endtime, long computeTime) throws Exception;

    /**
     * Calculate a baseline value for the given metric based on the specified date range, optionally setting the
     * metric's baseline to the calculated value.
     *
     * @param  subject
     * @param  measurementScheduleId measurement id to recalc.
     * @param  startDate             begin date range
     * @param  endDate               end date range
     * @param  save                  whether or not to save the recalculated baseline
     *
     * @return the calculated baseline value
     *
     * @throws BaselineCreationException
     * @throws MeasurementNotFoundException
     */
    MeasurementBaseline calculateAutoBaseline(Subject subject, Integer measurementScheduleId, long startDate,
        long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException;

    /**
     * Unset the {@link MeasurementBaseline#setUserEntered(boolean) user-entered} property in baselines so that we can
     * do autobaselines on it again.
     *
     * @param subject
     * @param resourceIds
     * @param definitionIds measurement definition IDs
     */
    void enableAutoBaselineCalculation(Subject subject, Integer[] resourceIds, Integer[] definitionIds);

    /**
     * Return a list of {@link MeasurementBaseline} objects for the given {@link Resource}.
     *
     * @param  resource the resource whose baselines are to be returned
     *
     * @return a list of baselines for all measurements scheduled on the given resource
     */
    List<MeasurementBaseline> findBaselinesForResource(Resource resource);

    /**
     * Returns the baseline for a measurement identified by its measurement definition and the resource whose
     * measurement it is.
     *
     * @param  subject
     * @param  resource
     * @param  measurementDefinitionId
     *
     * @return baseline of the resource's measurement with the given definition
     */
    MeasurementBaseline findBaselineForResourceAndMeasurementDefinition(Subject subject, Integer resource,
        Integer measurementDefinitionId);

    /**
     * Returns every baseline in the system whose measurement data numeric type is {@value NumericType#DYNAMIC}.
     *
     * @param  pc pagination control
     *
     * @return all baselines for all measurements whose values are dynamic in nature
     */
    PageList<MeasurementBaselineComposite> getAllDynamicMeasurementBaselines(int agentId, Subject subject,
        PageControl pc);

    /**
     * Insert the data contained side an {@link OutOfBoundsConditionMessage} into the table exposed by the
     * {@link MeasurementOutOfBounds} entity
     *  
     * @param outOfBoundsMessage the message that contains the OutOfBounds data to persist
     */
    void insertOutOfBoundsMessage(OutOfBoundsConditionMessage outOfBoundsMessage);
}