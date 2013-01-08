/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.Session;

import org.rhq.core.domain.measurement.MeasurementBaseline;

/**
 * @author Stefan Negrea
 */
public class MetricsBaselineCalculator {

    private MetricsDAO metricsDAO;

    public MetricsBaselineCalculator(Session session) {
        this.metricsDAO = new MetricsDAO(session);
    }

    public List<MeasurementBaseline> calculateBaselines(List<Integer> scheduleIds, long startTime, long endTime) {
        List<MeasurementBaseline> calculatedBaselines = new ArrayList<MeasurementBaseline>();

        MeasurementBaseline measurementBaseline;
        for (Integer scheduleId : scheduleIds) {
            measurementBaseline = this.calculateBaseline(scheduleId, startTime, endTime);
            if (measurementBaseline != null) {
                calculatedBaselines.add(measurementBaseline);
            }
        }

        return calculatedBaselines;
    }

    private MeasurementBaseline calculateBaseline(Integer scheduleId, long startTime, long endTime) {
        List<AggregatedSimpleNumericMetric> metrics = this.metricsDAO.findAggregateSimpleMetrics(MetricsTable.ONE_HOUR,
            scheduleId, startTime, endTime);

        if (metrics.size() != 0) {
            ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

            double max = Double.NaN;
            for (AggregatedSimpleNumericMetric entry : metrics) {
                if (AggregateType.MAX.equals(entry.getType())) {
                    max = entry.getValue();
                    break;
                }
            }

            double min = Double.NaN;
            for (AggregatedSimpleNumericMetric entry : metrics) {
                if (AggregateType.MIN.equals(entry.getType())) {
                    min = entry.getValue();
                    break;
                }
            }

            for (AggregatedSimpleNumericMetric entry : metrics) {
                if (AggregateType.AVG.equals(entry.getType())) {
                    mean.add(entry.getValue());
                } else if (AggregateType.MAX.equals(entry.getType())) {
                    if (max < entry.getValue()) {
                        max = entry.getValue();
                    }
                } else if (AggregateType.MIN.equals(entry.getType())) {
                    if (min > entry.getValue()) {
                        min = entry.getValue();
                    }
                }
            }

            MeasurementBaseline baseline = new MeasurementBaseline();
            baseline.setMax(max);
            baseline.setMin(min);
            baseline.setMean(mean.getArithmeticMean());
            baseline.setScheduleId(scheduleId);

            return baseline;
        }

        return null;
    }
}
