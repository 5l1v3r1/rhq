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

import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class RoleCriteria extends Criteria {

    private Integer filterId;
    private String filterName;
    private String filterDescription;

    private boolean fetchSubjects;
    private boolean fetchResourceGroups;
    private boolean fetchPermissions;
    private boolean fetchRoleNotifications;

    private PageOrdering sortName;

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void fetchSubjects(boolean fetchSubjects) {
        this.fetchSubjects = fetchSubjects;
    }

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

}
