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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Component to allow selection of Date/Time range for graphs using a radio button group.
 * The DateTimeButton enum defines the button labels and time ranges change this if you
 * wish to add/delete custom time ranges.
 *
 * @author Mike Thompson
 */
public class ButtonBarDateTimeRangeEditor extends EnhancedVLayout {

    static final String TIMERANGE = "graphtimerange";
    static final int BUTTON_WIDTH = 28;

    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractD3GraphListView d3GraphListView;
    private static final Messages MSG = CoreGUI.getMessages();
    private Label dateRangeLabel;
    //@todo: pull dateformat messages.properties
    private static final DateTimeFormat fmt = DateTimeFormat.getFormat("MM/dd/yyyy h:mm a");
    private DateTimeButtonBarClickHandler dateTimeButtonBarClickHandler;
    private AbstractMeasurementRangeEditor.MetricRangePreferences prefs;

    public ButtonBarDateTimeRangeEditor(MeasurementUserPreferences measurementUserPrefs,
                                        AbstractD3GraphListView d3GraphListView) {
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;

        dateTimeButtonBarClickHandler = new DateTimeButtonBarClickHandler();
        prefs = measurementUserPreferences.getMetricRangePreferences();
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start: " + prefs.begin + " end: " + prefs.end);
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start Date: " + new Date(prefs.begin) + " end Date: "
                + new Date(prefs.end));
        createButtons();
    }

    public void createButtons() {

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setHeight(24);

        toolStrip.addSpacer(10);

        for(DateTimeButton dateTimeButton : DateTimeButton.values()){
            IButton oneHourButton = new IButton(dateTimeButton.label);
            oneHourButton.setWidth(BUTTON_WIDTH);
            oneHourButton.setActionType(SelectionType.RADIO);
            oneHourButton.setRadioGroup(TIMERANGE);
            oneHourButton.addClickHandler(dateTimeButtonBarClickHandler);

            toolStrip.addMember(oneHourButton);
        }


        IButton customButton = new IButton("Custom...");
        customButton.setWidth(60);
        customButton.disable();
        customButton.setActionType(SelectionType.RADIO);
        customButton.setRadioGroup(TIMERANGE);
        customButton.addClickHandler(dateTimeButtonBarClickHandler);
        toolStrip.addMember(customButton);

        toolStrip.addSpacer(30);

        dateRangeLabel = new Label();
        dateRangeLabel.setWidth(400);
        dateRangeLabel.addStyleName("graphDateTimeRangeLabel");
        updateDateTimeRangeDisplay(new Date(prefs.begin), new Date(prefs.end));
        toolStrip.addMember(dateRangeLabel);

        toolStrip.addSpacer(20);

//        IButton resetZoomButton = new IButton("Reset Manual Zoom");
//        resetZoomButton.setWidth(150);
//        resetZoomButton.disable();
//        toolStrip.addMember(resetZoomButton);

        addMember(toolStrip);
    }


    public void redrawGraphs() {
        d3GraphListView.redrawGraphs();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createButtons();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        createButtons();
        //drawJsniChart();
    }

    public Label getDateRangeLabel() {
        return dateRangeLabel;
    }

    public Long getStartTime() {
        return measurementUserPreferences.getMetricRangePreferences().begin;
    }

    public Long getEndTime() {
        return measurementUserPreferences.getMetricRangePreferences().end;
    }

    public Date calculateStartDate(Date endDate, String dateTimeSelection) {
        long dateTimeOffset = 0;
        for(DateTimeButton dateTimeButton : DateTimeButton.values()){
            if(dateTimeButton.label.equals(dateTimeSelection)){
                dateTimeOffset = dateTimeButton.timeSpanInSeconds * 1000;
                break;
            }
        }

        Log.debug("DateTimeSelection: "+ dateTimeSelection + " = "+ dateTimeOffset);
        return new Date(endDate.getTime() - dateTimeOffset);
    }

    /**
     * Function meant to be called by the javascript inside the d3 charting javascript.
     * @param startDate double (as javascript doenst have long) representing unix date
     * @param endDate double
     */
    public void updateDateTimeRangeDisplayFromJavascript(double startDate, double endDate){
        updateDateTimeRangeDisplay(new Date((long)startDate), new Date((long)endDate));
    }

    public void updateDateTimeRangeDisplay(Date startDate, Date endDate) {
        String rangeString = fmt.format(startDate) + " - " + fmt.format(endDate);
        dateRangeLabel.setContents(rangeString);

    }

    /**
     * Whenever we make a change to the date range save it here so it gets propogated to
     * the correct places.
     *
     * @param startTime double because JSNI doesnt support long
     * @param endTime   double because JSNI doesnt support long
     */
    public void saveDateRange(double startTime, double endTime) {
        final boolean advanced = true;
        prefs.explicitBeginEnd = advanced;
        prefs.begin = (long) startTime;
        prefs.end = (long) endTime;
        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
        } else {
            measurementUserPreferences.setMetricRangePreferences(prefs);
        }

    }

    private class DateTimeButtonBarClickHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent clickEvent) {
            IButton button = (IButton) clickEvent.getSource();
            String selectedDateTimeRange = button.getTitle();
            Date calculatedStartDateTime = calculateStartDate(new Date(getEndTime()), selectedDateTimeRange);
            saveDateRange(calculatedStartDateTime.getTime(),new Date().getTime());
            redrawGraphs();
            updateDateTimeRangeDisplay(calculatedStartDateTime, new Date());
        }
    }

    @SuppressWarnings("GwtInconsistentSerializableClass")
    /**
     * This enum defines the button labels and time ranges used in the toolbar.
     */
    private enum DateTimeButton {
        oneHour( "1h", 60 * 60 ),
        fourHour( "4h", 4 * 60 * 60 ),
        eightHour( "8h", 8 * 60 * 60 ),
        twelveHour( "12h", 12 * 60 * 60 ),
        oneDay("1d", 24 * 60 * 60 ),
        fiveDay("5d", 5 * 24 * 60 * 60 ),
        oneMonth("1m", 30 * 24 * 60 * 60 ),
        threeMonth("3m", 3 * 30 * 24 * 60 * 60 ),
        sixMonth("6m", 6 * 30 * 24 * 60 * 60 );

        private final String label;
        private final long timeSpanInSeconds;
        private final ClickHandler clickHandler;

        DateTimeButton(String label, long timeSpanInSeconds) {
            this.label = label;
            this.timeSpanInSeconds = timeSpanInSeconds;
            this.clickHandler = new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    IButton button = (IButton) clickEvent.getSource();
                    Log.debug("Button pressed for: " + button.getTitle());

                }
            };

        }

    }

}
