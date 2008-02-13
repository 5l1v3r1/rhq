/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource;

import java.io.InputStream;
import javax.ejb.Local;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Jason Dobies
 */
@Local
public interface ResourceFactoryManagerLocal {
    // Calls from the agent  --------------------------------------------

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService#completeCreateResource(org.rhq.core.clientapi.agent.inventory.CreateResourceResponse)}
     * .
     */
    void completeCreateResource(CreateResourceResponse response);

    /**
     * For documentation, see
     * {@link org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService#completeDeleteResourceRequest(org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse)}
     * .
     */
    void completeDeleteResourceRequest(DeleteResourceResponse response);

    // Use case logic  --------------------------------------------

    /**
     * Creates a new physical resource. The resource will be created as a child of the specified parent. In other words,
     * the resource component of the indicated parent will be used to create the new resource. This call should only be
     * made for resource types that are defined with a create/delete policy of {@link CreateDeletePolicy#BOTH} or
     * {@link CreateDeletePolicy#CREATE_ONLY}. If this call is made for a resource type that cannot be created based on
     * this policy, the plugin container will throw an exception. This call should only be made for resource types that
     * are defined with a creation data type of {@link ResourceCreationDataType#CONFIGURATION}. If this call is made for
     * a resource type that cannot be created via a configuration, the plugin container will throw an exception.
     *
     * @param user                  user requesting the creation
     * @param parentResourceId      parent resource under which the new resource should be created
     * @param resourceTypeId        type of resource to create
     * @param resourceName          name of the resource being created
     * @param pluginConfiguration   optional plugin configuration that may be needed in order to create the new resource
     * @param resourceConfiguration resource configuration for the new resource
     */
    void createResource(Subject user, int parentResourceId, int resourceTypeId, String resourceName,
        Configuration pluginConfiguration, Configuration resourceConfiguration);

    /**
     * Creates a new physical resource. The resource will be created as a child of the specified parent. In other words,
     * the resource component of the indicated parent will be used to create the new resource. This call should only be
     * made for resource types that are defined with a create/delete policy of {@link CreateDeletePolicy#BOTH} or
     * {@link CreateDeletePolicy#CREATE_ONLY}. If this call is made for a resource type that cannot be created based on
     * this policy, the plugin container will throw an exception. This call should only be made for resource types that
     * are defined with a creation data type of {@link ResourceCreationDataType#CONTENT}. If this call is made for a
     * resource type that cannot be created via an package, the plugin container will throw an exception.
     *
     * @param user                        user requesting the creation
     * @param parentResourceId            parent resource under which the new resource should be created
     * @param newResourceTypeId           identifies the type of resource being created
     * @param newResourceName             name of the resource being created
     * @param pluginConfiguration         optional plugin configuration that may be needed in order to create the new
     *                                    resource
     * @param packageName                 name of the package that will be created as a result of this resource create
     * @param packageVersion              machine formatted identifier of the specific version of the package
     * @param architectureId              ID of the architecture of the package
     * @param deploymentTimeConfiguration dictates how the package will be deployed
     * @param packageBitStream            content of the package to create
     */
    void createResource(Subject user, int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration pluginConfiguration, String packageName, String packageVersion, int architectureId,
        Configuration deploymentTimeConfiguration, InputStream packageBitStream);

    /**
     * Deletes a physical resource from the agent machine. After this call, the resource will no longer be accessible
     * not only to JON, but in general. It is up to the plugin to determine how to complete the delete, but a deleted
     * resource will no longer be returned from resource discoveries.
     *
     * @param user       user requesting the delete
     * @param resourceId resource being deleted
     */
    void deleteResource(Subject user, int resourceId);

    // Internal Utilities  --------------------------------------------

    /**
     * Returns the history item corresponding to the specified ID.
     *
     * @param  historyItemId identifies the history item to return
     *
     * @return history item for the id; <code>null</code> if the id does not have a corresponding item
     */
    CreateResourceHistory getCreateHistoryItem(int historyItemId);

    /**
     * Returns the number of requests to create a new child resource under the specified parent known to the system.
     * These requests may be completed or still in progress; it represents the history of creation attempts for this
     * parent resource.
     *
     * @param  parentResourceId resource to check for child resource creation requests
     *
     * @return number of requests in the resource creation history for the specified parent
     */
    int getCreateChildResourceHistoryCount(int parentResourceId);

    /**
     * Returns the number of requests to delete a child resource from the specified parent resource. These requests may
     * be complete or still in progress; it represents the history of all delete attempts of child resources to this
     * resource.
     *
     * @param  parentResourceId resource to check for child resource delete requests
     *
     * @return number of delete requests for child resources
     */
    int getDeleteChildResourceHistoryCount(int parentResourceId);

    /**
     * Returns a pagination enabled list of requests for the creation of new child resources to the specified parent.
     * These requests may be completed or still in progress; it represents the history of creation attempts for this
     * parent resource.
     *
     * @param  parentResourceId resource to check for child resource creations
     * @param  pageControl      control for pagination
     *
     * @return list of requests
     */
    PageList<CreateResourceHistory> getCreateChildResourceHistory(int parentResourceId, PageControl pageControl);

    /**
     * Returns a pagination enabled list of requests to delete a child resource on the specified parent. These requests
     * may be complete or still in progress; it represents the history of all delete attempts of child resources to this
     * resource.
     *
     * @param  parentResourceId resource to check for deleted child resources
     * @param  pageControl      control for pagination
     *
     * @return list of requests
     */
    PageList<DeleteResourceHistory> getDeleteChildResourceHistory(int parentResourceId, PageControl pageControl);

    /**
     * Creates a new resource in the inventory.
     *
     * @param  parentResourceId parent of the new resource
     * @param  resourceTypeId   type of resource being created
     * @param  resourceName     name of the new resource
     * @param  resourceKey      resource key of the new resource
     * @param  owner            owner of the new resource
     *
     * @return resource object after it's been persisted
     */
    Resource createInventoryResource(int parentResourceId, int resourceTypeId, String resourceName, String resourceKey,
        Subject owner);

    /**
     * Persists a record in the resource history to indicate a request has been made to create a configuration-backed
     * resource.
     *
     * @param  user               user performing the create
     * @param  parentResourceId   parent resource under which the resource should be created
     * @param  resourceTypeId     type of resource being created
     * @param  createResourceName name of the resource being created
     * @param  configuration      resource configuration of the new resource
     *
     * @return persisted history entity (i.e. ID will be populated)
     */
    CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, Configuration configuration);

    /**
     * Persists a record in the resource history to indicate a request has been made to create an package-backed
     * resource.
     *
     * @param  user                        user performing the create
     * @param  parentResourceId            parent resource under which the resource should be created
     * @param  resourceTypeId              type of resource being created
     * @param  createResourceName          name of the resource being created
     * @param  packageVersion              package version being installed
     * @param  deploymentTimeConfiguration configuration of the package used when installing
     *
     * @return persisted history entity (i.e. ID will be populated)
     */
    CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, PackageVersion packageVersion, Configuration deploymentTimeConfiguration);

    /**
     * Persists a record in the resource history to indicate a request has been made to delete a resource.
     *
     * @param  user       use performing the delete
     * @param  resourceId resource being deleted
     *
     * @return persisted history entity (i.e. ID will be populated)
     */
    DeleteResourceHistory persistDeleteHistory(Subject user, int resourceId);

    /**
     * Will check to see if any in progress resource creation jobs are taking too long to finish and if so marks them as
     * failed. This method will be perodically called by the server.
     */
    void checkForTimedOutRequests();
}