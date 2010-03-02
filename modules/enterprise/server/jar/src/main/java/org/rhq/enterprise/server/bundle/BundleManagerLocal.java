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

import java.io.InputStream;
import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeployDefinitionCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * Local interface to the manager responsible for creating and managing bundles.
 *  
 * @author John Mazzitelli
 */
@Local
public interface BundleManagerLocal {

    /**
     * Adds a BundleFile to the BundleVersion and implicitly creates the backing PackageVersion. If the PackageVersion
     * already exists use {@link addBundleFile(Subject, int, String, int, boolean)} 
     *   
     * @param subject must be InventoryManager
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile 
     * @param name name of the BundleFile (and the resulting Package)
     * @param version version of the backing package
     * @param architecture architecture appropriate for the backing package.  Defaults to noarch (i.e. any architecture).
     * @param fileStream the file bits
     * @param pinToPackage if true a new version of the backing package can trigger automatic creation of a new
     *        BundleVersion. if false new versions of the backing package have no effect on the BundleFile or its BundleVersion. 
     * @return the new BundleFile
     * @throws Exception
     */
    BundleFile addBundleFile(Subject subject, int bundleVersionId, String bundleFileName, String version,
        Architecture architecture, InputStream fileStream, boolean pinToPackage) throws Exception;

    /**
     * A convenience method taking a byte array as opposed to a stream for the file bits.
     * 
     * @see {@link addBundleFile(Subject, int, String, String, Architecture, InputStream, boolean)}     
     */
    BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes, boolean pinToPackage) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param bundleVersionId id of the BundleVersion incorporating this BundleFile 
     * @param name name of the BundleFile (and the resulting Package)
     * @param packageVersionId id of the PackageVersion this BundleFile is tied to for this BundleVersion. It is assumed
     *        the PackageVersion is already associated with the Bundle's Repo and the package is classified for the
     *        Bundle. 
     * @param pinToPackage if true a new version of the backing package can trigger automatic creation of a new
     *        BundleVersion. if false new versions of the backing package have no effect on the BundleFile or its BundleVersion. 
     * @return the new BundleFile
     * @throws Exception
     */
    BundleFile addBundleFileViaPackageVersion(Subject subject, int bundleVersionId, String name, int packageVersionId,
        boolean pinToPackage) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param name not null or empty 
     * @param bundleTypeId valid bundleType
     * @return the persisted Bundle (id is assigned)
     */
    Bundle createBundle(Subject subject, String name, int bundleTypeId) throws Exception;

    /**
     * Not generally called. For use by Server Side Plugins when registering a Bundle Plugin.
     *  
     * @param subject must be InventoryManager
     * @param name not null or empty
     * @param resourceTypeId id of the ResourceType that handles this BundleType   
     * @return the persisted BundleType (id is assigned)
     */
    BundleType createBundleType(Subject subject, String name, int resourceTypeId) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param bundleId the bundle for which this will be the next version
     * @param name not null or empty
     * @param bundleVersion optional. If not supplied set to 1.0 for first version, or incremented (as best as possible) for subsequent version
     * @return the persisted BundleVersion (id is assigned)
     */
    BundleVersion createBundleVersion(Subject subject, int bundleId, String name, String bundleVersion, String recipe)
        throws Exception;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The remaining methods are shared with the Remote Interface.
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    PageList<BundleDeployDefinition> findBundleDeployDefinitionsByCriteria(Subject subject,
        BundleDeployDefinitionCriteria criteria);

    PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria);

    PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria);

    PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria);

    List<BundleType> getAllBundleTypes(Subject subject);

    void deleteBundles(Subject subject, int[] bundleIds);

    void deleteBundleVersions(Subject subject, int[] bundleVersionIds);
}
