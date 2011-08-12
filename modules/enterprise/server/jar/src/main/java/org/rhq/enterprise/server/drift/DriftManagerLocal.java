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
package org.rhq.enterprise.server.drift;

import java.io.InputStream;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftConfigurationCriteria;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;

@Local
public interface DriftManagerLocal extends DriftServerPluginFacet, DriftManagerRemote {

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the change-set file. Upon successful
     * upload of the change-set, it is processed. This may in turn generated requests for drift files to
     * be persisted.
     *  
     * @param resourceId The resource for which the change-set is being reported.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The change-set zip file stream
     * @throws Exception
     */
    void addChangeSet(int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * This method initiates an out-of-band (JMS-Based) server-side pull of the drift file zip. Upon successful
     * upload of the zip, the files are stored.
     *  
     * @param resourceId The resource from which the drift file is being supplied.
     * @param zipSize The size of the zip waiting to be streamed.
     * @param zipStream The drift files zip file stream
     * @throws Exception
     */
    void addFiles(int resourceId, long zipSize, InputStream zipStream) throws Exception;

    /**
     * Remove the provided driftConfig (identified by name) on the specified entityContext.
     * Agents, if available, will be notified of the change. 
     * @param subject
     * @param entityContext
     * @param driftConfigName
     */
    void deleteDriftConfiguration(Subject subject, EntityContext entityContext, String driftConfigName);

    /**
     * This is for internal use only - do not call it unless you know what you are doing.
     */
    void deleteResourceDriftConfiguration(Subject subject, int resourceId, int driftConfigId);

    /**
     * One time on-demand request to detect drift on the specified entities, using the supplied config.
     * 
     * @param entityContext
     * @param driftConfig
     * @throws RuntimeException
     */
    void detectDrift(Subject subject, EntityContext context, DriftConfiguration driftConfig);

    PageList<DriftConfiguration> findDriftConfigurationsByCriteria(Subject subject, DriftConfigurationCriteria criteria);

    /**
     * Get the specified drift configuration. Note, the full Configuration is fetched. 
     * 
     * @param driftConfigId
     * @return The drift configuration
     * @throws RuntimeException, IllegalArgumentException if entity or driftConfig not found.
     */
    DriftConfiguration getDriftConfiguration(Subject subject, int driftConfigId);

    /**
     * Update the provided driftConfig (identified by name) on the specified EntityContext.  If it exists it will be replaced. If not it will
     * be added.  Agents, if available, will be notified of the change. 
     * @param subject
     * @param entityContext
     * @param driftConfig
     */
    void updateDriftConfiguration(Subject subject, EntityContext entityContext, DriftConfiguration driftConfig);
}
