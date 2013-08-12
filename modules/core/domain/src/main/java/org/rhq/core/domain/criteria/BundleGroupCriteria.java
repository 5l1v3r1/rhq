/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.domain.criteria;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class BundleGroupCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private String filterName;
    private String filterDescription;
    private List<Integer> filterBundleIds; // requires overrides
    private List<Integer> filterRoleIds; // requires overrides

    private boolean fetchBundles;
    private boolean fetchRoles;

    private PageOrdering sortName;
    private PageOrdering sortDescription;

    public BundleGroupCriteria() {
        filterOverrides.put("bundleIds", "" //
            + "id IN ( SELECT bg.id " //
            + "          FROM Bundle b " //
            + "          JOIN b.bundleGroups bg"
            + "         WHERE b.id IN ( ? ) )");
        filterOverrides.put("roleIds", "" //
            + "id IN ( SELECT bg.id " //
            + "          FROM Role r " //
            + "          JOIN r.bundleGroups bg"
            + "         WHERE r.id IN ( ? ) )");
    }

    @Override
    public Class<BundleGroup> getPersistentClass() {
        return BundleGroup.class;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterBundleIds(Integer... filterBundleIds) {
        this.filterBundleIds = CriteriaUtils.getListIgnoringNulls(filterBundleIds);
    }

    public void addFilterRoleIds(Integer... filterRoleIds) {
        this.filterRoleIds = CriteriaUtils.getListIgnoringNulls(filterRoleIds);
    }

    public void fetchBundles(boolean fetchBundles) {
        this.fetchBundles = fetchBundles;
    }

    public void fetchRoles(boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortDescription(PageOrdering sortDescription) {
        addSortField("description");
        this.sortDescription = sortDescription;
    }
}
