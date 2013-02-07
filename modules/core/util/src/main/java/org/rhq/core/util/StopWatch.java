 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class StopWatch {
    private long start;
    private long end;
    private Map<String, TimeSlice> markerMap;

    public StopWatch() {
        reset();
    }

    public void markTimeBegin(String marker) {
        if (markerMap.containsKey(marker)) {
            TimeSlice ts = (TimeSlice) markerMap.get(marker);
            ts.cont();
        } else {
            markerMap.put(marker, new TimeSlice(marker));
        }
    }

    public void markTimeEnd(String marker) {
        if (!markerMap.containsKey(marker)) {
            throw new IllegalArgumentException("Invalid marker");
        }

        TimeSlice ts = (TimeSlice) markerMap.get(marker);
        ts.setFinished();
    }

    public StopWatch(long start) {
        this.start = start;
        markerMap = new HashMap<String, TimeSlice>();
    }

    public long reset() {
        try {
            return this.getElapsed();
        } finally {
            start = System.currentTimeMillis();
            markerMap = new HashMap<String, TimeSlice>();
        }
    }

    public long getElapsed() {
        end = System.currentTimeMillis();
        return end - start;
    }

    public String toString() {
        final long elap = this.getElapsed();
        final StringBuffer buf = new StringBuffer(formatMillis(elap));

        if (markerMap.size() > 0) {
            buf.append(" {StopWatch Markers:\n");

            long totalMarked = 0;
            TreeSet<String> keys = new TreeSet<String>(markerMap.keySet());
            for (String key : keys) {
                TimeSlice ts = markerMap.get(key);
                totalMarked += (ts.end - ts.begin);
                ts.writeBuf(buf);
                buf.append('\n');
            }

            long unmarked = elap - totalMarked;
            if (unmarked > 99) { // only show if the value is substantial
                buf.append(" [unmarked=").append(formatMillis(unmarked)).append("]\n");
            }

            buf.append("}");
        }

        return buf.toString();
    }

    private String formatMillis(long millis) {
        final String fraction = String.valueOf(millis % 1000L);
        int pad = 3 - fraction.length();

        final StringBuffer buf = new StringBuffer().append(millis / 1000L).append('.');

        // for example, 15 millseconds formatted as ".015" rather than ".15"
        while (pad-- > 0) {
            buf.append("0");
        }

        buf.append(fraction).append(" secs");
        return buf.toString();
    }

    class TimeSlice {
        String marker;
        long begin;
        long end;

        public TimeSlice(String marker) {
            this.marker = marker;
            this.begin = this.end = System.currentTimeMillis();
        }

        public void setFinished() {
            this.end = System.currentTimeMillis();
        }

        public void cont() {
            begin -= (end - begin);
        }

        public void writeBuf(StringBuffer buf) {
            long elap = end - begin;
            buf.append(" [").append(marker).append("=").append(formatMillis(elap)).append("] ");
        }
    }
}