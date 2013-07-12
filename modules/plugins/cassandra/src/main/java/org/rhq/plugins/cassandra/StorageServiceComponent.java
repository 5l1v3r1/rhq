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

package org.rhq.plugins.cassandra;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.FileSystemInfo;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * @author John Sanda
 */
public class StorageServiceComponent extends ComplexConfigurationResourceComponent {

    private static final String OWNERSHIP_METRIC_NAME = "Ownership";
    private static final String PARTITION_DISK_USED_PERCENTAGE_METRIC_NAME = "Calculated.PartitionDiskUsedPercentage";
    private static final String DATA_FILE_LOCATIONS_NAME = "AllDataFileLocations";
    private static final String LOAD_NAME = "Load";

    private Log log = LogFactory.getLog(StorageServiceComponent.class);
    private InetAddress host;

    @Override
    public void start(ResourceContext<JMXComponent<?>> context) {
        super.start(context);
        CassandraNodeComponent parrent = (CassandraNodeComponent) context.getParentResourceComponent();
        try {
            host = InetAddress.getByName(parrent.getHost());
        } catch (UnknownHostException e) {
            log.error(
                "Unable to convert hostname[" + parrent.getHost() + "] into IP address for " + context.getResourceKey(),
                e);
        }
    }

    @Override
    public AvailabilityType getAvailability() {
        ResourceContext<?> context = getResourceContext();
        try {
            EmsBean emsBean = loadBean();
            if (emsBean == null) {
                log.warn("Unable to establish JMX connection to " + context.getResourceKey());
                return DOWN;
            }

            AvailabilityType availability = UP;

            EmsAttribute nativeTransportEnabledAttr = emsBean.getAttribute("NativeTransportRunning");
            Boolean nativeTransportEnabled = (Boolean) nativeTransportEnabledAttr.getValue();

            if (!nativeTransportEnabled) {
                if (log.isWarnEnabled()) {
                    log.warn("Native transport is disabled for " + context.getResourceKey());
                }
                availability = DOWN;
            }

            EmsAttribute initializedAttr = emsBean.getAttribute("Initialized");
            Boolean initialized = (Boolean) initializedAttr.getValue();

            if (!initialized) {
                if (log.isWarnEnabled()) {
                    log.warn(context.getResourceKey() + " is not initialized");
                }
                availability = DOWN;
            }

            return availability;
        } catch (Exception e) {
            log.error("Unable to determine availability for " + context.getResourceKey(), e);
            return UNKNOWN;
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("takeSnapshot")) {
            return takeSnapshot(parameters);
        } else if (name.equals("setLog4jLevel")) {
            return setLog4jLevel(parameters);
        }

        return super.invokeOperation(name, parameters);
    }

    private OperationResult takeSnapshot(Configuration parameters) {
        EmsBean emsBean = getEmsBean();
        EmsOperation operation = emsBean.getOperation("takeSnapshot", String.class, String[].class);
        String snapshotName = parameters.getSimpleValue("snapshotName");
        if (snapshotName == null || snapshotName.trim().isEmpty()) {
            snapshotName = System.currentTimeMillis() + "";
        }

        operation.invoke(snapshotName, new String[] {});

        return new OperationResult();
    }

    private OperationResult setLog4jLevel(Configuration parameters) {
        EmsBean emsBean = getEmsBean();
        EmsOperation operation = emsBean.getOperation("setLog4jLevel", String.class, String.class);

        String classQualifier = parameters.getSimpleValue("classQualifier");
        String level = parameters.getSimpleValue("level");

        operation.invoke(classQualifier, level);

        return new OperationResult();
    }

    @Override
    protected void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests, EmsBean bean) {
        super.getValues(report, requests, bean);
        for (MeasurementScheduleRequest request : requests) {
            if (OWNERSHIP_METRIC_NAME.equals(request.getName()) && host != null) {
                EmsAttribute attribute = bean.getAttribute(OWNERSHIP_METRIC_NAME);
                Object valueObject = attribute.refresh();
                if (valueObject instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<InetAddress, Float> ownership = (Map<InetAddress, Float>) valueObject;
                    Float value = ownership.get(host);
                    if (value == null) {
                        // the inet address wasn't probably resolved, scan the map
                        for (Map.Entry<InetAddress, Float> entry : ownership.entrySet()) {
                            if (entry.getKey().getHostAddress().equals(host.getHostAddress())) {
                                value = entry.getValue();
                                break;
                            }
                        }
                    }
                    if (value > 1) {
                        value = 1f;
                    }
                    report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
                }
                break;
            } else if (PARTITION_DISK_USED_PERCENTAGE_METRIC_NAME.equals(request.getName())) {


                EmsAttribute loadAttribute = bean.getAttribute(LOAD_NAME);
                Object loadValue = loadAttribute.refresh();

                EmsAttribute dataFileLocationAttribute = bean.getAttribute(DATA_FILE_LOCATIONS_NAME);
                Object dataFileLocationValue = dataFileLocationAttribute.refresh();

                if (loadValue != null && dataFileLocationValue != null && dataFileLocationValue instanceof String[]) {
                    //Please visit for details: https://issues.apache.org/jira/browse/CASSANDRA-2749
                    //The average usage of all partitions with the data will be reported.
                    //Cassandra selects the partition with most free space for SStable flush and compaction.
                    double load = Double.parseDouble(loadValue.toString());

                    report.addData(new MeasurementDataNumeric(request, getPartitionDiskUsedPercentage(load,
                        (String[]) dataFileLocationValue)));
                }
            }
        }
    }

    private double getPartitionDiskUsedPercentage(double dataSize, String[] paths) {
        List<String> visitedMountPoints = new ArrayList<String>();
        long totalDiskSpace = 0;

        for (String path : paths) {
            try {
                FileSystemInfo fileSystemInfo  = this.getResourceContext().getSystemInformation().getFileSystem(path);
                if (!visitedMountPoints.contains(fileSystemInfo.getMountPoint())) {
                    visitedMountPoints.add(fileSystemInfo.getMountPoint());
                    totalDiskSpace += fileSystemInfo.getFileSystemUsage().getTotal();
                }
            } catch (Exception e) {
                log.error("Unable to determine file system usage information for data file location " + path, e);
            }
        }

        if (totalDiskSpace != 0) {
            double rawPercentage = dataSize / ((double) totalDiskSpace);
            return Math.round(rawPercentage * 100.0) / 100.0;
        }

        return 0;
    }
}
