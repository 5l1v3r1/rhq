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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.enterprise.gui.coregui.client.gwt.RemoteInstallGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class RemoteInstallGWTServiceImpl extends AbstractGWTServiceImpl implements RemoteInstallGWTService {

    private RemoteInstallManagerLocal remoteInstallManager = LookupUtil.getRemoteInstallManager();

    public AgentInstallInfo agentInstallCheck(RemoteAccessInfo remoteAccessInfo) {
        return SerialUtility.prepare(remoteInstallManager.agentInstallCheck(getSessionSubject(), remoteAccessInfo),
                "RemoteInstallService.agentInstallCheck");
    }

    public AgentInstallInfo installAgent(RemoteAccessInfo remoteAccessInfo, String path) {
        return SerialUtility.prepare(remoteInstallManager.installAgent(getSessionSubject(), remoteAccessInfo, path),
                "RemoteInstallService.installAgent");
    }

    public String[] remotePathDiscover(RemoteAccessInfo remoteAccessInfo, String parentPath) {
        return SerialUtility.prepare((remoteInstallManager.remotePathDiscover(getSessionSubject(), remoteAccessInfo, parentPath)),
                "RemoteInstallService.remotePathDiscover");
    }

    public String startAgent(RemoteAccessInfo remoteAccessInfo) {
        return SerialUtility.prepare(remoteInstallManager.startAgent(getSessionSubject(), remoteAccessInfo),
                "RemoteInstallService.startAgent");
    }

    public String stopAgent(RemoteAccessInfo remoteAccessInfo) {
        return SerialUtility.prepare(remoteInstallManager.stopAgent(getSessionSubject(), remoteAccessInfo),
                "RemoteInstallService.stopAgent");
    }

    public String agentStatus(RemoteAccessInfo remoteAccessInfo) {
        return SerialUtility.prepare(remoteInstallManager.agentStatus(getSessionSubject(), remoteAccessInfo),
                "RemoteInstallService.agentStatus");
    }
}
