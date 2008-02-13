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
package org.rhq.core.clientapi.server.plugin.content;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.resource.ResourceType;

/**
 * The key to a content source package details that a {@link ContentSourceAdapter} will use when refering to package
 * versions it finds in the remote repository. It is the same as {@link PackageDetailsKey} with the addition of a
 * resource type natural key (which is name and agent plugin name), since that is needed to make package types unique
 * (along with the package type name itself, which is specified in the {@link PackageDetailsKey} superclass).
 */
public class ContentSourcePackageDetailsKey extends PackageDetailsKey {
    private static final long serialVersionUID = 1L;

    private final String resourceTypeName;
    private final String resourceTypePluginName;

    public ContentSourcePackageDetailsKey(String name, String version, String packageTypeName, String architectureName,
        String resourceTypeName, String resourceTypePluginName) {
        super(name, version, packageTypeName, architectureName);

        if (resourceTypeName == null) {
            throw new IllegalArgumentException("resourceTypeName cannot be null");
        }

        if (resourceTypePluginName == null) {
            throw new IllegalArgumentException("resourceTypePluginName cannot be null");
        }

        this.resourceTypeName = resourceTypeName;
        this.resourceTypePluginName = resourceTypePluginName;
    }

    /**
     * The name of the {@link ResourceType} that this package's type belongs to. All package types are defined and
     * supported by a particular {@link ResourceType}, this name is part of the natural key of a resource type. See
     * {@link #getResourceTypePluginName() plugin name} for the other part.
     *
     * @return resource type name
     */
    public String getResourceTypeName() {
        return resourceTypeName;
    }

    /**
     * The name of the plugin that defined the {@link ResourceType} that this package's type belongs to. All package
     * types are defined and supported by a particular {@link ResourceType}, this plugin name is part of the natural key
     * of a resource type. See {@link #getResourceTypeName() resource type name} for the other part.
     *
     * @return the name of the plugin that defines the resource type that defined the package type
     */
    public String getResourceTypePluginName() {
        return resourceTypePluginName;
    }

    @Override
    public String toString() {
        return "ContentSourcePackageDetailsKey[" + super.toString() + ", ResourceTypeName=" + resourceTypeName
            + ", PluginName=" + resourceTypePluginName + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + resourceTypeName.hashCode();
        result = (prime * result) + resourceTypePluginName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof ContentSourcePackageDetailsKey)) {
            return false;
        }

        final ContentSourcePackageDetailsKey other = (ContentSourcePackageDetailsKey) obj;

        if (!resourceTypeName.equals(other.resourceTypeName)) {
            return false;
        }

        if (!resourceTypePluginName.equals(other.resourceTypePluginName)) {
            return false;
        }

        return true;
    }
}