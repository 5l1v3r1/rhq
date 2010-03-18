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
import java.util.Set;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeployDefinitionCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * Remote interface to the manager responsible for creating and managing bundles.
 *  
 * @author John Mazzitelli
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface BundleManagerRemote {

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
    @WebMethod
    BundleFile addBundleFile( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architecture") Architecture architecture, //
        @WebParam(name = "fileStream") InputStream fileStream, //
        @WebParam(name = "pinToPackage") boolean pinToPackage) throws Exception;

    /**
     * A convenience method taking a byte array as opposed to a stream for the file bits.
     * 
     * @see {@link addBundleFile(Subject, int, String, String, Architecture, InputStream, boolean)}     
     */
    BundleFile addBundleFileViaByteArray( //
        @WebParam(name = "subject") Subject subject, @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "version") String version, //
        @WebParam(name = "architecture") Architecture architecture, //
        @WebParam(name = "fileBytes") byte[] fileBytes, //
        @WebParam(name = "pinToPackage") boolean pinToPackage) throws Exception;

    /**
     * A convenience method taking an existing PackageVersion as opposed to a stream for the file bits.
     * 
     * @see {@link addBundleFile(Subject, int, String, String, Architecture, InputStream, boolean)}     
     */
    BundleFile addBundleFileViaPackageVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "packageVersionId") int packageVersionId, //
        @WebParam(name = "pinToPackage") boolean pinToPackage) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param name not null or empty 
     * @param description optional long description of the bundle 
     * @param bundleTypeId valid bundleType
     * @return the persisted Bundle (id is assigned)
     */
    Bundle createBundle( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "description") String description, //        
        @WebParam(name = "bundleTypeId") int bundleTypeId) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param BundleVersionId the BundleVersion being deployed by this definition
     * @param name a name for this definition. not null or empty
     * @param description an optional longer description describing this deploy def 
     * @param configuration a Configuration (pojo) to be associated with this deploy def. Although
     *        it is not enforceable 
     *        must be that of the associated BundleVersion.
     * @param enforcePolicy if true enforce policy on deployments made with this deploy def
     * @param enforceInterval if enforcePolicy is true check policy at this interval (in seconds), otherwise ignored
     * @param pinToBundle if true this deploy def is disabled if a newer BundleVersion is generated for the Bundle
     * @return the persisted deploy definition
     * @throws Exception
     */
    BundleDeployDefinition createBundleDeployDefinition( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionid") int bundleVersionId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "description") String description, //
        @WebParam(name = "configuration") Configuration configuration, //
        @WebParam(name = "enforcePolicy") boolean enforcePolicy, //
        @WebParam(name = "enforcementInterval") int enforcementInterval, //
        @WebParam(name = "pinToBundle") boolean pinToBundle) throws Exception;

    /**
     * @param subject must be InventoryManager
     * @param bundleId the bundle for which this will be the next version
     * @param name not null or empty
     * @param description optional long description of the bundle version 
     * @param version optional. If not supplied set to 1.0 for first version, or incremented (as best as possible) for subsequent version
     * @return the persisted BundleVersion (id is assigned)
     */
    BundleVersion createBundleVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleId") int bundleId, //
        @WebParam(name = "name") String name, //
        @WebParam(name = "description") String description, //                
        @WebParam(name = "version") String version, //
        @WebParam(name = "recipe") String recipe) throws Exception;

    /**
     * Remove everything associated with the Bundle with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     *    
     * @param subject
     * @param bundleId
     * @throws Exception if any part of the removal fails. 
     */
    @WebMethod
    void deleteBundle( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleId") int bundleId) throws Exception;

    /**
     * Remove everything associated with the BundleVersion with the exception of files laid down by related deployments.
     * Deployed files are left as is on the deployment platforms but the bundle mechanism will no longer track
     * the deployment.
     *    
     * @param subject
     * @param bundleVersionId
     * @throws Exception if any part of the removal fails. 
     */
    @WebMethod
    void deleteBundleVersion( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionId") int bundleVersionId) throws Exception;

    @WebMethod
    PageList<Bundle> findBundlesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleCriteria criteria);

    @WebMethod
    PageList<BundleDeployDefinition> findBundleDeployDefinitionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleDeployDefinitionCriteria criteria);

    @WebMethod
    PageList<BundleDeployment> findBundleDeploymentsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "BundleDeploymentCriteria") BundleDeploymentCriteria criteria);

    @WebMethod
    PageList<BundleFile> findBundleFilesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleFileCriteria criteria);

    @WebMethod
    PageList<BundleVersion> findBundleVersionsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") BundleVersionCriteria criteria);

    @WebMethod
    List<BundleType> getAllBundleTypes( //
        @WebParam(name = "subject") Subject subject);

    /**
     * Determine the files required for a BundleVersion and return all of the filenames or optionally, just those
     * that lack BundleFiles for the BundleVersion.  The recipe may be parsed as part of this call.
     *   
     * @param subject must be InventoryManager
     * @param bundleVersionId the BundleVersion being queried
     * @param withoutBundleFileOnly if true omit any filenames that already have a corresponding BundleFile for
     *        the BundleVersion.  
     * @return The List of filenames.
     * @throws Exception
     */
    Set<String> getBundleVersionFilenames( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleVersionId") int bundleVersionId, //
        @WebParam(name = "withoutBundleFileOnly") boolean withoutBundleFileOnly) throws Exception;

    /**
     * Deploy the bundle as described in the provided deploy definition to the specified resource.
     * Deployment is asynchronous so return of this method does not indicate deployments are complete. The
     * returned BundleDeployment can be used to track the history of the deployment.
     * 
     *  TODO: Add the scheduling capability, currently it's Immediate. 
     * 
     * @param subject must be InventoryManager
     * @param bundleDeployDefinitionId the BundleDeployDefinition being used to guide the deployments
     * @param resourceId the target resource (must exist), typically platforms, for the deployments
     * @return the BundleDeployment created to track the deployment. 
     * @throws Exception
     */
    @WebMethod
    BundleDeployment scheduleBundleDeployment( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "bundleDeployDefinitionId") int bundleDeployDefinitionId, //
        @WebParam(name = "resourceId") int resourceId) throws Exception;

}
