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
package org.rhq.core.domain.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.jetbrains.annotations.NotNull;

/**
 * Class representing a sub category, where a sub category is meant to group like resource types together
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_RESOURCE_SUBCAT_ID_SEQ")
@Table(name = "RHQ_RESOURCE_SUBCAT")
public class ResourceSubCategory implements Comparable<ResourceSubCategory> {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CTIME")
    private Long ctime;

    @Column(name = "MTIME")
    private Long mtime;

    @OneToMany(mappedBy = "parentSubCategory", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST,
        CascadeType.REMOVE })
    @OrderBy
    private List<ResourceSubCategory> childSubCategories = new ArrayList<ResourceSubCategory>();

    @JoinColumn(name = "PARENT_SUBCATEGORY_ID")
    @ManyToOne
    private ResourceSubCategory parentSubCategory;

    /**
     * A subcategory is associated with the type of resource it was defined in This is nullable since child
     * subcategories don't want to be directly associated with a resourceType, rather they can obtain their type through
     * their parent
     */
    @JoinColumn(name = "RESOURCE_TYPE_ID", updatable = false)
    @ManyToOne
    private ResourceType resourceType;

    /* no-arg constructor required by EJB spec */
    public ResourceSubCategory() {
    }

    public ResourceSubCategory(String name) {
        assert name != null;

        // Initialize empty ordered lists...
        this.childSubCategories = new ArrayList<ResourceSubCategory>();

        this.name = name;

        this.mtime = this.ctime = System.currentTimeMillis();
    }

    // TODO enterprise may want to do this differently using a query
    @NotNull
    public Collection<ResourceType> findTaggedResourceTypes() {
        ResourceType parentResourceType = findParentResourceType();
        Collection<ResourceType> taggedResourceTypes = new ArrayList<ResourceType>();
        findTaggedResourceTypes(parentResourceType, taggedResourceTypes);
        return taggedResourceTypes;
    }

    @NotNull
    private void findTaggedResourceTypes(ResourceType parentResourceType, Collection<ResourceType> taggedResourceTypes) {
        Set<ResourceType> childResourceTypes = parentResourceType.getChildResourceTypes();
        for (ResourceType childResourceType : childResourceTypes) {
            if (this.equals(childResourceType.getSubCategory())) {
                taggedResourceTypes.add(childResourceType);
            } else {
                findTaggedResourceTypes(childResourceType, taggedResourceTypes);
            }
        }
    }

    // TODO enterprise may want to do this differently using a query
    @NotNull
    public ResourceType findParentResourceType() {
        ResourceType resourceType = getResourceType();
        ResourceSubCategory parent = getParentSubCategory();
        while (parent != null) {
            resourceType = parent.getResourceType();
            parent = parent.getParentSubCategory();
        }

        return resourceType;
    }

    // TODO enterprise may want to do this differently using a query
    public boolean isCreatable() {
        for (ResourceType taggedResourceType : findTaggedResourceTypes()) {
            // if any resourceType is createable then this subCategory is
            if (taggedResourceType.isCreatable()) {
                return true;
            }
        }

        return false;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCtime() {
        return this.ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    public long getMtime() {
        return this.mtime;
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    /**
     * Updates the contents of this definition with values from the specified new defintion. The intention is for this
     * to be used as a merge between this attached instance and a detached instance. The name and resourceType will NOT
     * be updated as part of this call; they are used as identifiers and should already be the same if this merge is
     * being performed.
     *
     * @param newSubCategory contains new data to merge into this definition; cannot be <code>null</code>
     */
    public void update(@NotNull
    ResourceSubCategory newSubCategory) {
        this.displayName = newSubCategory.getDisplayName();
        this.description = newSubCategory.getDescription();
    }

    /**
     * Removes the given ResourceSubCategory as a child of this ResourceSubCategory
     */
    public void removeChildSubCategory(ResourceSubCategory oldChildSubCategory) {
        oldChildSubCategory.parentSubCategory = null;
        this.childSubCategories.remove(oldChildSubCategory);
    }

    /**
     * add a child ResourceSubCategory to this instance
     */
    public void addChildSubCategory(ResourceSubCategory childSubCategory) {
        childSubCategory.setParentSubCategory(this);
        this.childSubCategories.add(childSubCategory);
    }

    public List<ResourceSubCategory> getChildSubCategories() {
        return childSubCategories;
    }

    public void setChildSubCategories(List<ResourceSubCategory> childSubCategories) {
        this.childSubCategories = childSubCategories;
    }

    public void setParentSubCategory(ResourceSubCategory parentSubCategory) {
        this.parentSubCategory = parentSubCategory;
    }

    public ResourceSubCategory getParentSubCategory() {
        return parentSubCategory;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public int compareTo(ResourceSubCategory that) {
        return this.name.compareTo(that.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResourceSubCategory)) {
            return false;
        }

        ResourceSubCategory that = (ResourceSubCategory) o;

        if (!name.equals(that.getName())) {
            return false;
            //if (plugin != null ? !plugin.equals(that.plugin) : that.plugin != null) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = name.hashCode();

        //result = 31 * result + (plugin != null ? plugin.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResourceSubCategory[id=" + id + ", name=" + name + "]";
    }

    //   public void writeExternal(ObjectOutput out) throws IOException
    //   {
    //      out.writeUTF(name);
    //     // out.writeUTF(plugin);
    //   }

    //   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    //   {
    //      name = in.readUTF();
    //     // plugin = in.readUTF();
    //   }
}