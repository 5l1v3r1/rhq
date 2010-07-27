package org.rhq.core.pc.inventory;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.inventory.ChildResourceTypeDiscoveryFacet;
import org.rhq.core.util.exception.ThrowableUtil;

public class ChildResourceTypeDiscoveryRunner implements Callable<Set<ResourceType>>, Runnable {

    private Log log = LogFactory.getLog(ChildResourceTypeDiscoveryRunner.class);

    //TODO: Maybe will be needed for later purpose
    //private MeasurementManager measurementManager;
    //private int resourceId;

    //TODO: maybe to be implemented for later usage    
    //    public ChildResourceTypeDiscoveryRunner(MeasurementManager measurementManager) {
    //       
    //    }

    //    public ChildResourceTypeDiscoveryRunner(int resourceId) {
    //        this.resourceId = resourceId;
    //    }

    //Default Ctor
    public ChildResourceTypeDiscoveryRunner() {

    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Could not get measurement report.", e);
        }
    }

    public Set<ResourceType> call() {

        log.info("<ChildResourceTypeDiscoveryRunner>call() called");
        Set<ResourceType> resourceTypes = null;

        long start = System.currentTimeMillis();

        InventoryManager im = PluginContainer.getInstance().getInventoryManager();
        log.info("InventoryManager instance created");

        //TODO: Do testing and split code to more than one method if it works well 
        // Run a full scan for all resources in the inventory

        Resource platform = im.getPlatform();
        log.info("Platform returned with name: " + platform.getName());

        // Next discover all other services and non-top-level servers
        Set<Resource> servers = platform.getChildResources();
        log.info("Platform " + platform.getName() + " has " + servers.size() + " ChildResources");

        if (servers != null) {
            for (Resource server : servers) {
                log.info("Name of server: " + server.getName());
                log.info("Id of server: " + server.getId());
			log.info("Category of server: " + server.getResourceType().getCategory().toString());

                //Check if really is of Category SERVER
                if (server.getResourceType().getCategory() == ResourceCategory.SERVER) {

			log.info("Server " + server.getName() + " has passed the CategoryTest succesfull");

                    //check if child resource implements the interface ChildResourceTypeDiscoveryFacet
                    if (server instanceof ChildResourceTypeDiscoveryFacet) {

                        log.info("Server " + server.getName() + " is instanceof ChildResourceTypeDiscoveryFacet");
                        //Get ResourceContainer for each server instance

                        ResourceContainer container = im.getResourceContainer(server.getId());

                        log.info("Server " + server.getName() + " is running in ResourceContainer "
                            + container.toString());

                        if (container.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED
                            || container.getAvailability() == null
                            || container.getAvailability().getAvailabilityType() == AvailabilityType.DOWN) {
                            // Don't collect metrics for resources that are down
                            //if (log.isDebugEnabled()) {
                            log.info("ChildType not discoverd for inactive resource component: "
                                + container.getResource());
                            //}
                        } else {

                            try {

                                ChildResourceTypeDiscoveryFacet discoveryComponent = ComponentUtil.getComponent(server
                                    .getId(), ChildResourceTypeDiscoveryFacet.class, FacetLockType.READ, 30 * 1000,
                                    true, true);

                                //get Set<ResourceType> --> all the Services which are running under the specific server
                                resourceTypes = discoverChildResourceTypes(discoveryComponent);

                                //Iterate over all the ResourceTypes contained in the Set
                                for (ResourceType type : resourceTypes) {

                                    //Create a new ResourceType in the DB for the selected type 
                                    im.createNewResourceType(type.getName(), type.getName() + "Metric");
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Error submitting service scan", e);
                            }
                        }
                    }
                }
            }

        } else {
            log.info("Set<ResourceType> was returned with value null");
        }

        return null;
    }

    /**
     * 
     * @param discoveryComponent
     * @return
     */
    private Set<ResourceType> discoverChildResourceTypes(ChildResourceTypeDiscoveryFacet discoveryComponent) {

        Set<ResourceType> resourceTypes = null;
        try {
            long start = System.currentTimeMillis();
            resourceTypes = discoveryComponent.discoverChildResourceTypes();
            long duration = (System.currentTimeMillis() - start);

            if (duration > 2000) {
                log.info("[PERF] Discovery of childResourceTypes for [" + discoveryComponent + "] took [" + duration
                    + "ms]");
            }
        } catch (Throwable t) {

            log.warn("Failure to discover childResourceType data - cause: " + ThrowableUtil.getAllMessages(t));
        }

        return resourceTypes;
    }
}
