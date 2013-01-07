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
package org.rhq.enterprise.server.cloud;

import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEvent.ExecutionStatus;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 * @author Jay Shaughnessy
 */
@Local
public interface PartitionEventManagerLocal {

    FailoverListComposite agentPartitionEvent(Subject subject, String agentToken, PartitionEventType eventType,
        String eventDetail);

    void createPartitionEvent(Subject subject, PartitionEvent partitionEvent);

    /**
     * This call performs a full repartitioning of the agent population at the time of the call.  
     * @param subject
     * @param eventType
     * @param eventDetail Any useful information regarding the event generation. Should be suitable for display.
     *        Typically a relevant server name.   
     */
    Map<Agent, FailoverListComposite> cloudPartitionEvent(Subject subject, PartitionEventType eventType,
        String eventDetail);

    /**
     * This call requests full repartitioning of the agent population by the recurring cluster manager job.
     * 
     * the subject needs to have MANAGE_INVENTORY and MANAGE_SETTINGS permissions.
     * 
     * @param subject the caller
     * @param eventType
     * @param eventDetail Any useful information regarding the event generation. Should be suitable for display.
     *        Typically a relevant server name.   
     */
    void cloudPartitionEventRequest(Subject subject, PartitionEventType eventType, String eventDetail);

    /** This call performs no partitioning activity, it only audits that some event has taken place that could
     * affect, or contribute to, a future partitioning. For example, SERVER_DOWN, AGENT_LOAD_CHANGE, etc.
     * @param subject
     * @param eventType Can be any event type although typically a type used here will not also be used in a
     * @param eventDetail Any useful information regarding the event generation. Should be suitable for display.
     *        Typically s relevant server or agent name.  
     * server list generating call.
     */
    void auditPartitionEvent(Subject subject, PartitionEventType eventType, String eventDetail);

    /**
     * This call queries for and then processed all outstanding, requested partition events resulting from previous
     * calls to {@link cloudPartitionEventRequest}.  
     */
    void processRequestedPartitionEvents();

    /**
     * This is primarily a test entry point.
     * 
     * the subject needs to have MANAGE_INVENTORY and MANAGE_SETTINGS permissions.
     * 
     * @param subject
     * @param partitionEventIds
     */
    void deletePartitionEvents(Subject subject, Integer[] partitionEventIds);

    /**
     * Deletes all the partition events.
     * 
     * the subject needs to have MANAGE_INVENTORY and MANAGE_SETTINGS permissions.
     * 
     * @param subject
     * @return count of deleted events
     */
    int purgeAllEvents(Subject subject);

    PartitionEvent getPartitionEvent(Subject subject, int partitionEventId);

    PageList<PartitionEvent> getPartitionEvents(Subject subject, PartitionEventType type, ExecutionStatus status,
        String details, PageControl pageControl);

    /**
     * Returns the partition event details.
     * 
     * the subject needs to have MANAGE_INVENTORY and MANAGE_SETTINGS permissions.
     * 
     * @param subject the caller
     * @param partitionEventId
     * @param pageControl
     * @return list with partition event details
     */
    PageList<PartitionEventDetails> getPartitionEventDetails(Subject subject, int partitionEventId,
        PageControl pageControl);
    
    /**
     * Fetches the partition events based on provided criteria.
     * 
     * the subject needs to have MANAGE_INVENTORY and MANAGE_SETTINGS permissions.
     * 
     * @param subject the caller
     * @param criteria the criteria
     * @return list with partition events
     */
    PageList<PartitionEvent> findPartitionEventsByCriteria(Subject subject, PartitionEventCriteria criteria);
}
