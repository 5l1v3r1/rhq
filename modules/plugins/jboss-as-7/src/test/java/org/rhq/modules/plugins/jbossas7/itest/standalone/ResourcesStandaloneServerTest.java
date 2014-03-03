/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Stefan Negrea
 *
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class ResourcesStandaloneServerTest extends AbstractJBossAS7PluginTest  {
    private Log log = LogFactory.getLog(this.getClass());

    private Resource platform;
    private Resource server;

    @Test(priority = 10, groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void initialDiscoveryTest() throws Exception {
        platform = validatePlatform();
        server = waitForResourceByTypeAndKey(platform, platform, StandaloneServerComponentTest.RESOURCE_TYPE,
            StandaloneServerComponentTest.RESOURCE_KEY);
    }

    @Test(priority = 11)
    public void standaloneExecuteNoArgOperations() throws Exception {
        List<String> ignoredSubsystems = new ArrayList<String>();

        //ignored because mod_cluster is not setup in default server configuration
        //to be more specific, there is no server to fail-over to
        ignoredSubsystems.add("ModCluster Standalone Service");

        List<String> ignoredOperations = new ArrayList<String>();
        //ignored because there is no other server to fail-over to
        ignoredOperations.add("subsystem:force-failover");
        //ignored because this is not a true operation, it is handled
        //internally by a configuration property change
        ignoredOperations.add("enable");
        //ignored because the Osgi subsystem not configured out of box
        ignoredOperations.add("subsystem:activate");

        executeNoArgOperations(server, ignoredSubsystems, ignoredOperations);
    }

    @Test(priority = 12)
    public void loadUpdateResourceConfiguration() throws Exception {
        List<String> ignoredResources = new ArrayList<String>();

        //ignored because of differences between test plugin container and real application
        //works well with real agent
        ignoredResources.add("VHost");

        //created JIRA AS7-5011
        //server is started with the configuration but unable to write it back as is
        //due to marshaling error
        ignoredResources.add("Network Interface");

        //created JIRA AS7-5012
        //default value for  is float but the resource only accepts integers
        ignoredResources.add("Load Metric");

        //will revisit after BZ 826542 is resolved
        //        ignoredResources.add("Authentication (Classic)");

        ignoredResources.add("Memory Pool");
        ignoredResources.add("Periodic Rotating File Handler");

        //created BZ 1059882 for failures related to:
        //  attribute discovery-group-name (mutually exclusive issue?)
        ignoredResources.add("Pooled Connection Factory");
        ignoredResources.add("Connection Factory");
        //  attribute static-connectors (nullable list issue?)
        ignoredResources.add("Cluster Connection");

        ignoredResources.add("HornetQ");
        ignoredResources.add("HornetQ (Profile)");


        int errorCount = loadUpdateConfigChildResources(server, ignoredResources);
        Assert.assertEquals(errorCount, 0);
    }


}
