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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleGroupDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;

public interface BundleGWTService extends RemoteService {

    BundleVersion createBundleVersionViaURL(String url) throws Exception;

    BundleVersion createBundleVersionViaRecipe(String recipe) throws Exception;

    BundleDeployment createBundleDeployment(int bundleVersionId, String name, String description, String installDir,
        Configuration configuration, boolean enforcePolicy, int enforcementInterval, boolean pinToBundle)
        throws Exception;

    BundleVersion createBundleVersion(int bundleId, String name, String version, String recipe) throws Exception;

    void deleteBundle(int bundleId) throws Exception;

    void deleteBundleVersion(int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception;

    PageList<Bundle> findBundlesByCriteria(BundleCriteria criteria) throws Exception;

    PageList<BundleVersion> findBundleVersionsByCriteria(BundleVersionCriteria criteria) throws Exception;

    PageList<BundleWithLatestVersionComposite> findBundlesWithLastestVersionCompositesByCriteria(BundleCriteria criteria)
        throws Exception;

    HashMap<String, Boolean> getAllBundleVersionFilenames(int bundleVersionId) throws Exception;

    ArrayList<BundleType> getAllBundleTypes() throws Exception;

    /*
    BundleResourceDeployment scheduleBundleResourceDeployment(int bundleDeploymentId, int resourceId) throws Exception;
    */

    BundleGroupDeployment scheduleBundleGroupDeployment(int bundleDeploymentId, int resourceGroupId) throws Exception;
}
