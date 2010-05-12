/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.tagging.Tag;

/**
 * Defines a logical destination for deployment of a bundle.  Defines the target platform group and the
 * target deploy directory on those platforms.  A Bundle can have several defined destinations although 
 * Destination is specific to a single Bundle.
 *
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries( { @NamedQuery(name = BundleDestination.QUERY_FIND_ALL, query = "SELECT bd FROM BundleDestination bd") //
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_DESTINATION_ID_SEQ")
@Table(name = "RHQ_BUNDLE_DESTINATION")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleDestination implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "BundleDestination.findAll";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "DEPLOY_DIR", nullable = true)
    private String deployDir;

    @Column(name = "CTIME")
    private Long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private Long mtime = System.currentTimeMillis();

    @JoinColumn(name = "BUNDLE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Bundle bundle;

    @JoinColumn(name = "GROUP_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private ResourceGroup group;

    @OneToMany(mappedBy = "destination", fetch = FetchType.LAZY)
    private List<BundleDeployment> deployments;

    @ManyToMany(mappedBy = "bundleDestinations", fetch = FetchType.LAZY)
    private Set<Tag> tags;

    public BundleDestination() {
        // for JPA use
    }

    public BundleDestination(Bundle bundle, String name, ResourceGroup group, String deployDir) {
        this.bundle = bundle;
        this.name = name;
        this.group = group;
        this.deployDir = deployDir;
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

    public String getDeployDir() {
        return deployDir;
    }

    public void setDeployDir(String deployDir) {
        this.deployDir = deployDir;
    }

    public long getCtime() {
        return this.ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    /**
     * The time that any part of this entity was updated in the database.
     *
     * @return entity modified time
     */
    public long getMtime() {
        return this.mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    public List<BundleDeployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<BundleDeployment> deployments) {
        this.deployments = deployments;
    }

    public void addDeployment(BundleDeployment deployment) {
        if (null == this.deployments) {
            this.deployments = new ArrayList<BundleDeployment>();
        }
        this.deployments.add(deployment);
        deployment.setDestination(this);
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        if (this.tags == null) {
            tags = new HashSet<Tag>();
        }
        tags.add(tag);
    }

    public boolean removeTag(Tag tag) {
        if (tags != null) {
            return tags.remove(tag);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "BundleDeployment[id=" + id + ", bundle=" + bundle.getName() + ", group=" + group.getName() + ", name="
            + name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((deployDir == null) ? 0 : deployDir.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BundleDestination)) {
            return false;
        }

        BundleDestination other = (BundleDestination) obj;

        if (this.bundle == null) {
            if (other.bundle != null) {
                return false;
            }
        } else if (!this.bundle.equals(other.bundle)) {
            return false;
        }

        if (this.group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!this.group.equals(other.group)) {
            return false;
        }

        if (this.deployDir == null) {
            if (other.deployDir != null) {
                return false;
            }
        } else if (!this.deployDir.equalsIgnoreCase(other.deployDir)) {
            return false;
        }

        return true;
    }
}