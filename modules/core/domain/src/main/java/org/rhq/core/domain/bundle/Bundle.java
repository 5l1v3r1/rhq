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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Defines a bundle of content that can be versioned.
 *
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( { @NamedQuery(name = Bundle.QUERY_FIND_ALL, query = "SELECT b FROM Bundle b"), //
    @NamedQuery(name = Bundle.QUERY_FIND_BY_NAME, query = "SELECT b FROM Bundle b WHERE b.name = :name") //
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_ID_SEQ")
@Table(name = "RHQ_BUNDLE")
@XmlAccessorType(XmlAccessType.FIELD)
public class Bundle implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "Bundle.findAll";
    public static final String QUERY_FIND_BY_NAME = "Bundle.findByName";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinColumn(name = "BUNDLE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private BundleType bundleType;

    public Bundle() {
        // for JPA use
    }

    public Bundle(String name, BundleType type) {
        setName(name);
        setBundleType(type);
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

    public BundleType getBundleType() {
        return bundleType;
    }

    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
    }

    @Override
    public String toString() {
        return "Bundle[id=" + id + ",name=" + name + ",bundleType=" + bundleType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((bundleType == null) ? 0 : bundleType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Bundle)) {
            return false;
        }

        final Bundle other = (Bundle) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (bundleType == null) {
            if (other.bundleType != null) {
                return false;
            }
        } else if (!bundleType.equals(other.bundleType)) {
            return false;
        }

        return true;
    }
}