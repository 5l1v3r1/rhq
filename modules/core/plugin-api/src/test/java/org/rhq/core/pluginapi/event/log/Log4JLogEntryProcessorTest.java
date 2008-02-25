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
package org.rhq.core.pluginapi.event.log;

import java.util.Date;
import java.util.Calendar;

import org.testng.annotations.Test;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;

/**
 * @author Ian Springer
 */
@Test
public class Log4JLogEntryProcessorTest {
    public void testProcessLine() {
        String eventType = "logEntry";
        LogEntryProcessor processor = new Log4JLogEntryProcessor(eventType);
        Event event = processor.processLine(
                "2007-12-09 15:32:49,909 DEBUG [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000");
        assert event != null;
        //noinspection ConstantConditions
        assert event.getType().equals(eventType);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2007, 11, 9, 15, 32, 49);
        calendar.set(Calendar.MILLISECOND, 909);
        Date expectedDate = calendar.getTime();
        assert event.getTimestamp().equals(expectedDate);
        assert event.getSeverity().equals(EventSeverity.DEBUG);
        assert event.getDetail().equals("[com.example.FooBar] run: IdleRemover notifying pools, interval: 450000");
    }
}
