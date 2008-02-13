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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.rhq.core.domain.util.StringUtils;

@Entity
@NamedQueries( {
    @NamedQuery(name = GroupDefinition.QUERY_FIND_ALL, query = "SELECT gd " + "  FROM GroupDefinition AS gd "),
    @NamedQuery(name = GroupDefinition.QUERY_FIND_BY_NAME, query = "SELECT gd FROM GroupDefinition AS gd WHERE LOWER(gd.name) = LOWER(:name)"),
    @NamedQuery(name = GroupDefinition.QUERY_FIND_MEMBERS, query = "  SELECT new org.rhq.core.domain.resource.group.composite.ResourceGroupComposite( AVG(a.availabilityType), rg, COUNT(res) ) "
        + "    FROM ResourceGroup rg LEFT JOIN rg.implicitResources res LEFT JOIN res.availability a "
        + "   WHERE rg.groupDefinition.id = :groupDefinitionId "
        + "     AND (a is null OR a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa where res.id = aa.resource.id)) "
        + "GROUP BY rg, rg.name, rg.groupByClause "),
    @NamedQuery(name = GroupDefinition.QUERY_FIND_MEMBERS_count, query = "  SELECT COUNT(rg) "
        + "    FROM ResourceGroup rg " + "   WHERE rg.groupDefinition.id = :groupDefinitionId "),
    @NamedQuery(name = GroupDefinition.QUERY_FIND_MANAGED_RESOURCE_GROUP_IDS_ADMIN, query = "  SELECT rg.id "
        + "    FROM ResourceGroup rg " + "   WHERE rg.groupDefinition.id = :groupDefinitionId ") })
@SequenceGenerator(name = "id", sequenceName = "RHQ_GROUP_DEF_ID_SEQ")
@Table(name = "RHQ_GROUP_DEF")
public class GroupDefinition {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "GroupDefinition.findAll";
    public static final String QUERY_FIND_BY_NAME = "GroupDefinition.findByName";
    public static final String QUERY_FIND_MEMBERS = "GroupDefinition.findMembers";
    public static final String QUERY_FIND_MEMBERS_count = "GroupDefinition.findMembers_count";
    public static final String QUERY_FIND_MANAGED_RESOURCE_GROUP_IDS_ADMIN = "GroupDefinition.findManagedResourceGroupIds_admin";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "RECURSIVE")
    private boolean recursive;

    @Column(name = "CTIME")
    private Long createdTime;

    @Column(name = "MTIME")
    private Long modifiedTime;

    @Column(name = "CALC_TIME")
    private Long lastCalculationTime;

    @Column(name = "EXPRESSION")
    private String expression;

    @OneToMany(mappedBy = "groupDefinition", cascade = { CascadeType.PERSIST })
    private Set<ResourceGroup> managedResourceGroups;

    /* no-arg constructor required by EJB spec */
    protected GroupDefinition() {
    }

    public GroupDefinition(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GroupDefinition" + "[" + "id=" + getId() + ", " + "name=" + getName() + "]";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long ctime) {
        this.createdTime = ctime;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Long mtime) {
        this.modifiedTime = mtime;
    }

    @PrePersist
    void onPersist() {
        this.modifiedTime = this.createdTime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedTime = System.currentTimeMillis();
    }

    public Long getLastCalculationTime() {
        return lastCalculationTime;
    }

    public void setLastCalculationTime(Long lastCalculationTime) {
        this.lastCalculationTime = lastCalculationTime;
    }

    public String getExpression() {
        return expression;
    }

    public List<String> getExpressionAsList() {
        /*
         * should never be empty tokens, but if we return all tokens now (including empty ones) the business logic
         * should remove the empty tokens when the user saves this definition
         */
        return StringUtils.getStringAsList(expression, "\n", false);
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Set<ResourceGroup> getManagedResourceGroups() {
        return managedResourceGroups;
    }

    public void addResourceGroup(ResourceGroup resourceGroup) {
        if (this.managedResourceGroups == null) {
            this.managedResourceGroups = new HashSet<ResourceGroup>();
        }

        this.managedResourceGroups.add(resourceGroup);

        resourceGroup.setGroupDefinition(this);
    }

    public void removeResourceGroup(ResourceGroup resourceGroup) {
        if (this.managedResourceGroups == null) {
            return;
        }

        this.managedResourceGroups.remove(resourceGroup);

        resourceGroup.setGroupDefinition(null);
    }

    public void setManagedResourceGroups(Set<ResourceGroup> managedResourceGroups) {
        this.managedResourceGroups = managedResourceGroups;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof GroupDefinition))) {
            return false;
        }

        final GroupDefinition other = (GroupDefinition) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }
}