package org.rhq.enterprise.server.storage;

import java.net.InetAddress;

import javax.ejb.Asynchronous;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;

/**
 * @author John Sanda
 */
public interface StorageNodeOperationsHandlerLocal {

    @Asynchronous
    void handleOperationUpdateIfNecessary(OperationHistory operationHistory);

    void handleUpdateKnownNodes(ResourceOperationHistory operationHistory);

    void handlePrepareForBootstrap(ResourceOperationHistory operationHistory);

    void handleAddNodeMaintenance(ResourceOperationHistory operationHistory);

    void announceStorageNode(Subject subject, StorageNode storageNode);

    void bootstrapStorageNode(Subject subject, StorageNode storageNode);

    void performAddNodeMaintenanceIfNecessary(InetAddress storageNodeAddress);

    void performAddNodeMaintenance(Subject subject, StorageNode storageNode);

    void logError(StorageNode.OperationMode newStorageNodeOperationMode, String error, Exception e);
}
