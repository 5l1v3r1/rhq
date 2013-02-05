/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageList;

/**
 * Common Graph capability.
 * The MetricGraphData delegate is wrapped for JSNI access via d3 charts.
 *
 * @author Mike Thompson
 */
public abstract class AbstractGraph implements HasD3JsniChart {

    private MetricGraphData metricGraphData;

    public MetricGraphData getMetricGraphData() {
        return metricGraphData;
    }

    public void setMetricGraphData(MetricGraphData metricGraphData) {
        this.metricGraphData = metricGraphData;
    }

    public int getEntityId() {
        return metricGraphData.getEntityId();
    }

    @Override
    public void setEntityId(int entityId) {
        metricGraphData.setEntityId(entityId);
    }

    public String getEntityName() {
        return metricGraphData.getEntityName();
    }

    public int getDefinitionId() {
        return metricGraphData.getDefinitionId();
    }

    @Override
    public void setDefinitionId(int definitionId) {
        metricGraphData.setDefinitionId(definitionId);
    }

    public MeasurementDefinition getDefinition() {
        return metricGraphData.getDefinition();
    }

    @Override
    public void setDefinition(MeasurementDefinition definition) {
        metricGraphData.setDefinition(definition);
    }

    public String getChartId() {
        return metricGraphData.getChartId();
    }

    public List<MeasurementDataNumericHighLowComposite> getMetricData() {
        return metricGraphData.getMetricData();
    }

    public void setMetricData(List<MeasurementDataNumericHighLowComposite> metricData) {
        metricGraphData.setMetricData(metricData);
    }

    public void setAvailabilityList(PageList<Availability> availabilityDownList) {
        metricGraphData.setAvailabilityList(availabilityDownList);
    }

    public void setMeasurementOOBCompositeList(PageList<MeasurementOOBComposite> measurementOOBCompositeList) {
        metricGraphData.setMeasurementOOBCompositeList(measurementOOBCompositeList);
    }

    public String getChartTitleMinLabel() {
        return metricGraphData.getChartTitleMinLabel();
    }

    public String getChartTitleAvgLabel() {
        return metricGraphData.getChartTitleAvgLabel();
    }

    public String getChartTitlePeakLabel() {
        return metricGraphData.getChartTitlePeakLabel();
    }

    public String getChartDateLabel() {
        return metricGraphData.getChartDateLabel();
    }

    public String getChartDownLabel() {
        return metricGraphData.getChartDownLabel();
    }

    public String getChartTimeLabel() {
        return metricGraphData.getChartTimeLabel();
    }

    public String getChartUnknownLabel() {
        return metricGraphData.getChartUnknownLabel();
    }

    public String getChartNoDataLabel() {
        return metricGraphData.getChartNoDataLabel();
    }

    public String getChartHoverStartLabel() {
        return metricGraphData.getChartHoverStartLabel();
    }

    public String getChartHoverEndLabel() {
        return metricGraphData.getChartHoverEndLabel();
    }

    public String getChartHoverPeriodLabel() {
        return metricGraphData.getChartHoverPeriodLabel();
    }

    public String getChartHoverBarLabel() {
        return metricGraphData.getChartHoverBarLabel();
    }

    public Integer getChartHeight() {
        return metricGraphData.getChartHeight();
    }

    public void setChartHeight(Integer chartHeight) {
        metricGraphData.setChartHeight(chartHeight);
    }

    public String getYAxisTitle() {
        return metricGraphData != null ? metricGraphData.getYAxisTitle() : "";
    }

    public String getYAxisUnits() {
        return metricGraphData.getYAxisUnits();
    }

    public String getXAxisTitle() {
        return metricGraphData.getXAxisTitle();
    }

    public String getJsonMetrics() {
        return metricGraphData.getJsonMetrics();
    }

    public boolean shouldDisplayDayOfWeekInXAxisLabel() {
        return metricGraphData.shouldDisplayDayOfWeekInXAxisLabel();
    }


}
