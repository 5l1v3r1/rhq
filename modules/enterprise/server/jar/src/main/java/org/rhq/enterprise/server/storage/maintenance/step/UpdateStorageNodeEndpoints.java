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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.util.PageList;

/**
 * @author Stefan Negrea
 *
 */
//@Stateless
public class UpdateStorageNodeEndpoints extends BaseStepRunner {

    protected static final int DEFAULT_OPERATION_TIMEOUT = 300;

    //    @Override
    public void execute() {

//        StorageNode storageNode = storageNodeManager.findStorageNodeByAddress(maintenanceStep.getStorageNode()
//            .getAddress());
//        Resource storageNodeResource = storageNode.getResource();
//        //scheduling the operation
//        long operationStartTime = System.currentTimeMillis();
//
//        ResourceOperationSchedule newSchedule = new ResourceOperationSchedule();
//        newSchedule.setJobTrigger(JobTrigger.createNowTrigger());
//        newSchedule.setResource(storageNodeResource);
//        newSchedule.setOperationName("updateEndpoints");
//        newSchedule.setDescription("Run by StorageNodeManagerBean");
//        newSchedule.setParameters(new Configuration());
//
//        storageNodeManager.scheduleOperationInNewTransaction(subjectManager.getOverlord(), newSchedule);
//
//        //waiting for the operation result then return it
//        int iteration = 0;
//        boolean successResultFound = false;
//        while (iteration < 10 && !successResultFound) {
//            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
//            criteria.addFilterResourceIds(storageNodeResource.getId());
//            criteria.addFilterStartTime(operationStartTime);
//            criteria.addFilterOperationName("updateEndpoints");
//            criteria.addFilterStatus(OperationRequestStatus.SUCCESS);
//            criteria.setPageControl(PageControl.getUnlimitedInstance());
//
//            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
//                subjectManager.getOverlord(), criteria);
//
//            if (results != null && results.size() > 0) {
//                successResultFound = true;
//            }
//
//            if (successResultFound) {
//                break;
//            } else {
//                try {
//                    Thread.sleep(100);
//                } catch (Exception e) {
//                }
//            }
//
//            iteration++;
//        }
//
//        if (!successResultFound) {
//            throw new RuntimeException();
//        }
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return null;
    }

    protected OperationHistory executeOperation(String storageNodeAddress, String operation, Configuration parameters) {
        StorageNode node = storageNodeManager.findStorageNodeByAddress(storageNodeAddress);
        int resourceId = node.getResource().getId();
        ResourceOperationSchedule operationSchedule = operationManager.scheduleResourceOperation(
            subjectManager.getOverlord(), resourceId, operation, 0, 0, 0, StartStorageClient.DEFAULT_OPERATION_TIMEOUT,
            parameters.deepCopyWithoutProxies(), "");
        return waitForOperationToComplete(operationSchedule);
    }

    private OperationHistory waitForOperationToComplete(ResourceOperationSchedule schedule) {
        try {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());

            Thread.sleep(5000);
            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
                subjectManager.getOverlord(), criteria);
            if (results.isEmpty()) {
                throw new RuntimeException("Failed to find resource operation history for " + schedule);
            }
            OperationHistory history = results.get(0);


            while (history.getStatus() == OperationRequestStatus.INPROGRESS) {
                Thread.sleep(5000);
                history = operationManager.getOperationHistoryByHistoryId(subjectManager.getOverlord(),
                    history.getId());
            }
            return history;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
