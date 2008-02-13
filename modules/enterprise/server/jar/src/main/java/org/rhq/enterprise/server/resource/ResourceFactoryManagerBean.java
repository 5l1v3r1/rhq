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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * Bean to handle interaction with the resource factory subsystem of the plugin container. !! Warning, the factory
 * interface is there to remove managed things from disk. Use caution when using this and don't confuse it with removing
 * something from the system inventory. !!
 *
 * @author Jason Dobies
 */
@Stateless
public class ResourceFactoryManagerBean implements ResourceFactoryManagerLocal {
    // Constants  --------------------------------------------

    /**
     * Amount of time a request may be outstanding against an agent before it is marked as timed out.
     */
    private static final int REQUEST_TIMEOUT = 1000 * 60 * 60;

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(ResourceFactoryManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private SubjectManagerLocal subjectManagerBean;

    @EJB
    private ResourceFactoryManagerLocal resourceFactoryManager;

    @EJB
    private ContentManagerLocal contentManagerLocal;

    @EJB
    private ContentUIManagerLocal contentUIManagerLocal;

    // ResourceFactoryManagerLocal Implementation  --------------------------------------------

    public void createResource(Subject user, int parentResourceId, int resourceTypeId, String resourceName,
        Configuration pluginConfiguration, Configuration resourceConfiguration) {
        log.debug("Received call to create configuration backed resource under parent: " + parentResourceId
            + " of type: " + resourceTypeId);

        ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
        Resource resource = entityManager.find(Resource.class, parentResourceId);
        Agent agent = resource.getAgent();

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, resource.getId())) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to create a child resource for resource [" + resource + "]");
        }

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        CreateResourceHistory persistedHistory = resourceFactoryManager.persistCreateHistory(user, parentResourceId,
            resourceTypeId, resourceName, resourceConfiguration);

        // Package into transfer object
        CreateResourceRequest request = new CreateResourceRequest(persistedHistory.getId(), parentResourceId,
            resourceName, resourceType.getName(), resourceType.getPlugin(), pluginConfiguration, resourceConfiguration);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.createResource(request);
        } catch (Exception e) {
            log.error("Error while sending create resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            CreateResourceResponse response = new CreateResourceResponse(persistedHistory.getId(), null,
                CreateResourceStatus.FAILURE, errorMessage, resourceConfiguration);
            resourceFactoryManager.completeCreateResource(response);

            throw new RuntimeException("Error while sending create resource request to agent service", e);
        }
    }

    public void createResource(Subject user, int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration pluginConfiguration, String packageName, String packageVersionNumber, int architectureId,
        Configuration deploymentTimeConfiguration, InputStream packageBitStream) {
        log.info("Received call to create package backed resource under parent [" + parentResourceId + "]");

        Resource resource = entityManager.find(Resource.class, parentResourceId);
        ResourceType newResourceType = entityManager.find(ResourceType.class, newResourceTypeId);
        PackageType newPackageType = contentUIManagerLocal.getResourceCreationPackageType(newResourceTypeId);
        Agent agent = resource.getAgent();

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, resource.getId())) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to create a child resource for resource [" + resource + "]");
        }

        /* Once we add support for selecting an existing package to deploy, that lookup will probably go here. We'll
         * probably split it into a different call for selecting an existing package v. deploying a new one. For now
         * since we don't have the full content source and package infrastructure in place, plus the need to get JON
         * Beta 1 functionality back, I'm going to proceed with the idea that the package and package version do not
         * exist and create them here. jdobies, Nov 28, 2007
         */

        // Create package and package version
        PackageVersion packageVersion = contentManagerLocal.createPackageVersion(packageName, newPackageType.getId(),
            packageVersionNumber, architectureId);

        // Write the content into the newly created package version. This may eventually move, but for now we'll just
        // use the byte array in the package version to store the bits.
        byte[] packageBits;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            packageBits = new byte[1024];

            while (packageBitStream.read(packageBits) != -1) {
                baos.write(packageBits);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading in the package file", e);
        }

        PackageBits bits = new PackageBits();
        bits.setBits(packageBits);

        packageVersion.setPackageBits(bits);

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        CreateResourceHistory persistedHistory = resourceFactoryManager.persistCreateHistory(user, parentResourceId,
            newResourceTypeId, newResourceName, packageVersion, deploymentTimeConfiguration);

        // Package into transfer object
        CreateResourceRequest request = new CreateResourceRequest(persistedHistory.getId(), parentResourceId,
            newResourceName, newResourceType.getName(), newResourceType.getPlugin(), pluginConfiguration, packageName,
            newPackageType.getName(), deploymentTimeConfiguration);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.createResource(request);
        } catch (Exception e) {
            log.error("Error while sending create resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            CreateResourceResponse response = new CreateResourceResponse(persistedHistory.getId(), null,
                CreateResourceStatus.FAILURE, errorMessage, null);
            resourceFactoryManager.completeCreateResource(response);

            throw new RuntimeException("Error while sending create resource request to agent service", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Resource createInventoryResource(int parentResourceId, int resourceTypeId, String resourceName,
        String resourceKey, Subject owner) {
        // Load persisted entities
        Resource parentResource = entityManager.find(Resource.class, parentResourceId);
        ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);

        // Create the resource
        Resource newResource = new Resource(resourceKey, resourceName, resourceType);
        newResource.setParentResource(parentResource);
        newResource.setAgent(parentResource.getAgent());

        // Since this is a user initiated create, we assume it's committed
        newResource.setInventoryStatus(InventoryStatus.COMMITTED);

        // Persist the resource
        entityManager.persist(newResource);

        return newResource;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeCreateResource(CreateResourceResponse response) {
        log.debug("Received call to complete create resource: " + response);

        // Load the persisted history entry
        CreateResourceHistory history = entityManager.find(CreateResourceHistory.class, response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (history == null) {
            log.error("Attemping to complete a request that was not found in the database: " + response.getRequestId());
            return;
        }

        // Update the history entry
        history.setNewResourceKey(response.getResourceKey());
        history.setErrorMessage(response.getErrorMessage());
        history.setStatus(response.getStatus());

        // The subject who created the request will be used as the owner of the newly created resource.
        String ownerName = history.getSubjectName();
        Subject owner = subjectManagerBean.findSubjectByName(ownerName);

        // The configuration may now have error messages in it, so merge with the persisted one
        if (response.getResourceConfiguration() != null) {
            entityManager.merge(response.getResourceConfiguration());
        }

        // If the plugin reports it as successful, create the resource and mark it as committed
        if (response.getStatus() == CreateResourceStatus.SUCCESS) {
            resourceFactoryManager.createInventoryResource(history.getParentResource().getId(), history
                .getResourceType().getId(), history.getCreatedResourceName(), response.getResourceKey(), owner);
        }
    }

    public void deleteResource(Subject user, int resourceId) {
        log.debug("Received call to delete resource: " + resourceId);

        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.DELETE_RESOURCE, resource.getId())) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to delete resource ["
                + resource + "]");
        }

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        DeleteResourceHistory persistedHistory = resourceFactoryManager.persistDeleteHistory(user, resourceId);

        // Package into transfer object
        DeleteResourceRequest request = new DeleteResourceRequest(persistedHistory.getId(), resourceId);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.deleteResource(request);
        } catch (Exception e) {
            log.error("Error while sending delete resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            DeleteResourceResponse response = new DeleteResourceResponse(persistedHistory.getId(),
                DeleteResourceStatus.FAILURE, errorMessage);
            resourceFactoryManager.completeDeleteResourceRequest(response);

            throw new RuntimeException("Error while sending delete resource request to agent service", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeDeleteResourceRequest(DeleteResourceResponse response) {
        log.debug("Received call to complete delete resource: " + response);

        // Load the persisted history entry
        DeleteResourceHistory history = entityManager.find(DeleteResourceHistory.class, response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (history == null) {
            log.error("Attemping to complete a request that was not found in the database: " + response.getRequestId());
            return;
        }

        // Update the history entry
        history.setErrorMessage(response.getErrorMessage());
        history.setStatus(response.getStatus());

        // Mark resource as deleted if the response was successful
        if (response.getStatus() == DeleteResourceStatus.SUCCESS) {
            Resource resource = history.getResource();
            resource.setInventoryStatus(InventoryStatus.DELETED);
            resource.setItime(System.currentTimeMillis());
        }
    }

    public void checkForTimedOutRequests() {
        try {
            Query query;

            // Create Requests
            query = entityManager.createNamedQuery(CreateResourceHistory.QUERY_FIND_WITH_STATUS);
            query.setParameter("status", CreateResourceStatus.IN_PROGRESS);
            List<CreateResourceHistory> createHistories = query.getResultList();

            if (createHistories == null) {
                return;
            }

            for (CreateResourceHistory request : createHistories) {
                long duration = request.getDuration();

                // If the duration exceeds the timeout threshold, mark it as timed out
                if (duration > REQUEST_TIMEOUT) {
                    log.debug("Timing out request after duration: " + duration + " Request: " + request);

                    request.setErrorMessage("Request with duration " + duration + " exceeded the timeout threshold of "
                        + REQUEST_TIMEOUT);
                    request.setStatus(CreateResourceStatus.TIMED_OUT);
                }
            }

            // Delete Requests
            query = entityManager.createNamedQuery(CreateResourceHistory.QUERY_FIND_WITH_STATUS);
            query.setParameter("status", CreateResourceStatus.IN_PROGRESS);
            List<DeleteResourceHistory> deleteHistories = query.getResultList();

            if (deleteHistories == null) {
                return;
            }

            for (DeleteResourceHistory request : deleteHistories) {
                long duration = request.getDuration();

                // If the duration exceeds the timeout threshold, mark it as timed out
                if (duration > REQUEST_TIMEOUT) {
                    log.debug("Timing out request after duration: " + duration + " Request: " + request);

                    request.setErrorMessage("Request with duration " + duration + " exceeded the timeout threshold of "
                        + REQUEST_TIMEOUT);
                    request.setStatus(DeleteResourceStatus.TIMED_OUT);
                }
            }
        } catch (Throwable e) {
            log.error("Error while processing timed out requests", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, Configuration configuration) {
        // Load relationships
        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        ResourceType resourceType = entityManager.getReference(ResourceType.class, resourceTypeId);

        // CreateResourceHistory.configuration is one-to-one, so make sure to clone the config, zeroing out all id's.
        Configuration configurationClone = configuration.deepCopy(false);

        // Persist and establish relationships
        CreateResourceHistory history = new CreateResourceHistory(parentResource, resourceType, user.getName(),
            configurationClone);
        history.setCreatedResourceName(createResourceName);
        history.setStatus(CreateResourceStatus.IN_PROGRESS);

        entityManager.persist(history);
        parentResource.addCreateChildResourceHistory(history);

        // Caller will need this
        parentResource.getAgent();

        return history;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, PackageVersion packageVersion, Configuration deploymentTimeConfiguration) {
        // Load relationships
        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        ResourceType resourceType = entityManager.getReference(ResourceType.class, resourceTypeId);

        // Create installed package to attach to the history entry
        // This should probably be moved to the ContentManagerBean, but we'll do that when we add in generic user
        // package creation
        InstalledPackage installedPackage = new InstalledPackage();
        installedPackage.setDeploymentConfigurationValues(deploymentTimeConfiguration);
        installedPackage.setInstallationDate(new Date());
        installedPackage.setPackageVersion(packageVersion);
        installedPackage.setResource(parentResource);
        installedPackage.setUser(user);

        // Persist and establish relationships
        CreateResourceHistory history = new CreateResourceHistory(parentResource, resourceType, user.getName(),
            installedPackage);
        history.setCreatedResourceName(createResourceName);
        history.setStatus(CreateResourceStatus.IN_PROGRESS);

        entityManager.persist(history);
        parentResource.addCreateChildResourceHistory(history);

        // Caller will need this
        parentResource.getAgent();

        return history;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DeleteResourceHistory persistDeleteHistory(Subject user, int resourceId) {
        // Load relationships
        Resource resource = entityManager.find(Resource.class, resourceId);

        // Persist and establish relationships
        DeleteResourceHistory history = new DeleteResourceHistory(resource, user.getName());
        history.setStatus(DeleteResourceStatus.IN_PROGRESS);

        entityManager.persist(history);
        resource.addDeleteResourceHistory(history);

        // Caller will need this
        resource.getAgent();

        return history;
    }

    public CreateResourceHistory getCreateHistoryItem(int historyItemId) {
        Query query = entityManager.createNamedQuery(CreateResourceHistory.QUERY_FIND_BY_ID);
        query.setParameter("id", historyItemId);

        CreateResourceHistory history = (CreateResourceHistory) query.getSingleResult();

        return history;
    }

    public int getCreateChildResourceHistoryCount(int parentResourceId) {
        Query query = PersistenceUtility.createCountQuery(entityManager,
            CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID);
        query.setParameter("id", parentResourceId);

        long totalCount = (Long) query.getSingleResult();

        return (int) totalCount;
    }

    @SuppressWarnings("unchecked")
    public PageList<CreateResourceHistory> getCreateChildResourceHistory(int parentResourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("crh.id", PageOrdering.DESC);

        int totalCount = getCreateChildResourceHistoryCount(parentResourceId);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, pageControl);
        query.setParameter("id", parentResourceId);

        List<CreateResourceHistory> history = query.getResultList();

        PageList<CreateResourceHistory> pageList = new PageList<CreateResourceHistory>(history, totalCount, pageControl);
        return pageList;
    }

    public int getDeleteChildResourceHistoryCount(int parentResourceId) {
        Query query = PersistenceUtility.createCountQuery(entityManager,
            DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID);
        query.setParameter("id", parentResourceId);

        long totalCount = (Long) query.getSingleResult();

        return (int) totalCount;
    }

    @SuppressWarnings("unchecked")
    public PageList<DeleteResourceHistory> getDeleteChildResourceHistory(int parentResourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("drh.id", PageOrdering.DESC);

        int totalCount = getDeleteChildResourceHistoryCount(parentResourceId);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, pageControl);
        query.setParameter("id", parentResourceId);

        List<DeleteResourceHistory> history = query.getResultList();

        PageList<DeleteResourceHistory> pageList = new PageList<DeleteResourceHistory>(history, totalCount, pageControl);
        return pageList;
    }
}