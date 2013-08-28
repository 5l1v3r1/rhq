/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.storage;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.plugins.cassandra.CassandraNodeDiscoveryComponent;

/**
 * @author Stefan Negrea
 */
public class StorageNodeDiscoveryComponent extends CassandraNodeDiscoveryComponent {

    private static final String RESOURCE_NAME = "RHQ Storage Node";

    @SuppressWarnings({ "rawtypes" })
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> discoveredResources = this.scanForResources(context);
        Set<DiscoveredResourceDetails> storageNodes = new HashSet<DiscoveredResourceDetails>();

        for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
            Configuration configuration = discoveredResource.getPluginConfiguration();

            if (!isCassandraNode(discoveredResource)) {
                String resourceKey = StorageNodeDiscoveryComponent.RESOURCE_NAME + "("
                    + configuration.getSimpleValue(HOST_PROPERTY, "localhost") + ")";
                String resourceName = resourceKey;

                discoveredResource.setResourceKey(resourceKey);
                discoveredResource.setResourceName(resourceName);

                storageNodes.add(discoveredResource);
            }
        }

        return storageNodes;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext ruc) {
        ResourceUpgradeReport result = null;

        // We want to update the plugin config.  To get the new plugin config we need to:
        // 1) perform a discovery
        // 2) make sure the reskey matches telling us we're dealing with the same logical resource
        // 3) update the plugin config props if they differ

        // generate a discovery context from the resource context (i was amazed I could do this!)
        ResourceDiscoveryContext dc = new ResourceDiscoveryContext(ruc.getResourceType(),
            ruc.getParentResourceComponent(), ruc.getParentResourceContext(), ruc.getSystemInformation(),
            ruc.getNativeProcessesForType(), ruc.getPluginContainerName(), ruc.getPluginContainerDeployment());

        Set<DiscoveredResourceDetails> discoveredResources = discoverResources(dc);
        boolean upgrade = false;

        for (DiscoveredResourceDetails drd : discoveredResources) {
            if (drd.getResourceKey().equals(ruc.getResourceKey())) {
                Configuration newPluginConfig = drd.getPluginConfiguration();
                Configuration oldPluginConfig = ruc.getPluginConfiguration();

                String newCommandLine = newPluginConfig.getSimpleValue(COMMAND_LINE_CONFIG_PROPERTY);
                String oldCommandLine = oldPluginConfig.getSimpleValue(COMMAND_LINE_CONFIG_PROPERTY);
                if (null != newCommandLine && !newCommandLine.equals(oldCommandLine)) {
                    oldPluginConfig.put(new PropertySimple(COMMAND_LINE_CONFIG_PROPERTY, newCommandLine));
                    upgrade = true;
                }

                String newBasedir = newPluginConfig.getSimpleValue(BASEDIR_PROPERTY);
                String oldBasedir = oldPluginConfig.getSimpleValue(BASEDIR_PROPERTY);
                if (null != newBasedir && !newBasedir.equals(oldBasedir)) {
                    oldPluginConfig.put(new PropertySimple(BASEDIR_PROPERTY, newBasedir));
                    upgrade = true;
                }

                String newYaml = newPluginConfig.getSimpleValue(YAML_PROPERTY);
                String oldYaml = oldPluginConfig.getSimpleValue(YAML_PROPERTY);
                if (null != newYaml && !newYaml.equals(oldYaml)) {
                    oldPluginConfig.put(new PropertySimple(YAML_PROPERTY, newYaml));
                    upgrade = true;
                }

                if (upgrade) {
                    result = new ResourceUpgradeReport();
                    result.setNewPluginConfiguration(oldPluginConfig);
                }

                break;
            }
        }

        return result;
    }
}
