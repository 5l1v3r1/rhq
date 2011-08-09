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
package org.rhq.enterprise.server.plugins.drift;

import static org.rhq.enterprise.server.util.LookupUtil.getDriftManager;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftSnapshot;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;

/**
 * A drift server-side plugin component that the server uses to process drift files.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class DriftServerPluginComponent implements DriftServerPluginFacet {

    private final Log log = LogFactory.getLog(DriftServerPluginComponent.class);

    @SuppressWarnings("unused")
    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        log.debug("The RHQ Drift plugin has been initialized!!! : " + this);
    }

    public void start() {
        log.debug("The RHQ Drift plugin has started!!! : " + this);
    }

    public void stop() {
        log.debug("The RHQ Drift plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        log.debug("The RHQ Drift plugin has been shut down!!! : " + this);
    }

    @Override
    public PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject,
        DriftChangeSetCriteria criteria) {
        PageList<? extends DriftChangeSet<?>> results = getDriftManager().findDriftChangeSetsByCriteria(subject,
            criteria);
        return results;
    }

    @Override
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        PageList<? extends Drift<?, ?>> results = getDriftManager().findDriftsByCriteria(subject, criteria);
        return results;
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {
        return getDriftManager().findDriftCompositesByCriteria(subject, criteria);
    }

    @Override
    public void saveChangeSet(int resourceId, File changeSetZip) throws Exception {
        DriftManagerLocal driftMgr = getDriftManager();
        driftMgr.storeChangeSet(resourceId, changeSetZip);
    }

    @Override
    public void saveChangeSetFiles(File changeSetFilesZip) throws Exception {
        DriftManagerLocal driftMgr = getDriftManager();
        driftMgr.storeFiles(changeSetFilesZip);
    }

    @Override
    public DriftSnapshot createSnapshot(Subject subject, DriftChangeSetCriteria criteria) {
        DriftManagerLocal driftMgr = getDriftManager();
        return driftMgr.createSnapshot(subject, criteria);
    }
}
