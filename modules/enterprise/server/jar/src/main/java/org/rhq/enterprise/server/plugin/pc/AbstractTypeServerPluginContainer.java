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

package org.rhq.enterprise.server.plugin.pc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The abstract superclass for all plugin containers of the different plugin types.
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractTypeServerPluginContainer {

    private final Log log = LogFactory.getLog(this.getClass());

    private final MasterServerPluginContainer master;

    /**
     * Instantiates the plugin container.
     * 
     * @param master the master plugin container that is creating this instance.
     */
    public AbstractTypeServerPluginContainer(MasterServerPluginContainer master) {
        this.master = master;
    }

    /**
     * Returns the master plugin container that is responsible for managing this instance.
     * 
     * @return this plugin container's master
     */
    public MasterServerPluginContainer getMasterServerPluginContainer() {

        return this.master;
    }

    /**
     * The initialize method that all plugin container subclasses must implement in order to initialize
     * themselves.
     * 
     * @throws Exception if the plugin container failed to initialize for some reason
     */
    public abstract void initialize() throws Exception;

    /**
     * The shutdown method that all plugin container subclasses must implement in order to shutdown
     * themselves.
     */
    public abstract void shutdown();

    /**
     * Returns the logger that can be used to log messages. A convienence object so all
     * subclasses don't have to explicitly declare and create their own.
     * 
     * @return this instance's logger object
     */
    protected Log getLog() {
        return this.log;
    }
}
