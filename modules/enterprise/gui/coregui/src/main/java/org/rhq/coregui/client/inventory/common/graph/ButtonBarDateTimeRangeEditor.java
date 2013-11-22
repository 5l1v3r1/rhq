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
package org.rhq.coregui.client.inventory.common.graph;

import java.util.Date;

import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.TimeItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.coregui.client.components.measurement.RefreshIntervalMenu;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Component to allow selection of Date/Time range for graphs using a radio button group.
 * The DateTimeButton enum defines the button labels and time ranges change this if you
 * wish to add/delete custom time ranges.
 *
 * @author Mike Thompson
 */
public class ButtonBarDateTimeRangeEditor extends EnhancedVLayout {

    private static final String TIMERANGE = "graphtimerange";
    private static final int BUTTON_WIDTH = 28;
    public final String DATE_TIME_FORMAT = MSG.common_buttonbar_datetime_format_moment_js();

    private Refreshable refreshable;
    private Label dateRangeLabel;
    private DateTimeButtonBarClickHandler dateTimeButtonBarClickHandler;
    // just a reference to pass to CustomDateRangeWindow as it must be final
    // so 'this' won't work
    final private ButtonBarDateTimeRangeEditor self;
    private boolean isAutoRefresh;
    private RefreshIntervalMenu refreshIntervalMenu;
    private EnhancedIButton refreshButton;

    public ButtonBarDateTimeRangeEditor(Refreshable refreshable) {
        this.self = this;
        this.refreshable = refreshable;
        this.isAutoRefresh = this.refreshable instanceof AutoRefresh;

        dateTimeButtonBarClickHandler = new DateTimeButtonBarClickHandler();

    }

