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

package org.rhq.plugins.cassandra.util;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

/**
 * @author John Sanda
 */
public class KeyspaceService {

    public static final String STORAGE_SERVICE_BEAN = "org.apache.cassandra.db:type=StorageService";

    public static final String REPAIR_OPERATION = "forceTableRepair";

    public static final String REPAIR_PRIMARY_RANGE = "forceTableRepairPrimaryRange";

    public static final String CLEANUP_OPERATION = "forceTableCleanup";

    public static final String COMPACT_OPERATION = "forceTableCompaction";

    private EmsConnection emsConnection;

    public KeyspaceService(EmsConnection emsConnection) {
        this.emsConnection = emsConnection;
    }

    public void repair(String keyspace) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(REPAIR_OPERATION, String.class, boolean.class,
            boolean.class, String[].class);

        operation.invoke(keyspace, new String[] {});
    }

    public void repairPrimaryRange(String keyspace) {
        EmsBean emsBean = loadBean(KeyspaceService.STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(REPAIR_PRIMARY_RANGE, String.class, boolean.class, boolean.class,
            String[].class);

        operation.invoke(keyspace);
    }

    public void cleanup(String keyspace) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(CLEANUP_OPERATION, String.class, String[].class);

        operation.invoke(keyspace, new String[] {});
    }

    public void compact(String keyspace) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(COMPACT_OPERATION, String.class, String[].class);

        operation.invoke(keyspace, new String[] {});
    }

    private EmsBean loadBean(String objectName) {
        EmsBean bean = emsConnection.getBean(objectName);
        if (bean == null) {
            // In some cases, this resource component may have been discovered by some means other than querying its
            // parent's EMSConnection (e.g. ApplicationDiscoveryComponent uses a filesystem to discover EARs and
            // WARs that are not yet deployed). In such cases, getBean() will return null, since EMS won't have the
            // bean in its cache. To cover such cases, make an attempt to query the underlying MBeanServer for the
            // bean before giving up.
            emsConnection.queryBeans(objectName);
            bean = emsConnection.getBean(objectName);
        }

        return bean;
    }

}
