/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import static org.joda.time.DateTime.now;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.field.DividedDateTimeField;

/**
 * @author John Sanda
 */
public class DateTimeService {

    static final int SEVEN_DAYS = Duration.standardDays(7).toStandardSeconds().getSeconds();
    static final int TWO_WEEKS = Duration.standardDays(14).toStandardSeconds().getSeconds();
    static final int ONE_MONTH = Duration.standardDays(31).toStandardSeconds().getSeconds();
    static final int ONE_YEAR = Duration.standardDays(365).toStandardSeconds().getSeconds();

    private DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

    private MetricsConfiguration configuration;

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    public DateTime getTimeSlice(DateTime dateTime, Minutes interval) {
        Chronology chronology = GregorianChronology.getInstance();
        DateTimeField hourField = chronology.hourOfDay();
        DividedDateTimeField dividedField = new DividedDateTimeField(hourField, DateTimeFieldType.clockhourOfDay(),
            interval.toStandardHours().getHours());
        long timestamp = dividedField.roundFloor(dateTime.getMillis());

        return new DateTime(timestamp);
        //return dateTimeFloor(dateTime, interval.toPeriod());
    }

    public DateTime getTimeSlice(DateTime dt, Duration duration) {
        Period p = duration.toPeriod();

        if (p.getYears() != 0) {
            return dt.yearOfEra().roundFloorCopy().minusYears(dt.getYearOfEra() % p.getYears());
        } else if (p.getMonths() != 0) {
            return dt.monthOfYear().roundFloorCopy().minusMonths((dt.getMonthOfYear() - 1) % p.getMonths());
        } else if (p.getWeeks() != 0) {
            return dt.weekOfWeekyear().roundFloorCopy().minusWeeks((dt.getWeekOfWeekyear() - 1) % p.getWeeks());
        } else if (p.getDays() != 0) {
            return dt.dayOfMonth().roundFloorCopy().minusDays((dt.getDayOfMonth() - 1) % p.getDays());
        } else if (p.getHours() != 0) {
            return dt.hourOfDay().roundFloorCopy().minusHours(dt.getHourOfDay() % p.getHours());
        } else if (p.getMinutes() != 0) {
            return dt.minuteOfHour().roundFloorCopy().minusMinutes(dt.getMinuteOfHour() % p.getMinutes());
        } else if (p.getSeconds() != 0) {
            return dt.secondOfMinute().roundFloorCopy().minusSeconds(dt.getSecondOfMinute() % p.getSeconds());
        }
        return dt.millisOfSecond().roundCeilingCopy().minusMillis(dt.getMillisOfSecond() % p.getMillis());
    }

    public boolean isInRawDataRange(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getRawRetention()), dateTime) < 0;
    }

    public boolean isIn1HourDataRange(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getOneHourRetention()), dateTime) < 0;
    }

    public boolean isIn6HourDataRnage(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getSixHourRetention()), dateTime) < 0;
    }

    public boolean isIn24HourDataRnage(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getTwentyFourHourRetention()), dateTime) < 0;
    }

    public DateTime hour0() {
        DateTime rightNow = now();
        return rightNow.hourOfDay().roundFloorCopy().minusHours(
            rightNow.hourOfDay().roundFloorCopy().hourOfDay().get());
    }

}
