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
package org.rhq.core.domain.operation.composite;

import org.rhq.core.domain.operation.ScheduleJobId;

public class ResourceOperationScheduleComposite extends OperationScheduleComposite {

    private static final long serialVersionUID = 1L;

    private int resourceId;
    private String resourceName;
    private String resourceTypeName;

    //private no args constructor for serialization. Not to be used.
    private ResourceOperationScheduleComposite() {
        super();
        this.resourceTypeName = "(unitialized type)";
        this.resourceName = "(uninitialized)";
        this.resourceId = 0;
    }

    public ResourceOperationScheduleComposite(String jobName, String jobGroup, String operationName,
        long operationNextFireTime, int resourceId, String resourceName, String resourceTypeName) {
        super(new ScheduleJobId(jobName, jobGroup), operationName, operationNextFireTime);
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceTypeName = resourceTypeName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceOperationScheduleComposite: " + super.toString());
        str.append(", resource-id=[" + resourceId);
        str.append("], resource-name=[" + resourceName);
        str.append("], resource-type-name=[" + resourceTypeName);
        str.append("]");
        return str.toString();
    }
}