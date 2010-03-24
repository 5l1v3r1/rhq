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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;
import java.util.HashMap;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BundleGWTServiceImpl extends AbstractGWTServiceImpl implements BundleGWTService {
    private static final long serialVersionUID = 1L;

    private BundleManagerLocal bundleManager = LookupUtil.getBundleManager();

    public Bundle createBundle(String name, int bundleTypeId) throws Exception {
        Bundle results;
        try {
            results = bundleManager.createBundle(getSessionSubject(), name, null, bundleTypeId);
        } catch (Exception e) {
            throw new Exception(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "createBundle");
    }

    public BundleVersion createBundleAndBundleVersion(String bundleName, int bundleTypeId, String name, String version,
        String description, String recipe) throws Exception {

        BundleVersion results;
        try {
            results = bundleManager.createBundleAndBundleVersion(getSessionSubject(), bundleName, description,
                bundleTypeId, name, null, version, recipe);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "createBundleAndBundleVersion");
    }

    public BundleDeployDefinition createBundleDeployDefinition(int bundleVersionId, String name, String description,
        Configuration configuration, boolean enforcePolicy, int enforcementInterval, boolean pinToBundle)
        throws Exception {

        try {
            BundleDeployDefinition result = bundleManager.createBundleDeployDefinition(getSessionSubject(),
                bundleVersionId, name, description, configuration, enforcePolicy, enforcementInterval, pinToBundle);

            return SerialUtility.prepare(result, "createBundleDeployDefinition");
        } catch (Exception e) {
            throw new Exception(ThrowableUtil.getAllMessages(e));
        }
    }

    public BundleVersion createBundleVersion(int bundleId, String name, String version, String recipe) throws Exception {
        BundleVersion results;
        try {
            results = bundleManager.createBundleVersion(getSessionSubject(), bundleId, name, null, version, recipe);
        } catch (Exception e) {
            throw new Exception(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "createBundleVersion");
    }

    public void deleteBundle(int bundleId) throws Exception {
        try {
            bundleManager.deleteBundle(getSessionSubject(), bundleId);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void deleteBundleVersion(int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception {
        try {
            bundleManager.deleteBundleVersion(getSessionSubject(), bundleVersionId, deleteBundleIfEmpty);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<Bundle> findBundlesByCriteria(BundleCriteria criteria) throws Exception {
        try {
            PageList<Bundle> results = bundleManager.findBundlesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundlesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<BundleVersion> findBundleVersionsByCriteria(BundleVersionCriteria criteria) throws Exception {
        try {
            PageList<BundleVersion> results = bundleManager.findBundleVersionsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundleVersionsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<BundleWithLatestVersionComposite> findBundlesWithLastestVersionCompositesByCriteria(
        BundleCriteria criteria) throws Exception {
        try {
            PageList<BundleWithLatestVersionComposite> results;
            results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(results, "findBundlesWithLastestVersionCompositesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public ArrayList<BundleType> getAllBundleTypes() throws Exception {
        try {
            ArrayList<BundleType> bundleTypes = new ArrayList<BundleType>();
            bundleTypes.addAll(bundleManager.getAllBundleTypes(getSessionSubject()));
            return SerialUtility.prepare(bundleTypes, "getBundleTypes");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public HashMap<String, Boolean> getAllBundleVersionFilenames(int bundleVersionId) throws Exception {
        HashMap<String, Boolean> results = new HashMap<String, Boolean>();
        try {
            results.putAll(bundleManager.getAllBundleVersionFilenames(getSessionSubject(), bundleVersionId));
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return SerialUtility.prepare(results, "getAllBundleVersionFilenames");
    }

    public BundleDeployment scheduleBundleDeployment(int bundleDeployDefinitionId, int resourceId) throws Exception {
        try {
            BundleDeployment result = bundleManager.scheduleBundleDeployment(getSessionSubject(),
                bundleDeployDefinitionId, resourceId);
            return SerialUtility.prepare(result, "scheduleBundleDeployment");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}