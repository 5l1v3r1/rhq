package org.rhq.plugins.nagios.rhqNagiosPlugin;

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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.nagios.network.NetworkConnection;
import org.rhq.plugins.nagios.reply.LqlReply;
import org.rhq.plugins.nagios.request.LqlResourceTypeRequest;

/**
 *
 * @author Alexander Kiefer
 *
 */
public class NagiosMonitorChildTypeDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Don run the auto-discovery for the services below the NagiosMonitor server type.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        log.info("Discovering for type " + discoveryContext.getResourceType());

        ResourceComponent tmpComponent = discoveryContext.getParentResourceComponent();

        NagiosMonitorComponent parentComponent = (NagiosMonitorComponent) tmpComponent;
        String nagiosHost = parentComponent.getNagiosHost();
        int nagiosPort = parentComponent.getNagiosPort();

        //Method requests available nagios services an returns the names of them
        LqlReply resourceTypeReply = getResourceTypeInformation(nagiosHost, nagiosPort);

        if (resourceTypeReply==null) {
            log.warn("Null response from Nagios server at [" + nagiosHost + ":" + nagiosPort +"]");
                return discoveredResources;
        }

        // the resource type we are interested in this invocation
        ResourceType wanted = discoveryContext.getResourceType();
        //for each available service
        for (int i = 0; i < resourceTypeReply.getLqlReply().size(); i++) {

            String nagiosType = resourceTypeReply.getLqlReply().get(i);
            if (!nagiosType.equals(wanted.getName()))
                continue;

            //create new DiscoveredResourceDetails instance
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
            //new ResourceType instance per service
                wanted, "nagiosKey@" + "Nr:" + i + ":" + resourceTypeReply.getLqlReply().get(i), "Nagios@" + "Nr:" + i
                    + ":" + resourceTypeReply.getLqlReply().get(i), null, "NagiosService: "
                    + resourceTypeReply.getLqlReply().get(i), null, null);

            //add DiscoveredResourceDetails instance to Set
            discoveredResources.add(detail);
            log.info("Discovered a nagios service: " + detail);
        }

        // Now handle the special case of our dynamically discovered type
        if (wanted.getName().equals(NagiosMonitorComponent.DYNAMIC_TYPE)) {
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                wanted,
                "nagiosKey@" + "Nr: foo", // resource key
                "nagiosKey@" + "Nr: foo", // resource name
                 null, // version
                    "Automatically discovered type ", // description
                  null, null);
            log.info("Discovered a nagios service: " + detail);
            discoveredResources.add(detail);

        }

        return discoveredResources;
    }

    /**
     * Don't run the auto-discovery of this "nagios" server type,
     * as we probably won't have one on each platform. Rather have the admin
     * explicitly add it to one platform.
     */
    private LqlReply getResourceTypeInformation(String nagiosIp, int nagiosPort) {
        LqlResourceTypeRequest resourceTypeRequest = new LqlResourceTypeRequest();
        LqlReply resourceTypeReply;

        NetworkConnection connection = new NetworkConnection(nagiosIp, nagiosPort);
        resourceTypeReply = connection.sendAndReceive(resourceTypeRequest);

        return resourceTypeReply;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration configuration,
        ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {

        String nagiosHost = configuration.getSimpleValue("nagiosHost", NagiosMonitorComponent.DEFAULT_NAGIOSIP);
        String nagiosPort = configuration.getSimpleValue("nagiosPort", NagiosMonitorComponent.DEFAULT_NAGIOSPORT);

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceDiscoveryContext.getResourceType(),
            "nagios@" + nagiosHost + ":" + nagiosPort, "Nagios@" + nagiosHost + ":" + nagiosPort, null,
            "Nagios server @ " + nagiosHost + ":" + nagiosPort, configuration, null);
        log.info("Adding NagiosMonitor " + detail);

        return detail;
    }

}
