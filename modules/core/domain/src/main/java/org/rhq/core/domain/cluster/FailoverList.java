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
package org.rhq.core.domain.cluster;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Agent;

/**
 * An object to capture a snapshot of the order in which particular agents
 * will fail over to particular servers.  The {@link FailoverListDetails}
 * will contain the ordered references back to other {@link Server}s.
 * 
 * @author jmarques
 *
 */
@Entity(name = "FailoverList")
@NamedQueries( //
{
    @NamedQuery(name = FailoverList.QUERY_DELETE_FOR_AGENT, query = "DELETE FROM FailoverList fl WHERE fl.agent = :agent"),
    @NamedQuery(name = FailoverList.QUERY_TRUNCATE, query = "DELETE FROM FailoverList") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_FAILOVER_LIST_ID_SEQ")
@Table(name = "RHQ_FAILOVER_LIST")
public class FailoverList implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_FOR_AGENT = "FailoverList.deleteForAgent";
    public static final String QUERY_TRUNCATE = "FailoverList.truncate";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @JoinColumn(name = "PARTITION_EVENT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    protected PartitionEvent partitionEvent;

    @JoinColumn(name = "AGENT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    protected Agent agent;

    @Column(name = "AGENT_ID", insertable = false, updatable = false)
    private int agentId;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    // required for JPA
    protected FailoverList() {
    }

    public FailoverList(PartitionEvent event, Agent agent) {
        this.partitionEvent = event;
        this.agent = agent;
    }

    public PartitionEvent getPartitionEvent() {
        return partitionEvent;
    }

    public void setPartitionEvent(PartitionEvent partitionEvent) {
        this.partitionEvent = partitionEvent;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + agentId;
        result = prime * result + (int) (ctime ^ (ctime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof FailoverList)) {
            return false;
        }

        final FailoverList other = (FailoverList) obj;

        if (agentId != other.agentId) {
            return false;
        }

        if (ctime != other.ctime) {
            return false;
        }

        return true;
    }
}
