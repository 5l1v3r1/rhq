/*
 * RHQ Management
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.bundle;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;

/**
 * Manages the bundle subsystem, which allows bundles of content to be installed. 
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author John Mazzitelli
 */
public class BundleManager extends AgentService implements BundleAgentService, BundleManagerProvider, ContainerService {
    private final Log log = LogFactory.getLog(BundleManager.class);

    private PluginContainerConfiguration configuration;

    public BundleManager() {
        super(BundleAgentService.class);
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void initialize() {
    }

    public void shutdown() {
    }

    public BundleScheduleResponse schedule(BundleScheduleRequest request) {
        BundleScheduleResponse response = new BundleScheduleResponse();

        try {
            BundleDeployDefinition bundleDef = request.getBundleDeployDefinition();

            // find the resource that will handle the bundle processing
            InventoryManager im = PluginContainer.getInstance().getInventoryManager();
            BundleType bundleType = bundleDef.getBundleVersion().getBundle().getBundleType();
            ResourceType resourceType = bundleType.getResourceType();
            Set<Resource> resources = im.getResourcesWithType(resourceType);
            if (resources.isEmpty()) {
                throw new Exception("No bundle plugin supports bundle type [" + bundleType + "]");
            }
            int bundleHandlerResourceId = resources.iterator().next().getId();

            // get the bundle facet object that will process the bundle
            int facetMethodTimeout = 4 * 60 * 60 * 1000; // 4 hours is given to the bundle plugin to do its thing
            BundleFacet bundlePluginComponent = getBundleFacet(bundleHandlerResourceId, facetMethodTimeout);

            // deploy the bundle utilizing the bundle facet object
            BundleDeployRequest deployRequest = new BundleDeployRequest();
            deployRequest.setBundleManagerProvider(this);
            deployRequest.setBundleDeployDefinition(request.getBundleDeployDefinition());
            BundleDeployResult result = bundlePluginComponent.deployBundle(deployRequest);
            if (!result.isSuccess()) {
                response.setErrorMessage(result.getErrorMessage());
            }
        } catch (Throwable t) {
            log.error("Failed to schedule bundle request: " + request, t);
            response.setErrorMessage(t);
        }

        return response;
    }

    public List<PackageVersion> getAllBundleVersionPackageVersions(BundleVersion bundleVersion) throws Exception {
        int bvId = bundleVersion.getId();
        List<PackageVersion> pvs = getBundleServerService().getAllBundleVersionPackageVersions(bvId);
        return pvs;
    }

    public long getFileContent(PackageVersion packageVersion, OutputStream outputStream) throws Exception {
        long size = getBundleServerService().downloadPackageBits(packageVersion, outputStream);
        return size;
    }

    /**
     * If this manager can talk to a server-side {@link BundleServerService}, a proxy to that service is returned.
     *
     * @return the server-side proxy; <code>null</code> if this manager doesn't have a server to talk to
     */
    private BundleServerService getBundleServerService() {
        if (configuration.getServerServices() != null) {
            return configuration.getServerServices().getBundleServerService();
        }

        throw new IllegalStateException("There is no bundle server service available to obtain bundle files");
    }

    /**
     * Given a resource, this obtains that resource's {@link BundleFacet} interface.
     * If the resource does not support that facet, an exception is thrown.
     * The resource must be in the STARTED (i.e. connected) state.
     *
     * @param  resourceId identifies the resource that is to perform the bundle activities
     * @param  timeout    if any facet method invocation thread has not completed after this many milliseconds, interrupt
     *                    it; value must be positive
     *
     * @return the resource's bundle facet interface
     *
     * @throws PluginContainerException on error
     */
    private BundleFacet getBundleFacet(int resourceId, long timeout) throws PluginContainerException {
        return ComponentUtil.getComponent(resourceId, BundleFacet.class, FacetLockType.READ, timeout, false, true);
    }
}
