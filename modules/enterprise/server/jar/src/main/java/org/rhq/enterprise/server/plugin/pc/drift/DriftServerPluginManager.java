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
package org.rhq.enterprise.server.plugin.pc.drift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;

/**
 * This loads in all drift server plugins that can be found. You can obtain a loaded plugin's
 * {@link ServerPluginEnvironment environment}, including its classloader, from this object as well.
 *
 * @author Jay Shaughnessy 
 * @author John Sanda
 */
public class DriftServerPluginManager extends ServerPluginManager {
    private final Log log = LogFactory.getLog(this.getClass());

    public DriftServerPluginManager(DriftServerPluginContainer pc) {
        super(pc);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public synchronized void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        super.loadPlugin(env, enabled);

    }

    @Override
    protected void startPlugin(String pluginName) {
        super.startPlugin(pluginName);

    }

    /**
     * All of the plugins have been loaded, so now let the metadata manager sort through the definitions.
     *
     * @see org.rhq.enterprise.server.plugin.pc.ServerPluginManager#startPlugins()
     */
    @Override
    public synchronized void startPlugins() {
        super.startPlugins();

    }

    @Override
    protected void stopPlugin(String pluginName) {

        super.stopPlugin(pluginName);
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.plugin.pc.ServerPluginManager#stopPlugins()
     */
    @Override
    public synchronized void stopPlugins() {

        super.stopPlugins();
    }

    /* At unload-time ensure that any WAR files packaged with the server plugin are un-deployed on
     * the RHQ Server.
     * 
     * @see org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer#unloadPlugin(org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment)
     */
    @Override
    public synchronized void unloadPlugin(String pluginName) throws Exception {
        // ServerPluginEnvironment env = getPluginEnvironment(pluginName);

        super.unloadPlugin(pluginName);
    }
}