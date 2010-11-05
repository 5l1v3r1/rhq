package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.resource.ResourceType;

import javax.ejb.Local;

@Local
public interface EventMetdataManagerLocal {

    void updateMetadata(ResourceType existingType, ResourceType newType);

}
