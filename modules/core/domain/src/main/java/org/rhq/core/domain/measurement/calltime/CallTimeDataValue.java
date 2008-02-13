/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.measurement.calltime;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.DateFormat;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.jetbrains.annotations.Nullable;

/**
 * Measurement data for a set of calls to a particular destination over a certain time span.
 *
 * @author Ian Springer
 */
@Entity
@NamedQueries( {
// NOTE: This query only includes data chunks that are fully within the specified time interval, because it would
//       not be possible to extrapolate the stats only for the overlapping portion of partially overlapping chunks.
@NamedQuery(name = CallTimeDataValue.QUERY_FIND_COMPOSITES_FOR_RESOURCE, query = "SELECT new org.rhq.core.domain.measurement.calltime.CallTimeDataComposite("
    + "key.callDestination, "
    + "MIN(value.minimum), "
    + "MAX(value.maximum), "
    + "SUM(value.total), "
    + "SUM(value.count), "
    + "SUM(value.total) / SUM(value.count)) "
    + "FROM CallTimeDataValue value "
    + "JOIN value.key key "
    + "WHERE key.schedule.id = :scheduleId "
    + "AND value.count != 0 "
    + "AND value.minimum != -1 "
    + "AND value.beginTime >= :beginTime "
    + "AND value.endTime <= :endTime "
    + "GROUP BY key.callDestination ") })
@SequenceGenerator(name = "idGenerator", sequenceName = "RHQ_CALLTIME_DATA_VALUE_ID_SEQ")
@Table(name = "RHQ_CALLTIME_DATA_VALUE")
public class CallTimeDataValue implements Externalizable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_COMPOSITES_FOR_RESOURCE = "CallTimeDataValue.findCompositesForResource";

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGenerator")
    @Id
    private int id;

    @JoinColumn(name = "KEY_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private CallTimeDataKey key;

    @Column(name = "BEGIN_TIME", nullable = false)
    private long beginTime;

    @Column(name = "END_TIME", nullable = false)
    private long endTime;

    @Column(name = "MINIMUM", nullable = false)
    private double minimum = Double.NaN;

    @Column(name = "MAXIMUM", nullable = false)
    private double maximum;

    @Column(name = "TOTAL", nullable = false)
    private double total;

    @Column(name = "COUNT", nullable = false)
    private long count;

    /**
     * Create a new <code>CallTimeDataValue</code>.
     *
     * @param beginTime the begin time of the time range for which the call-time data was collected
     * @param endTime   the end time of the time range for which the call-time data was collected
     */
    public CallTimeDataValue(Date beginTime, Date endTime) {
        this.beginTime = beginTime.getTime();
        this.endTime = endTime.getTime();
    }

    public CallTimeDataValue() {
        /* for JPA and deserialization use only */
    }

    public int getId() {
        return id;
    }

    @Nullable
    public CallTimeDataKey getKey() {
        return key;
    }

    public void setKey(@Nullable
    CallTimeDataKey key) {
        this.key = key;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void mergeCallTime(double callTime) {
        if (callTime < 0) {
            throw new IllegalArgumentException("Call time is a duration and so must be >= 0.");
        }

        this.count++;
        this.total += callTime;
        if ((callTime < this.minimum) || Double.isNaN(this.minimum)) {
            this.minimum = callTime;
        }

        if (callTime > this.maximum) {
            this.maximum = callTime;
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(this.beginTime);
        out.writeLong(this.endTime);
        out.writeDouble(this.minimum);
        out.writeDouble(this.maximum);
        out.writeDouble(this.total);
        out.writeLong(this.count);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.beginTime = in.readLong();
        this.endTime = in.readLong();
        this.minimum = in.readDouble();
        this.maximum = in.readDouble();
        this.total = in.readDouble();
        this.count = in.readLong();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + "key=" + this.key + ", " + "beginTime="
            + DateFormat.getInstance().format(this.beginTime) + ", " + "endTime="
            + DateFormat.getInstance().format(this.endTime) + ", " + "minimum=" + this.minimum + ", " + "maximum="
            + this.maximum + ", " + "total=" + this.total + ", " + "count=" + this.count + "]";
    }
}