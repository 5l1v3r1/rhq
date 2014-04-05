/*
 * RHQ Management Platform
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
package org.rhq.enterprise.server.install.remote;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * Installs, starts and stops remote agents via SSH.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
@Stateless
public class RemoteInstallManagerBean implements RemoteInstallManagerLocal, RemoteInstallManagerRemote {

    @EJB
    private AgentManagerLocal agentManager;

    /**
     * Call this when an SSH session is already connected and this will store the credentials if the user wanted
     * them remembered. If the user did not want them remembers, the credentials will be nulled out from the backend data store.
     *
     * If the session is not connected, nothing will be remembered - this method will just end up as a no-op in that case.
     *
     * @param subject user making the request
     * @param sshSession the session that is currently connected
     */
    private void processRememberMe(Subject subject, SSHInstallUtility sshSession) {
        RemoteAccessInfo remoteAccessInfo = sshSession.getRemoteAccessInfo();
        String agentName = remoteAccessInfo.getAgentName();

        if (agentName == null) {
            return; // nothing we can do, don't know what agent this is for
        }

        boolean credentialsOK = sshSession.isConnected();
        if (!credentialsOK) {
            return; // do not store anything - the credentials are probably bad and why we aren't connected so no sense remembering them
        }

        AgentInstall ai = agentManager.getAgentInstallByAgentName(subject, agentName);
        if (ai == null) {
            ai = new AgentInstall();
            ai.setAgentName(agentName);
        }

        // ai.setSshHost(remoteAccessInfo.getHost()); do NOT change the host
        ai.setSshPort(remoteAccessInfo.getPort());
        if (remoteAccessInfo.getRememberMe()) {
            ai.setSshUsername(remoteAccessInfo.getUser());
            ai.setSshPassword(remoteAccessInfo.getPassword());
        } else {
            // user doesn't want to remember the creds, null them out
            ai.setSshUsername(null);
            ai.setSshPassword(null);
        }

        try {
            agentManager.updateAgentInstall(subject, ai);
        } catch (Exception e) {
            // TODO: I don't think we want to abort this - we don't technically need the install info persisted, user can manually give it again
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            boolean results = sshUtil.agentInstallCheck(agentInstallPath);
            processRememberMe(subject, sshUtil);
            return results;
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        return installAgent(subject, remoteAccessInfo, parentPath, false);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath,
        boolean overwriteExistingAgent) {

        boolean agentAlreadyInstalled = agentInstallCheck(subject, remoteAccessInfo, parentPath);
        if (agentAlreadyInstalled) {
            if (!overwriteExistingAgent) {
                throw new IllegalStateException("Agent appears to already be installed under: " + parentPath);
            } else {
                // we were asked to overwrite it; make sure we shut it down first before the install happens (which will remove it)
                stopAgent(subject, remoteAccessInfo, parentPath);
            }
        }

        // before we install, let's create a AgentInstall and pass its ID
        // as the install ID so the agent can link up with it when it registers.
        AgentInstall agentInstall = new AgentInstall();
        agentInstall.setSshHost(remoteAccessInfo.getHost());
        agentInstall.setSshPort(remoteAccessInfo.getPort());
        if (remoteAccessInfo.getRememberMe()) {
            agentInstall.setSshUsername(remoteAccessInfo.getUser());
            agentInstall.setSshPassword(remoteAccessInfo.getPassword());
        }

        AgentInstall ai = agentManager.updateAgentInstall(subject, agentInstall);
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.installAgent(parentPath, String.valueOf(ai.getId()));
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String uninstallAgent(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        String agentName = remoteAccessInfo.getAgentName();
        AgentInstall ai = agentManager.getAgentInstallByAgentName(subject, agentName);
        if (ai == null || ai.getInstallLocation() == null || ai.getInstallLocation().trim().length() == 0) {
            return null;
        }

        // for security reasons, don't connect to a different machine than where the AgentInstall thinks the agent is.
        // If there is no known host in AgentInstall, then we accept the caller's hostname.
        if (ai.getSshHost() != null && !ai.getSshHost().equals(remoteAccessInfo.getHost())) {
            throw new IllegalArgumentException("Agent [" + agentName + "] is not known to be on host ["
                + remoteAccessInfo.getHost() + "] - aborting uninstall");
        }

        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            return sshUtil.uninstallAgent(ai.getInstallLocation());
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            String results = sshUtil.startAgent(agentInstallPath);
            processRememberMe(subject, sshUtil);
            return results;
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            String results = sshUtil.stopAgent(agentInstallPath);
            processRememberMe(subject, sshUtil);
            return results;
        } finally {
            sshUtil.disconnect();
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String agentStatus(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            String results = sshUtil.agentStatus(agentInstallPath);
            processRememberMe(subject, sshUtil);
            return results;
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String findAgentInstallPath(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            String results = sshUtil.findAgentInstallPath(parentPath);
            processRememberMe(subject, sshUtil);
            return results;
        } finally {
            sshUtil.disconnect();
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath) {
        SSHInstallUtility sshUtil = getSSHConnection(remoteAccessInfo);
        try {
            String[] results = sshUtil.pathDiscovery(parentPath);
            processRememberMe(subject, sshUtil);
            return results;
        } finally {
            sshUtil.disconnect();
        }
    }

    private SSHInstallUtility getSSHConnection(RemoteAccessInfo remoteAccessInfo) {
        if (remoteAccessInfo.getHost() == null) {
            throw new RuntimeException("Enter a host");
        }
        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo);
        return sshUtil;
    }
}
