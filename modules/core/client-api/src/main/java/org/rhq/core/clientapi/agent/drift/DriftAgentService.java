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
     * Ask the agent to send down the requested drift file content as a zip of all requested files.
     * 
     * @param driftFiles
     * @return the results of the immediate scheduling
     */
    boolean requestDriftFiles(int resourceId, Headers headers, List<DriftFile> driftFiles);

    /**
     * Execute drift detection immediately. If any drift is found, the agent will send the
     * change set back to the server in a separate request from agent to server.
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
     * Note that this method only schedules the drift detection and that it may return
     * before drift detection actually starts executing.
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

}