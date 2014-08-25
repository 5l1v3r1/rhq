/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.apache;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.StringUtil;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;

/**
 * Discovery component for Apache discovery directives.
 *
 * @author Lukas Krejci
 * @author Jeremie Lagarde
 */
public class ApacheDirectoryDiscoveryComponent implements ResourceDiscoveryComponent<ApacheVirtualHostServiceComponent> {

    private static final String IFMODULE_DIRECTIVE_NAME = "<IfModule";

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheVirtualHostServiceComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        ApacheDirective vhost = context.getParentResourceComponent().getDirective();
        return discoverResources(context, discoveredResources, vhost, "");
    }

    private Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheVirtualHostServiceComponent> context,
        Set<DiscoveredResourceDetails> discoveredResources, ApacheDirective parent, String parentKey) {
        final Map<String, Integer> ifModuleIndex = new HashMap<String, Integer>();
        for (ApacheDirective directive : parent.getChildDirectives()) {
            if (directive.getName().startsWith(ApacheDirectoryComponent.DIRECTORY_DIRECTIVE)) {
                ResourceType resourceType = context.getResourceType();
                String directoryParam;
                boolean isRegexp;
                List<String> params = directive.getValues();
                if (params.size() > 1 && StringUtil.isNotBlank(params.get(1))) {
                    directoryParam = params.get(1);
                    isRegexp = true;
                } else {
                    directoryParam = params.get(0);
                    isRegexp = false;
                }

                Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
                pluginConfiguration.put(new PropertySimple(ApacheDirectoryComponent.REGEXP_PROP, isRegexp));
                String resourceName = AugeasNodeValueUtil.unescape(directoryParam);

                int index = 1;
                for (DiscoveredResourceDetails detail : discoveredResources) {
                    if (detail.getResourceName().equals(resourceName)) {
                        index++;
                    }
                }
                StringBuilder resourceKey = new StringBuilder();
                resourceKey.append(directive.getName()).append("|").append(directoryParam).append("|").append(index)
                    .append(";").append(parentKey);
                discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey.toString(),
                    resourceName, null, null, pluginConfiguration, null));

            } else if (directive.getName().startsWith(IFMODULE_DIRECTIVE_NAME)) {
                int index = 1;
                String moduleName = directive.getValues().get(0);
                if (ifModuleIndex.containsKey(moduleName)) {
                    index = ifModuleIndex.get(moduleName) + 1;
                }
                ifModuleIndex.put(moduleName, index);
                String ifModuleKey = IFMODULE_DIRECTIVE_NAME + "|" + moduleName + "|" + index + ";" + parentKey;
                Set<DiscoveredResourceDetails> discoveredSubResources = new LinkedHashSet<DiscoveredResourceDetails>();
                discoverResources(context, discoveredSubResources, directive, ifModuleKey);
                discoveredResources.addAll(discoveredSubResources);
            }
        }
        return discoveredResources;
    }
}
