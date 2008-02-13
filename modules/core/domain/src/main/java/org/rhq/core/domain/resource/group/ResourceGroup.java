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
package org.rhq.core.domain.resource.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A {@link Group} that contains {@link Resource}s. It cannot contain other groups.
 *
 * @author Greg Hinkle
 */

@Entity
@NamedQueries( {
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY, query = "SELECT new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite(AVG(a.availabilityType), g, COUNT(res), COUNT(p)) "
        + "FROM ResourceGroup g JOIN g.roles r JOIN r.subjects s JOIN r.resourceGroups rg JOIN r.permissions p "
        + "LEFT JOIN rg.implicitResources res LEFT JOIN res.availability a "
        + "LEFT JOIN g.resourceType type "
        + "WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) "
        + "AND s = :subject "
        + "AND g.groupCategory = :groupCategory "
        + "AND p = 11 "
        + "AND "
        + "(UPPER(g.name) LIKE :search "
        + "OR UPPER(g.description) LIKE :search "
        + "OR :search is null) "
        + "AND ( type is null OR ( "
        + "      (type = :resourceType AND :resourceType is not null) "
        + "    OR "
        + "      (type.category = :category AND :category is not null) "
        + "    OR "
        + "      (:resourceType is null AND :category is null ) "
        + "     ) ) "
        + "GROUP BY g,g.name,g.resourceType.name,g.description "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY_COUNT, query = "SELECT count(g) "
        + "FROM ResourceGroup g JOIN g.roles r JOIN r.subjects s " + "LEFT JOIN g.resourceType type "
        + "WHERE s = :subject " + "AND g.groupCategory = :groupCategory " + "AND " + "(UPPER(g.name) LIKE :search "
        + "OR UPPER(g.description) LIKE :search " + "OR :search is null) " + "AND ( type is null OR ( "
        + "      (type = :resourceType AND :resourceType is not null) " + "    OR "
        + "      (type.category = :category AND :category is not null) " + "    OR "
        + "      (:resourceType is null AND :category is null ) " + "     ) ) "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY_ADMIN, query = "SELECT new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite(AVG(a.availabilityType), g, COUNT(res)) "
        + "FROM ResourceGroup g "
        + "LEFT JOIN g.implicitResources res LEFT JOIN res.availability a "
        + "LEFT JOIN g.resourceType type "
        + "WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) "
        + "AND g.groupCategory = :groupCategory "
        + "AND "
        + "(UPPER(g.name) LIKE :search "
        + "OR UPPER(g.description) LIKE :search "
        + "OR :search is null) "
        + "AND ( type is null OR ( "
        + "      (type = :resourceType AND :resourceType is not null) "
        + "    OR "
        + "      (type.category = :category AND :category is not null) "
        + "    OR "
        + "      (:resourceType is null AND :category is null ) "
        + "     ) ) "
        + "GROUP BY g,g.name,g.resourceType.name,g.description "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY_COUNT_ADMIN, query = "SELECT count(g) FROM ResourceGroup g "
        + "LEFT JOIN g.resourceType type "
        + "WHERE g.groupCategory = :groupCategory "
        + "AND "
        + "(UPPER(g.name) LIKE :search "
        + "OR UPPER(g.description) LIKE :search "
        + "OR :search is null) "
        + "AND ( type is null OR ( "
        + "      (type = :resourceType AND :resourceType is not null) "
        + "    OR "
        + "      (type.category = :category AND :category is not null) "
        + "    OR "
        + "      (:resourceType is null AND :category is null) " + "     ) ) "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY, query = "SELECT rg "
        + "  FROM ResourceGroup AS rg JOIN rg.roles r JOIN r.subjects s " + " WHERE s = :subject "
        + "   AND rg.groupCategory = :category "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_admin, query = "SELECT rg "
        + "  FROM ResourceGroup AS rg " + " WHERE rg.groupCategory = :category "),

    // finds all the groups that the given resource belongs to
    @NamedQuery(name = ResourceGroup.QUERY_FIND_GROUP_IDS_BY_RESOURCE_ID, query = "SELECT DISTINCT g.id "
        + "  FROM ResourceGroup g " + "       LEFT JOIN g.explicitResources er "
        + "       LEFT JOIN g.implicitResources ir " + " WHERE er.id = :id " + "    OR ir.id = :id "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_NAME, query = "SELECT rg FROM ResourceGroup AS rg WHERE LOWER(rg.name) = LOWER(:name)"),
    @NamedQuery(name = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE, query = "SELECT DISTINCT rg "
        + "FROM ResourceGroup AS rg LEFT JOIN rg.roles AS r " + "WHERE rg.id NOT IN " + "( SELECT irg.id "
        + "  FROM Role ir JOIN ir.resourceGroups AS irg " + "  WHERE ir.id = :roleId )"),
    @NamedQuery(name = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE_WITH_EXCLUDES, query = "SELECT DISTINCT rg "
        + "FROM ResourceGroup AS rg LEFT JOIN rg.roles AS r "
        + "WHERE rg.id NOT IN "
        + "( SELECT irg.id "
        + "  FROM Role ir JOIN ir.resourceGroups AS irg "
        + "  WHERE ir.id = :roleId ) "
        + "AND rg.id NOT IN ( :excludeIds )"),
    @NamedQuery(name = ResourceGroup.QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE, query = "SELECT rg FROM ResourceGroup AS rg JOIN rg.roles AS r WHERE r.id = :id"),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_IDS, query = "SELECT rg FROM ResourceGroup AS rg WHERE rg.id IN ( :ids )"),

    /*
    * For recursive group stuff, we want dups in the results
    *
    * Can *not* do <code>rg.implicitResources impR where impR.id IN ( :ids )</code> because that won't
    * cover the case when impR is in the implicit list more than once.
    *
    * We need all dups here
    */
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT rg FROM Resource AS res JOIN res.implicitGroups rg WHERE res.id = :id "),

    // TODO: Add authz checks to the following two queries (i.e. only return groups that are viewable by the specified subject).
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_RESOURCE_ID_COMPOSITE, query = "SELECT new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite(AVG(a.availabilityType), rg, COUNT(memberRes)) "
        + "FROM Resource AS res JOIN res.implicitGroups rg "
        + "LEFT JOIN rg.implicitResources memberRes LEFT JOIN memberRes.availability a "
        + "WHERE res.id = :resourceId "
        + "AND (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where memberRes.id = aa.resource.id)) "
        + "GROUP BY rg, rg.name, rg.description "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_RESOURCE_ID_COMPOSITE_COUNT, query = "SELECT COUNT(rg) "
        + "FROM Resource AS res JOIN res.implicitGroups rg " + "WHERE res.id = :resourceId "),

    /* the following two are for auto-groups summary */
    @NamedQuery(name = ResourceGroup.QUERY_FIND_AUTOGROUP_BY_ID, query = "SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(AVG(a.availabilityType), res.resourceType, COUNT(res)) "
        + "FROM Resource res JOIN res.implicitGroups irg JOIN irg.roles r JOIN r.subjects s JOIN res.availability a "
        + "WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) "
        + "AND s = :subject " + "AND res.id = :resourceId " + "GROUP BY res.resourceType "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_AUTOGROUP_BY_ID_ADMIN, query = "SELECT new org.rhq.core.domain.resource.group.composite.AutoGroupComposite(AVG(a.availabilityType), res.resourceType, COUNT(res)) "
        + "FROM Resource res JOIN res.availability a "
        + "WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) "
        + "AND res.id = :resourceId " + "GROUP BY res.resourceType "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_GROUP_COMPOSITE_BY_ID, query = "SELECT new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite(AVG(a.availabilityType), g, COUNT(res)) "
        + "FROM ResourceGroup g LEFT JOIN g.implicitResources res LEFT JOIN res.availability a "
        + "WHERE (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) "
        + "AND g.id = :groupId " + "GROUP BY g "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_RESOURCE_NAMES_BY_GROUP_ID, query = "SELECT new org.rhq.core.domain.common.composite.IntegerOptionItem(res.id, res.name) "
        + "  FROM ResourceGroup g " + "  JOIN g.implicitResources res " + " WHERE g.id = :groupId "),
    @NamedQuery(name = ResourceGroup.QUERY_FIND_BY_GROUP_DEFINITION_AND_EXPRESSION, query = "SELECT g "
        + "  FROM ResourceGroup g " + " WHERE g.groupByClause = :groupByClause "
        + "   AND g.groupDefinition.id = :groupDefinitionId ") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_RESOURCE_GROUP_ID_SEQ")
