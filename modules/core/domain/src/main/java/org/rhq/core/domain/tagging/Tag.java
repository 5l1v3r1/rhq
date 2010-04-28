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
package org.rhq.core.domain.tagging;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;


/**
 * @author Greg Hinkle
 */
@SequenceGenerator(name = "id", sequenceName = "RHQ_TAGGING_ID_SEQ")
@Table(name = "RHQ_TAGGING")
@XmlAccessorType(XmlAccessType.FIELD)
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;


    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_TAGGING_ID_SEQ")
    @Id
    private int id;


    @Column(name = "NAMESPACE", nullable = true)
    private String namespace;

    @Column(name = "SEMANTIC", nullable = true)
    private String semantic;

    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinTable(name = "RHQ_TAGGING_RESOURCE_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    @ManyToMany
    private Set<Resource> resources;


    @JoinTable(name = "RHQ_TAGGING_RES_GRP_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") })
    @ManyToMany
    private Set<ResourceGroup> resourceGroups;

    @JoinTable(name = "RHQ_TAGGING_BUNDLE_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_ID") })
    @ManyToMany
    private Set<Bundle> bundles;

    @JoinTable(name = "RHQ_TAGGING_BND_VER_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_VERSION_ID") })
    @ManyToMany
    private Set<BundleVersion> bundleVersions;


    @JoinTable(name = "RHQ_TAGGING_BND_DEP_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_DEPLOYMENT_ID") })
    @ManyToMany
    private Set<BundleDeployment> bundleDeployments;


    protected Tag() {
    }


    public Tag(String namespace, String semantic, String name) {
        this.namespace = namespace;
        this.semantic = semantic;
        this.name = name;
    }

    public Tag(String tag) {
        // Tag format (namespace:)(semantic=)name
        if (tag.contains(":")) {
            namespace = tag.split(":")[0];
            tag = tag.split(":")[1];
        }
        if (tag.contains("=")) {
            semantic = tag.split("=")[0];
            tag = tag.split("=")[1];
        }
        name = tag;
    }

    public int getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSemantic() {
        return semantic;
    }

    public void setSemantic(String semantic) {
        this.semantic = semantic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return (namespace != null ? namespace + ":" : "") +
                (semantic != null ? semantic + "=" : "") +
                name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;

        if (!name.equals(tag.name)) return false;
        if (namespace != null ? !namespace.equals(tag.namespace) : tag.namespace != null) return false;
        if (semantic != null ? !semantic.equals(tag.semantic) : tag.semantic != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (semantic != null ? semantic.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }
}
