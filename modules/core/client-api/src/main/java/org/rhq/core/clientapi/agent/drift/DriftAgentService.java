/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.core.clientapi.agent.drift;

import java.util.List;

import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;

/**
 * The interface to agent's drift subsystem which allows the server to request
 * the agent to perform drift-related tasks.
 * 
 * @author Jay Shaughnessy
 * @suthor John Sanda
 */
public interface DriftAgentService {

    /**
     * Ask the agent to send the requested drift file content as a zip of all requested files.
     * 
     * @param driftFiles
     * @param headers Used to identify the change set to which the requested files belong
     * @return the results of the immediate scheduling
     */
    boolean requestDriftFiles(int resourceId, Headers headers, List<? extends DriftFile> driftFiles);

    /**
     * Schedule drift detection to occur immediately. If any changes are foind, the agent
     * will report thenm back to the server in a separate request from agent to server.
     * This method returns quickly and can be considered a non-blocking operation since
     * the drift detection happens asynchronously.
     * <p/>
     * Because the drift detection happens asynchronously, it cannot be guaranteed that the
     * drift detection will actually happen immediately. Drift detection schedules are
     * maintained by a priority queue and are ordered by their next scan time. The schedule
     * corresponding to <code>resourceId</code> and <code>driftConfiguration</code> has its
     * next scan time reset in order to move the schedule to the front of the queue.
     * <p/>
     * Note that when the drift detection occurs is largely dependent on a couple of
     * factors. First there could be other schedules already at the front of the queue that
     * would first be processed if their next scan times have also been reset. Secondly,
     * drift detection occurs at regularly scheduled intervals in its own thread. If this
     * method is called immediately after drift detection has just started, the earliest
     * that the detection would occur is the time it takes for the current detection to
     * finish plus the time period between executions of the drift detector task.
     *
     * @param resourceId The id of the resource for which the request is being made
     * @param driftConfiguration Specifies how the detection should be carried out. This
     * includes any filtering rules that should be applied.
     */
    void detectDrift(int resourceId, DriftConfiguration driftConfiguration);

    /**
     * Requests that the agent start performing drift detection for a resource with the
     * specified drift configuration. The interval at which the drift detection occurs is
     * specified by the drift configuration.
     * <p/>
     * Note that drift detection occurs asynchronously which means that this method should
     * return very quickly which could be before the drift detection for this configuration
     * has actually started.
     *
     * @param resourceId The id of the resource for which the request is being made
     * @param driftConfiguration Specifies how and when the detection should be carried out
     * @see DriftConfiguration
     */
    void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration);

    /**
     * Requests that the agent stop performing the specified drift detection for the resource. (In
     * effect, a remove operation).
     *
     * @param resourceId The id of the resource for which the request is being made
     * @param driftConfiguration The doomed drift config
     * @see DriftConfiguration
     */
    void unscheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration);

    /**
     * Requests that the agent update its processing of the specified driftConfiguration.  The filters
     * are unchanged, but something else may have changed (enablement/interval, etc).
     *
     * @param resourceId The id of the resource for which the request is being made
     * @param driftConfiguration The updated driftConfiguration.
     * @see DriftConfiguration
     */
    void updateDriftDetection(int resourceId, DriftConfiguration driftConfiguration);

    /**
     * This method is invoked to inform the agent that the server has successfully
     * persisted the last change set sent to the server for the specified drift
     * configuration. The agent will proceed to purge the older, previous snapshot
     * as well as the the change set zip file.
     * <br/><br/>
     * <strong>Note:</strong> The agent will not resume drift detection for this drift
     * configuration until the server has acknowledged the last change set sent.
     *
     * @param resourceId The id of the resource to which the acknowledged change set
     * belongs.
     * @param driftConfigName The name of the drift configuration to which the change set
     * belongs.
     */
    void ackChangeSet(int resourceId, String driftConfigName);

    /**
     * This method is invoked to inform the agent that the server has successfully
     * persisted change set content that was previously sent to the server. The agent
     * will proceed to purge the referenced content zip file.
     *
     * @param resourceId The id of the resource to which the referenced drift configuration
     * belongs.
     * @param driftConfigName The name of the drift configuration to which the content
     * belongs.
     * @param token A token that is used to identify the content zip file
     */
    void ackChangeSetContent(int resourceId, String driftConfigName, String token);
}