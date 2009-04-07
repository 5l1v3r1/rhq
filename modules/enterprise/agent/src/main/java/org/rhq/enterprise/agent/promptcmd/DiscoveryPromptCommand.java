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
package org.rhq.enterprise.agent.promptcmd;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mazz.i18n.Msg;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Allows the user to ask a plugin to run a discovery just as a means to debug a plugin discovery run.
 *
 * @author John Mazzitelli
 */
public class DiscoveryPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (PluginContainer.getInstance().isStarted()) {
            // strip the first argument, which is the name of our prompt command
            String[] realArgs = new String[args.length - 1];
            System.arraycopy(args, 1, realArgs, 0, args.length - 1);

            // use getAgentName because it is the name of the plugin container
            processCommand(agent.getConfiguration().getAgentName(), realArgs, out);
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_PC_NOT_STARTED));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_DETAILED_HELP);
    }

    private void processCommand(String pcName, String[] args, PrintWriter out) {
        String pluginName = null;
        String resourceTypeName = null;
        boolean verbose = false;

        String sopts = "-p:r:fv";
        LongOpt[] lopts = { new LongOpt("plugin", LongOpt.REQUIRED_ARGUMENT, null, 'p'), //
            new LongOpt("resourceType", LongOpt.REQUIRED_ARGUMENT, null, 'r'), //
            new LongOpt("full", LongOpt.NO_ARGUMENT, null, 'f'), //
            new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v')};

        Getopt getopt = new Getopt("discovery", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return;
            }

            case 'p': {
                pluginName = getopt.getOptarg();
                break;
            }

            case 'r': {
                resourceTypeName = getopt.getOptarg();
                break;
            }

            case 'f': {
                long start = System.currentTimeMillis();
                PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
                PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
                out.println("Full discovery run in " + (System.currentTimeMillis() - start) + "ms");
                return;
            }

            case 'v': {
                verbose = true;
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        try {
            discovery(pcName, out, pluginName, resourceTypeName, verbose);
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_ERROR, ThrowableUtil.getAllMessages(e)));
            return;
        }

        return;
    }

    private void discovery(String pcName, PrintWriter out, String pluginName, String resourceTypeName, boolean verbose)
        throws Exception {
        PluginContainer pc = PluginContainer.getInstance();
        PluginMetadataManager metadataManager = pc.getPluginManager().getMetadataManager();
        Set<ResourceType> typesToDiscover = new HashSet<ResourceType>();

        // make sure the plugin exists first (if one was specified)
        Set<String> allPlugins = metadataManager.getPluginNames();
        if (pluginName != null) {
            if (!allPlugins.contains(pluginName)) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BAD_PLUGIN_NAME, pluginName));
                return;
            }
        }

        // determine which resource types are to be discovered
        Set<ResourceType> allTypes = metadataManager.getAllTypes();
        if (resourceTypeName != null) {
            for (ResourceType type : allTypes) {
                if (type.getName().equals(resourceTypeName)) {
                    if ((pluginName == null) || (pluginName.equals(type.getPlugin()))) {
                        typesToDiscover.add(type);
                    }
                }
            }
        } else {
            // if a plugin was specified, only discover its types; otherwise, discover ALL types
            if (pluginName != null) {
                for (ResourceType type : allTypes) {
                    if (pluginName.equals(type.getPlugin())) {
                        typesToDiscover.add(type);
                    }
                }
            } else {
                typesToDiscover.addAll(allTypes);
            }
        }

        if (typesToDiscover.size() == 0) {
            if (pluginName == null) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BAD_RESOURCE_TYPE_NAME, resourceTypeName));
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_BAD_PLUGIN_RESOURCE_TYPE_NAME, pluginName,
                    resourceTypeName));
            }

            return;
        }

        for (ResourceType typeToDiscover : typesToDiscover) {
            if (typeToDiscover.getCategory().equals(ResourceCategory.SERVER)
                && (typeToDiscover.getParentResourceTypes().size() == 0)) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_DISCOVERING_RESOURCE_TYPE, typeToDiscover
                    .getPlugin(), typeToDiscover.getName()));
                discoveryForSingleResourceType(pcName, out, typeToDiscover, verbose);
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_DISCOVERING_RESOURCE_TYPE_DONE, typeToDiscover
                    .getPlugin(), typeToDiscover.getName()));
                out.println();
            }
        }

        return;
    }

    private void discoveryForSingleResourceType(String pcName, PrintWriter out, ResourceType resourceType,
        boolean verbose) throws Exception {
        // perform auto-discovery PIQL queries now to see if we can auto-detect resources that are running now
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        Set<ProcessScan> processScans = resourceType.getProcessScans();
        if ((processScans != null) && (processScans.size() > 0)) {
            try {
                ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
                if (processScans != null) {
                    for (ProcessScan processScan : processScans) {
                        List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                        if ((queryResults != null) && (queryResults.size() > 0)) {
                            for (ProcessInfo autoDiscoveredProcess : queryResults) {
                                scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_PROCESS_SCAN, resourceType
                                    .getPlugin(), resourceType.getName(), processScan, autoDiscoveredProcess));
                            }
                        }
                    }
                }
            } catch (UnsupportedOperationException uoe) {
                // don't worry if we do not have a native library to support process scans
            }
        }

        PluginComponentFactory componentFactory = PluginContainer.getInstance().getPluginComponentFactory();
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceDiscoveryComponent discoveryComponent = componentFactory.getDiscoveryComponent(resourceType);
        ResourceContainer platformContainer = inventoryManager.getResourceContainer(inventoryManager.getPlatform());
        ResourceComponent platformComponent = inventoryManager.getResourceComponent(inventoryManager.getPlatform());

        ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, platformComponent,
            platformContainer.getResourceContext(), systemInfo, scanResults, Collections.EMPTY_LIST, pcName);

        Set<DiscoveredResourceDetails> discoveredResources = discoveryComponent.discoverResources(context);

        if (discoveredResources != null) {
            for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.DISCOVERY_COMPONENT_RESULT, discoveredResource
                    .getResourceType().getPlugin(), discoveredResource.getResourceType().getName(), discoveredResource
                    .getResourceKey(), discoveredResource.getResourceName(), discoveredResource.getResourceVersion(),
                    discoveredResource.getResourceDescription()));
                if (verbose) {
                    printConfiguration(discoveredResource.getPluginConfiguration(), out);
                }
            }
        }

        return;
    }

    private static void printConfiguration(Configuration config, PrintWriter out) {
        for (Property property : config.getMap().values()) {
            StringBuilder builder = new StringBuilder();
            builder.append("    ");
            builder.append(property.getName());
            builder.append("=");
            if (property instanceof PropertySimple) {
                String value = ((PropertySimple) property).getStringValue();
                builder.append((value != null) ? "\"" + value + "\"" : value);
            } else {
                builder.append(property);
            }
            out.println(builder);
        }
    }
}