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

package org.rhq.core.pc.configuration;

import static org.testng.Assert.*;

import org.rhq.core.pc.inventory.ComponentService;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.jmock.Expectations;

public class LoadRawTest extends JMockTest {

    ComponentService componentService;

    ConfigurationUtilityService configUtilityService;

    int resourceId = -1;

    boolean daemonThread = true;

    boolean onlyIfStarted = true;

    ResourceConfigurationFacet configFacet;

    LoadRaw loadRaw;

    @BeforeMethod
    public void setup() {
        componentService = context.mock(ComponentService.class);
        configUtilityService = context.mock(ConfigurationUtilityService.class);

        configFacet = context.mock(ResourceConfigurationFacet.class);

        loadRaw = new LoadRaw();
        loadRaw.setComponentService(componentService);
        loadRaw.setConfigurationUtilityService(configUtilityService);
    }

    @Test
    public void theResourceConfigFacetShouldGetLoaded() throws Exception {
        context.checking(new Expectations() {{
            atLeast(1).of(componentService).getComponent(resourceId,
                                                         ResourceConfigurationFacet.class,
                                                         FacetLockType.READ,
                                                         LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                         daemonThread,
                                                         onlyIfStarted);
            will(returnValue(configFacet));

            ignoring(configFacet);
            ignoring(configUtilityService);
        }});

        loadRaw.execute(resourceId, false);
    }

    @Test
    public void theConfigShouldGetLoadedByTheFacet() throws Exception {
        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            atLeast(1).of(configFacet).loadRawConfigurations();

            ignoring(configUtilityService);
        }});

        loadRaw.execute(resourceId, false);
    }

    @Test
    public void theConfigNotesShouldGetSetIfItIsNotAlreadySet() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setName("test resource");

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadRawConfigurations(); will(returnValue(configuration));

            ignoring(configUtilityService);
        }});

        Configuration loadedConfig = loadRaw.execute(resourceId, false);

        String expectedNotes = "Resource config for " + resourceType.getName() + " Resource w/ id " + resourceId;

        assertEquals(loadedConfig.getNotes(), expectedNotes, "The notes property should be set to a default when it is not already initialized.");
    }

    @Test
    public void theConfigShouldGetNormalized() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadRawConfigurations(); will(returnValue(configuration));

            atLeast(1).of(configUtilityService).normalizeConfiguration(configuration, resourceType.getResourceConfigurationDefinition());

            allowing(configUtilityService).validateConfiguration(configuration, resourceType.getResourceConfigurationDefinition());
        }});

        loadRaw.execute(resourceId, false);
    }

    @Test
    public void theConfigShouldGetValidated() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadRawConfigurations(); will(returnValue(configuration));

            allowing(configUtilityService).normalizeConfiguration(configuration, resourceType.getResourceConfigurationDefinition());

            atLeast(1).of(configUtilityService).validateConfiguration(configuration, resourceType.getResourceConfigurationDefinition());
        }});

        loadRaw.execute(resourceId, false);
    }

    @Test
    public void theStrategyShouldReturnTheConfig() throws Exception {
        final Configuration configuration = new Configuration();

        final ResourceType resourceType = new ResourceType();
        resourceType.setResourceConfigurationDefinition(new ConfigurationDefinition("", ""));

        context.checking(new Expectations() {{
            allowing(componentService).getComponent(resourceId,
                                                    ResourceConfigurationFacet.class,
                                                    FacetLockType.READ,
                                                    LoadResourceConfiguration.FACET_METHOD_TIMEOUT,
                                                    daemonThread,
                                                    onlyIfStarted);
            will(returnValue(configFacet));

            allowing(componentService).getResourceType(resourceId); will(returnValue(resourceType));

            allowing(configFacet).loadRawConfigurations(); will(returnValue(configuration));

            ignoring(configUtilityService);
        }});

        Configuration loadedConfig = loadRaw.execute(resourceId, false);

        assertSame(
            loadedConfig,
            configuration,
            "The loadRaw commnand should return the configuration it receives from the facet component."
        );
    }

}
