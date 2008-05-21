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

package org.rhq.enterprise.server.resource.metadata.test;

import java.util.Iterator;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;

/**
 * More plugin update testing
 * @author hrupp
 *
 */
@Test
public class PluginHandling3Test extends TestBase {

    @EJB
    ResourceManagerLocal resMgr;

    TestServerCommunicationsService agentServiceContainer;

    int agentId;
    int server2id;

    @BeforeSuite
    @Override
    protected void init() {
        super.init();
    }

    @BeforeClass
    public void beforeClass() {
        System.out.println("======== PluginHandling3Test ===============");
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.measurementService = new MockAgentService();

        prepareScheduler();

    }

    /**
     * I am renaming (= removing + adding ) a server with metrics on it.
     * Make sure all stuff hanging on the server gets deleted. So we need
     * to add some relations like MeasurementSchedules 
     * @throws Exception
     */
    public void testRenameServer() throws Exception {

        int plugin1Id;

        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update6-1.xml");
            ResourceType server = getResourceType("testServer1");

            EntityManager entityManager = getEntityManager();

            Plugin existingPlugin = null;
            try {
                existingPlugin = (Plugin) entityManager.createNamedQuery("Plugin.findByName").setParameter("name",
                    PLUGIN_NAME).getSingleResult();
                plugin1Id = existingPlugin.getId();
            } catch (NoResultException nre) {
                throw nre;
            }

            Set<MeasurementDefinition> definitions1 = server.getMetricDefinitions();
            assert definitions1.size() == 1;

            /*
             * Create a Fake Resource and a MeasurementSchedule
             */
            Resource testResource = new Resource("-test-", "-test resource", server);
            entityManager.persist(testResource);

            MeasurementSchedule sched = new MeasurementSchedule(definitions1.iterator().next(), testResource);
            entityManager.persist(sched);
            entityManager.flush();
            MeasurementOutOfBounds oob = new MeasurementOutOfBounds(sched, System.currentTimeMillis(), 0.0);
            entityManager.persist(oob);
            MeasurementBaseline baseline = new MeasurementBaseline();
            baseline.setSchedule(sched);
            baseline.setUserEntered(true);
            entityManager.persist(baseline);
            EventDefinition eDef = new EventDefinition(server, "-test event definition-");
            entityManager.persist(eDef);

            Agent agent = new Agent("-dummy agent-", "localhost", 12345, "http://localhost:12345/", "-dummy token-");
            entityManager.persist(agent);
            testResource.setAgent(agent);
            agentServiceContainer.addStartedAgent(agent);
            agentId = agent.getId();

            getTransactionManager().commit();
        } catch (Exception e) {
            System.err.println("Setup of v1 failed");
            throw e;
        }

        // Set up done, now replace the plugin with a new one.

        try {
            getTransactionManager().begin();

            EntityManager entityManager = getEntityManager();
            entityManager.flush();

            System.out.println("Done with v1");

            try {
                registerPlugin("./test/metadata/update6-2.xml");
            } catch (Throwable t) {
                System.err.println(t);
            }
            ResourceType server;
            try {
                server = getResourceType("testServer1");
                assert server == null : "testServer1 found, but should not";
            } catch (NoResultException nre) {
                ; // no issue 
            }

            server = getResourceType("testServer2");
            assert server != null : "testServer2 not found";
            server2id = server.getId();
        } finally {
            getTransactionManager().commit();
        }

        // clean up
        try {
            getTransactionManager().begin();

            EntityManager entityManager = getEntityManager();

            ResourceType server = entityManager.find(ResourceType.class, server2id);
            Set<MeasurementDefinition> defs = server.getMetricDefinitions();
            if (defs != null) {
                Iterator<MeasurementDefinition> defIter = defs.iterator();
                while (defIter.hasNext()) {
                    MeasurementDefinition def = defIter.next();
                    // we can directly delete this, as there are no schdules on v2 defined.
                    entityManager.remove(def);
                    defIter.remove();
                }
            }
            ResourceType parent = server.getParentResourceTypes().iterator().next();
            entityManager.remove(server);
            entityManager.remove(parent);

            Agent agent = entityManager.getReference(Agent.class, agentId);
            entityManager.remove(agent);

            Plugin plugin1 = entityManager.getReference(Plugin.class, plugin1Id);
            entityManager.remove(plugin1);

        } finally {
            getTransactionManager().commit();
        }

    }
}
