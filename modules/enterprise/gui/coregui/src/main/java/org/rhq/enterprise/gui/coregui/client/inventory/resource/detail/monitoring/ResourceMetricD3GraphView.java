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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.List;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.HasD3JsniChart;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;


public class ResourceMetricD3GraphView extends AbstractMetricD3GraphView
{
    /**
     * Defines the jsniChart type like area, line, etc...
     *
     */
    private HasD3JsniChart jsniChart;

    public ResourceMetricD3GraphView(String locatorId){
        super(locatorId);
        //setChartHeight("150px");
    }


    public ResourceMetricD3GraphView(String locatorId, int entityId, String entityName, MeasurementDefinition def,
                                     List<MeasurementDataNumericHighLowComposite> data, HasD3JsniChart jsniChart ) {

        super(locatorId, entityId, entityName, def, data);
        this.jsniChart = jsniChart;
        //setChartHeight("150px");
    }



    @Override
    protected void renderGraph() {

        if (null == getDefinition()) {
            Log.debug(" ***  RenderGraph  Single Graph path *** ");

            ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.addFilterId(getEntityId());
            resourceService.findResourcesByCriteria(resourceCriteria, new AsyncCallback<PageList<Resource>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
                }

                @Override
                public void onSuccess(PageList<Resource> result) {
                    if (result.isEmpty()) {
                        return;
                    }
                    final Resource firstResource = result.get(0);

                    // now return the availability
                    AvailabilityCriteria c = new AvailabilityCriteria();
                    c.addFilterResourceId(firstResource.getId());
                    c.addFilterInitialAvailability(false);
                    c.addSortStartTime(PageOrdering.ASC);
                    GWTServiceLookup.getAvailabilityService().findAvailabilityByCriteria(c,
                            new AsyncCallback<PageList<Availability>>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                                }

                                @Override
                                public void onSuccess(PageList<Availability> availList) {
                                    Log.debug("************** ** availList: "+ availList.size()+"\n\n");

                                    PageList<Availability> downAvailList = new PageList<Availability>();
                                    for (Availability availability : availList)
                                    {
                                        if(availability.getAvailabilityType().equals(AvailabilityType.DOWN)
                                                || availability.getAvailabilityType().equals(AvailabilityType.DISABLED)){
                                           downAvailList.add(availability);
                                        }

                                    }
                                    Log.debug("************** ** Down availList: "+ downAvailList.size()+"\n\n");
                                    setAvailabilityDownList(downAvailList);

                                    HashSet<Integer> typesSet = new HashSet<Integer>();
                                    typesSet.add(firstResource.getResourceType().getId());
                                    HashSet<String> ancestries = new HashSet<String>();
                                    ancestries.add(firstResource.getAncestry());
                                    // In addition to the types of the result resources, get the types of their ancestry
                                    typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                                    ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                                            typesSet.toArray(new Integer[typesSet.size()]),
                                            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                                            new ResourceTypeRepository.TypesLoadedCallback() {

                                                @Override
                                                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                                                    ResourceType type = types.get(firstResource.getResourceType().getId());
                                                    for (MeasurementDefinition def : type.getMetricDefinitions()) {
                                                        Log.debug("  **** MIKE Before data: "+getDefinitionId());
                                                        if (def.getId() == getDefinitionId()) {
                                                            setDefinition(def);

                                                            //GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(getEntityId(),
                                                             GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(firstResource.getId(),
                                                                    new int[] { getDefinitionId() }, 8, MeasurementUtils.UNIT_HOURS, 60,
                                                                    new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                                                        @Override
                                                                        public void onFailure(Throwable caught) {
                                                                            CoreGUI.getErrorHandler().handleError(
                                                                                    MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                                        }

                                                                        @Override
                                                                        public void onSuccess(final
                                                                                              List<List<MeasurementDataNumericHighLowComposite>> measurementData) {
                                                                            // return the data
                                                                            Log.debug(" ********** Got the metric data!");

                                                                            setData(measurementData.get(0));

                                                                            drawGraph();
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                }
                                            });

                                }
                            });



                }
            });

        } else {
            // path for monitoring d3 Graphs multiple charts
            Log.debug("Chart path for: multiple charts");
            drawGraph();
        }
    }
    @Override
    protected boolean supportsLiveGraphViewDialog() {
        return true;
    }



    @Override
    /**
     * Delegate the call to rendering the JSNI chart.
     * This way the chart type can be swapped out at any time.
     */
    public void drawJsniChart()
    {
        jsniChart.drawJsniChart();
    }

    @Override
    protected void displayLiveGraphViewDialog() {
        LiveGraphD3View.displayAsDialog(getLocatorId(), getEntityId(), getDefinition());
    }

    public AbstractMetricD3GraphView getInstance(String locatorId, int entityId, String entityName, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data, HasD3JsniChart jsniChart) {

        return new ResourceMetricD3GraphView(locatorId, entityId, entityName, def, data, jsniChart);
    }
}
