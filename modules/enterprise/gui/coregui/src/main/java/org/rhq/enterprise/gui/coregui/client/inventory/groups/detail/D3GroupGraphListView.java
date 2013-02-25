/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricStackedBarGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceMetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Build the Group version of the View that shows the individual graph views.
 * @author Mike Thompson
 */
public class D3GroupGraphListView extends EnhancedVLayout {

    private ResourceGroup resourceGroup;
    private Label loadingLabel = new Label(MSG.common_msg_loading());
    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;

    public D3GroupGraphListView(ResourceGroup resourceGroup) {
        super();
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor();
        this.resourceGroup = resourceGroup;
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        destroyMembers();

        addMember(measurementRangeEditor);

        if (resourceGroup != null) {
            buildGraphs();
        }
    }

    /**
     * Build whatever graph metrics (MeasurementDefinitions) are defined for the resource.
     */
    private void buildGraphs() {

        List<Long> startEndList = measurementRangeEditor.getBeginEndTimes();
        final long startTime = startEndList.get(0);
        final long endTime = startEndList.get(1);

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceGroup.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(final ResourceType type) {

                    final ArrayList<MeasurementDefinition> measurementDefinitions = new ArrayList<MeasurementDefinition>();

                    for (MeasurementDefinition def : type.getMetricDefinitions()) {
                        if (def.getDataType() == DataType.MEASUREMENT && def.getDisplayType() == DisplayType.SUMMARY) {
                            measurementDefinitions.add(def);
                        }
                    }

                    Collections.sort(measurementDefinitions, new Comparator<MeasurementDefinition>() {
                        public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                            return new Integer(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
                        }
                    });

                    int[] measDefIdArray = new int[measurementDefinitions.size()];
                    for (int i = 0; i < measDefIdArray.length; i++) {
                        measDefIdArray[i] = measurementDefinitions.get(i).getId();
                    }

                    GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(resourceGroup.getId(),
                        measDefIdArray, startTime, endTime, 60,
                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(),
                                    caught);
                                loadingLabel.setContents(MSG.view_resource_monitor_graphs_loadFailed());
                            }

                            @Override
                            public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> result) {
                                if (result.isEmpty()) {
                                    loadingLabel.setContents(MSG.view_resource_monitor_graphs_noneAvailable());
                                } else {
                                    loadingLabel.hide();
                                    int i = 0;
                                    for (List<MeasurementDataNumericHighLowComposite> data : result) {
                                        buildIndividualGraph(measurementDefinitions.get(i++), data);
                                    }
                                }
                            }
                        });

                }
            });
    }

    private void buildIndividualGraph(MeasurementDefinition measurementDefinition,
        List<MeasurementDataNumericHighLowComposite> data) {

        MetricGraphData metricGraphData = new MetricGraphData(resourceGroup.getId(), resourceGroup.getName(),
            measurementDefinition, data);
        MetricStackedBarGraph graph = new MetricStackedBarGraph(metricGraphData);
        ResourceMetricD3Graph graphView = new ResourceMetricD3Graph(graph);

        graphView.setWidth("95%");
        graphView.setHeight(250);

        addMember(graphView);
    }

}
