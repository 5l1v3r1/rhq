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

/**
 * Contains the javascript chart definition for a d3 Stacked Bar graph chart.
 *
 * @author Mike Thompson
 */
public final class MetricStackedBarGraph extends MetricGraphData implements HasD3JsniChart {
    /**
     * Constructor for dashboard portlet view as chart definition and data are deferred to later in the portlet
     * configuration.
     */
    public MetricStackedBarGraph() {
        //super(locatorId);
    }

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public MetricStackedBarGraph(MetricGraphData metricGraphData) {
        super(metricGraphData.getEntityId(), metricGraphData.getEntityName(), metricGraphData.getDefinition(), metricGraphData.getMetricData());
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

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI.
        var chartContext = new $wnd.ChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartId()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getJsonMetrics()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getXAxisTitle()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getYAxisTitle()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getYAxisUnits()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartTitleMinLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartTitleAvgLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartTitlePeakLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartDateLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartTimeLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartDownLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartUnknownLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartHoverStartLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartHoverEndLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartHoverPeriodLabel()()
        );


        // Define the Stacked Bar Graph function using the module pattern
        var metricStackedBarGraph = function () {
            "use strict";
            // privates
            var margin = {top: 10, right: 5, bottom: 30, left: 40},
                    width = 850 - margin.left - margin.right,
                    height = 250 - margin.top - margin.bottom,
                    titleHeight = 43, titleSpace = 10,
                    barOffset = 2,
                    interpolation = "basis";

            var avg = $wnd.d3.mean(chartContext.data.map(function (d) {
                        return d.y;
                    })),
                    peak = $wnd.d3.max(chartContext.data.map(function (d) {
                        return d.high;
                    })),
                    min = $wnd.d3.min(chartContext.data.map(function (d) {
                        return d.low;
                    })),
                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(chartContext.data, function (d) {
                                return d.x;
                            })),

            // adjust the min scale so blue low line is not in axis
                    determineLowBound = function (min, peak) {
                        var newLow = min - ((peak - min) * 0.1);
                        if (newLow < 0) {
                            return 0;
                        }
                        else {
                            return newLow;
                        }
                    },
                    lowBound = determineLowBound(min, peak),
                    highBound = peak + ((peak - min) * 0.1),

                    yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([lowBound, highBound]),

                    xAxis = $wnd.d3.svg.axis()
                            .scale(timeScale)
                            .ticks(12)
                            .tickSubdivide(5)
                            .tickSize(4, 4, 0)
                            .orient("bottom"),

                    yAxis = $wnd.d3.svg.axis()
                            .scale(yScale)
                            .tickSubdivide(2)
                            .ticks(10)
                            .tickSize(4, 4, 0)
                            .orient("left"),


            // create the actual chart group
                    chart = $wnd.d3.select(chartContext.chartSelection),

