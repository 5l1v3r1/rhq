/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.EventGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class EventGWTServiceImpl extends AbstractGWTServiceImpl implements EventGWTService {

    private EventManagerLocal eventManager = LookupUtil.getEventManager();


    public EventSeverity[] getSeverityBuckets(int resourceId, long begin, long end, int numBuckets) {
        return SerialUtility.prepare(
                eventManager.getSeverityBuckets(getSessionSubject(), resourceId, begin, end, numBuckets),
                "EventService.getSeverityBuckets");
    }

    public EventSeverity[] getSeverityBucketsForAutoGroup(int parentResourceId, int resourceTypeId, long begin, long end, int numBuckets) {
        return SerialUtility.prepare(
                eventManager.getSeverityBucketsForAutoGroup(getSessionSubject(), parentResourceId, resourceTypeId, begin, end, numBuckets),
                "EventService.getSeverityBucketsForAutoGroup");
    }

    public EventSeverity[] getSeverityBucketsForCompGroup(int resourceGroupId, long begin, long end, int numBuckets) {
        return SerialUtility.prepare(
                    eventManager.getSeverityBucketsForCompGroup(getSessionSubject(), resourceGroupId, begin, end, numBuckets),
                    "EventService.getSeverityBucketsForCompGroup");
        }

    public PageList<Event> findEventsByCriteria(EventCriteria criteria) {
        return SerialUtility.prepare(
                eventManager.findEventsByCriteria(getSessionSubject(), criteria),
                "EventService.findEventsByCriteria");
    }
}
