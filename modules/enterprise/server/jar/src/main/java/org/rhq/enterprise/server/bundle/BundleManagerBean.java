/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeployDefinitionCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * Manages the creation and usage of bundles.
 *
 * @author John Mazzitelli
 */
@Stateless
public class BundleManagerBean implements BundleManagerLocal, BundleManagerRemote {
    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    public Bundle createBundle(Subject subject, Bundle bundle) {
        entityManager.persist(bundle);
        return bundle;
    }

    public BundleType createBundleType(Subject subject, BundleType bundleType) {
        entityManager.persist(bundleType);
        return bundleType;
    }

    public BundleVersion createBundleVersion(Subject subject, BundleVersion bundleVersion) {
        entityManager.persist(bundleVersion);
        return bundleVersion;
    }

    @SuppressWarnings("unchecked")
    public List<BundleType> getAllBundleTypes(Subject subject) {
        // the list of types will be small, no need to support paging
        Query q = entityManager.createNamedQuery(BundleType.QUERY_FIND_ALL);
        List<BundleType> types = q.getResultList();
        return types;
    }

    public PageList<BundleDeployDefinition> findBundleDeployDefinitionsByCriteria(Subject subject,
        BundleDeployDefinitionCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<BundleDeployDefinition> queryRunner = new CriteriaQueryRunner<BundleDeployDefinition>(
            criteria, generator, entityManager);
        return queryRunner.execute();
    }

    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }

            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, null,
                subject.getId());
        }

        CriteriaQueryRunner<BundleDeployment> queryRunner = new CriteriaQueryRunner<BundleDeployment>(criteria,
            generator, entityManager);

        return queryRunner.execute();
    }

    public PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Bundle> queryRunner = new CriteriaQueryRunner<Bundle>(criteria, generator, entityManager);
        return queryRunner.execute();
    }

}
