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

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.communications.GlobalSuspendCommandListener;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * If you want to manipulate or report on the {@link Server} instance that
 * some piece of code is currently executing on, use the {@link ServerManagerBean}.
 * 
 * This session bean determines the identity of the server it's running on by
 * reading the <code>rhq.server.high-availability.name</code> property from the
 * rhq-server.properties file.
 * 
 * The functionality provided here is useful when you need to execute something
 * on every server in the cloud, such as partitioned services and data.
 * 
 * @author Joseph Marques
 */
@Stateless
public class ServerManagerBean implements ServerManagerLocal {
    private final Log log = LogFactory.getLog(ServerManagerBean.class);

    static private final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";

    static private Server.OperationMode lastEstablishedServerMode = null;

    @Resource
    private TimerService timerService;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private CloudManagerLocal cloudManager;

    @EJB
    private StatusManagerLocal agentStatusManager;

    private final String TIMER_DATA = "ServerManagerBean.beat";

    @SuppressWarnings("unchecked")
    public void scheduleServerHeartbeat() {
        /* each time the webapp is reloaded, it would create 
         * duplicate events if we don't cancel the existing ones
         */
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            log.debug("Found timer - attempting to cancel: " + existingTimer.toString());
            try {
                existingTimer.cancel();
            } catch (Exception e) {
                log.warn("Failed in attempting to cancel timer: " + existingTimer.toString());
            }
        }
        // single-action timer that will trigger in 30 seconds
        timerService.createTimer(30000, TIMER_DATA);
    }

    @Timeout
    public void handleHeartbeatTimer(Timer timer) {
        try {
            beat();
        } finally {
            // reschedule ourself to trigger in another 30 seconds
            timerService.createTimer(30000, TIMER_DATA);
        }

    }

    public int create(Server server) {
        entityManager.persist(server);
        return server.getId();
    }

    public String getIdentity() {
        String identity = System.getProperty(RHQ_SERVER_NAME_PROPERTY, "");
        if (identity.equals("")) {
            return "localhost";
        }
        return identity;
    }

    public List<Agent> getAgents() {
        String identity = getIdentity();
        List<Agent> results = cloudManager.getAgentsByServerName(identity);
        return results;
    }

    public List<Integer> getAndClearAgentsWithStatus() {
        List<Integer> results = agentStatusManager.getAndClearAgentsWithStatusForServer(getIdentity());
        return results;
    }

    public boolean getAndClearServerStatus() {
        String identity = getIdentity();
        Server server = cloudManager.getServerByName(identity);
        if (server == null) {
            return false; // don't reload caches if we don't know who we are
        }
        boolean hadStatus = (server.getStatus() != 0);
        server.clearStatus();
        return hadStatus;
    }

    public Server getServer() throws ServerNotFoundException {
        String identity = getIdentity();
        Server result = cloudManager.getServerByName(identity);
        if (result == null) {
            throw new ServerNotFoundException("Could not find server; is the " + RHQ_SERVER_NAME_PROPERTY
                + " property set in rhq-server.properties?");
        }
        return result;
    }

    public void establishCurrentServerMode() {
        Server server = getServer();
        Server.OperationMode serverMode = server.getOperationMode();

        // no state change means no work
        if (serverMode == lastEstablishedServerMode)
            return;

        // whenever starting up clear the agent references to this server. Agent references will exist
        // for previously connected agents that did not fail-over while this server was unavailable. This
        // is done to avoid unnecessary cache re/load and moreover provides a logically initialized environment.
        if (null == lastEstablishedServerMode) {
            clearAgentReferences(server);
        }

        try {
            if (Server.OperationMode.NORMAL == serverMode) {

                // If moving into normal operating mode from Maintenance Mode then:
                // 1) Ensure lingering agent references are cleared
                //    - this may have been done at startup already, this covers the case when we go in and
                //    - out of MM without ever taking down the server
                // 2) Re-establish server communication by taking away the MM listener
                if (Server.OperationMode.MAINTENANCE == lastEstablishedServerMode) {
                    clearAgentReferences(server);

                    ServerCommunicationsServiceUtil.getService().safeGetServiceContainer().removeCommandListener(
                        getMaintenanceModeListener());

                    log.info("Notified communication layer of server operation mode " + serverMode);
                }
            } else if (Server.OperationMode.MAINTENANCE == serverMode) {

                // If moving into Maintenance Mode from any other mode then stop processing agent commands
                ServerCommunicationsServiceUtil.getService().safeGetServiceContainer().addCommandListener(
                    getMaintenanceModeListener());

                log.info("Notified communication layer of server operation mode " + serverMode);

            } else if (Server.OperationMode.INSTALLED == serverMode) {

                // The server must have just been installed and must be coming for the first time
                // up as of this call. So, update the mode to NORMAL and update mtime as an initial heart beat.
                // This will prevent a running CloudManagerJob from resetting to DOWN before the real
                // ServerManagerJob starts updating the heart beat regularly.
                lastEstablishedServerMode = serverMode;
                serverMode = Server.OperationMode.NORMAL;
                server.setOperationMode(serverMode);
                server.setMtime(System.currentTimeMillis());

            } else if (Server.OperationMode.DOWN == serverMode) {

                // The server can't be DOWN if this code is executing, it means the server must be coming
                // up as of this call. So, update the mode to NORMAL and update mtime as an initial heart beat.
                // This will prevent a running CloudManagerJob from resetting to DOWN before the real
                // ServerManagerJob starts updating the heart beat regularly.
                lastEstablishedServerMode = serverMode;
                serverMode = Server.OperationMode.NORMAL;
                server.setOperationMode(serverMode);
                server.setMtime(System.currentTimeMillis());
            }

            // If this server just transitioned from INSTALLED to NORMAL operation mode then it 
            // has just been added to the cloud. Changing the number of servers in the cloud requires agent 
            // distribution work, even if this is a 1-Server cloud. Generate a request for a repartitioning
            // of agent load, it will be executed on the next invocation of the cluster manager job.
            // Otherwise, audit the operation mode change as a partition event of interest.
            String audit = server.getName() + ": "
                + ((null != lastEstablishedServerMode) ? lastEstablishedServerMode : Server.OperationMode.DOWN)
                + " --> " + serverMode;

            if ((Server.OperationMode.NORMAL == serverMode)
                && (Server.OperationMode.INSTALLED == lastEstablishedServerMode)) {

                LookupUtil.getPartitionEventManager().cloudPartitionEventRequest(
                    LookupUtil.getSubjectManager().getOverlord(), PartitionEventType.OPERATION_MODE_CHANGE, audit);
            } else {
                LookupUtil.getPartitionEventManager().auditPartitionEvent(LookupUtil.getSubjectManager().getOverlord(),
                    PartitionEventType.OPERATION_MODE_CHANGE, audit);
            }

            lastEstablishedServerMode = serverMode;

        } catch (Exception e) {
            log.error("Unable to change HA Server Mode from " + lastEstablishedServerMode + " to " + serverMode + ": "
                + e);
        }
    }

    private void clearAgentReferences(Server server) {
        Query query = entityManager.createNamedQuery(Agent.QUERY_REMOVE_SERVER_REFERENCE);
        query.setParameter("serverId", server.getId());
        int numRows = query.executeUpdate();
        if (numRows > 0) {
            log.info("Removed " + numRows + " obsolete agent reference(s) to server " + server.getName());
        }
    }

    // use this to ensure a listener of the same name. not using static singleton in case of class reload by different
    // classloaders (in case an exception bubbles up to the slsb layer)
    private GlobalSuspendCommandListener getMaintenanceModeListener() {
        return new GlobalSuspendCommandListener(Server.OperationMode.MAINTENANCE.name(),
            Server.OperationMode.MAINTENANCE.name());
    }

    public void beat() {
        Server server = getServer();
        server.setMtime(System.currentTimeMillis());

        // Handles server mode state changes 
        // note: this call should be fast. if not we need to break the heart beat into its own job
        establishCurrentServerMode();
    }

}
