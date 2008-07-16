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
package org.rhq.core.pc.inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.agent.AgentRegistrar;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.operation.OperationManager;
import org.rhq.core.pc.operation.OperationServicesAdapter;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Manages the process of both auto-detection of servers and runtime detection of services across all plugins. Manages
 * their scheduling and result sending as well as the general inventory model.
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class InventoryManager extends AgentService implements ContainerService, DiscoveryAgentService {
    private static final String INVENTORY_THREAD_POOL_NAME = "InventoryManager.discovery";
    private static final String AVAIL_THREAD_POOL_NAME = "InventoryManager.availability";
    private static final int AVAIL_THREAD_POOL_CORE_POOL_SIZE = 1;

    private static final int COMPONENT_START_TIMEOUT = 20 * 1000; // 20 seconds
    private static final int COMPONENT_STOP_TIMEOUT = 5 * 1000; // 5 seconds

    private final Log log = LogFactory.getLog(InventoryManager.class);

    private PluginContainerConfiguration configuration;

    private ScheduledThreadPoolExecutor inventoryThreadPoolExecutor;
    private ScheduledThreadPoolExecutor availabilityThreadPoolExecutor;

    // The executors are Callable
    private AutoDiscoveryExecutor serverScanExecutor;
    private RuntimeDiscoveryExecutor serviceScanExecutor;
    private AvailabilityExecutor availabilityExecutor;

    private Agent agent;

    /**
     * Root platform resource, required to be root of entire inventory tree in this agent
     */
    private Resource platform;

    /**
     * if the {@link #getPlatform() platform} has inventory status of NEW, this indicates it was committed before but
     * was deleted recently
     */
    private boolean newPlatformWasDeletedRecently = false; // value only is valid/relevant if platform.getInventoryStatus == NEW

    private ReentrantReadWriteLock inventoryLock = new ReentrantReadWriteLock(true);

    /**
     * Used only for the outside the agent model to # resources
     */
    private AtomicInteger temporaryKeyIndex = new AtomicInteger(-1);

    /**
     * UUID to ResourceContainer map
     */
    private Map<String, ResourceContainer> resourceContainers = Collections
        .synchronizedMap(new HashMap<String, ResourceContainer>(1000));

    /**
     * Collection of event listeners to inform of changes to the inventory.
     */
    private Set<InventoryEventListener> inventoryEventListeners = new HashSet<InventoryEventListener>();

    private PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();

    public InventoryManager() {
        super(DiscoveryAgentService.class);
    }

    /**
     * @see ContainerService#initialize()
     */
    public void initialize() {
        inventoryLock.writeLock().lock();

        try {
            log.info("Initializing Inventory Manager...");

            this.agent = new Agent(this.configuration.getContainerName(), null, 0, null, null);

            if (configuration.isInsideAgent()) {
                loadFromDisk();
            }

            executePlatformScan();

            // Never run more than one discovery at a time.
            inventoryThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new LoggingThreadFactory(
                INVENTORY_THREAD_POOL_NAME, true));

            serverScanExecutor = new AutoDiscoveryExecutor(null, this, configuration);
            // After ten seconds, periodically run the autodiscovery scan

            if (configuration.isInsideAgent()) {
                inventoryThreadPoolExecutor.scheduleWithFixedDelay(serverScanExecutor, configuration
                    .getServerDiscoveryInitialDelay(), configuration.getServerDiscoveryPeriod(), TimeUnit.SECONDS);
            }

            serviceScanExecutor = new RuntimeDiscoveryExecutor(this, configuration);
            if (configuration.isInsideAgent()) {
                inventoryThreadPoolExecutor.scheduleWithFixedDelay(serviceScanExecutor, configuration
                    .getServiceDiscoveryInitialDelay(), configuration.getServiceDiscoveryPeriod(), TimeUnit.SECONDS);
            }

            // Never run more than one availability check at a time.
            availabilityThreadPoolExecutor = new ScheduledThreadPoolExecutor(AVAIL_THREAD_POOL_CORE_POOL_SIZE,
                new LoggingThreadFactory(AVAIL_THREAD_POOL_NAME, true));
            availabilityExecutor = new AvailabilityExecutor(this);
            availabilityThreadPoolExecutor.scheduleWithFixedDelay(availabilityExecutor, configuration
                .getAvailabilityScanInitialDelay(), configuration.getAvailabilityScanPeriod(), TimeUnit.SECONDS);
        } finally {
            inventoryLock.writeLock().unlock();
        }

        log.info("Inventory Manager initialized.");
    }

    /**
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        inventoryThreadPoolExecutor.shutdownNow();
        availabilityThreadPoolExecutor.shutdownNow();
        if (configuration.isInsideAgent()) {
            this.persistToDisk();
        }
    }

    @Nullable
    public ResourceContainer getResourceContainer(Resource resource) {
        return this.resourceContainers.get(resource.getUuid());
    }

    @Nullable
    public ResourceContainer getResourceContainer(Integer resourceId) {
        if ((resourceId == null) || (resourceId == 0)) {
            // i've already found one place where passing in 0 was very bad - I want to be very noisy in the log
            // when this happens but not throw an exception, for fear I might break something.
            // I'll just return null instead; hopefully, callers are checking for null appropriately
            log.warn("Cannot get a resource container for an invalid resource ID=" + resourceId);
            if (log.isDebugEnabled()) {
                //noinspection ThrowableInstanceNeverThrown
                log.debug("Stack trace follows:", new Throwable("This is where resource ID=[" + resourceId
                    + "] was passed in"));
            }

            return null;
        }

        List<ResourceContainer> containers = new ArrayList<ResourceContainer>(this.resourceContainers.values()); // avoids concurrent mod exception
        for (ResourceContainer container : containers) {
            if (resourceId.equals(container.getResource().getId())) {
                return container;
            }
        }

        return null;
    }

    void executePlatformScan() {
        log.debug("Executing platform scan...");
        Resource discoveredPlatform = discoverPlatform();
        try {
            mergeResourceFromDiscovery(discoveredPlatform, null);
        }
        catch (PluginContainerException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
        throws InvalidPluginConfigurationClientException, PluginContainerException {
        ResourceContainer container = getResourceContainer(resourceId);
        if (container == null) {
            throw new PluginContainerException("Cannot update plugin configuration for unknown Resource with id ["
                + resourceId + "]");
        }

        Resource resource = container.getResource();
        // First stop the resource component.
        deactivateResource(resource);
        // Then update the resource's plugin config.
        resource.setPluginConfiguration(newPluginConfiguration);
        // And finally restart the resource component.
        try {
            activateResource(resource, container, true);
            // TODO: What about re-activating the Resource's descendants?
        } catch (InvalidPluginConfigurationException e) {
            String errorMessage = "Unable to connect to managed resource of type '"
                + resource.getResourceType().getName() + "' using the specified connection properties.";
            log.info(errorMessage, e);
            errorMessage += ((e.getLocalizedMessage() != null) ? (" " + e.getLocalizedMessage()) : "");

            // In the exception we throw over to the server, strip the InvalidPluginConfigurationException out of the
            // stack trace, but append the message from that exception to the message of the exception we throw. This
            // will make for a nicer error message for the server to display in the UI.
            throw new InvalidPluginConfigurationClientException(errorMessage,
                (e.getCause() != null) ? new WrappedRemotingException(e.getCause()) : null);
        }
    }

    public InventoryReport executeServerScanImmediately() {
        try {
            return inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serverScanExecutor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Server scan execution was interrupted");
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public InventoryReport executeServiceScanImmediately() {
        try {
            return inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serviceScanExecutor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Service scan execution was interrupted", e);
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public void executeServiceScanDeferred() {
        inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serviceScanExecutor);
    }

    public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
        try {
            AvailabilityExecutor availExec = new AvailabilityExecutor(this);

            if (changedOnlyReport) {
                availExec.sendChangedOnlyReportNextTime();
            } else {
                availExec.sendFullReportNextTime();
            }

            return availabilityThreadPoolExecutor.submit((Callable<AvailabilityReport>) availExec).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Availability scan execution was interrupted", e);
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public Availability getAvailability(Resource resource) {
        Availability avail = getAvailabilityIfKnown(resource);
        if (avail == null) {
            avail = new Availability(resource, new Date(), null);
        }

        return avail;
    }

    @SuppressWarnings("unchecked")
    public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int ownerSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException {
        // TODO (ghinkle): This is hugely flawed. It assumes discovery components will only return the manually discovered
        // resource, but never says this is required. It then proceeds to auto-import the first resource returned. For
        // discoveries that are process based it works because this passes in a null process scan... also a bad idea.

        // Lookup the full, local Resource type (the provided one is just the keys).
        ResourceType fullResourceType = this.pluginManager.getMetadataManager().getType(resourceType);
        if (fullResourceType == null) {
            throw new IllegalStateException("Server specified unknown Resource type: " + resourceType);
        }

        MergeResourceResponse mergeResourceResponse;
        Resource resource = null;
        boolean resourceAlreadyExisted = false;
        try {
            // Get the discovery component responsible for discovering resources of the specified resource type.
            PluginComponentFactory pluginComponentFactory = PluginContainer.getInstance().getPluginComponentFactory();
            ResourceDiscoveryComponent discoveryComponent = pluginComponentFactory.getDiscoveryComponent(resourceType);

            List<Configuration> pluginConfigurations = new ArrayList<Configuration>(1);
            pluginConfigurations.add(pluginConfiguration);
            ResourceContainer parentResourceContainer = getResourceContainer(parentResourceId);
            ResourceComponent parentResourceComponent = parentResourceContainer.getResourceComponent();
            ResourceDiscoveryContext<ResourceComponent> resourceDiscoveryContext = new ResourceDiscoveryContext<ResourceComponent>(
                resourceType, parentResourceComponent, parentResourceContainer.getResourceContext(), SystemInfoFactory
                    .createSystemInfo(), new ArrayList<ProcessScanResult>(0), pluginConfigurations, configuration
                    .getContainerName());

            // Ask the plugin's discovery component to find the new resource, throwing exceptions if it cannot be found at all.
            Set<DiscoveredResourceDetails> discoveredResources = discoveryComponent
                .discoverResources(resourceDiscoveryContext);
            if ((discoveredResources == null) || discoveredResources.isEmpty()) {
                log
                    .info("Plugin Warning: discovery component "
                        + discoveryComponent.getClass().getName()
                        + " returned no resources when passed a single plugin configuration (the plugin developer probably did not implement support for manually discovered resources).");
                throw new PluginContainerException("The " + resourceType.getPlugin()
                    + " plugin does not support manual addition of '" + resourceType.getName() + "' resources.");
            }

            // Create the new resource and add it to inventory if it isn't already there.
            DiscoveredResourceDetails discoveredResourceDetails = discoveredResources.iterator().next();
            resource = createNewResource(discoveredResourceDetails);
            Resource parentResource = getResourceContainer(parentResourceId).getResource();
            Resource existingResource = findMatchingChildResource(resource, parentResource);
            if (existingResource != null) {
                log.debug("Manual add for resource type '" + resourceType.getName() + "' and parent resource id "
                    + parentResourceId
                    + " found a resource that already exists in inventory - updating existing resource "
                    + existingResource + "...");
                resourceAlreadyExisted = true;
                resource = existingResource;
                if (resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
                    resource.setPluginConfiguration(pluginConfiguration);
                }
            } else {
                log.debug("Adding manually discovered resource " + resource + " to inventory...");
                initResourceContainer(resource);
                parentResource.addChildResource(resource);
            }

            // Make sure the resource's component is activated (i.e. started).
            boolean newPluginConfig = true;
            ResourceContainer resourceContainer = getResourceContainer(resource);
            log.debug("Activating resource " + resource + "...");
            activateResource(resource, resourceContainer, newPluginConfig);

            // NOTE: We don't mess with inventory status - that's the server's responsibility.

            // Tell the server to merge the resource into its inventory.
            DiscoveryServerService discoveryServerService = this.configuration.getServerServices()
                .getDiscoveryServerService();
            mergeResourceResponse = discoveryServerService.addResource(resource, ownerSubjectId);

            // Sync our local resource up with the one now in server inventory.
            resource.setId(mergeResourceResponse.getResourceId());
            synchronizeInventory(resource.getId(), EnumSet.allOf(SynchronizationType.class));
        }

        // Catch any other RuntimeExceptions or Errors, so the server doesn't have to worry about deserializing or
        // catching them. Before rethrowing, wrap them in a WrappedRemotingException and then wrap that in either an
        // InvalidPluginConfigurationException or a PluginContainerException.
        catch (Throwable t) {
            if ((resource != null) && !resourceAlreadyExisted && (getResourceContainer(resource) != null)) {
                // If the resource got added to inventory, roll it back (i.e. deactivate it, then remove it from inventory).
                log.debug("Rolling back manual add of resource of type " + resourceType.getName()
                    + "' - removing resource with id " + resource.getId() + " from inventory...");
                deactivateResource(resource);
                removeResource(resource.getId());
            }

            if (t instanceof InvalidPluginConfigurationException) {
                String errorMessage = "Unable to connect to managed resource of type '" + resourceType.getName()
                    + "' using the specified connection properties - resource will not be added to inventory.";
                log.info(errorMessage, t);

                // In the exception we throw over to the server, strip the InvalidPluginConfigurationException out of the
                // stack trace, but append the message from that exception to the message of the exception we throw. This
                // will make for a nicer error message for the server to display in the UI.
                errorMessage += ((t.getLocalizedMessage() != null) ? (" " + t.getLocalizedMessage()) : "");
                throw new InvalidPluginConfigurationClientException(errorMessage,
                    (t.getCause() != null) ? new WrappedRemotingException(t.getCause()) : null);
            } else {
                log.error("Manual add failed for resource of type '" + resourceType.getName()
                    + "' and parent resource id [" + parentResourceId + "].", t);
                throw new PluginContainerException("Failed to add resource with type [" + resourceType.getName()
                    + "] and parent resource id [" + parentResourceId + "].", new WrappedRemotingException(t));
            }
        }

        return mergeResourceResponse;
    }

    static Resource createNewResource(DiscoveredResourceDetails details) {
        Resource resource = new Resource();
        resource.setResourceKey(details.getResourceKey());
        resource.setName(details.getResourceName());
        resource.setVersion(details.getResourceVersion());
        resource.setDescription(details.getResourceDescription());
        resource.setResourceType(details.getResourceType());

        Configuration pluginConfiguration = details.getPluginConfiguration();
        ConfigurationUtility.normalizeConfiguration(details.getPluginConfiguration(), details.getResourceType()
            .getPluginConfigurationDefinition());

        resource.setPluginConfiguration(pluginConfiguration);
        return resource;
    }

    /**
     * Returns the known availability for the resource. If the availability is not known, <code>null</code> is returned.
     *
     * @param  resource the resource whose availability should be returned
     *
     * @return resource availability or <code>null</code> if not known
     */
    @Nullable
    public Availability getAvailabilityIfKnown(Resource resource) {
        ResourceContainer resourceContainer = getResourceContainer(resource);

        if (resourceContainer != null) {
            if (ResourceComponentState.STARTED == resourceContainer.getResourceComponentState()) {
                Availability availability = resourceContainer.getAvailability();
                return availability;
            }
        }

        return null;
    }

    public void handleReport(AvailabilityReport report) {
        // a null report means a non-committed inventory - we are either brand new or our platform was deleted recently
        if (report == null) {
            if ((this.platform != null) && (this.platform.getInventoryStatus() == InventoryStatus.NEW)
                && newPlatformWasDeletedRecently) {
                // let's make sure we are registered; its probable that our platform was deleted and we need to re-register
                log
                    .info("No committed resources to send in our availability report - the platform/agent was deleted, let's re-register again");
                registerWithServer();
                newPlatformWasDeletedRecently = false; // we've tried to recover from our platform being deleted, let's not do it again
            }

            return;
        }

        List<Availability> reportAvails = report.getResourceAvailability();

        if (configuration.isInsideAgent() && (reportAvails != null)) {
            // Due to the asynchronous nature of the availability collection,
            // it is possible we may have collected availability of a resource that has just recently been deleted;
            // therefore, as a secondary check, let's remove any availabilities for resources that no longer exist.
            // I suppose after we do this check and before we send the report to the server that a resource could
            // then be deleted, but that time period where that could happen is now very small and thus this will
            // be a rare event.  And even if that does happen, nothing catastrophic would happen on the server,
            // the report would fail, an error would be logged on the server, and the exception thrown would
            // cause us to send a full report next time.
            this.inventoryLock.readLock().lock();
            try {
                Availability[] avails = reportAvails.toArray(new Availability[0]);
                for (Availability avail : avails) {
                    ResourceContainer container = getResourceContainer(avail.getResource());
                    if ((container == null)
                        || (container.getResource().getInventoryStatus() == InventoryStatus.DELETED)) {
                        reportAvails.remove(avail);
                    }
                }
            } finally {
                this.inventoryLock.readLock().unlock();
            }

            if (reportAvails.size() > 0) {
                try {
                    log.info("Sending availability report to Server...");
                    if (log.isDebugEnabled()) {
                        log.debug("Availability report content: " + report.toString(log.isTraceEnabled()));
                    }

                    boolean ok = configuration.getServerServices().getDiscoveryServerService().mergeAvailabilityReport(
                        report);
                    if (!ok) {
                        // I guess I could immediately call executeAvailabilityScanImmediately and pass its results to
                        // mergeAvailabilityReport again right now, but what happens if we've queued up a bunch of
                        // changed-only reports and the server is out of sync - each time the server processes those
                        // reports, we'd do an extra round trip with a full report (which will get very expensive).
                        // Let's just flag our executor for the next time it runs to send a full report; this way
                        // if we've got 100 queued changed-only reports, let the server fully process them and only
                        // at the next time we run the avail scan will we send it a full report.  It might make the
                        // server sync up alittle slower than we'd like, but it avoids a potential hammering of the
                        // server with tons of full reports when that would be unnecessary.
                        availabilityExecutor.sendFullReportNextTime();
                    }
                } catch (Exception e) {
                    log.warn("Could not transmit availability report to server", e);
                    availabilityExecutor.sendFullReportNextTime(); // just in case the agent and server are out of sync
                }
            }
        }
    }

    /**
     * Send an inventory report to the Server.
     *
     * @param  report the inventory report to be sent
     *
     * @return true if sending the report to the Server succeeded, or false otherwise
     */
    public boolean handleReport(InventoryReport report) {
        if (!configuration.isInsideAgent()) {
            return true;
        }
        ResourceSyncInfo syncInfo;
        try {
            String reportType = (report.isRuntimeReport()) ? "runtime" : "server";
            log.info("Sending " + reportType + " inventory report to Server...");
            long startTime = System.currentTimeMillis();
            DiscoveryServerService discoveryServerService = configuration.getServerServices()
                .getDiscoveryServerService();
            syncInfo = discoveryServerService.mergeInventoryReport(report);
            log.debug(String.format("Server DONE merging inventory report (%d)ms.",
                (System.currentTimeMillis() - startTime)));
        } catch (InvalidInventoryReportException e) {
            log.error("Failure sending inventory report to Server - was this Agent's platform deleted?", e);
            if ((this.platform != null) && (this.platform.getInventoryStatus() == InventoryStatus.NEW)
                && newPlatformWasDeletedRecently) {
                // let's make sure we are registered; its probable that our platform was deleted and we need to re-register
                log
                    .info("The inventory report was invalid probably because the platform/Agent was deleted; let's re-register...");
                registerWithServer();
                newPlatformWasDeletedRecently = false; // we've tried to recover from our platform being deleted, let's not do it again
            }
            return false;
        }

        synchronizeInventory(syncInfo);
        return true;
    }

    public void synchronizeInventory(ResourceSyncInfo syncInfo) {
        log.info("Syncing local inventory with Server inventory...");
        long startTime = System.currentTimeMillis();
        Set<Resource> syncedResources = new LinkedHashSet();
        Set<Integer> unknownResourceIds = new LinkedHashSet();
        Set<Integer> modifiedResourceIds = new LinkedHashSet();
        Set<String> allUuids = new HashSet();
        processSyncInfo(syncInfo, syncedResources, unknownResourceIds, modifiedResourceIds, allUuids);
        mergeUnknownResources(unknownResourceIds);
        mergeModifiedResources(modifiedResourceIds);
        purgeObsoleteResources(allUuids);
        log.debug(String.format("DONE syncing local inventory (%d)ms.", (System.currentTimeMillis() - startTime)));
        // If we synced any Resources, one or more Resource components were probably started,
        // so run an avail scan to report on their availabilities immediately. Also kick off
        // a service scan to scan those Resources for new child Resources.
        if (!syncedResources.isEmpty() || !unknownResourceIds.isEmpty() || !modifiedResourceIds.isEmpty()) {
            performAvailabilityChecks(true);
            this.inventoryThreadPoolExecutor.schedule((Callable<? extends Object>) this.serviceScanExecutor, 5,
                TimeUnit.SECONDS);
        }
    }

    /**
     * Registers the plugin container with a remote server, if there is one. A no-op if we are not talking to a remote
     * server in which we need to be registered.
     */
    private void registerWithServer() {
        AgentRegistrar registrar = PluginContainer.getInstance().getAgentRegistrar();
        if (registrar != null) {
            try {
                registrar.register(10000L);

                // now that we are registered, let's kick off an inventory report
                // just to make sure the server has our initial inventory
                inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serverScanExecutor);
            } catch (Exception e) {
                log.error("Cannot re-register with the agent, something bad is happening", e);
            }
        }

        return;
    }

    /**
     * Performs a service scan on the specified Resource. NOTE: This method will block until the scan completes.
     *
     * @param resourceId the id of the Resource on which to discover services
     */
    public void performServiceScan(int resourceId) {
        ResourceContainer resourceContainer = getResourceContainer(resourceId);
        Resource resource = resourceContainer.getResource();
        RuntimeDiscoveryExecutor oneTimeExecutor = new RuntimeDiscoveryExecutor(this, configuration, resource);

        try {
            inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) oneTimeExecutor).get();
        } catch (Exception e) {
            throw new RuntimeException("Error submitting service scan", e);
        }
    }

    @Nullable
    public ResourceComponent<?> getResourceComponent(Resource resource) {
        ResourceContainer resourceContainer = this.resourceContainers.get(resource.getUuid());

        if (resourceContainer == null) {
            return null;
        }

        return resourceContainer.getResourceComponent();
    }

    public void removeResource(int resourceId) {
        ResourceContainer resourceContainer = getResourceContainer(resourceId);
        if (resourceContainer == null) {
            log.debug("Could not remove Resource [" + resourceId + "] because its container was null.");
            return;
        }
        boolean scan = removeResourceAndIndicateIfScanIsNeeded(resourceContainer.getResource());

        if (scan) {
            log.info("Deleted resource #[" + resourceId + "] - this will trigger a server scan now");
            inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serverScanExecutor);
        }
    }

    /**
     * Removes the resource and its children and returns true if a scan is needed.
     *
     * @param resource the Resource to be removed
     * @return true if this method deleted things that requires a scan.
     */
    boolean removeResourceAndIndicateIfScanIsNeeded(Resource resource) {
        boolean scanIsNeeded = false;

        this.inventoryLock.writeLock().lock();
        try {
            log.debug("Removing " + resource + " from local inventory...");

            // this will deactivate the resource starting bottom-up - so this ends up as a no-op if we are being called
            // recursively, but we need to do this now to ensure everything is stopped prior to removing them from
            // inventory
            deactivateResource(resource);

            Set<Resource> children = new HashSet<Resource>(resource.getChildResources()); // put in new set to avoid concurrent mod exceptions
            for (Resource child : children) {
                scanIsNeeded |= removeResourceAndIndicateIfScanIsNeeded(child);
            }

            Resource parent = resource.getParentResource();
            if (parent != null) {
                parent.removeChildResource(resource);
            }

            PluginContainer.getInstance().getMeasurementManager().unscheduleCollection(
                Collections.singleton(resource.getId()));

            if (this.resourceContainers.remove(resource.getUuid()) == null) {
                log.debug("Asked to remove an unknown Resource [" + resource + "] with UUID [" + resource.getUuid()
                    + "]");
            }

            fireResourcesRemoved(Collections.singleton(resource));

            // if we just so happened to have removed our top level platform, we need to re-discover it, can't go living without it
            // once we discover the platform, let's schedule an immediate server scan
            if ((this.platform == null) || (this.platform.getId() == resource.getId())) {
                log.debug("Platform [" + resource.getId() + "] was deleted - running platform scan now...");
                this.platform = null;
                executePlatformScan();
                newPlatformWasDeletedRecently = true;
                scanIsNeeded = true;
            } else {
                boolean isTopLevelServer = (this.platform != null)
                    && (this.platform.equals(resource.getParentResource()))
                    && (resource.getResourceType().getCategory() != ResourceCategory.SERVICE);
                if (isTopLevelServer) {
                    log.debug("Top-level server [" + resource.getId() + "] was deleted - server discovery is needed.");
                    // if we got here, we just deleted a top level server (whose parent is the platform), let's request a scan
                    scanIsNeeded = true;
                }
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }

        return scanIsNeeded;
    }

    public Resource getPlatform() {
        return platform;
    }

    public Agent getAgent() {
        return this.agent;
    }

    /**
     * Inject a new availability
     *
     * @param  resource
     * @param  availabilityType
     *
     * @return
     */
    public Availability updateAvailability(Resource resource, AvailabilityType availabilityType) {
        ResourceContainer resourceContainer = this.resourceContainers.get(resource.getUuid());
        return resourceContainer.updateAvailability(availabilityType);
    }

    public Resource mergeResourceFromDiscovery(Resource resource, Resource parent) throws PluginContainerException {
        // If the Resource is already in inventory, make sure its version is up-to-date, then simply return the
        // existing Resource.
        Resource existingResource = findMatchingChildResource(resource, parent);
        if (existingResource != null) {
            updateResourceVersion(existingResource, resource.getVersion());
            return existingResource;
        }

        // Auto-generate id and auto-commit if embedded within JBossAS.
        if (!this.configuration.isInsideAgent()) {
            resource.setId(this.temporaryKeyIndex.decrementAndGet());
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
        }

        // Add the Resource to the Resource hierarchy.
        if (parent != null) {
            log.debug("Detected new Resource [" + resource + "] - adding to local inventory...");
            parent.addChildResource(resource);
        } else {
            log.info("Detected new platform [" + resource + "] - adding to local inventory...");
            this.platform = resource;
        }

        // Initialize a container for the Resource.
        ResourceContainer resourceContainer = getResourceContainer(resource);
        if (resourceContainer != null) {
            // This should never happen...
            log.warn("Resource container already existed for Resource that was supposed to be NEW: " + resource);
        } else {
            resourceContainer = initResourceContainer(resource);
        }

        // Auto-activate if embedded within JBossAS (if within Agent, we need to wait until the Resource has been
        // imported into the Server's inventory before activating it).
        if (!this.configuration.isInsideAgent()) {
            try {
                activateResource(resource, resourceContainer, true); // just start 'em up as we find 'em for the embedded side
            } catch (InvalidPluginConfigurationException e) {
                log.error("Failed to activate " + resource + ": " + e.getLocalizedMessage());
                // TODO: I don't think it makes any sense to call the below method w/in the embedded console.
                // (ips, 07/16/08)
                sendInvalidPluginConfigurationResourceError(resource, e);
            }
        }

        return resource;
    }

    private ResourceContainer initResourceContainer(Resource resource) {
        ResourceContainer resourceContainer = getResourceContainer(resource);
        if (resourceContainer == null) {
            resourceContainer = new ResourceContainer(resource);
            if (!this.configuration.isInsideAgent()) {
                // Auto-sync if the PC is running within the embedded JBossAS console.
                resourceContainer.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
            }
            this.resourceContainers.put(resource.getUuid(), resourceContainer);
            if (resource.getParentResource() == null) {
                // Resource has no parent - it must be the platform. This little bit of code is needed by some
                // unit tests.
                if (this.platform.getResourceType().equals(PluginMetadataManager.TEST_PLATFORM_TYPE)) {
                    resourceContainer.setResourceComponent(createTestPlatformComponent());
                }
            }
        }
        return resourceContainer;
    }

    /**
     * This will start the resource's plugin component, creating it first if it has not yet been created. If the
     * component is already created and started, this method is a no-op.
     *
     * @param  resource        the resource that the component will manage
     * @param  container       the wrapper around the resource and its component
     * @param  updatedPluginConfig if <code>true</code>, this will indicate that the resource's plugin configuration is
     *                         known to have changed since the last time the resource component was started
     *
     * @throws InvalidPluginConfigurationException when connecting to the managed resource fails due to an invalid
     *                                             plugin configuration
     * @throws PluginContainerException            for all other errors
     */
    @SuppressWarnings("unchecked")
    public void activateResource(Resource resource, @NotNull
    ResourceContainer container, boolean updatedPluginConfig) throws InvalidPluginConfigurationException,
        PluginContainerException {
        ResourceComponent component = container.getResourceComponent();

        // if the component already exists and is started, and the resource's plugin config has not changed, there is
        // nothing to do, so return immediately
        if ((component != null) && (container.getResourceComponentState() == ResourceComponentState.STARTED)
            && !updatedPluginConfig) {
            log.trace("Skipping activation of " + resource + " - its component is already started and its plugin "
                + "config has not been updated since it was last started.");
            return;
        }

        log.debug("Starting component for " + resource + "(current state = " + container.getResourceComponentState()
            + ", new plugin config = " + updatedPluginConfig + ")...");

        // If the component does not even exist yet, we need to instantiate it and set it on the container.
        if (component == null) {
            log.debug("Creating component for " + resource + "...");
            try {
                component = PluginContainer.getInstance().getPluginComponentFactory().buildResourceComponent(
                    resource.getResourceType());
            } catch (Throwable e) {
                throw new PluginContainerException("Could not build component for Resource [" + resource + "]", e);
            }
            container.setResourceComponent(component);
        }

        // Wrap the component in a proxy that will provide locking and a timeout for the call to start().
        component = container.createResourceComponentProxy(ResourceComponent.class, FacetLockType.READ,
            COMPONENT_START_TIMEOUT, true, false);

        // start the resource, but only if its parent component is running
        if ((resource.getParentResource() == null)
            || (getResourceContainer(resource.getParentResource()).getResourceComponentState() == ResourceComponentState.STARTED)) {
            PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();
            ResourceDiscoveryComponent discoveryComponent;
            discoveryComponent = factory.getDiscoveryComponent(resource.getResourceType());

            ConfigurationUtility.normalizeConfiguration(resource.getPluginConfiguration(), resource.getResourceType()
                .getPluginConfigurationDefinition());

            ResourceComponent parentComponent = null;
            if (resource.getParentResource() != null) {
                parentComponent = getResourceComponent(resource.getParentResource());
            }

            File pluginDataDir = new File(this.configuration.getDataDirectory(), resource.getResourceType().getPlugin());

            ResourceContext context = new ResourceContext(resource, // the resource itself
                parentComponent, // its parent component
                discoveryComponent, // the discovery component
                SystemInfoFactory.createSystemInfo(), // for native access
                this.configuration.getTemporaryDirectory(), // location for plugin to write temp files
                pluginDataDir, // location for plugin to write data files
                this.configuration.getContainerName(), // the name of the agent/PC
                getEventContext(resource), // for event access
                getOperationContext(resource), // for operation manager access
                getContentContext(resource)); // for content manager access

            container.setResourceContext(context);
            ClassLoader startingClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
                // One last check to make sure another thread didn't beat us to the punch.
                // TODO: Add some real synchronization to this method. (ips, 07/09/07)
                if (container.getResourceComponentState() == ResourceComponentState.STARTED) {
                    log.trace("Skipping activation of " + resource + " - its component is already started.");
                    return;
                }
                component.start(context);
                container.setResourceComponentState(ResourceComponentState.STARTED);
                resource.setConnected(true); // This tells the server-side that the resource has connected successfully.
            } catch (Throwable t) {
                if (updatedPluginConfig || (t instanceof InvalidPluginConfigurationException)) {
                    if (log.isDebugEnabled())
                        log.debug("Resource has a bad config, waiting for this to go away " + resource);
                    InventoryEventListener iel = new ResourceGotActivatedListener();
                    addInventoryEventListener(iel);
                    throw new InvalidPluginConfigurationException("Failed to start component for resource " + resource
                        + ".", t);
                }
                throw new PluginContainerException("Failed to start component for resource " + resource + ".", t);
            } finally {
                Thread.currentThread().setContextClassLoader(startingClassLoader);
            }

            // We purposefully do not get availability of this resource yet
            // We need availability checked during the normal availability executor timeframe.
            // Otherwise, new resources will not have their availabilities shipped up to the server because
            // they will look like they haven't changed status since the last avail report - but the new
            // resources statuses never got sent up in the last avail report because they didn't exist at that time

            // Finally, inform the rest of the plugin container that this resource has been activated
            fireResourceActivated(resource);
        } else {
            log.debug("Not activating resource [" + resource + "] because its parent isn't started: "
                + getResourceContainer(resource.getParentResource()));
        }
    }

    /**
     * This will send a resource error to the server (if applicable) to indicate that the given resource could not be
     * connected to due to an invalid plugin configuration.
     *
     * @param resource the resource that could not be connected to
     * @param t        the exception that indicates the problem with the plugin configuration
     */
    private void sendInvalidPluginConfigurationResourceError(Resource resource, Throwable t) {
        resource.setConnected(false); // invalid plugin configuration infers the resource component is disconnected

        DiscoveryServerService serverService = configuration.getServerServices().getDiscoveryServerService();
        if (serverService != null) {
            // give the server-side an error message describing the connection failure that can be
            // displayed on the resource's Inventory page.
            ResourceError resourceError = new ResourceError(resource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION,
                t, System.currentTimeMillis());
            try {
                serverService.setResourceError(resourceError);
            } catch (Exception e) {
                log.warn("Cannot inform the server about a resource error [" + resourceError + "]. Cause: " + e);
            }
        }
    }

    private Resource findMatchingChildResource(Resource resource, Resource parent) {
        if (parent == null) {
            // Resource must be a platform - see if it matches our local platform
            if (this.platform != null && matches(resource, this.platform)) {
                return this.platform;
            }
        } else {
            for (Resource child : parent.getChildResources()) {
                if (matches(resource, child)) {
                    return child;
                }
            }
        }
        return null;
    }

    private static boolean matches(Resource newResource, Resource existingResource) {
        return ((existingResource.getId() != 0) && (existingResource.getId() == newResource.getId()))
            || (existingResource.getUuid().equals(newResource.getUuid()))
            || (existingResource.getResourceType().equals(newResource.getResourceType()) && existingResource.getResourceKey().equals(
                newResource.getResourceKey()));
    }

    /**
     * Lookup all the servers with a particular server type
     *
     * @param  serverType the server type to match against
     *
     * @return the set of servers matching the provided type
     */
    public Set<Resource> getResourcesWithType(ResourceType serverType) {
        return getResourcesWithType(serverType, this.platform.getChildResources());
    }

    private Set<Resource> getResourcesWithType(ResourceType serverType, Set<Resource> resources) {
        Set<Resource> servers = new HashSet<Resource>();

        if (resources == null) {
            return servers;
        }

        for (Resource server : resources) {
            servers.addAll(getResourcesWithType(serverType, server.getChildResources()));

            if (serverType.equals(server.getResourceType())) {
                servers.add(server);
            }
        }

        return servers;
    }

    // TODO: I think we can get rid of most/all of this method. (ips, 07/07/08)
    private void activateFromDisk(Resource resource) throws PluginContainerException {
        if (resource.getId() == 0) {
            return; // This is for the case of a resource that hadn't been synced to the server (there are probably better places to handle this)
        }

        resource.setAgent(this.agent);
        ResourceContainer container = getResourceContainer(resource.getId());
        if (container.getSynchronizationState() != ResourceContainer.SynchronizationState.SYNCHRONIZED) {
            log.debug("Stopped activating resources at unsynchronized resource [" + resource + "]");
            return;
        }

        try {
            activateResource(resource, container, false);
        } catch (Exception e) {
            log.debug("Failed to activate from disk [" + resource + "]");
        }

        for (Resource child : resource.getChildResources()) {
            activateFromDisk(child);
        }
    }

    private void loadFromDisk() {
        this.inventoryLock.writeLock().lock();

        try {
            File file = new File(this.configuration.getDataDirectory(), "inventory.dat");
            if (file.exists()) {
                long start = System.currentTimeMillis();
                log.info("Loading inventory from persistent data file");

                InventoryFile inventoryFile = new InventoryFile(file);
                inventoryFile.loadInventory();

                this.platform = inventoryFile.getPlatform();
                this.resourceContainers = inventoryFile.getResourceContainers();

                initResourceContainer(this.platform);
                activateFromDisk(this.platform);

                log.info("Inventory size [" + this.resourceContainers.size() + "] initialized from disk in ["
                    + (System.currentTimeMillis() - start) + "ms]");
            }
        } catch (Exception e) {
            log.error("Could not load inventory data from disk", e);
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    /**
     * Shutdown the ResourceComponents from the bottom up.
     */
    private void deactivateResource(Resource resource) {
        this.inventoryLock.writeLock().lock();
        try {
            ResourceContainer container = getResourceContainer(resource);
            if ((container != null) && (container.getResourceComponentState() == ResourceComponentState.STARTED)) {
                for (Resource child : resource.getChildResources()) {
                    deactivateResource(child);
                }

                try {
                    ResourceComponent<?> component = container.createResourceComponentProxy(ResourceComponent.class,
                        FacetLockType.WRITE, COMPONENT_STOP_TIMEOUT, true, true);
                    component.stop();
                    log.debug("Successfully deactivated resource with id [" + resource.getId() + "].");
                } catch (Throwable t) {
                    log.warn("Plugin Error: Failed to stop component for [" + resource + "].");
                }

                container.setResourceComponentState(ResourceComponentState.STOPPED);
                log.debug("Set component state to STOPPED for resource with id [" + resource.getId() + "].");
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    private void persistToDisk() {
        try {
            deactivateResource(this.platform);
            File file = new File(this.configuration.getDataDirectory(), "inventory.dat");
            InventoryFile inventoryFile = new InventoryFile(file);
            inventoryFile.storeInventory(this.platform, this.resourceContainers);
        } catch (Exception e) {
            log.error("Could not persist inventory data to disk", e);
        }
    }

    /**
     * Detects the top platform resource and starts its ResourceComponent.
     *
     * TODO GH: Move this to another class (this one is getting too big)
     */
    @SuppressWarnings("unchecked")
    private Resource discoverPlatform() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginComponentFactory componentFactory = PluginContainer.getInstance().getPluginComponentFactory();
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        Set<ResourceType> platformTypes = pluginManager.getMetadataManager().getTypesForCategory(
            ResourceCategory.PLATFORM);

        // This should only ever have 1 or, at most, 2 Resources
        // (always the Java fallback platform, and the native platform if supported).
        Set<DiscoveredResourceDetails> allDiscoveredPlatforms = new HashSet<DiscoveredResourceDetails>(2);

        if ((platformTypes != null) && (platformTypes.size() > 0)) {
            // Go through all the platform types that are supported and see if they can detect our platform.
            for (ResourceType platformType : platformTypes) {
                try {
                    ResourceDiscoveryComponent component = componentFactory.getDiscoveryComponent(platformType);
                    ResourceDiscoveryContext context = new ResourceDiscoveryContext(platformType, null, null,
                        systemInfo, Collections.EMPTY_LIST, Collections.EMPTY_LIST, configuration.getContainerName());
                    Set<DiscoveredResourceDetails> discoveredResources = null;

                    try {
                        discoveredResources = component.discoverResources(context);
                    } catch (Throwable e) {
                        log.warn("Platform plugin discovery failed - skipping", e);
                    }

                    if (discoveredResources != null) {
                        allDiscoveredPlatforms.addAll(discoveredResources);
                    }
                } catch (Throwable e) {
                    log.error("Error in platform discovery", e);
                }
            }
        } else {
            // This is very strange - there are no platform types - we should never be missing the built-in platform plugin.
            log
                .error("Missing platform plugin(s) - falling back to dummy platform impl; this should only occur in tests!");
            // TODO: Set sysprop (e.g. rhq.test.mode=true) in integration tests,
            //       and throw a runtime exception here if that sysprop is not set.
            return createTestPlatform();
        }

        if (allDiscoveredPlatforms.isEmpty()) {
            throw new IllegalStateException("Neither a native nor a Java platform was discovered - "
                + "this should never happen. Known platform types are " + platformTypes + ".");
        }

        if (allDiscoveredPlatforms.size() > 2) {
            log.warn("Platform discovery reported too many platforms - "
                + "the platform discovery components for platform types " + platformTypes + " "
                + "should be fixed so together they report no more than 2 platforms total. " + "Reported platforms: "
                + allDiscoveredPlatforms + ".");
        }

        DiscoveredResourceDetails javaPlatform = null;
        DiscoveredResourceDetails nativePlatform = null;
        for (DiscoveredResourceDetails discoveredPlatform : allDiscoveredPlatforms) {
            // We know the Java resource type in the descriptor is named "Java".
            if (discoveredPlatform.getResourceType().getName().equalsIgnoreCase("Java")) {
                javaPlatform = discoveredPlatform;
            } else {
                nativePlatform = discoveredPlatform;
            }
        }

        // In most cases, we will have both (since we support most platforms natively),
        // so use the native platform if we have it; if not, fall back to the Java platform.
        DiscoveredResourceDetails platformToUse = (nativePlatform != null) ? nativePlatform : javaPlatform;

        // Build our actual platform resource now that we've discovered it.
        Resource platform = createNewResource(platformToUse);
        platform.setAgent(this.agent);

        return platform;
    }

    /**
     * If for some reason the platform plugin is not available, this method can be called to add a "dummy" platform
     * resource. This is normally only used during tests.
     */
    private Resource createTestPlatform() {
        ResourceType type = PluginContainer.getInstance().getPluginManager().getMetadataManager().addTestPlatformType();
        Resource platform = new Resource("testkey" + configuration.getContainerName(), "testplatform", type);
        platform.setAgent(this.agent);
        return platform;
    }

    public void synchronizeInventory(int resourceId, EnumSet<SynchronizationType> synchronizationTypes) {
        log.info("Synchronizing local inventory with Server inventory for Resource " + resourceId
            + " and its descendants...");
        // Get the latest resource data rooted at the given id.

        Resource resource = getResourceContainer(resourceId).getResource();
        if (synchronizationTypes.contains(DiscoveryAgentService.SynchronizationType.STATUS)) {
            syncInventoryStatusRecursively(resource);
        }

        if (synchronizationTypes.contains(DiscoveryAgentService.SynchronizationType.MEASUREMENT_SCHEDULES)) {
            syncSchedulesRecursively(resource);
        }

        if (synchronizationTypes.contains(DiscoveryAgentService.SynchronizationType.ALERT_TEMPLATES)) {
            syncAlertTemplatesRecursively(resource);
        }
    }

    private void syncInventoryStatusRecursively(Resource rootResource) {
        Map<Integer, InventoryStatus> statuses = configuration.getServerServices().getDiscoveryServerService()
            .getInventoryStatus(rootResource.getId(), true);
        inventoryLock.writeLock().lock();
        try {
            for (Integer resourceId : statuses.keySet()) {
                ResourceContainer resourceContainer = getResourceContainer(resourceId);
                if (resourceContainer != null) {
                    InventoryStatus statusFromServer = statuses.get(resourceId);
                    resourceContainer.getResource().setInventoryStatus(statusFromServer);
                    refreshResourceComponentState(resourceContainer, false);
                } else {
                    log.debug("Resource with id " + resourceId + " exists on Server, but not on Agent. "
                        + "It will be synced on the next inventory report");
                }
            }
        } finally {
            inventoryLock.writeLock().unlock();
        }
        // Scan for new child Resources...
        performServiceScan(rootResource.getId()); // NOTE: This will block.
        performAvailabilityChecks(true); // NOTE: And this will not.
    }

    private void syncSchedulesRecursively(Resource resource) {
        if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
            if (ResourceCategory.PLATFORM == resource.getResourceType().getCategory()) {
                // Get and schedule the latest measurement schedules rooted at the given id
                // This should include disabled schedules to make sure that previously enabled schedules are shut off
                Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
                    .getMeasurementServerService().getLatestSchedulesForResourceId(resource.getId(), false);
                installSchedules(scheduleRequests);
                for (Resource child : resource.getChildResources()) {
                    scheduleRequests = configuration.getServerServices().getMeasurementServerService()
                        .getLatestSchedulesForResourceId(child.getId(), true);
                    installSchedules(scheduleRequests);
                }
            } else {
                Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
                    .getMeasurementServerService().getLatestSchedulesForResourceId(resource.getId(), true);
                installSchedules(scheduleRequests);
            }
        }
    }

    private void syncAlertTemplatesRecursively(Resource resource) {
        if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
            if (ResourceCategory.PLATFORM == resource.getResourceType().getCategory()) {
                configuration.getServerServices().getDiscoveryServerService().applyAlertTemplate(resource.getId(), false);
                for (Resource child : resource.getChildResources()) {
                    configuration.getServerServices().getDiscoveryServerService().applyAlertTemplate(child.getId(), true);
                }
            } else {
                configuration.getServerServices().getDiscoveryServerService().applyAlertTemplate(resource.getId(), true);
            }
        }
    }

    private void installSchedules(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        if (PluginContainer.getInstance().getMeasurementManager() != null) {
            PluginContainer.getInstance().getMeasurementManager().scheduleCollection(scheduleRequests);
        } else {
            // MeasurementManager hasn't been started yet
            for (ResourceMeasurementScheduleRequest resourceRequest : scheduleRequests) {
                ResourceContainer resourceContainer = getResourceContainer(resourceRequest.getResourceId());
                resourceContainer.setMeasurementSchedule(resourceRequest.getMeasurementSchedules());
            }
        }
    }

    /**
     * Calling this method will immediately perform an availability check on all inventories resources. The availability
     * checks will be made asynchronously; this method will not block.
     *
     * @param sendFullReport if <code>true</code>, the availability report that is sent will contain availability
     *                       records for all resources; if <code>false</code> the report will only contain records for
     *                       those resources whose availability changed from their last known state.
     */
    private void performAvailabilityChecks(boolean sendFullReport) {
        if (sendFullReport) {
            availabilityExecutor.sendFullReportNextTime();
        }

        availabilityThreadPoolExecutor.schedule((Runnable) availabilityExecutor, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Instructs the inventory manager to notify the specified listener of inventory change events.
     *
     * @param listener instance to notify of change events
     */
    public void addInventoryEventListener(InventoryEventListener listener) {
        this.inventoryEventListeners.add(listener);
    }

    /**
     * Removes the specified listener from notification of inventory change events.
     *
     * @param listener instance to remove from event notification
     */
    public void removeInventoryEventListener(InventoryEventListener listener) {
        this.inventoryEventListeners.remove(listener);
    }

    /**
     * Notifies all inventory listeners that the specified resources have been added to the inventory.
     *
     * @param resources resources that were added to trigger this event; will not fire an event if this is <code>
     *                  null</code>
     */
    void fireResourcesAdded(Set<Resource> resources) {
        if (resources == null) {
            return;
        }

        InventoryEventListener[] iteratorSafeListeners = new InventoryEventListener[inventoryEventListeners.size()];
        iteratorSafeListeners = inventoryEventListeners.toArray(iteratorSafeListeners);
        for (InventoryEventListener listener : iteratorSafeListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourcesAdded(resources);
            } catch (Throwable t) {
                log.error("Error while invoking resources added event on listener", t);
            }
        }
    }

    void fireResourceActivated(Resource resource) {
        if ((resource == null) || (resource.getId() == 0)) {
            log.debug("Not firing activated event for resource: " + resource);
            return;
        }

        log.debug("Firing activated for resource: " + resource);

        InventoryEventListener[] iteratorSafeListeners = new InventoryEventListener[inventoryEventListeners.size()];
        iteratorSafeListeners = inventoryEventListeners.toArray(iteratorSafeListeners);
        for (InventoryEventListener listener : iteratorSafeListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourceActivated(resource);
            } catch (Throwable t) {
                log.error("Error while invoking resource activated event on listener", t);
            }
        }
    }

    /**
     * Notifies all inventory listeners that the specified resources have been removed from the inventory.
     *
     * @param resources resources that were removed to trigger this event; will not fire an event if this is <code>
     *                  null</code>
     */
    void fireResourcesRemoved(Set<Resource> resources) {
        if (resources == null) {
            return;
        }

        InventoryEventListener[] iteratorSafeListeners = new InventoryEventListener[inventoryEventListeners.size()];
        iteratorSafeListeners = inventoryEventListeners.toArray(iteratorSafeListeners);
        for (InventoryEventListener listener : iteratorSafeListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourcesRemoved(resources);
            } catch (Throwable t) {
                log.error("Error while invoking resources removed event on listener", t);
            }
        }
    }

    public void enableServiceScans(int serverResourceId, Configuration config) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO: Implement this method.
    }

    public void disableServiceScans(int serverResourceId) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO: Implement this method.
    }

    @Nullable
    private EventContext getEventContext(Resource resource) {
        EventContext eventContext;
        if (resource.getResourceType().getEventDefinitions() != null
            && !resource.getResourceType().getEventDefinitions().isEmpty()) {
            eventContext = new EventContextImpl(resource);
        } else {
            eventContext = null;
        }
        return eventContext;
    }

    private OperationContext getOperationContext(Resource resource) {
        if (resource.getResourceType().getOperationDefinitions() == null
            || resource.getResourceType().getOperationDefinitions().isEmpty()) {
            return null;
        }

        if (resource.getId() == 0) {
            log.warn("RESOURCE ID IS 0! Operation features may not work - resource needs to be synced with server");
        }

        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationServices operationServices = new OperationServicesAdapter(operationManager);
        OperationContext operationContext = new OperationContextImpl(resource.getId(), operationServices);
        return operationContext;
    }

    private ContentContext getContentContext(Resource resource) {
        if (resource.getResourceType().getPackageTypes() == null
            || resource.getResourceType().getPackageTypes().isEmpty()) {
            return null;
        }

        if (resource.getId() == 0) {
            log.warn("RESOURCE ID IS 0! Content features may not work - resource needs to be synced with server");
        }

        ContentServices cm = PluginContainer.getInstance().getContentManager();
        ContentContext contentContext = new ContentContextImpl(resource.getId(), cm);
        return contentContext;
    }

    private ResourceComponent<?> createTestPlatformComponent() {
        return new ResourceComponent() {
            public AvailabilityType getAvailability() {
                return AvailabilityType.UP;
            }

            public void start(ResourceContext context) {
            }

            public void stop() {
            }
        };
    }

    private void updateResourceVersion(Resource resource, String version) {
        String existingVersion = resource.getVersion();
        boolean versionChanged = (existingVersion != null) ? !existingVersion.equals(version) : version != null;
        if (versionChanged) {
            log.debug("Discovery reported that version of " + resource + " changed from '" + existingVersion + "' to '"
                + version + "'.");
            boolean versionShouldBeUpdated = resource.getInventoryStatus() != InventoryStatus.COMMITTED
                || updateResourceVersionOnServer(resource, version);
            if (versionShouldBeUpdated) {
                resource.setVersion(version);
                log.info("Version of " + resource + " changed from '" + existingVersion + "' to '" + version + "'.");
            }
        }
    }

    private boolean updateResourceVersionOnServer(Resource resource, String newVersion) {
        boolean versionUpdated = false;
        ServerServices serverServices = this.configuration.getServerServices();
        if (serverServices != null) {
            try {
                DiscoveryServerService discoveryServerService = serverServices.getDiscoveryServerService();
                discoveryServerService.updateResourceVersion(resource.getId(), newVersion);
                // Only update the version in local inventory if the server sync succeeded, otherwise we won't know
                // to try again the next time this method is called.
                versionUpdated = true;
                if (log.isDebugEnabled()) {
                    log.debug("New version for " + resource + " (" + newVersion
                        + ") was successfully synced to the Server.");
                }
            } catch (Exception e) {
                log.error("Failed to sync-to-Server new version for " + resource + ".");
            }
            // TODO: It would be cool to publish a Resource-version-changed Event here. (ips, 02/29/08)
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sync-to-Server of new version for " + resource
                    + " cannot be done, because Plugin Container is not connected to Server.");
            }
        }
        return versionUpdated;
    }

    private void processSyncInfo(ResourceSyncInfo syncInfo, Set<Resource> syncedResources,
        Set<Integer> unknownResourceIds, Set<Integer> modifiedResourceIds, Set<String> allUuids) {
        long startTime = System.currentTimeMillis();
        boolean initialCall = allUuids.isEmpty();
        if (initialCall)
            log.debug("Processing Server sync info...");
        allUuids.add(syncInfo.getUuid());
        ResourceContainer container = this.resourceContainers.get(syncInfo.getUuid());
        if (container == null) {
            // Either a manually added Resource or just something we haven't discovered.
            unknownResourceIds.add(syncInfo.getId());
        } else {
            Resource resource = container.getResource();
            if (resource.getId() == 0) {
                // This must be a Resource we just reported to the server. Just update its id, mtime, and status.
                resource.setId(syncInfo.getId());
                resource.setMtime(syncInfo.getMtime());
                resource.setInventoryStatus(syncInfo.getInventoryStatus());
                refreshResourceComponentState(container, true);
                syncedResources.add(resource);
            } else {
                // It's a resource that was already synced at least once.
                if (resource.getId() != syncInfo.getId()) {
                    // This really should never happen, but check for it just to be bulletproof.
                    log.error("PC Resource id (" + resource.getId() + ") does not match Server Resource id ("
                        + syncInfo.getId() + ") for Resource with uuid " + resource.getUuid() + ": " + resource);
                    modifiedResourceIds.add(syncInfo.getId());
                }
                // See if it's been modified on the Server since the last time we synced.
                else if (resource.getMtime() < syncInfo.getMtime()) {
                    modifiedResourceIds.add(resource.getId());
                } else {
                    // Only try to start up the component if the Resource has *not* been modified on the Server.
                    // Otherwise, hold off until we've synced the Resource with the Server.
                    refreshResourceComponentState(container, false);
                }
            }

            // Recurse...
            for (ResourceSyncInfo childSyncInfo : syncInfo.getChildSyncInfos()) {
                processSyncInfo(childSyncInfo, syncedResources, unknownResourceIds, modifiedResourceIds, allUuids);
            }
        }
        if (initialCall && log.isDebugEnabled()) {
            log.debug(String.format(
                "DONE Processing sync info - took %d ms. Processed %d Resources - synced %d Resources "
                    + "- found %d unknown Resources and %d modified Resources.",
                (System.currentTimeMillis() - startTime), allUuids.size(), syncedResources.size(), unknownResourceIds
                    .size(), modifiedResourceIds.size()));
        }
    }

    private void mergeModifiedResources(Set<Integer> modifiedResourceIds) {
        log.debug("Merging " + modifiedResourceIds.size() + " modified Resources into local inventory...");
        Set<Resource> modifiedResources = configuration.getServerServices().getDiscoveryServerService().getResources(
            modifiedResourceIds, false);
        for (Resource modifiedResource : modifiedResources) {
            mergeResource(modifiedResource);
        }
    }

    private void mergeUnknownResources(Set<Integer> unknownResourceIds) {
        log.debug("Merging " + unknownResourceIds.size()
            + " unknown Resources and their descendants into local inventory...");
        Set<Resource> unknownResources = configuration.getServerServices().getDiscoveryServerService().getResources(
            unknownResourceIds, true);
        for (Resource unknownResource : unknownResources) {
            mergeResource(unknownResource);
            syncSchedulesRecursively(unknownResource);
        }
    }

    private void mergeResource(Resource resource) {
        log.debug("Merging " + resource + " into local inventory...");
        Resource parentResource;
        if (resource.getParentResource() != null) {
            ResourceContainer parentResourceContainer = getResourceContainer(resource.getParentResource());
            if (parentResourceContainer == null) {
                parentResourceContainer = getResourceContainer(resource.getParentResource().getId());
            }
            parentResource = parentResourceContainer.getResource();
        } else {
            parentResource = null;
        }
        Resource existingResource = findMatchingChildResource(resource, parentResource);
        if (parentResource == null && existingResource == null) {
            // This should never happen, but add a check so we'll know if it ever does.
            log.error("Existing platform " + this.platform + " has different Resource type and/or Resource key than "
                + "platform in Server inventory " + resource);
        }
        ResourceContainer resourceContainer;
        boolean pluginConfigUpdated = false;
        this.inventoryLock.writeLock().lock();
        try {
            if (existingResource != null) {
                // First grab the existing Resource's container, so we can reuse it.
                resourceContainer = this.resourceContainers.remove(existingResource.getUuid());
                if (resourceContainer != null) {
                    this.resourceContainers.put(resource.getUuid(), resourceContainer);
                }
                if (parentResource != null) {
                    // It's critical to remove the existing Resource from the parent's child Set if the UUID has
                    // changed (i.e. altering the hashCode of an item in a Set == BAD), so just always remove it.
                    parentResource.removeChildResource(existingResource);
                }
                // Now merge the new Resource into the existing Resource...
                pluginConfigUpdated = mergeResource(resource, existingResource);
                resource = existingResource;
            }
            resourceContainer = initResourceContainer(resource);
            if (parentResource != null) {
                parentResource.addChildResource(resource);
            } else {
                this.platform = resource;
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
        // Replace the stripped-down ResourceType that came from the Server with the full ResourceType - it's
        // critical to do this before refreshing the state (i.e. calling start on the ResourceComponent).
        ResourceType fullResourceType = this.pluginManager.getMetadataManager().getType(resource.getResourceType());
        if (fullResourceType == null) {
            log.error("Unable to merge Resource " + resource + " - its type is unknown - perhaps the '"
                + resource.getResourceType().getPlugin()
                + "' plugin jar was manually removed from the Server's rhq-plugins dir?");
            return;
        }
        resource.setResourceType(fullResourceType);
        refreshResourceComponentState(resourceContainer, pluginConfigUpdated);

        // Recurse...
        Set<Resource> childResources = new HashSet(resource.getChildResources()); // wrap in new HashSet to avoid CMEs
        for (Resource childResource : childResources) {
            mergeResource(childResource);
        }
    }

    private boolean mergeResource(Resource sourceResource, Resource targetResource) {
        targetResource.setId(sourceResource.getId());
        if (targetResource.getId() != 0 && targetResource.getId() != sourceResource.getId()) {
            log.warn("Id for " + targetResource + " changed from [" + targetResource.getId() + "] to ["
                + sourceResource.getId() + "].");
        }
        targetResource.setUuid(sourceResource.getUuid());
        if (!targetResource.getResourceKey().equals(sourceResource.getResourceKey())) {
            log.warn("Resource key for " + targetResource + " changed from [" + targetResource.getResourceKey()
                + "] to [" + sourceResource.getResourceKey() + "].");
        }
        targetResource.setResourceKey(sourceResource.getResourceKey());
        targetResource.setResourceType(sourceResource.getResourceType());
        targetResource.setMtime(sourceResource.getMtime());
        targetResource.setInventoryStatus(sourceResource.getInventoryStatus());
        boolean pluginConfigUpdated = (!targetResource.getPluginConfiguration().equals(
            sourceResource.getPluginConfiguration()));
        targetResource.setPluginConfiguration(sourceResource.getPluginConfiguration());
        targetResource.setName(sourceResource.getName());
        targetResource.setDescription(sourceResource.getDescription());
        targetResource.setLocation(sourceResource.getLocation());
        return pluginConfigUpdated;
    }

    private void purgeObsoleteResources(Set<String> allUuids) {
        // Remove previously synchronized Resources that no longer exist in the Server's inventory...
        log.debug("Purging obsolete Resources...");
        this.inventoryLock.writeLock().lock();
        try {
            int removedResources = 0;
            for (String uuid : this.resourceContainers.keySet()) {
                if (!allUuids.contains(uuid)) {
                    ResourceContainer resourceContainer = this.resourceContainers.get(uuid);
                    Resource resource = resourceContainer.getResource();
                    // Only purge stuff that was synchronized at some point. Other stuff may just be newly discovered.
                    if (resource.getId() != 0) {
                        removeResource(resource.getId());
                        removedResources++;
                    }
                }
            }
            log.debug("Purged " + removedResources + " obsolete Resources.");
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    private void refreshResourceComponentState(ResourceContainer container, boolean pluginConfigUpdated) {
        Resource resource = container.getResource();
        switch (resource.getInventoryStatus()) {
        case COMMITTED: {
            try {
                if (pluginConfigUpdated) {
                    deactivateResource(resource);
                }
                activateResource(resource, container, pluginConfigUpdated);
            } catch (InvalidPluginConfigurationException ipce) {
                sendInvalidPluginConfigurationResourceError(resource, ipce);
                log.warn("Cannot start component for " + resource
                    + " from synchronized merge due to invalid plugin config: " + ipce.getLocalizedMessage());
            } catch (Exception e) {
                log.error("Failed to start component for " + resource + " from synchronized merge.", e);
            }
            break;
        }
        case DELETED: {
            removeResource(resource.getId());
            break;
        }
        }
        container.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
    }

    /**
     * That class implements a listener that gets called when the resource got activated
     * @author hrupp
     *
     */
    class ResourceGotActivatedListener implements InventoryEventListener {

        public void resourceActivated(Resource resource) {
            if (resource != null && resource.getId() > 0) {
                if (log.isDebugEnabled())
                    log.debug("Resource got finally activated, cleaning out config errors " + resource);

                DiscoveryServerService serverService = configuration.getServerServices().getDiscoveryServerService();
                if (serverService != null) {
                    serverService.clearResourceConfigError(resource.getId());
                }
            }
            removeInventoryEventListener(this);
        }

        public void resourcesAdded(Set<Resource> resources) {
            // nothing to do

        }

        public void resourcesRemoved(Set<Resource> resources) {
            // nothing to do

        }

    }
}