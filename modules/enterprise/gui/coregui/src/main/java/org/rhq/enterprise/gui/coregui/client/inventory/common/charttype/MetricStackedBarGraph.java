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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

import java.util.List;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView;

/**
 * Contains the javascript chart definition for a d3 Stacked Bar graph chart.
 *
 * @author Mike Thompson
 */
public final class MetricStackedBarGraph extends AbstractMetricD3GraphView implements HasD3JsniChart
{
    /**
     * Constructor for dashboard portlet view as chart definition and data are deferred to later
     * in the portlet configuration.
     *
     * @param locatorId
     */
    public MetricStackedBarGraph(String locatorId){
        super(locatorId);
    }

    /**
     * General constructor for stacked bar graph when you have all the data needed to
     * produce the graph. (This is true for all cases but the dashboard portlet).
     * @param locatorId
     * @param entityId
     * @param entityName
     * @param def
     * @param data
     */
    public MetricStackedBarGraph(String locatorId, int entityId, String entityName, MeasurementDefinition def,
                                 List<MeasurementDataNumericHighLowComposite> data) {
        super(locatorId, entityId, entityName, def, data);
    }

    @Override
    protected void renderGraph()
    {
        drawJsniChart();
    }

    /**
     * The magic JSNI to draw the charts with $wnd.d3.js
     */
    @Override
    public native void drawJsniChart() /*-{

        console.log("Draw Stacked Bar jsni chart");
        var global = this;

        // json metrics data for testing purposes
        //var jsonMetrics = [{ x:1352204720548, high:0.016642348035599646, low:0.016642348035599646, y:0.016642348035599646},{ x:1352211680548, high:12.000200003333388, low:0.0, y:3.500050000833347},{ x:1352211920548, high:2.000033333888898, low:1.999966667222213, y:2.000000000277778},{ x:1352212160548, high:5.0, low:1.999966667222213, y:2.750000000277778},{ x:1352212400548, high:4.0, low:2.0, y:2.5000083334722243},{ x:1352212640548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352212880548, high:3.0, low:2.0, y:2.2500083334722243},{ x:1352213120548, high:3.000050000833347, low:1.999966667222213, y:2.2500041672916677},{ x:1352213360548, high:4.0, low:1.999966667222213, y:2.7499916668055535},{ x:1352213600548, high:2.000033333888898, low:1.999966667222213, y:2.000008333750002},{ x:1352213840548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352214080548, high:3.0, low:1.999966667222213, y:2.250000000277778},{ x:1352214320548, high:4.0, low:2.0, y:2.5},{ x:1352214560548, high:3.0, low:1.999966667222213, y:2.250000000833347},{ x:1352214800548, high:2.000033333888898, low:1.999966667222213, y:2.000000000277778},{ x:1352215040548, high:4.0, low:2.0, y:2.5},{ x:1352215280548, high:3.0, low:2.0, y:2.2500083334722243},{ x:1352215520548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352215760548, high:3.0, low:1.999966667222213, y:2.250000000277778},{ x:1352216000548, high:4.0, low:2.0, y:2.5},{ x:1352216240548, high:2.000066668888963, low:1.999966667222213, y:2.000008334027794},{ x:1352216480548, high:3.0, low:1.999966667222213, y:2.2499916668055535}];

        var chartContext = new $wnd.ChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()()
        );


        function draw(chartContext) {
            "use strict";

            var margin = {top: 10, right: 5, bottom: 30, left: 40},
                    width = 850 - margin.left - margin.right,
                    height = 250 - margin.top - margin.bottom,
                    titleHeight = 43, titleSpace = 10;

            console.log("chart id: "+chartContext.chartSelection );
            console.log("Json Data:\n"+chartContext.data);

            var avg = $wnd.d3.mean(chartContext.data.map(function (d) {
                return d.y;
            }));
            var peak = $wnd.d3.max(chartContext.data.map(function (d) {
                return d.high;
            }));
            var min = $wnd.d3.min(chartContext.data.map(function (d) {
                return d.low;
            }));


            var timeScale = $wnd.d3.time.scale()
                    .range([0, width])
                    .domain($wnd.d3.extent(chartContext.data, function (d) {
                        return d.x;
                    }));

            // adjust the min scale so blue low line is not in axis
            var determineLowBound = function (min, peak) {
                var newLow = min - ((peak - min) * 0.1);
                if (newLow < 0) {
                    return 0;
                } else {
                    return newLow;
                }
            };
            var lowBound = determineLowBound(min,peak);
            var highBound = peak + ((peak - min) * 0.1);

            var yScale = $wnd.d3.scale.linear()
                    .clamp(true)
                    .rangeRound([height, 0])
                    .domain([lowBound, highBound]);

            var xAxis = $wnd.d3.svg.axis()
                    .scale(timeScale)
                    .ticks(10)
                    .tickSize(0, 0, 0)
                    .orient("bottom");

            var yAxis = $wnd.d3.svg.axis()
                    .scale(yScale)
                    .tickSubdivide(2)
                    .ticks(10)
                    .tickSize(4, 4, 0)
                    .orient("left");


            var interpolation = "basis";
            var avgLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale((avg));
                    });
            var peakLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale((peak));
                    });
            var minLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale(min);
                    });

            // our own x-axis because ours is custom
            var xAxisLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale(lowBound);
                    });


            // create the actual chart group
            var chart = $wnd.d3.select(chartContext.chartSelection);


            var createHeader = (function (resourceName, minLabel, minValue, avgLabel, avgValue, highLabel, highValue) {
                var fontSize = 14,
                        yTitle = 37,
                        fgColor = "#FFFFFF",
                        baseX = 490,
                        xInc = 50;


                // title/header
                var title = chart.append("g").append("rect")
                        .attr("class", "title")
                        .attr("x", 10)
                        .attr("y", margin.top)
                        .attr("height", titleHeight)
                        .attr("width", width + 30 + margin.left)
                        .attr("fill", "url(#headerGrad)");

                chart.append("text")
                        .attr("class", "titleName")
                        .attr("x", 30)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(resourceName)
                        .attr("fill", fgColor);


                chart.append("text")
                        .attr("class", "minLabel")
                        .attr("x", baseX)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(minLabel)
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "minText")
                        .attr("x", baseX + xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(minValue.toPrecision(3))
                        .attr("fill", fgColor);

                //avg
                chart.append("text")
                        .attr("class", "avgLabel")
                        .attr("x", baseX + 2 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(avgLabel)
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "avgText")
                        .attr("x", baseX + 3 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(avgValue.toPrecision(3))
                        .attr("fill", fgColor);

                // high
                chart.append("text")
                        .attr("class", "highLabel")
                        .attr("x", baseX + 4 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(highLabel)
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "highText")
                        .attr("x", baseX + 5 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(highValue.toPrecision(3))
                        .attr("fill", fgColor);

            });
            createHeader(chartContext.yAxisLabel, "Min -", min, "Avg -", avg, "High -", peak);


            var svg = chart.append("g")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top - titleHeight - titleSpace + margin.bottom)
                    .attr("transform", "translate(" + margin.left + "," + (+titleHeight + titleSpace + margin.top) + ")");


            // create the y axis grid lines
            svg.append("g").classed("grid y_grid", true)
                    .call($wnd.d3.svg.axis()
                            .scale(yScale)
                            .orient("left")
                            .ticks(10)
                            .tickSize(-width, 0, 0)
                            .tickFormat("")
                    );


            var barOffset = 2, pixelsOffHeight = 0;

            // The gray bars at the bottom leading up
            svg.selectAll("rect.leaderBar")
                    .data(chartContext.data)
                    .enter().append("rect")
                    .attr("class", "leaderBar")
                    .attr("x", function (d) {
                        return timeScale(d.x);
                    })
                    .attr("y", function (d) {
                        if (d.down || d.nodata) {
                            return yScale(highBound);
                        } else {
                            return yScale(d.low);
                        }
                    })
                    .attr("height", function (d) {
                        if (d.down || d.nodata) {
                            return height - yScale(highBound) - pixelsOffHeight;
                        } else {
                            return height - yScale(d.low) - pixelsOffHeight;
                        }
                    })
                    .attr("width", function (d) {
                        return  (width / chartContext.data.length - barOffset  );
                    })

                    .attr("opacity", ".55")
                    .attr("fill", function (d, i) {
                        if (d.down) {
                            return  "url(#redStripes)";
                        } else if (d.nodata) {
                            return  "url(#grayStripes)";
                        } else {
                            if (i % 10 == 0) {
                                return  "url(#heavyLeaderBarGrad)";
                            } else {
                                return  "url(#leaderBarGrad)";
                            }
                        }
                    });


            // upper portion representing avg to high
            svg.selectAll("rect.high")
                    .data(chartContext.data)
                    .enter().append("rect")
                    .attr("class", "high")
                    .attr("x", function (d) {
                        return timeScale(d.x);
                    })
                    .attr("y", function (d) {
                        return isNaN(d.high) ? yScale(lowBound)  : yScale(d.high);
                    })
                    .attr("height", function (d) {
                        if (d.down || d.nodata) {
                            return height - yScale(lowBound);
                        } else {
                            return  yScale(d.y) - yScale(d.high);
                        }
                    })
                    .attr("width", function (d) {
                        return  (width / chartContext.data.length - barOffset  );
                    })
                    .attr("opacity", 0.8)
                    .attr("fill", "url(#topBarGrad)");


            // lower portion representing avg to low
            svg.selectAll("rect.low")
                    .data(chartContext.data)
                    .enter().append("rect")
                    .attr("class", "low")
                    .attr("x", function (d) {
                        return timeScale(d.x);
                    })
                    .attr("y", function (d) {
                        return isNaN(d.y) ? height : yScale(d.y);
                    })
                    .attr("height", function (d) {
                        if (d.down || d.nodata) {
                            return height - yScale(lowBound);
                        } else {
                            return  yScale(d.low) - yScale(d.y);
                        }
                    })
                    .attr("width", function (d) {
                        return  (width / chartContext.data.length - barOffset );
                    })
                    .attr("opacity", 0.8)
                    .attr("fill", "url(#bottomBarGrad)");

            // create x-axis
            svg.append("g")
                    .attr("class", "x axis")
                    .attr("transform", "translate(0," + height + ")")
                    .call(xAxis);


            // create y-axis
            svg.append("g")
                    .attr("class", "y axis")
                    .call(yAxis)
                    .append("text")
                    .attr("transform", "rotate(-90),translate( -60,0)")
                    .attr("y", -30)
                    .attr("font-size", "10px")
                    .attr("font-family", "'Liberation Sans', Arial, Helvetica, sans-serif")
                    .attr("letter-spacing", "3")
                    .style("text-anchor", "end")
                    .text(chartContext.yAxisUnits === "NONE" ? "" : chartContext.yAxisUnits);

            console.log("finished axes");

            // peak Line (must be before line.high to look right
            svg.append("path")
                    .datum(chartContext.data)
                    .attr("class", "peakLine")
                    .attr("fill", "none")
                    .attr("stroke", "#ff8a9a")
                    .attr("stroke-width", "1")
                    .attr("stroke-dasharray", "3,3")
                    .attr("stroke-opacity", ".4")
                    .attr("d", peakLine);

            // min Line
            svg.append("path")
                    .datum(chartContext.data)
                    .attr("class", "minLine")
                    .attr("fill", "none")
                    .attr("stroke", "#8ad6ff")
                    .attr("stroke-width", "1")
                    .attr("stroke-dasharray", "3,3")
                    .attr("stroke-opacity", ".6")
                    .attr("d", minLine);

            // avg line
            svg.append("path")
                    .datum(chartContext.data)
                    .attr("class", "avgLine")
                    .attr("fill", "none")
                    .attr("stroke", "#b0d9b0")
                    .attr("stroke-width", "1")
                    .attr("stroke-dasharray", "3,3")
                    .attr("stroke-opacity", ".6")
                    .attr("d", avgLine);
            // xaxis line
            svg.append("path")
                    .datum(chartContext.data)
                    .attr("class", "xAxisLine")
                    .attr("fill", "none")
                    .attr("stroke", "#cccdcf")
                    .attr("stroke-width", "1")
                    .attr("d", xAxisLine);


            $wnd.jQuery('svg rect').tipsy({
                gravity: 'w',
                html: true,
                title: function() {
                    var d = this.__data__ ;
                    var xValue = (d.x == undefined) ? 0 : +d.x;
                    var date = new Date(+xValue);
                    var timeFormatter = $wnd.d3.time.format("%I:%M:%S %P");
                    var dateFormatter = $wnd.d3.time.format("%m/%d/%y");
                    var highValue = (d.high == undefined) ? 0 : d.high.toFixed(2);
                    var lowValue = (d.low == undefined) ? 0 : d.low.toFixed(2);
                    var avgValue = (d.y == undefined) ? 0 : d.y.toFixed(2);
                    return '<div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#d3d3d6";">Time: </span>' +timeFormatter(date)+ '</div>'+
                            '<div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#d3d3d6"";">Date: </span>' +dateFormatter(date)+ '</div>'+
                            '<div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#ff8a9a";">High: </span>'
                            + highValue +'</div><div style="text-align: left;"><span style="width:50px;font-weight: bold;color: #b0d9b0";">Avg:  </span>'+ avgValue+
                            '</div><div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#8ad6ff";">Low:  </span>'+ lowValue + '</div>';
                }
            });

            console.log("finished drawing paths");
        }
        draw(chartContext);

    }-*/;

}
