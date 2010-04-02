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
 package org.rhq.core.clientapi.server.discovery;

import org.rhq.core.domain.state.discovery.AutoDiscoveryScanType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AutoDiscoveryRequest implements Serializable {
    private Properties properties;

    private List<AutoDiscoveryScanType> scanTypes;

    private List<String> serverPlugins;

    private List<String> paths;

    private List<String> excludes;

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public synchronized List<AutoDiscoveryScanType> getScanTypes() {
        if (scanTypes == null) {
            scanTypes = new ArrayList<AutoDiscoveryScanType>();
        }

        return scanTypes;
    }

    public void setScanTypes(List<AutoDiscoveryScanType> scanTypes) {
        this.scanTypes = scanTypes;
    }

    public List<String> getServerPlugins() {
        return serverPlugins;
    }

    public void setServerPlugins(List<String> serverPlugins) {
        this.serverPlugins = serverPlugins;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}