@Table(name = "RHQ_RESOURCE_GROUP")
public class ResourceGroup extends Group {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY = "ResourceGroup.findAllCompositeByCategory";
    public static final String QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY_COUNT = "ResourceGroup.findAllCompositeByCategory_Count";
    public static final String QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY_ADMIN = "ResourceGroup.findAllCompositeByCategory_Admin";
    public static final String QUERY_FIND_ALL_COMPOSITE_BY_CATEGORY_COUNT_ADMIN = "ResourceGroup.findAllCompositeByCategory_Count_Admin";

    public static final String QUERY_FIND_ALL_BY_CATEGORY = "ResourceGroup.findAllByCategory";
    public static final String QUERY_FIND_ALL_BY_CATEGORY_admin = "ResourceGroup.findAllByCategory_admin";

    public static final String QUERY_FIND_GROUP_IDS_BY_RESOURCE_ID = "ResourceGroup.findGroupIdsByResourceId";
    public static final String QUERY_FIND_BY_NAME = "ResourceGroup.findByName";
    public static final String QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE_WITH_EXCLUDES = "ResourceGroup.getAvailableResourceGroupsForRoleWithExcludes";
    public static final String QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE = "ResourceGroup.getAvailableResourceGroupsForRole";
    public static final String QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE = "ResourceGroup.getResourceGroupsAssignedToRole";
    public static final String QUERY_FIND_BY_IDS = "ResourceGroup.findByIds";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "ResourceGroup.findByResourceId";
    public static final String QUERY_FIND_BY_RESOURCE_ID_COMPOSITE = "ResourceGroup.findByResourceIdComposite";
    public static final String QUERY_FIND_BY_RESOURCE_ID_COMPOSITE_COUNT = "ResourceGroup.findByResourceIdCompositeCount";

