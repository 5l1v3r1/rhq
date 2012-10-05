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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.List;


import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Mike Thompson
 */
public abstract class AbstractMetricD3GraphView extends LocatableVLayout {

    protected HTMLFlow resourceTitle;

    private int entityId;
    private int definitionId;

    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> data;

    public AbstractMetricD3GraphView(String locatorId) {
        super(locatorId);
    }


    public AbstractMetricD3GraphView(String locatorId, int entityId, MeasurementDefinition def,
                                     List<MeasurementDataNumericHighLowComposite> data) {
        this(locatorId);

        this.entityId = entityId;
        this.definition = def;
        this.data = data;
        setHeight100();
        setWidth100();
    }

    public abstract AbstractMetricD3GraphView getInstance(String locatorId, int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data);

    protected abstract void renderGraph();

    protected HTMLFlow getEntityTitle(){
        return resourceTitle;
    }

    public int getEntityId() {
        return this.entityId;
    }


    public void setEntityId(int entityId) {
        this.entityId = entityId;
        this.definition = null;
    }
    public int getDefinitionId() {
        return definitionId;
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

    public List<MeasurementDataNumericHighLowComposite> getData() {
        return data;
    }

    public void setData(List<MeasurementDataNumericHighLowComposite> data) {
        this.data = data;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        renderGraph();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        renderGraph();
    }

    protected void drawGraph() {
        Log.debug(" *** drawGraph   in AbstractMetricD3GraphView  *****" + definition + ","+definitionId);

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            HTMLFlow entityTitle = getEntityTitle();
            if (null != entityTitle) {
                entityTitle.setWidth("*");
                titleHLayout.addMember(entityTitle);
            }

            if (supportsLiveGraphViewDialog()) {
                Img liveGraph = createLiveGraphImage();
                titleHLayout.addMember(liveGraph);
            }

            addMember(titleHLayout);

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.setWidth100();
            addMember(title);
            HTMLFlow graph = new HTMLFlow("<div id=\"rchart\" ><svg style=\"height:200px;\"></svg></div>");
            graph.setWidth100();
            graph.setHeight(200);
            addMember(graph);

            // @todo: if(IE) drawIE8Charts()
            drawJsniCharts();
        }



    }

    private Img createLiveGraphImage() {
        Img liveGraph = new LocatableImg(extendLocatorId("Live"), "subsystems/monitor/Monitor_16.png", 16, 16);
        liveGraph.setTooltip(MSG.view_resource_monitor_graph_live_tooltip());

        liveGraph.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                displayLiveGraphViewDialog();
            }
        });
        return liveGraph;
    }

    protected boolean supportsLiveGraphViewDialog() {
        return false;
    }

    protected void displayLiveGraphViewDialog() {
        return;
    }

    @Override
    public void destroy() {
        //hoverLabel.destroy();
        super.destroy();
    }

    @Override
    public void hide() {
        super.hide();
        //hoverLabel.hide();
    }

    public native void drawJsniCharts() /*-{
        console.log("Draw nvd3 charts");
        var data = function() {
            var sin = [],
                    cos = [];

            for (var i = 0; i < 100; i++) {
                sin.push({x: i, y: Math.sin(i/10)});
                cos.push({x: i, y: .5 * Math.cos(i/10)});
            }

            return [
                {
                    values: sin,
                    key: 'Sine Wave',
                    color: '#ff7f0e'
                },
                {
                    values: cos,
                    key: 'Cosine Wave',
                    color: '#2ca02c'
                }
            ];
        };
        console.log("Starting NVD3 graph");
        $wnd.nv.addGraph(function() {
            var chart = $wnd.nv.models.lineChart();

            chart.xAxis
                    .axisLabel('Time (ms)')
                    .tickFormat($wnd.d3.format(',r'));

            chart.yAxis
                    .axisLabel('Voltage (v)')
                    .tickFormat($wnd.d3.format('.02f'));

            $wnd.d3.select('#rchart svg')
                    .datum(data())
                    .transition().duration(500)
                    .call(chart);

            $wnd.nv.utils.windowResize(chart.update);

            return chart;
        });

    }-*/;

}
