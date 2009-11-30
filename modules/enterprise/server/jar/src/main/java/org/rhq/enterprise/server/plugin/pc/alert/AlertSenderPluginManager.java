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
package org.rhq.enterprise.server.plugin.pc.alert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.w3c.dom.Element;

import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;

/**
 * Plugin manager that takes care of loading the plug-ins and instantiating
 * of {@link AlertSender} etc.
 * @author Heiko W. Rupp
 */
public class AlertSenderPluginManager extends ServerPluginManager {

    private final Log log = getLog();
    private Map<String,String> pluginNameToType = new HashMap<String, String>();
    private Map<String,String> pluginClassByName = new HashMap<String, String>();
    private Map<String,ServerPluginEnvironment> pluginEnvByName = new HashMap<String, ServerPluginEnvironment>();
    private Map<String,AlertSenderInfo> senderInfoByName = new HashMap<String, AlertSenderInfo>();

    public AlertSenderPluginManager(AbstractTypeServerPluginContainer pc) {
        super(pc);
    }

    /**
     * Postprocess the loading of the plugin - the actual load is done
     * in the super class.
     * Here we verify that the passed &lt;plugin-class&gt; is valid and build the
     * list of plugins that can be queried by the UI etc.
     * @param env the environment of the plugin to be loaded
     *
     * @throws Exception
     */
    @Override
    public void loadPlugin(ServerPluginEnvironment env) throws Exception {
        super.loadPlugin(env);

        AlertPluginDescriptorType type = (AlertPluginDescriptorType) env.getPluginDescriptor();

        String className = ((Element)type.getPluginClass()).getTextContent();
        if (!className.contains(".")) {
            className = type.getPackage() + "." + className;
        }
        try {
            Class.forName(className,false,env.getPluginClassLoader());
        }
        catch (Exception e) {
            log.warn("Can't find pluginClass " + className + ". Plugin will be ignored");
            try {
                unloadPlugin(env);
            }
            catch (Throwable t) {
                log.warn("  +--> unload failed too " + t.getMessage());
            }
            return;
        }
        String shortName = ((Element) type.getShortName()).getTextContent();
        pluginClassByName.put(shortName,className);
        senderInfoByName.put(shortName,new AlertSenderInfo(shortName,"-TODO-",env.getPluginKey()));

        pluginEnvByName.put(shortName,env);

        ConfigurationDescriptor desc = type.getAlertConfiguration();

        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        mgr.handleAlertConfigurationDefinition(null); // TODO
    }

    /**
     * Instantiate an AlertSender for the passed shortName, which is the name you have provided
     * in the plugin descriptor in the &lt;shortName&gt; element
     * @param notification
     * @return a new AlertSender with preferences set
     * @see AlertSender
     */
    public AlertSender getAlertSenderForNotification(AlertNotification notification) {

        String className = pluginClassByName.get(notification.getSenderName());
        ServerPluginEnvironment env = pluginEnvByName.get(notification.getSenderName());
        Class clazz;
        try {
            clazz = Class.forName(className,true,env.getPluginClassLoader());
        }
        catch (Exception e) {
            log.error(e); // TODO
            return null;
        }

        AlertSender sender;
        try {
            sender = (AlertSender) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return null;
        }

        // TODO We have no entityManager lying around here, which means
        // Configuration is an uninitialized Proxy and we'd get a LazyInit
        // Exception later.
        // So lets get a session and attach the stuff... TODO
        ServerPluginContext ctx = getServerPluginContext(env);
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();


        sender.alertParameters = mgr.getAlertPropertiesConfiguration(notification);
        if (sender.alertParameters == null)
            sender.alertParameters = new Configuration(); // Safety measure

        ServerPluginsLocal pluginsMgr = LookupUtil.getServerPlugins();

        PluginKey key = ctx.getPluginEnvironment().getPluginKey();
        ServerPlugin plugin = pluginsMgr.getServerPlugin(key);
        plugin = pluginsMgr.getServerPluginRelationships(plugin);

        sender.preferences = plugin.getPluginConfiguration();
        if (sender.preferences==null)
            sender.preferences = new Configuration(); // Safety measure

        return sender;
    }

    /**
     * Return the list of deployed alert sender plug-ins by their &lt;shortName&gt;
     * @return List of plugin names
     */
    public List<String> getPluginList() {
        return new ArrayList<String>(pluginClassByName.keySet());
    }


    public String getPluginNameForShortName(String shortName) {
        return pluginNameToType.get(shortName);
    }

    public AlertSenderInfo getAlertSenderInfo(String shortName) {
        return senderInfoByName.get(shortName);
    }
}
