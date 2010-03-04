/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.bundle;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.criteria.BundleDeploymentHistoryCriteria;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;

/**
 * Manages the creation and usage of bundles.
 *
 * @author Adam Young
 */
@Stateless
public class BundleDeploymentHistoryManagerBean implements BundleDeploymentHistoryManagerLocal,
    BundleDeploymentHistoryManagerRemote {
    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    /**
     * 
     * @param bundleDeployment
     * @return
     */
    public List<BundleDeploymentHistory> findBundleDeploymentHistoryByBundleDeployment(BundleDeployment bundleDeployment) {
        List<BundleDeploymentHistory> histories = new ArrayList<BundleDeploymentHistory>();
        return histories;
    }

    /**
    * gets history for a specific bundle deployment
    * @param bundleId
    * @param platformResourceId
    * @return
    */
    public List<BundleDeploymentHistory> findBundleDeploymentHistoryByBundleAndPlatform(int bundleId,
        int platformResourceId) {
        List<BundleDeploymentHistory> histories = new ArrayList<BundleDeploymentHistory>();
        return histories;
    }

    /**
     * 
     * @param platformResourceId
     * @return
     */
    public List<BundleDeploymentHistory> findBundleDeploymentHistoryByPlatform(int platformResourceId) {
        List<BundleDeploymentHistory> histories = new ArrayList<BundleDeploymentHistory>();
        return histories;

    }

    public List<BundleDeploymentHistory> findBundleDeploymentHistoryByCriteria(Subject subject,
        BundleDeploymentHistoryCriteria criteria) {
        List<BundleDeploymentHistory> histories = new ArrayList<BundleDeploymentHistory>();
        return histories;
    }

    public void addBundleDeploymentHistoryByBundleDeployment(BundleDeploymentHistory history)
        throws IllegalArgumentException {
        // TODO Auto-generated method stub

    }
}
