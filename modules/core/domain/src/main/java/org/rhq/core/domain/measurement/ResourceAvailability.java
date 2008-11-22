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
package org.rhq.core.domain.measurement;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Resource;

/**
 * This entity represents the latest known availability data for a resource.
 *
 * @author Joseph Marques
 */
@Entity
@Table(name = ResourceAvailability.TABLE_NAME)
@NamedQueries( //
{ @NamedQuery(name = ResourceAvailability.QUERY_FIND_BY_RESOURCE_ID, query = "" //
    + "  SELECT ra FROM ResourceAvailability ra WHERE ra.resourceId = :resourceId "),
    @NamedQuery(name = ResourceAvailability.UPDATE_BY_AGENT_ID, query = "" //
        + "  UPDATE ResourceAvailability " //
        + "     SET availabilityType = :availabilityType " //
        + "   WHERE resourceId IN ( SELECT res.id " //
        + "                           FROM Resource res " //
        + "                          WHERE res.agent.id = :agentId ) "),
    @NamedQuery(name = ResourceAvailability.INSERT_BY_RESOURCE_IDS, query = "" //
        + "  INSERT INTO ResourceAvailability ( resourceId ) " //
        + "       SELECT res.id " //
        + "         FROM Resource res " //
        + "    LEFT JOIN res.currentAvailability avail " //
        + "        WHERE res.id IN ( :resourceIds ) " //
        + "          AND avail IS NULL ") })
@SequenceGenerator(name = "RHQ_RESOURCE_AVAIL_SEQ", sequenceName = "RHQ_RESOURCE_AVAIL_ID_SEQ", allocationSize = 100)
public class ResourceAvailability implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_RESOURCE_AVAIL";

    public static final String QUERY_FIND_BY_RESOURCE_ID = "ResourceAvailability.findByResourceId";
    public static final String UPDATE_BY_AGENT_ID = "ResourceAvailability.updateByAgentId";
    public static final String INSERT_BY_RESOURCE_IDS = "ResourceAvailability.insertByResourceIds";

    @SuppressWarnings("unused")
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_RESOURCE_AVAIL_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private Resource resource;

    @Column(name = "RESOURCE_ID", insertable = false, updatable = false)
    private int resourceId;

    /**
     * Availability state for this time period
     */
    @Column(name = "AVAILABILITY_TYPE", nullable = true)
    @Enumerated(EnumType.ORDINAL)
    private AvailabilityType availabilityType;

    protected ResourceAvailability() {
        // for JPA use only
    }

    /**
     * Constructor for {@link ResourceAvailability}. If <code>type</code> is <code>null</code>, it will be 
     * considered unknown.
     *
     * @param resource
     * @param type
     */
    public ResourceAvailability(Resource resource, AvailabilityType type) {
        if (resource == null) {
            throw new IllegalArgumentException("resource==null");
        }

        this.resource = resource;
        this.resourceId = resource.getId();
        this.availabilityType = type;
    }

    public Resource getResource() {
        return resource;
    }

    /**
     * Indicates the availability status as either UP or DOWN; if <code>null</code> is returned, the status is unknown.
     *
     * @return availability status
     */
    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    /**
     * Sets the availability status. This can be <code>null</code> to indicate an "unknown" availability status.
     *
     * @param availabilityType
     */
    public void setAvailabilityType(AvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    @Override
    public String toString() {
        return "Availability[resourceId=" + resourceId + ", avail=" + this.availabilityType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((availabilityType == null) ? 0 : availabilityType.hashCode());
        result = (prime * result) + resourceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof ResourceAvailability)) {
            return false;
        }

        final ResourceAvailability other = (ResourceAvailability) obj;

        if (resourceId != other.resourceId) {
            return false;
        }

        if (availabilityType == null) {
            if (other.availabilityType != null) {
                return false;
            }
        } else if (!availabilityType.equals(other.availabilityType)) {
            return false;
        }

        return true;
    }
}