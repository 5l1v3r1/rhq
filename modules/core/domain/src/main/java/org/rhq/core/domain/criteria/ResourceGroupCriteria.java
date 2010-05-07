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

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceGroupCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterDownMemberCount; // required overrides
    private String filterName;
    private Boolean filterRecursive;
    private Integer filterResourceTypeId; // requires overrides
    private String filterResourceTypeName; // requires overrides
    private String filterPluginName; // requires overrides
    private GroupCategory filterGroupCategory;
    private List<Integer> filterExplicitResourceIds; // requires overrides
    private List<Integer> filterImplicitResourceIds; // requires overrides
    private ResourceCategory filterExplicitResourceCategory; // requires overrides    
    private Integer filterExplicitResourceTypeId; // requires overrides    
    private String filterExplicitResourceTypeName; // requires overrides    

    private boolean fetchExplicitResources;
    private boolean fetchImplicitResources;
    private boolean fetchOperationHistories;
    private boolean fetchConfigurationUpdates;
    private boolean fetchGroupDefinition;
    private boolean fetchResourceType;
    private boolean fetchRoles;
    private boolean fetchTags;

    private PageOrdering sortName;
    private PageOrdering sortResourceTypeName; // requires overrides

    public ResourceGroupCriteria() {
        filterOverrides.put("resourceTypeId", "resourceType.id = ?");
        filterOverrides.put("downMemberCount", "" //
            + "id IN ( SELECT implicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.implicitGroups implicitGroup " //
            + "         WHERE res.currentAvailability.availabilityType = 0 " //
            + "      GROUP BY implicitGroup.id " // 
            + "         HAVING COUNT(res) >= ? )");
        filterOverrides.put("resourceTypeName", "resourceType.name like ?");
        filterOverrides.put("pluginName", "resourceType.plugin like ?");
        filterOverrides.put("explicitResourceIds", "" //
            + "id IN ( SELECT explicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.explicitGroups explicitGroup " //
            + "         WHERE res.id IN ( ? ) )");
        filterOverrides.put("implicitResourceIds", "" //
            + "id IN ( SELECT implicitGroup.id " //
            + "          FROM Resource res " //
            + "          JOIN res.implicitGroups implicitGroup " //
            + "         WHERE res.id IN ( ? ) )");
        filterOverrides.put("explicitResourceCategory", "" //
            + "NOT EXISTS " //
            + "(  SELECT res " //
            + "   FROM Resource res " //
            + "   JOIN res.explicitGroups explicitGroup " //
            + "   WHERE resourcegroup.id = explicitGroup.id AND NOT res.resourceType.category = ? )");
        filterOverrides.put("explicitResourceTypeId", "" //
            + "NOT EXISTS " //
            + "(  SELECT res " //
            + "   FROM Resource res " //
            + "   JOIN res.explicitGroups explicitGroup " //
            + "   WHERE resourcegroup.id = explicitGroup.id AND NOT res.resourceType.id = ? )");
        filterOverrides.put("explicitResourceTypeName", "" //
            + "NOT EXISTS " //
            + "(  SELECT res " //
            + "   FROM Resource res " //
            + "   JOIN res.explicitGroups explicitGroup " //
            + "   WHERE resourcegroup.id = explicitGroup.id AND NOT res.resourceType.name = ? )");

        sortOverrides.put("resourceTypeName", "resourceType.name");
    }

    @Override
    public Class getPersistentClass() {
        return ResourceGroup.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    /**
     * Only returns groups with at least this many downed implicit resource members
     */
    public void addFilterDownMemberCount(Integer filterDownMemberCount) {
        this.filterDownMemberCount = filterDownMemberCount;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterRecursive(Boolean filterRecursive) {
        this.filterRecursive = filterRecursive;
    }

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterPluginName(String filterPluginName) {
        this.filterPluginName = filterPluginName;
    }

    public void addFilterGroupCategory(GroupCategory filterGroupCategory) {
        this.filterGroupCategory = filterGroupCategory;
    }

    public void addFilterExplicitResourceIds(Integer... filterExplicitResourceIds) {
        this.filterExplicitResourceIds = Arrays.asList(filterExplicitResourceIds);
    }

    public void addFilterImplicitResourceIds(Integer... filterImplicitResourceIds) {
        this.filterImplicitResourceIds = Arrays.asList(filterImplicitResourceIds);
    }

    /** A somewhat special case filter that ensures that all explicit group members
     * are of the specified category (e.g. PLATFORM). Useful for filtering Mixed groups.
     * 
     * @param filterExplicitResourceCategory
     */
    public void addFilterExplicitResourceCategory(ResourceCategory filterExplicitResourceCategory) {
        this.filterExplicitResourceCategory = filterExplicitResourceCategory;
    }

    /** A somewhat special case filter that ensures that all explicit group members
     * are of the specified resource type (id). Useful for filtering Mixed groups.
     * 
     * @param filterExplicitResourceTypeId
     */
    public void addFilterExplicitResourceTypeId(Integer filterExplicitResourceTypeId) {
        this.filterExplicitResourceTypeId = filterExplicitResourceTypeId;
    }

    /** A somewhat special case filter that ensures that all explicit group members
     * are of the specified resource type (id). Useful for filtering Mixed groups.
     * 
     * @param filterExplicitResourceTypeName
     */
    public void addFilterExplicitResourceTypeName(String filterExplicitResourceTypeName) {
        this.filterExplicitResourceTypeName = filterExplicitResourceTypeName;
    }

    public void fetchExplicitResources(boolean fetchExplicitResources) {
        this.fetchExplicitResources = fetchExplicitResources;
    }

    public void fetchImplicitResources(boolean fetchImplicitResources) {
        this.fetchImplicitResources = fetchImplicitResources;
    }

    public void fetchOperationHistories(boolean fetchOperationHistories) {
        this.fetchOperationHistories = fetchOperationHistories;
    }

    public void fetchConfigurationUpdates(boolean fetchConfigurationUpdates) {
        this.fetchConfigurationUpdates = fetchConfigurationUpdates;
    }

    public void fetchGroupDefinition(boolean fetchGroupDefinition) {
        this.fetchGroupDefinition = fetchGroupDefinition;
    }

    public void fetchResourceType(boolean fetchResourceType) {
        this.fetchResourceType = fetchResourceType;
    }

    public void fetchTags(boolean fetchTags) {
        this.fetchTags = fetchTags;
    }

    /**
     * Requires MANAGE_SECURITY
     * @param fetchRoles
     */
    public void fetchRoles(boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortResourceTypeName(PageOrdering sortResourceTypeName) {
        addSortField("resourceTypeName");
        this.sortResourceTypeName = sortResourceTypeName;
    }

    /** subclasses should override as necessary */
    public boolean isSecurityManagerRequired() {
        return this.fetchRoles;
    }

}
