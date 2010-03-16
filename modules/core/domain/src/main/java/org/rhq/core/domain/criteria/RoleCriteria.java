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
package org.rhq.core.domain.criteria;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class RoleCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterDescription;
    private String filterName;
    private Integer filterSubjectId;

    private boolean fetchPermissions;
    private boolean fetchResourceGroups;
    private boolean fetchRoleNotifications;
    private boolean fetchSubjects;

    private PageOrdering sortName;

    public RoleCriteria() {
        filterOverrides.put("subjectId", "id in (select sr.id from Role sr JOIN sr.subjects s where s.id = :id)");
    }

    @Override
    public Class<Role> getPersistentClass() {
        return Role.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterSubjectId(Integer filterSubjectId) {
        this.filterSubjectId = filterSubjectId;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchSubjects
     */
    public void fetchSubjects(boolean fetchSubjects) {
        this.fetchSubjects = fetchSubjects;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchResourceGroups
     */
    public void fetchResourceGroups(boolean fetchResourceGroups) {
        this.fetchResourceGroups = fetchResourceGroups;
    }

    public void fetchPermissions(boolean fetchPermissions) {
        this.fetchPermissions = fetchPermissions;
    }

    public void fetchRoleNotifications(boolean fetchRoleNotifications) {
        this.fetchRoleNotifications = fetchRoleNotifications;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    /** subclasses should override as necessary */
    public boolean isSecurityManagerRequired() {
        return (this.fetchSubjects || this.fetchResourceGroups);
    }

    /**
     * @Deprecated use addFilterId
     */
    @Deprecated
    public void setFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    /**
     * @Deprecated use addFilterName
     */
    @Deprecated
    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    /**
     * @Deprecated use addFilterDescription
     */
    @Deprecated
    public void setFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    @Deprecated
    public Integer getFilterId() {
        return this.filterId;
    }

    @Deprecated
    public boolean getFetchSubjects() {
        return this.fetchSubjects;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchSubjects
     * @Deprecated use fetchSubjects
     */
    @Deprecated
    public void setFetchSubjects(boolean fetchSubjects) {
        this.fetchSubjects = fetchSubjects;
    }

    /**
     * @Deprecated use fetchResourceGroups
     */
    @Deprecated
    public void setFetchResourceGroups(boolean fetchResourceGroups) {
        this.fetchResourceGroups = fetchResourceGroups;
    }

    /**
     * @Deprecated use fetchPermissions
     */
    @Deprecated
    public void setFetchPermissions(boolean fetchPermissions) {
        this.fetchPermissions = fetchPermissions;
    }

    /**
     * @Deprecated use fetchRoleNotifications
     */
    @Deprecated
    public void setFetchRoleNotifications(boolean fetchRoleNotifications) {
        this.fetchRoleNotifications = fetchRoleNotifications;
    }

    /**
     * @Deprecated use addSortName
     */
    @Deprecated
    public void setSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }
}