    private void createButtons() {

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setHeight(34);

        toolStrip.addSpacer(10);

        for (DateTimeButton dateTimeButton : DateTimeButton.values()) {
            IButton oneHourButton = new IButton(dateTimeButton.label);
            oneHourButton.setWidth(BUTTON_WIDTH);
            oneHourButton.setActionType(SelectionType.RADIO);
            oneHourButton.setRadioGroup(TIMERANGE);
            oneHourButton.addClickHandler(dateTimeButtonBarClickHandler);

            toolStrip.addMember(oneHourButton);
        }

        IButton customButton = new IButton(MSG.common_buttonbar_custom());
        customButton.setWidth(60);
        customButton.setActionType(SelectionType.RADIO);
        customButton.setRadioGroup(TIMERANGE);
        customButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                CustomDateRangeWindow customDateRangeWindow = new CustomDateRangeWindow(MSG
                    .common_buttonbar_custom_window_title(), MSG.common_buttonbar_custom_window_subtitle(), self,
                    new Date(CustomDateRangeState.getInstance().getStartTime()), new Date(CustomDateRangeState
                        .getInstance().getEndTime()));
                CustomDateRangeState.getInstance().setCustomDateRangeActive(true);
                customDateRangeWindow.show();
            }
        });
        toolStrip.addMember(customButton);

        toolStrip.addSpacer(30);

        dateRangeLabel = new Label();
        dateRangeLabel.setWidth(400);
        dateRangeLabel.addStyleName("graphDateTimeRangeLabel");
        showUserFriendlyTimeRange(CustomDateRangeState.getInstance().getStartTime(), CustomDateRangeState.getInstance()
            .getEndTime());
        toolStrip.addMember(dateRangeLabel);

        // Only allow auto refresh rate change in views actually performing auto refresh, otherwise offer only refresh
        toolStrip.addSpacer(20);
        if (isAutoRefresh) {
            refreshIntervalMenu = new RefreshIntervalMenu((AutoRefresh)refreshable);
            toolStrip.addMember(refreshIntervalMenu);
        } else {
            this.refreshButton = new EnhancedIButton(MSG.common_button_refresh());
            refreshButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    redrawGraphs();
                }
            });
            toolStrip.addMember(refreshButton);
        }

        addMember(toolStrip);
    }

    public boolean isCustomTimeRangeActive() {
        return CustomDateRangeState.getInstance().isCustomDateRangeActive();
    }

    public void setCustomDateRangeActive(boolean customDateRangeActive) {
        CustomDateRangeState.getInstance().setCustomDateRangeActive(customDateRangeActive);
    }

    public void redrawGraphs() {
        refreshable.refreshData();
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
    }

    public Long getStartTime() {
        return CustomDateRangeState.getInstance().getStartTime();
    }

    public Long getEndTime() {
        return CustomDateRangeState.getInstance().getEndTime();
    }

    public Date calculateStartDate(Date endDate, String dateTimeSelection) {
        long dateTimeOffset = 0;
        for (DateTimeButton dateTimeButton : DateTimeButton.values()) {
            if (dateTimeButton.label.equals(dateTimeSelection)) {
                dateTimeOffset = dateTimeButton.timeSpanInSeconds * 1000;
                break;
            }
        }

        return new Date(endDate.getTime() - dateTimeOffset);
    }

    public native void showUserFriendlyTimeRange(double startTime, double endTime) /*-{
        "use strict";
        var startDateMoment = $wnd.moment(startTime),
            endDateMoment = $wnd.moment(endTime),
            dateTimeFormat = this.@org.rhq.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor::DATE_TIME_FORMAT,
            formattedDateRange = startDateMoment.format(dateTimeFormat) + '  -  ' + endDateMoment.format(dateTimeFormat),
        timeRange = endDateMoment.from(startDateMoment,true);
        $wnd.jQuery('.graphDateTimeRangeLabel').text(formattedDateRange+' ('+timeRange+')');
    }-*/;

    public void updateTimeRangeToNow() {
        if (!isCustomTimeRangeActive()) {
            Date now = new Date();
            long timeRange = CustomDateRangeState.getInstance().getTimeRange();
            Date newStartDate = new Date(now.getTime() - timeRange);
            showUserFriendlyTimeRange(newStartDate.getTime(), now.getTime());
            saveDateRange(newStartDate.getTime(), now.getTime());
        }
    }

    /**
     * Whenever we make a change to the date range save it here so it gets propagated to
     * the correct places.
     *
     * @param startTime double because JSNI doesn't support long
     * @param endTime   double because JSNI doesn't support long
     */
    public void saveDateRange(double startTime, double endTime) {
        // if the encompassing view already handles its own refresh (e.g. AbstractD3GrpahListView) then don't
        // let a preference update cause a whole gui refresh (which it does by default to apply the new preference).
        // the two refreshes are redundant at best, step on each other at worst..
        boolean allowPreferenceUpdateRefresh = !isAutoRefresh;
        CustomDateRangeState.getInstance().saveDateRange(startTime, endTime, allowPreferenceUpdateRefresh);
    }

    private class DateTimeButtonBarClickHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent clickEvent) {
            IButton button = (IButton) clickEvent.getSource();
            String selectedDateTimeRange = button.getTitle();
            CustomDateRangeState.getInstance().setCustomDateRangeActive(false);
            Date now = new Date();
            Date calculatedStartDateTime = calculateStartDate(now, selectedDateTimeRange);
            saveDateRange(calculatedStartDateTime.getTime(), now.getTime());
            redrawGraphs();
            showUserFriendlyTimeRange(calculatedStartDateTime.getTime(), now.getTime());
        }
    }

    @SuppressWarnings("GwtInconsistentSerializableClass")
    /**
     * This enum defines the button labels and time ranges used in the toolbar.
     */
    private enum DateTimeButton {
        oneHour("1h", 60 * 60), fourHour("4h", 4 * 60 * 60), eightHour("8h", 8 * 60 * 60), twelveHour("12h",
            12 * 60 * 60), oneDay("1d", 24 * 60 * 60), fiveDay("5d", 5 * 24 * 60 * 60), oneMonth("1m",
            30 * 24 * 60 * 60), threeMonth("3m", 3 * 30 * 24 * 60 * 60), sixMonth("6m", 6 * 30 * 24 * 60 * 60);

        private final String label;
        private final long timeSpanInSeconds;

        DateTimeButton(String label, long timeSpanInSeconds) {
            this.label = label;
            this.timeSpanInSeconds = timeSpanInSeconds;
        }

    }

    public class CustomDateRangeWindow extends Window {

        public CustomDateRangeWindow(String title, String windowTitle,
            final ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor, Date startTime, Date endTime) {
            super();
            setTitle(windowTitle + ": " + title);
            setShowMinimizeButton(false);
            setShowMaximizeButton(false);
            setShowCloseButton(true);
            setIsModal(true);
            setShowModalMask(true);
            setWidth(450);
            setHeight(340);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            DynamicForm form = new DynamicForm();
            form.setMargin(25);
            form.setAutoFocus(true);
            form.setShowErrorText(true);
            form.setErrorOrientation(FormErrorOrientation.BOTTOM);
            form.setHeight100();
            form.setWidth100();
            form.setPadding(5);
            form.setLayoutAlign(VerticalAlignment.BOTTOM);
            final DateItem startDateItem = new DateItem("startDate", MSG.common_buttonbar_start_date());
            startDateItem.setValue(startTime);
            final TimeItem startTimeItem = new TimeItem("startTime", MSG.common_buttonbar_start_time());
            startTimeItem.setValue(startTime);
            final DateItem endDateItem = new DateItem("endDate", MSG.common_buttonbar_end_date());
            endDateItem.setValue(endTime);
            final TimeItem endTimeItem = new TimeItem("endTime", MSG.common_buttonbar_end_time());
            endTimeItem.setValue(endTime);
            form.setFields(startDateItem, startTimeItem, new RowSpacerItem(), endDateItem, endTimeItem,
                new RowSpacerItem());
            this.addItem(form);

            HLayout buttonHLayout = new HLayout();
            buttonHLayout.setMargin(75);
            buttonHLayout.setMembersMargin(20);
            IButton cancelButton = new IButton(MSG.common_buttonbar_custom_cancel());
            cancelButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    CustomDateRangeWindow.this.destroy();
                }
            });
            buttonHLayout.addMember(cancelButton);

            IButton saveButton = new IButton(MSG.common_buttonbar_custom_save());
            saveButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    Date startTimeDate = (Date) startTimeItem.getValue();
                    Date endTimeDate = (Date) endTimeItem.getValue();

                    Date newStartDate = new Date(startDateItem.getValueAsDate().getYear(), startDateItem
                        .getValueAsDate().getMonth(), startDateItem.getValueAsDate().getDate(), startTimeDate
                        .getHours(), startTimeDate.getMinutes());
                    Date newEndDate = new Date(endDateItem.getValueAsDate().getYear(), endDateItem.getValueAsDate()
                        .getMonth(), endDateItem.getValueAsDate().getDate(), endTimeDate.getHours(), endTimeDate
                        .getMinutes());
                    buttonBarDateTimeRangeEditor.saveDateRange(newStartDate.getTime(), newEndDate.getTime());
                    redrawGraphs();
                    showUserFriendlyTimeRange(newStartDate.getTime(), newEndDate.getTime());
                    CustomDateRangeWindow.this.destroy();
                }
            });

            buttonHLayout.addMember(saveButton);
            addItem(buttonHLayout);

            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    try {
                        CustomDateRangeWindow.this.destroy();
                    } catch (Throwable e) {
                        Log.warn("Cannot destroy custom date range window.", e);
                    }
                }
            });

        }
    }

}
