/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.rhq.core.domain.cloud.StorageNode;

/**
 * @author John Sanda
 */
public class SchemaManager {

    /**
     * The username that RHQ will use to connect to the storage cluster.
     */
    private final String username;

    /**
     * The password that RHQ will use to connect to the storage cluster.
     */
    private final String password;

    private final List<StorageNode> nodes = new ArrayList<StorageNode>();

    /**
     *
     * @param username The username RHQ will use to connect to the storage cluster.
     * @param password The password RHQ will use to connect to the storage cluster.
     * @param nodes A list of seeds nodes that are assumed to be already running and
     *              clustered prior to apply schema changes. The format for each node
     *              should be address|jmx_port|cql_port,address|jmx_port|cql_port.
     *              Each node consists of three fields that are pipe-delimited.
     */
    public SchemaManager(String username, String password, String... nodes) {
        this(username, password, parseNodeInformation(nodes));
    }

    /**
     *
     * @param username The username RHQ will use to connect to the storage cluster.
     * @param password The password RHQ will use to connect to the storage cluster.
     * @param nodes A list of seeds nodes that are assumed to be already running and
     *              clustered prior to apply schema changes.
     */
    public SchemaManager(String username, String password, List<StorageNode> nodes) {
        this.username = username;
        this.password = password;
        this.nodes.addAll(nodes);
    }

    /**
     * Install and update the RHQ schema.
     *
     * @throws Exception
     */
    public void install() throws Exception {
        VersionManager version = new VersionManager(username, password, nodes);
        version.install();
    }

    /**
     * Drop RHQ schema and revert the database to pre-RHQ state.
     *
     * @throws Exception
     */
    public void drop() throws Exception {
        VersionManager version = new VersionManager(username, password, nodes);
        version.drop();
    }

    /**
     * Update cluster topology settings, such as replication factor.
     *
     * @param isNewSchema
     * @return
     * @throws Exception
     */
    public void updateTopology() throws Exception {
        TopologyManager topology = new TopologyManager(username, password, nodes);
        topology.updateTopology();
    }

    /**
     * Returns the list of storage nodes.
     *
     * @return list of storage nodes
     */
    public List<StorageNode> getStorageNodes() {
        return nodes;
    }

    /**
     * Parse raw string that contains the list of storage nodes.
     *
     * @param nodes list of storage nodes
     * @return
     */
    private static List<StorageNode> parseNodeInformation(String... nodes) {
        List<StorageNode> parsedNodes = new ArrayList<StorageNode>();
        for (String node : nodes) {
            StorageNode storageNode = new StorageNode();
            storageNode.parseNodeInformation(node);
            parsedNodes.add(storageNode);
        }

        return parsedNodes;
    }

    /**
     * A main runner used for direct usage of the schema manager.
     *
     * @param args arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            Logger root = Logger.getRootLogger();
            if (!root.getAllAppenders().hasMoreElements()) {
                root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
            }
            Logger migratorLogging = root.getLoggerRepository().getLogger("org.rhq");
            migratorLogging.setLevel(Level.ALL);

            if (args.length < 4) {
                System.out.println("Usage      : command username password nodes...");
                System.out.println("\n");
                System.out.println("Commands   : install | drop | topology");
                System.out.println("Node format: hostname|jmxPort|cqlPort");
                return;
            }

            String command = args[0];
            String username = args[1];
            String password = args[2];
            String[] hosts = Arrays.copyOfRange(args, 3, args.length);

            SchemaManager schemaManager = new SchemaManager(username, password, hosts);

            if ("install".equalsIgnoreCase(command)) {
                schemaManager.install();
            } else if ("drop".equalsIgnoreCase(command)) {
                schemaManager.drop();
            } else if ("topology".equalsIgnoreCase(command)) {
                schemaManager.updateTopology();
            } else {
                throw new IllegalArgumentException(command + " not available.");
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
