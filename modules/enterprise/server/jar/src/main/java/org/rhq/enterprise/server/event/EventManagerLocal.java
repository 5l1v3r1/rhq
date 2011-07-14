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

package org.rhq.enterprise.server.event;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Interface for the Event Manager
 * @author Heiko W. Rupp
 * @author Joseph Marques
 *
 */
@Local
public interface EventManagerLocal {

    /**
     * Add the passed events to the database
     * @param events a set of events.
     */
    void addEventData(Map<EventSource, Set<Event>> events);

    /**
     * Deletes event data older than the specified time.
     *
     * @param deleteUpToTime event data older than this time will be deleted
     * @return number of deleted Events
     */
    int purgeEventData(Date deleteUpToTime) throws SQLException;

    Map<EventSeverity, Integer> getEventCountsBySeverity(Subject subject, int resourceId, long startDate, long endDate);

    Map<EventSeverity, Integer> getEventCountsBySeverityForGroup(Subject subject, int groupId, long startDate,
        long endDate);

    /**
     * Retrieve the count of events for the given resource in the time between begin and end, nicely separated
     * in numBuckets.
     * @param subject    Subject of the caller
     * @param resourceId Id of the resource we want to know the data
     * @param begin      Begin date
     * @param end        End date
     * @param numBuckets Number of buckets to distribute into.
     * @return
     */
    int[] getEventCounts(Subject subject, int resourceId, long begin, long end, int numBuckets);

    /**
     * Obtain detail information about the passed event
     * @param subject Subject of the caller
     * @param eventId ID of the desired event.
     * @return
     */
    EventComposite getEventDetailForEventId(Subject subject, int eventId) throws EventException;

    void deleteEventSourcesForDefinition(EventDefinition def);

    int deleteEventsForContext(Subject subject, EntityContext context, List<Integer> eventIds);

    int purgeEventsForContext(Subject subject, EntityContext context);

    PageList<EventComposite> findEventComposites(Subject subject, EntityContext context, long begin, long end,
        EventSeverity[] severities, String source, String detail, PageControl pc);

    PageList<EventComposite> findEventCompositesByCriteria(Subject subject, EventCriteria criteria);

    EventSeverity[] getSeverityBucketsByContext(Subject subject, EntityContext context, long begin, long end,
        int bucketCount);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    PageList<Event> findEventsByCriteria(Subject subject, EventCriteria criteria);

    EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets);

    EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentResourceId, int resourceTypeId,
        long begin, long end, int numBuckets);

    EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int resourceGroupId, long begin, long end,
        int numBuckets);
}
