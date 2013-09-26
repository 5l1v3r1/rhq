/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.cassandra;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import org.rhq.cassandra.schema.SchemaManager;

/**
 * @author John Sanda
 */
public class CCMTestNGListener implements IInvokedMethodListener {

    private final Log log = LogFactory.getLog(CCMTestNGListener.class);

    private CassandraClusterManager ccm;

    @Override
    public void beforeInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {
        Method method = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
        if (method.isAnnotationPresent(DeployCluster.class)) {
            try {
                deployCluster(method.getAnnotation(DeployCluster.class));
            } catch (Exception e) {
                log.warn("Failed to deploy cluster", e);
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {
        Method method = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
        if (method.isAnnotationPresent(ShutdownCluster.class)) {
            try {
                Boolean skipShutdown = Boolean
                    .valueOf(System.getProperty("rhq.storage.cluster.skip-shutdown", "false"));
                if (!skipShutdown) {
                    shutdownCluster();
                }
            } catch (Exception e) {
                log.warn("An error occurred while shutting down the cluster", e);
            }
        }
    }

    private void deployCluster(DeployCluster annotation) throws Exception {
        boolean deploy = Boolean.valueOf(System.getProperty("rhq.storage.cluster.deploy", "true"));
        if (!deploy) {
            return;
        }

        String clusterDir = System.getProperty("rhq.storage.cluster.dir");
        if (clusterDir == null || clusterDir.isEmpty()) {
            File basedir = new File("target");
            clusterDir = new File(basedir, "cassandra").getAbsolutePath();
        }

        int numNodes = annotation.numNodes();
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir);
        deploymentOptions.setNumNodes(numNodes);
        deploymentOptions.setUsername(annotation.username());
        deploymentOptions.setPassword(annotation.password());
        deploymentOptions.setStartRpc(true);
        deploymentOptions.setHeapSize("256M");
        deploymentOptions.setHeapNewSize("64M");

        // TODO Figure where/when to initialize ccm
        // Ideally I would like to support multiple test/configuration methods using
        // @DeployCluster to facilitate testing different scenarios for around
        // consistency and failover. If we start doing that at some point, then
        // we cannot initialize ccm here.
        ccm = new CassandraClusterManager(deploymentOptions);
        ClusterInitService clusterInitService = new ClusterInitService();
        ccm.createCluster();

        String[] nodes = ccm.getNodes();
        int[] jmxPorts = ccm.getJmxPorts();

        if (System.getProperty("rhq.storage.cluster.skip-shutdown") == null) {
            for (int index = 0; index < nodes.length; index++) {
                try {
                    if (clusterInitService.isNativeTransportRunning(nodes[index], jmxPorts[index])) {
                        throw new RuntimeException("A cluster is already running on the same ports.");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Unable to check whether node is running.", e);
                }
            }
        }
        ccm.startCluster(false);

        clusterInitService.waitForClusterToStart(nodes, jmxPorts, nodes.length, 2000, 20, 10);

        SchemaManager schemaManager = new SchemaManager(annotation.username(), annotation.password(), nodes,
            ccm.getCqlPort());
        schemaManager.install();
        if (annotation.waitForSchemaAgreement()) {
            clusterInitService.waitForSchemaAgreement(nodes, jmxPorts);
        }
        schemaManager.updateTopology();
    }

    private void shutdownCluster() throws Exception {
        ccm.shutdownCluster();
    }

}
