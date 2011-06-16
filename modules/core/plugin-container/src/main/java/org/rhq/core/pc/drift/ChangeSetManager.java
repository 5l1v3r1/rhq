package org.rhq.core.pc.drift;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.core.domain.drift.DriftConfiguration;

public interface ChangeSetManager {

    /**
     * Locates the latest change set for the give resource and drift configuration and
     * returns a ChangeSetReader for that change set. Note that a resource can have
     * multiple drift configurations; so, both the resource id and the drift configuration
     * are required to uniquely identify a particular change set.
     *
     * @param resourceId The id of the resource to which the change set belongs
     * @param driftConfiguration The drift configuration for which the change set was generated
     * @return A ChangeSetReader that is open on the change set identified by resourceId
     * and driftConfiguration. Returns null if no change set has previously been generated.
     * @see ChangeSetReader
     */
    ChangeSetReader getChangeSetReader(int resourceId, DriftConfiguration driftConfiguration);

    ChangeSetWriter getChangeSetWriter(int resourceId, DriftConfiguration driftConfiguration);

}
