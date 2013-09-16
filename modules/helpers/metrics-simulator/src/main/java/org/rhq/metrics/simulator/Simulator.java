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

package org.rhq.metrics.simulator;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Minutes;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.metrics.simulator.plan.ClusterConfig;
import org.rhq.metrics.simulator.plan.ScheduleGroup;
import org.rhq.metrics.simulator.plan.SimulationPlan;
import org.rhq.metrics.simulator.stats.Stats;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
public class Simulator implements ShutdownManager {

    private final Log log = LogFactory.getLog(Simulator.class);

    private boolean shutdown = false;

    private CassandraClusterManager ccm;

    public void run(SimulationPlan plan) {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(plan.getThreadPoolSize(),
            new SimulatorThreadFactory());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown(executorService);
            }
        });

        initCluster(plan);
        createSchema();

        Session session;
        if (plan.getClientCompression() == null) {
            session = createSession();
        } else {
            ProtocolOptions.Compression compression = Enum.valueOf(ProtocolOptions.Compression.class,
                plan.getClientCompression().toUpperCase());
            session = createSession(compression);
        }

        StorageSession storageSession = new StorageSession(session);

        MetricsDAO metricsDAO = new MetricsDAO(storageSession, plan.getMetricsServerConfiguration());
        MetricsServer metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setConfiguration(plan.getMetricsServerConfiguration());

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(plan.getMetricsServerConfiguration());
        metricsServer.setDateTimeService(dateTimeService);

        Set<Schedule> schedules = initSchedules(plan.getScheduleSets().get(0));
        PriorityQueue<Schedule> queue = new PriorityQueue<Schedule>(schedules);
        ReentrantLock queueLock = new ReentrantLock();

        MeasurementAggregator measurementAggregator = new MeasurementAggregator();
        measurementAggregator.setMetricsServer(metricsServer);
        measurementAggregator.setShutdownManager(this);

        Stats stats = new Stats();
        StatsCollector statsCollector = new StatsCollector(stats);

        log.info("Starting executor service");
        executorService.scheduleAtFixedRate(statsCollector, 0, 1, TimeUnit.MINUTES);


        for (int i = 0; i < plan.getNumMeasurementCollectors(); ++i) {
            MeasurementCollector measurementCollector = new MeasurementCollector();
            measurementCollector.setMetricsServer(metricsServer);
            measurementCollector.setQueue(queue);
            measurementCollector.setQueueLock(queueLock);
            measurementCollector.setStats(stats);
            measurementCollector.setShutdownManager(this);

            executorService.scheduleAtFixedRate(measurementCollector, 0, plan.getCollectionInterval(),
                TimeUnit.MILLISECONDS);
        }

        executorService.scheduleAtFixedRate(measurementAggregator, 0, plan.getAggregationInterval(),
            TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(Minutes.minutes(plan.getSimulationTime()).toStandardDuration().getMillis());
        } catch (InterruptedException e) {
        }
        statsCollector.reportSummaryStats();
        log.info("Simulation has completed. Initiating shutdown...");
        shutdown(0);
    }

    @Override
    public synchronized void shutdown(int status) {
        if (shutdown) {
            return;
        }
        shutdown = true;
        log.info("Preparing to shutdown simulator...");
        System.exit(status);
    }

    private void shutdown(ScheduledExecutorService executorService) {
        log.info("Shutting down executor service");
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!executorService.isTerminated()) {
            log.info("Forcing executor service shutdown.");
            executorService.shutdownNow();
        }
        shutdownCluster();
        log.info("Shut down complete");
    }

    private void initCluster(SimulationPlan plan) {
        try {
            deployCluster(plan.getClusterConfig());
            waitForClusterToInitialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. Cluster initialization failed.", e);
        }
    }

    private void deployCluster(ClusterConfig clusterConfig) throws IOException {
        File clusterDir = new File(clusterConfig.getClusterDir(), "cassandra");
        log.info("Deploying cluster to " + clusterDir);
        clusterDir.mkdirs();

        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
        deploymentOptions.setNumNodes(clusterConfig.getNumNodes());
        deploymentOptions.setHeapSize(clusterConfig.getHeapSize());
        deploymentOptions.setHeapNewSize(clusterConfig.getHeapNewSize());
        if (clusterConfig.getStackSize() != null) {
            deploymentOptions.setStackSize(clusterConfig.getStackSize());
        }
        deploymentOptions.setLoggingLevel("INFO");
        deploymentOptions.load();

        ccm = new CassandraClusterManager(deploymentOptions);
        ccm.createCluster();
        ccm.startCluster(false);
    }

    private void shutdownCluster() {
        log.info("Shutting down cluster");
        ccm.shutdownCluster();
    }

    private void waitForClusterToInitialize() {
        log.info("Waiting for cluster to initialize");
        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(ccm.getNodes(), ccm.getJmxPorts(), ccm.getNodes().length, 2000, 20, 10);
    }

    private void createSchema() {
        try {
            log.info("Creating schema");
            SchemaManager schemaManager = new SchemaManager("rhqadmin", "rhqadmin", ccm.getNodes(), ccm.getCqlPort());
            schemaManager.install();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. An error occurred during schema creation.", e);
        }
    }

    private Session createSession() throws NoHostAvailableException {
        try {
            Cluster cluster = new ClusterBuilder().addContactPoints(ccm.getNodes()).withPort(ccm.getCqlPort())
                .withCredentials("rhqadmin", "rhqadmin")
                .build();

            log.debug("Created cluster object with " + cluster.getConfiguration().getProtocolOptions().getCompression()
                + " compression.");

            return initSession(cluster);
        } catch (Exception e) {
            log.error("Failed to start simulator. Unable to create " + Session.class, e);
            throw new RuntimeException("Failed to start simulator. Unable to create " + Session.class, e);
        }
    }

    private Session createSession(ProtocolOptions.Compression compression)
        throws NoHostAvailableException {
        try {
            log.debug("Creating session using " + compression.name() + " compression");

            Cluster cluster = new ClusterBuilder().addContactPoints(ccm.getNodes()).withPort(ccm.getCqlPort())
                .withCredentials("cassandra", "cassandra")
                .withCompression(compression)
                .build();

            log.debug("Created cluster object with " + cluster.getConfiguration().getProtocolOptions().getCompression()
                + " compression.");

            return initSession(cluster);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start simulator. Unable to create " + Session.class, e);
        }
    }

    @SuppressWarnings("deprecation")
    private Session initSession(Cluster cluster) {
        NodeFailureListener listener = new NodeFailureListener();
        for (Host host : cluster.getMetadata().getAllHosts()) {
            host.getMonitor().register(listener);
        }

        return cluster.connect("rhq");
    }

    private Set<Schedule> initSchedules(ScheduleGroup scheduleSet) {
        long nextCollection = System.currentTimeMillis();
        Set<Schedule> schedules = new HashSet<Schedule>();
        for (int i = 0; i < scheduleSet.getCount(); ++i) {
            Schedule schedule = new Schedule(i);
            schedule.setInterval(scheduleSet.getInterval());
            schedule.setNextCollection(nextCollection);
            schedules.add(schedule);
        }
        return schedules;
    }

    private static class NodeFailureListener implements Host.StateListener {

        private Log log = LogFactory.getLog(NodeFailureListener.class);

        @Override
        public void onAdd(Host host) {
        }

        @Override
        public void onUp(Host host) {
        }

        @Override
        public void onDown(Host host) {
            log.warn("Node " + host + " has gone down.");
            log.warn("Preparing to shutdown simulator...");
            System.exit(1);
        }

        @Override
        public void onRemove(Host host) {
        }
    }

}
