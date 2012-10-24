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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * This composite graph view has different graph types and data structures for
 * graphing multiple individual resources of the composite resource as
 * multi-line graph. Single Metric Multiple Resource graph.
 *
 * @author  Mike Thompson
 */
public class CompositeGroupD3GraphListView extends LocatableVLayout
{

    private HTMLFlow resourceTitle;

    private int groupId;
    private int definitionId;


    private MeasurementDefinition definition;

    private UserPreferencesMeasurementRangeEditor measurementRangeEditor;

    /**
     * measurementForEachResource is a list of a list of single Measurement data for multiple resources.
     */
    private List<List<MeasurementDataNumericHighLowComposite>> measurementForEachResource;



    public CompositeGroupD3GraphListView(String locatorId, int groupId, int defId) {
        super(locatorId);
        this.groupId = groupId;
        setDefinitionId(defId);
        measurementForEachResource = new ArrayList<List<MeasurementDataNumericHighLowComposite>>();
        measurementRangeEditor = new UserPreferencesMeasurementRangeEditor(this.getLocatorId());
        setHeight100();
        setWidth100();
    }

    public void populateData() {
        ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.fetchResourceType(true);
        criteria.addFilterVisible(false);
        criteria.addFilterPrivate(true);
        criteria.fetchExplicitResources(true);

        groupService.findResourceGroupCompositesByCriteria(criteria, new AsyncCallback<PageList<ResourceGroupComposite>>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
            }

            @Override
            public void onSuccess(PageList<ResourceGroupComposite> result)
            {
                if (result.isEmpty())
                {
                    return;
                }
                measurementForEachResource.clear();

                final ResourceGroup group = result.get(0).getResourceGroup();
                Log.debug("group name: " + group.getName());
                Log.debug("# of child resources: " + group.getExplicitResources().size());

                Set<Resource> childResources = group.getExplicitResources();

                for (Resource childResource : childResources)
                {
                    Log.debug(" *** Adding composite: "+childResource.getName());

                    ResourceTypeRepository.Cache.getInstance().getResourceTypes(childResource.getResourceType().getId(),
                        EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                        new ResourceTypeRepository.TypeLoadedCallback()
                        {
                            @Override
                            public void onTypesLoaded(final ResourceType type)
                            {

                                for (MeasurementDefinition def : type.getMetricDefinitions())
                                {
                                    // only need the one selected measurement
                                    if (def.getId() == getDefinitionId())
                                    {
                                        setDefinition(def);

                                        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroupForLast(
                                                groupId, new int[]{getDefinitionId()}, 8,
                                                MeasurementUtility.UNIT_HOURS, 60,
                                                new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>()
                                                {
                                                    @Override
                                                    public void onFailure(Throwable caught)
                                                    {
                                                        CoreGUI.getErrorHandler().handleError(
                                                                MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                    }

                                                    @Override
                                                    public void onSuccess(
                                                            List<List<MeasurementDataNumericHighLowComposite>> result)
                                                    {
                                                        addMeasurementForEachResource(result.get(0));
                                                        drawGraph();
                                                    }
                                                });
                                    }
                                }
                            }
                        });
            }
            }
        });

    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementForEachResource() {
        return measurementForEachResource;
    }

    /**
     * Adding is done asynchronously, so we must synchronize the add.
     * @param resourceMeasurementList
     */
    synchronized public void addMeasurementForEachResource(List<MeasurementDataNumericHighLowComposite> resourceMeasurementList) {
       measurementForEachResource.add(resourceMeasurementList);
    }

    public void setMeasurementForEachResource(List<List<MeasurementDataNumericHighLowComposite>> measurementForEachResource) {
        this.measurementForEachResource = measurementForEachResource;
    }
    public int getDefinitionId() {
        return definitionId;
    }
    public String getChartId(){
        return groupId + "-" + definition.getId();
    }
    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
    }

    public MeasurementDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }


    private void drawGraph() {
        Log.debug("drawGraph in CompositeGroupD3GraphListView for: "+ definition + ","+definitionId);

        addMember(measurementRangeEditor );

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            if (null != resourceTitle) {
                resourceTitle.setWidth("*");
                titleHLayout.addMember(resourceTitle);
            }

            addMember(titleHLayout);

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.setWidth100();
            addMember(title);
            HTMLFlow graph = new HTMLFlow("<div id=\"mChart-"+getChartId()+"\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:100%;\"></svg></div>");
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            drawJsniChart();
            markForRedraw();

        }

    }

    public native void drawJsniChart() /*-{
        console.log("Draw nvd3 charts for composite multiline graph");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()();
        var chartHandle = "#mChart-"+chartId;
        var chartSelection = chartHandle + " svg";
        var yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()();
        var yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()();
        var xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()();
        var json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()());

        var data = function() {
        return [
                {
                values: json,
                key: yAxisLabel ,
                color: '#ff7f0e'
                }
            ];
        };

        $wnd.nv.addGraph(function() {
            var chart = $wnd.nv.models.lineChart();

        chart.xAxis.axisLabel(xAxisLabel)
            .tickFormat(function(d) { return $wnd.d3.time.format('%a %I %p')(new Date(d)) });

        chart.yAxis
            .axisLabel(yAxisUnits)
            .tickFormat($wnd.d3.format(',f'));

        $wnd.d3.select(chartSelection)
            .datum(data())
            .transition().duration(300)
            .call(chart);

        $wnd.nv.utils.windowResize(chart.update);

        return chart;
        });

    }-*/;


    public CompositeGroupD3GraphListView getInstance(String locatorId, int groupId, int definitionId){
        return new CompositeGroupD3GraphListView(locatorId, groupId, definitionId);
    }


}