    public static final String QUERY_FIND_AUTOGROUP_BY_ID = "ResourceGroup.findAutoGroupById";
    public static final String QUERY_FIND_AUTOGROUP_BY_ID_ADMIN = "ResourceGroup.findAutoGroupById_admin";

    public static final String QUERY_FIND_GROUP_COMPOSITE_BY_ID = "ResourceGroup.findGroupCompositeById";

    public static final String QUERY_FIND_RESOURCE_NAMES_BY_GROUP_ID = "ResourceGroup.findResourceNamesByGroupId";
    public static final String QUERY_FIND_BY_GROUP_DEFINITION_AND_EXPRESSION = "ResourceGroup.findByGroupDefinitionAndExpression";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @JoinTable(name = "RHQ_RESOURCE_GROUP_RES_EXP_MAP", joinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    @ManyToMany
    private Set<Resource> explicitResources;

    @JoinTable(name = "RHQ_RESOURCE_GROUP_RES_IMP_MAP", joinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    @ManyToMany
    private Set<Resource> implicitResources;

    @JoinTable(name = "RHQ_ROLE_RESOURCE_GROUP_MAP", joinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "ROLE_ID") })
    @ManyToMany
    private Set<Role> roles = new HashSet<Role>();

    @OneToMany(mappedBy = "group", cascade = { CascadeType.ALL })
    @OrderBy
    // by primary key which will also put the operation histories in chronological order
    private List<GroupOperationHistory> operationHistories = new ArrayList<GroupOperationHistory>();

    @JoinColumn(name = "GROUP_DEFINITION_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private GroupDefinition groupDefinition;

    @Column(name = "GROUP_BY", nullable = true)
    private String groupByClause;

    @Column(name = "RECURSIVE")
    private boolean recursive;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupCategory groupCategory;

    @JoinColumn(name = "RESOURCE_TYPE_ID", nullable = true)
    @ManyToOne
    private ResourceType resourceType; // if non-null, it implies a compatible gorup

    /* no-arg constructor required by EJB spec */
    protected ResourceGroup() {
    }

    public ResourceGroup(@NotNull
    String name) {
        super(name);
        setResourceType(null);
    }

    public ResourceGroup(@NotNull
    String name, ResourceType type) {
        super(name);
        setResourceType(type);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addExplicitResource(@NotNull
    Resource resource) {
        getExplicitResources().add(resource);
        resource.addExplicitGroup(this);
    }

    @NotNull
    public Set<Resource> getExplicitResources() {
        if (this.explicitResources == null) {
            this.explicitResources = new HashSet<Resource>();
        }

        return this.explicitResources;
    }

    public boolean removeExplicitResource(@NotNull
    Resource resource) {
        boolean removed = getExplicitResources().remove(resource);
        resource.removeExplicitGroup(this);
        return removed;
    }

    public void addImplicitResource(@NotNull
    Resource resource) {
        getImplicitResources().add(resource);
        resource.addImplicitGroup(this);
    }

    public GroupCategory getGroupCategory() {
        return groupCategory;
    }

    private void setGroupCategory(GroupCategory groupCategory) {
        this.groupCategory = groupCategory;
    }

    @NotNull
    public Set<Resource> getImplicitResources() {
        if (this.implicitResources == null) {
            this.implicitResources = new HashSet<Resource>();
        }

        return this.implicitResources;
    }

    public boolean removeImplicitResource(@NotNull
    Resource resource) {
        boolean removed = getImplicitResources().remove(resource);
        resource.removeImplicitGroup(this);
        return removed;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    @NotNull
    public List<GroupOperationHistory> getOperationHistories() {
        return operationHistories;
    }

    public void setOperationHistories(@NotNull
    List<GroupOperationHistory> operationHistories) {
        this.operationHistories = operationHistories;
    }

    public GroupDefinition getGroupDefinition() {
        return groupDefinition;
    }

    public void setGroupDefinition(GroupDefinition groupDefinition) {
        this.groupDefinition = groupDefinition;
    }

    public String getGroupByClause() {
        return groupByClause;
    }

    public void setGroupByClause(String groupByClause) {
        this.groupByClause = groupByClause;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
        if (resourceType == null) {
            setGroupCategory(GroupCategory.MIXED);
        } else {
            setGroupCategory(GroupCategory.COMPATIBLE);
        }
    }
}