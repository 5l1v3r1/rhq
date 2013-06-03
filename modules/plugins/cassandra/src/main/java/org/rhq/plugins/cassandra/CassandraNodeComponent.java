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
package org.rhq.plugins.cassandra;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.core.system.OperatingSystemType.WINDOWS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.SeedProviderDef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.SigarException;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.ProcessInfo.ProcessInfoSnapshot;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jmx.JMXServerComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeComponent extends JMXServerComponent<ResourceComponent<?>> implements OperationFacet {

    private Log log = LogFactory.getLog(CassandraNodeComponent.class);

    private Session cassandraSession;
    private String host;

    @SuppressWarnings("rawtypes")
    @Override
    public void start(ResourceContext context) throws Exception {
        super.start(context);

        host = context.getPluginConfiguration().getSimpleValue("host", "localhost");
        String clusterName = context.getPluginConfiguration().getSimpleValue("clusterName", "unknown");
        String username = context.getPluginConfiguration().getSimpleValue("username", "cassandra");
        String password = context.getPluginConfiguration().getSimpleValue("password", "cassandra");
        String authenticatorClassName = context.getPluginConfiguration().getSimpleValue("authenticator",
            "org.apache.cassandra.auth.AllowAllAuthenticator");

        Integer nativePort = 9042;
        try {
            nativePort = Integer.parseInt(context.getPluginConfiguration()
                .getSimpleValue("nativeTransportPort", "9042"));
        } catch (Exception e) {
            log.debug("Native transport port parsing failed...", e);
        }


        try {
            Builder clusterBuilder = Cluster
                .builder()
                .addContactPoints(new String[] { host })
                .withoutMetrics()
                .withPort(nativePort);

            if (authenticatorClassName.endsWith("PasswordAuthenticator")) {
                clusterBuilder = clusterBuilder.withCredentials(username, password);
            }

            this.cassandraSession = clusterBuilder.build().connect(clusterName);
        } catch (Exception e) {
            log.error("Connect to Cassandra " + host + ":" + nativePort, e);
            throw e;
        }
    };

    @Override
    public void stop() {
        log.info("Shutting down");
        cassandraSession.getCluster().shutdown();
    }

    @Override
    public AvailabilityType getAvailability() {
        ResourceContext<?> context = getResourceContext();
        ProcessInfo processInfo = context.getNativeProcess();

        if (processInfo == null) {
            return UNKNOWN;
        } else {
            // It is safe to read prior snapshot as getNativeProcess always return a fresh instance
            ProcessInfoSnapshot processInfoSnaphot = processInfo.priorSnaphot();
            if (processInfoSnaphot.isRunning()) {
                return UP;
            } else {
                return DOWN;
            }
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {

        if (name.equals("shutdown")) {
            return shutdownNode();
        } else if (name.equals("start")) {
            return startNode();
        } else if (name.equals("restart")) {
            return restartNode();
        } else if (name.equals("updateSeedsList")) {
            return updateSeedsList(parameters);
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    protected OperationResult shutdownNode() {
        ResourceContext<?> context = getResourceContext();

        if (log.isInfoEnabled()) {
            log.info("Starting shutdown operation on " + CassandraNodeComponent.class.getName() +
                " with resource key " + context.getResourceKey());
        }
        EmsConnection emsConnection = getEmsConnection();
        EmsBean storageService = emsConnection.getBean("org.apache.cassandra.db:type=StorageService");
        Class[] emptyParams = new Class[0];

        if (log.isDebugEnabled()) {
            log.debug("Disabling thrift...");
        }
        EmsOperation operation = storageService.getOperation("stopRPCServer", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Disabling gossip...");
        }
        operation = storageService.getOperation("stopGossiping", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Initiating drain...");
        }
        operation = storageService.getOperation("drain", emptyParams);
        operation.invoke((Object[]) emptyParams);

        ProcessInfo process = context.getNativeProcess();
        long pid = process.getPid();
        try {
            process.kill("KILL");
            return new OperationResult("Successfully shut down Cassandra daemon with pid " + pid);
        } catch (SigarException e) {
            log.warn("Failed to shut down Cassandra node with pid " + pid, e);
            OperationResult failure = new OperationResult("Failed to shut down Cassandra node with pid " + pid);
            failure.setErrorMessage(ThrowableUtil.getAllMessages(e));
            return failure;
        }
    }

    protected OperationResult startNode() {
        ResourceContext<?> context = getResourceContext();
        Configuration pluginConfig = context.getPluginConfiguration();
        String baseDir = pluginConfig.getSimpleValue("baseDir");
        File binDir = new File(baseDir, "bin");
        File startScript = new File(binDir, getStartScript());

        ProcessExecution scriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        SystemInfo systemInfo = context.getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(scriptExe);

        if  (results.getError() == null) {
            return new OperationResult("Successfully started Cassandra daemon");
        } else {
            OperationResult failure = new OperationResult("Failed to start Cassandra daemon");
            failure.setErrorMessage(ThrowableUtil.getAllMessages(results.getError()));
            return failure;
        }
    }

    protected OperationResult restartNode() {
        OperationResult result = shutdownNode();

        if (result.getErrorMessage() == null) {
            result = startNode();
        }

        return result;
    }

    protected OperationResult updateSeedsList(Configuration params) {
        List<String> addresses = new ArrayList<String>();
        PropertyList list = params.getList("seedsList");
        for (Property property : list.getList()) {
            PropertySimple simple = (PropertySimple) property;
            addresses.add(simple.getStringValue());
        }

        OperationResult result = new OperationResult();
        try {
            updateSeedsList(addresses);
        }  catch (Exception e) {
            log.error("An error occurred while updating the seeds list property", e);
            Throwable rootCause = ThrowableUtil.getRootCause(e);
            result.setErrorMessage(ThrowableUtil.getStackAsString(rootCause));
        }
        return result;
    }

    protected void updateSeedsList(List<String> addresses) throws IOException {
        ResourceContext<?> context = getResourceContext();
        Configuration pluginConfig = context.getPluginConfiguration();

        String yamlProp = pluginConfig.getSimpleValue("yamlConfiguration");
        if (yamlProp == null || yamlProp.isEmpty()) {
            throw new IllegalStateException("Plugin configuration property [yamlConfiguration] is undefined. This " +
                "property must specify be set and specify the location of cassandra.yaml in order to complete " +
                "this operation");
        }

        File yamlFile = new File(yamlProp);
        if (!yamlFile.exists()) {
            throw new IllegalStateException("Plug configuration property [yamlConfiguration] has as its value a " +
                "non-existent file.");
        }

        org.yaml.snakeyaml.constructor.Constructor constructor = new org.yaml.snakeyaml.constructor.Constructor(Config.class);
        TypeDescription seedDesc = new TypeDescription(SeedProviderDef.class);
        seedDesc.putMapPropertyType("parameters", String.class, String.class);
        constructor.addTypeDescription(seedDesc);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(constructor, new Representer(), options);
        Config conf = (Config)yaml.load(new FileInputStream(yamlFile));

        SeedProviderDef seedProviderDef = conf.seed_provider;
        seedProviderDef.parameters.put("seeds", StringUtil.listToString(addresses));
        Map<String, String> params = seedProviderDef.parameters;

        // remove the original file
        // TODO create a backup first in case something goes wrong so we can rollback
        yamlFile.delete();
        FileWriter writer = new FileWriter(yamlFile);
        yaml.dump(conf, writer);
        writer.close();
    }

    private String getStartScript() {
        ResourceContext<?> context = getResourceContext();
        SystemInfo systemInfo = context.getSystemInformation();

        if (systemInfo.getOperatingSystemType() == WINDOWS) {
            return "cassandra.bat";
        } else {
            return "cassandra";
        }
    }

    public Session getCassandraSession() {
        return this.cassandraSession;
    }

    public String getHost() {
        return host;
    }
}
