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
package org.rhq.enterprise.server.cloud.instance;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Timer;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Agent;

/**
 * @author Joseph Marques
 * @author Jay Shaughnessy
 */
@Local
public interface ServerManagerLocal {

    void scheduleServerHeartbeat();

    void handleHeartbeatTimer(Timer timer);

    /**
     * Persist the fully defined <server>. 
     * 
     * @param server
     * 
     * @return The internal Id of the new Server
     */
    int create(Server server);

    /**
     * Determine the identity (i.e. Server Name) of this server by inspecting the configures system property. This is
     * the mechanism used for a server to determine which server it is in the registered cloud servers.
     * 
     * @return The server name for this server.
     */
    String getIdentity();

    /**
     * At any time an active agent is communicating (either registered with, or connected to) a server in the cloud. Note
     * that an agent that went down unexpectedly may still be referencing a server although it is not actively communicating.
     *  
     * @return The list of Agents referencing this server.
     */
    List<Agent> getAgents();

    /**
     * An Agent can have various status settings {@Link org.rhq.core.domain.resource.Agent}.  The Status, when set, indicates
     * that this agent has some necessary work pending, typically processed by a periodic job.
     * 
     * @return The subset of agents referencing this server that currently have some Status set.  
     */
    List<Integer> getAndClearAgentsWithStatus();

    /**
     * Returns an object representing this server as it is known within the registered cloud of servers.
     *
     * @return object representing this server
     *
     * @throws ServerNotFoundException
     *
     * @see {@link #getIdentity()}
     */
    Server getServer() throws ServerNotFoundException;

    /**
     * Checks current server mode against previous serverMode and takes any state change actions necessary. Note that
     * a server can not be DOWN after this call since the call itself is evidence of the server running. So, this
     * can take care of a server starting up.
     */
    void establishCurrentServerMode();

    /**
     * Updates server mtime to register active heart beat
     */
    void beat();
}
