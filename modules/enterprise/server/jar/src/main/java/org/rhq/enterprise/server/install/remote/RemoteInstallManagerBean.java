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

import java.io.File;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.CustomAgentInstallData;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;

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

    @EJB
    private SystemManagerLocal systemSettingsManager;

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
            // user doesn't want to remember the creds, set them to "" which tells our persistence layer to null them out
            ai.setSshUsername("");
            ai.setSshPassword("");
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
        CustomAgentInstallData data = new CustomAgentInstallData(parentPath, false, null, null);
        return installAgent(subject, remoteAccessInfo, data);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo,
        CustomAgentInstallData customData) {

        try {
            String parentPath = customData.getParentPath();
            boolean agentAlreadyInstalled = agentInstallCheck(subject, remoteAccessInfo, parentPath);
            if (agentAlreadyInstalled) {
                if (!customData.isOverwriteExistingAgent()) {
                    throw new IllegalStateException("Agent appears to already be installed under: " + parentPath);
                } else {
                    // we were asked to overwrite it; make sure we shut it down first before the install happens (which will remove it)
                    stopAgent(subject, remoteAccessInfo, parentPath);
                }
            }

            // we know the uploaded files had to have their contents obfuscated, we need to deobfuscate them
            if (customData.getAgentConfigurationXml() != null) {
                deobfuscateFile(new File(customData.getAgentConfigurationXml()));
            }
            if (customData.getRhqAgentEnv() != null) {
                deobfuscateFile(new File(customData.getRhqAgentEnv()));
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
                return sshUtil.installAgent(customData, String.valueOf(ai.getId()));
            } finally {
                sshUtil.disconnect();
            }
        } finally {
            // don't leave these around - whether we succeeded or failed, its a one-time-chance with these.
            // we want to delete them in case they have some sensitive info
            if (customData.getAgentConfigurationXml() != null) {
                new File(customData.getAgentConfigurationXml()).delete();
            }
            if (customData.getRhqAgentEnv() != null) {
                new File(customData.getRhqAgentEnv()).delete();
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String uninstallAgent(Subject subject, RemoteAccessInfo remoteAccessInfo) {
        String agentName = remoteAccessInfo.getAgentName();
        AgentInstall ai = agentManager.getAgentInstallByAgentName(subject, agentName);
        if (ai == null || ai.getInstallLocation() == null || ai.getInstallLocation().trim().length() == 0) {
            throw new IllegalArgumentException("Agent [" + agentName
                + "] does not have a known install location. For security purposes, the uninstall will not be allowed."
                + " You will have to manually uninstall it from that machine.");
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

        SystemSettings settings = systemSettingsManager.getUnmaskedSystemSettings(false);
        String username = settings.get(SystemSetting.REMOTE_SSH_USERNAME_DEFAULT);
        String password = settings.get(SystemSetting.REMOTE_SSH_PASSWORD_DEFAULT);
        SSHInstallUtility.Credentials creds = null;
        if ((username != null && username.length() > 0) || (password != null && password.length() > 0)) {
            creds = new SSHInstallUtility.Credentials(username, password);
        }

        SSHInstallUtility sshUtil = new SSHInstallUtility(remoteAccessInfo, creds);
        return sshUtil;
    }

    private void deobfuscateFile(File f) {
        if (!f.exists()) {
            throw new RuntimeException("Uploaded file has been purged and no longer available: " + f);
        }

        try {
            FileUtil.decompressFile(f); // we really just compressed it with our special compressor since its faster than obsfucation
        } catch (Exception e) {
            throw new RuntimeException("Cannot unobfuscate uploaded file [" + f + "]", e);
        }
    }
}