                    svg = chart.append("g")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top - titleHeight - titleSpace + margin.bottom)
                            .attr("transform", "translate(" + margin.left + "," + (+titleHeight + titleSpace + margin.top) + ")");


            function createHeader(resourceName, minLabel, minValue, avgLabel, avgValue, highLabel, highValue) {
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
                        .text(minLabel + " - ")
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
                        .text(avgLabel + " - ")
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
                        .attr("x", 10 + baseX + 4 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(highLabel + " - ")
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "highText")
                        .attr("x", 10 + baseX + 5 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(highValue.toPrecision(3))
                        .attr("fill", fgColor);
                return title;

            }

            function createStackedBars() {

                var pixelsOffHeight = 0;

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
                            }
                            else {
                                return yScale(d.low);
                            }
                        })
                        .attr("height", function (d) {
                            if (d.down || d.nodata) {
                                return height - yScale(highBound) - pixelsOffHeight;
                            }
                            else {
                                return height - yScale(d.low) - pixelsOffHeight;
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset  );
                        })

                        .attr("opacity", ".9")
                        .attr("fill", function (d, i) {
                            if (d.down) {
                                return  "url(#redStripes)";
                            }
                            else if (d.nodata) {
                                return  "url(#grayStripes)";
                            }
                            else {
                                return  "url(#leaderBarGrad)";
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
                            return isNaN(d.high) ? yScale(lowBound) : yScale(d.high);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.y) - yScale(d.high);
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset  );
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", "#1794bc");


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
                            }
                            else {
                                return  yScale(d.low) - yScale(d.y);
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset );
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", "#70c4e2");

                // if high == low put a "cap" on the bar to show non-aggregated bar
                svg.selectAll("rect.singleValue")
                        .data(chartContext.data)
                        .enter().append("rect")
                        .attr("class", "singleValue")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y)-2;
                        })
                        .attr("height", function (d) {
                            if (d.down || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                if(d.low === d.high  ){
                                    return  yScale(d.low) - yScale(d.y) +2;
                                }else {
                                    return  yScale(d.low) - yScale(d.y);
                                }
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset );
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", function (d) {
                                if(d.low === d.high  ){
                                    return  "#50505a";
                                }else {
                                    return  "#70c4e2";
                                }
                        });
            }

            function createYAxisGridLines() {
                // create the y axis grid lines
                svg.append("g").classed("grid y_grid", true)
                        .call($wnd.d3.svg.axis()
                                .scale(yScale)
                                .orient("left")
                                .ticks(10)
                                .tickSize(-width, 0, 0)
                                .tickFormat("")
                        );
            }

            function createXandYAxes() {

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("transform", "translate(0," + height + ")")
                        .attr("font-size", "10px")
                        .attr("font-family", "'Liberation Sans', Arial, Helvetica, sans-serif")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
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

            }

            function createMinAvgPeakLines() {

                var avgLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale((avg));
                        }),
                peakLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale((peak));
                        }),
                minLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale(min);
                        }),
                minBaselineLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale(d.baselineMin);
                        }),
                maxBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMax);
                                }),
                barAvgLine = $wnd.d3.svg.line()
                                .interpolate("linear")
                                .x(function (d) {
                                    return timeScale(d.x)+ ((width / chartContext.data.length - barOffset)/ 2);
                                })
                                .y(function (d) {
                                    if(d.y == undefined){
                                        return yScale(0);
                                    }else {
                                        return yScale(+d.y);
                                    }
                                });

                // peak Line (must be before line.high to look right
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "peakLine")
                        .attr("fill", "none")
                        .attr("stroke", "#ff8a9a")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "3,3")
                        .attr("stroke-opacity", ".7")
                        .attr("d", peakLine)
                        .text("Peak")
                        .append("title")
                        .text(function(d ) { return "Peak"; });

                // min Line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "minLine")
                        .attr("fill", "none")
                        .attr("stroke", "#8ad6ff")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-dasharray", "3,3")
                        .attr("stroke-opacity", ".9")
                        .attr("d", minLine);

                // avg line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "avgLine")
                        .attr("fill", "none")
                        .attr("stroke", "#b0d9b0")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-dasharray", "3,3")
                        .text("Avg")
                        //.attr("stroke-opacity", ".9")
                        .attr("d", avgLine);

                // min baseline Line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "minBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "purple")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".9")
                        .attr("d", minBaselineLine);

                // max baseline Line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "maxBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "orange")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".7")
                        .attr("d", maxBaselineLine);

                // Bar avg line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "barAvgLine")
                        .attr("fill", "none")
                        .attr("stroke", "#2e376a")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-opacity", ".7")
                        .attr("d", barAvgLine);
            }

            function formatHovers(chartContext, d) {
                var hoverString,
                        xValue = (d.x == undefined) ? 0 : +d.x,
                        date = new Date(+xValue),
                        availStartDate = new Date(+d.availStart),
                        availEndDate = new Date(+d.availEnd),
                        availDuration = d.availDuration,
                        barDuration = d.barDuration,
                        unknownStartDate = new Date(+d.unknownStart),
                        unknownEndDate = new Date(+d.unknownEnd),
                        timeFormatter = $wnd.d3.time.format("%I:%M:%S %p"),
                        dateFormatter = $wnd.d3.time.format("%m/%d/%y"),
                        highValue = (d.high == undefined) ? 0 : d.high.toFixed(2),
                        lowValue = (d.low == undefined) ? 0 : d.low.toFixed(2),
                        avgValue = (d.y == undefined) ? 0 : d.y.toFixed(2);

                if (d.down) {
                    hoverString =
                            '<div style="text-align:left;z-index:401000;"><span style="width:50px;font-weight: bold;color:#d3d3d6";">' + chartContext.timeLabel + ': </span>' + timeFormatter(date) + '</div>' +
                                    '<div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#d3d3d6"";">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr style="width:100%;text-align: center;border: #d3d3d3 solid thin;"></hr>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+chartContext.hoverStartLabel+": "+ timeFormatter(availStartDate)+ '</span></div>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+chartContext.hoverEndLabel+": "+ timeFormatter(availEndDate) + '</span></div>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+chartContext.hoverPeriodLabel+": "+ availDuration + '</span></div>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight: bold;color:#ff8a9a"";">'+chartContext.downLabel +'</span></div>' +
                                    '</div>';
                }
                else if (d.y == undefined) {
                    hoverString =
                            '<div style="text-align:left;z-index:401000;"><span style="width:50px;font-weight: bold;color:#d3d3d6";">' + chartContext.timeLabel + ': </span>' + timeFormatter(date) + '</div>' +
                                    '<div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#d3d3d6"";">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr style="width:100%;text-align: center;border: #d3d3d3 solid thin;"></hr>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+chartContext.hoverStartLabel+": "+ timeFormatter(unknownStartDate)+ '</span></div>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+chartContext.hoverEndLabel+": "+ timeFormatter(unknownEndDate) + '</span></div>' +
                                    '<div style="text-align: right;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+chartContext.unknownLabel+'</span></div>' +
                                    '</div>';

                }
                else {
                    hoverString =
                            '<div style="text-align:left;z-index:401000;"><span style="width:50px;font-weight: bold;color:#d3d3d6";">' + chartContext.timeLabel + ':  </span><span style="width:50px;">' + timeFormatter(date) + '</span></div>' +
                                    '<div style="text-align: left;"><span style="width:50px;font-weight: bold;color:#d3d3d6"";">' + chartContext.dateLabel + ':  </span><span style="width:50px;">' + dateFormatter(date) + '</span></div>' +
                                    '<div style="text-align: left;"><span style="width:100%;font-weight:bold;color:#d3d3d6"";">'+"Bar"+": "+ barDuration + '</span></div>' +
                                    '<hr style="width:100%;text-align: center;border: #d3d3d3 solid thin;"></hr>' +
                                    '<div style="text-align: right;"><span style="width:50px;font-weight:bold;color:#ff8a9a;";">' + chartContext.peakChartTitle + ': </span><span style="width:50px;">' + highValue + '</span></div>' +
                                    '<div style="text-align: right;"><span style="text-align:right;width:50px;font-weight:bold;color: #b0d9b0;"">' + chartContext.avgChartTitle + ':  </span><span style="width:50px;">' + avgValue + '</span></div>' +
                                    '<div style="text-align: right;"><span style="width:50px;font-weight:bold;color:#8ad6ff"">' + chartContext.minChartTitle + ': </span><span style="width:50px;">' + lowValue + '</span></div>' +
                                    '</div>';
                }
                return hoverString;

            }

            function createHovers(chartContext) {
                //console.log("Create Hovers");
                $wnd.jQuery('svg rect.leaderBar, svg rect.high, svg rect.low').tipsy({
                    gravity: 'w',
                    html: true,
                    trigger: 'hover',
                    title: function () {
                        var d = this.__data__;
                        console.log("y: " + d.y);
                        return formatHovers(chartContext, d);
                    }
                });
            }

            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    console.log("chart id: " + chartContext.chartSelection);
                    //console.log("Json Data:\n"+chartContext.data);

                    createHeader(chartContext.yAxisLabel, chartContext.minChartTitle, min, chartContext.avgChartTitle, avg, chartContext.peakChartTitle, peak);
                    createYAxisGridLines();
                    createStackedBars();
                    createXandYAxes();
                    createMinAvgPeakLines();
                    createHovers(chartContext);
                    console.log("finished drawing paths");
                }
            }; // end public closure
        }();

        metricStackedBarGraph.draw(chartContext);

    }-*/;

}
