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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

import com.google.gwt.i18n.client.DateTimeFormat;

/**
 * @author Greg Hinkle
 */
public class ExecutionSchedule implements Serializable {

    public enum Start {
        Immediately, Future
    };

    public enum End {
        Never, EndOn
    };

    public enum Recurr {
        Once, EveryNMinutes, Hourly, Daily, Weekly, Monthly
    };

    public enum DayOfWeek {
        Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    };

    private Start start = Start.Immediately;

    // start.Future
    private Recurr recurr = Recurr.Once;

    // Once
    private Date onceDateTime;


    // EveryNMinutes
    private Integer minuteInterval;


    // Hourly
    private Integer minuteInHour;


    // Daily
    private Date timeOfDay;


    // Weekly
    // Time of Day above
    private HashSet<DayOfWeek> daysOfWeek; // Can't use EnumSet because it doesn't have default instantiator


    // Monthly
    // Time of Day above
    private Integer dayOfMonth;


    // Used for all start.Future except for once
    private Date startDate;

    private Date endDate;


    public Start getStart() {
        return start;
    }

    public void setStart(Start start) {
        this.start = start;
    }

    public Recurr getRecurr() {
        return recurr;
    }

    public void setRecurr(Recurr recurr) {
        this.recurr = recurr;
    }

    public Date getOnceDateTime() {
        return onceDateTime;
    }

    public void setOnceDateTime(Date onceDateTime) {
        this.onceDateTime = onceDateTime;
    }

    public Integer getMinuteInterval() {
        return minuteInterval;
    }

    public void setMinuteInterval(Integer minuteInterval) {
        this.minuteInterval = minuteInterval;
    }

    public Integer getMinuteInHour() {
        return minuteInHour;
    }

    public void setMinuteInHour(Integer minuteInHour) {
        this.minuteInHour = minuteInHour;
    }

    public HashSet<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(HashSet<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(Date timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getMessage() {
        DateTimeFormat dateFormat = DateTimeFormat.getMediumDateFormat();
        DateTimeFormat timeFormat = DateTimeFormat.getMediumTimeFormat();


        String message = "Will execute";
        switch (start) {

            case Immediately:
                message += " immediately";
                break;
            case Future:

                if (recurr == Recurr.Once) {
                    message += " once at " + onceDateTime;

                } else {
                    switch (recurr) {
                        case EveryNMinutes:
                            message += " every " + minuteInterval + " minutes";
                            break;
                        case Hourly:
                            message += " every hour on the " + suffix(minuteInHour) + " minute";
                            break;
                        case Daily:
                            message += " every day at " + timeFormat.format(timeOfDay);
                            break;
                        case Weekly:
                            message += " every week on " + daysOfWeek.toString() + " at " + timeFormat.format(timeOfDay);
                            break;
                        case Monthly:
                            message += " every month on the " + suffix(dayOfMonth) + " day at " + timeFormat.format(timeOfDay);
                            break;
                    }

                    message += ", starting on " + dateFormat.format(startDate);
                    if (endDate != null) {
                        message += " and ending on " + dateFormat.format(endDate);
                    }

                }
                break;
        }
        message += ".";
        return message;
    }


    public String getCronString() {

        /* Cron fields
        1. Seconds
        2. Minutes
        3. Hours
        4. Day-of-Month
        5. Month
        6. Day-of-Week
        7. Year (optional field)
        */

        String cseconds, cminutes = null, chours = null, cdayOfMonth = null, cmonth = null, cdayOfWeek = null;

        cseconds = "0";

        DateTimeFormat dateFormat = DateTimeFormat.getMediumDateFormat();
        DateTimeFormat timeFormat = DateTimeFormat.getMediumTimeFormat();

        switch (start) {

            case Immediately:
                return null; // No cron string for immediate execution

            case Future:

                if (recurr == Recurr.Once) {
                    return null; // No cron string for non recurring
                } else {
                    switch (recurr) {
                        case EveryNMinutes:
                            cminutes = "0/" + minuteInterval;
                            chours = cdayOfMonth = cmonth = "*";
                            cdayOfWeek = "?";
                            break;
                        case Hourly:
                            cminutes = "" + minuteInHour;
                            chours = "*";
                            cdayOfMonth = cmonth = cdayOfWeek = "*";
                            break;
                        case Daily:
                            cminutes = "" + timeOfDay.getMinutes();
                            chours = "" + timeOfDay.getHours();
                            cdayOfMonth = cmonth = cdayOfWeek = "*";
                            break;
                        case Weekly:
                            cminutes = "" + timeOfDay.getMinutes();
                            chours = "" + timeOfDay.getHours();
                            cdayOfMonth = "?";
                            cmonth = "*";
                            cdayOfWeek = daysOfWeek.toString();
                            break;
                        case Monthly:
                            cminutes = "" + timeOfDay.getMinutes();
                            chours = "" + timeOfDay.getHours();
                            cdayOfMonth = "" + dayOfMonth;
                            cmonth = "*";
                            cdayOfWeek = "?";
                            break;
                    }
                }
                break;
        }
        return cseconds + " " + cminutes + " " + chours + " " + cdayOfMonth + " " + cmonth + " " + cdayOfWeek;

    }

    public String suffix(int num) {
        int last = num % 10;
        switch (last) {
            case 1:
                return num + "st";
            case 2:
                return num + "nd";
            case 3:
                return num + "rd";
            default:
                return num + "th";
        }
    }

}
