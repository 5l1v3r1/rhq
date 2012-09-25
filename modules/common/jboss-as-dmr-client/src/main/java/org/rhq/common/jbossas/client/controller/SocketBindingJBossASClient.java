/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convenience methods associated with socket binding management.
 * 
 * @author John Mazzitelli
 */
public class SocketBindingJBossASClient extends JBossASClient {

    public static final String SOCKET_BINDING_GROUP = "socket-binding-group";
    public static final String SOCKET_BINDING = "socket-binding";
    public static final String PORT = "port";
    public static final String PORT_OFFSET = "port-offset";
    public static final String STANDARD_SOCKETS = "standard-sockets";
    public static final String JBOSS_SYSPROP_PORT_OFFSET = "jboss.socket.binding.port-offset";
    public static final String DEFAULT_BINDING_MGMT_NATIVE = "management-native";
    public static final String DEFAULT_BINDING_MGMT_HTTP = "management-http";
    public static final String DEFAULT_BINDING_MGMT_HTTPS = "management-https";
    public static final String DEFAULT_BINDING_AJP = "ajp";
    public static final String DEFAULT_BINDING_HTTP = "http";
    public static final String DEFAULT_BINDING_HTTPS = "https";
    public static final String DEFAULT_BINDING_JACORB = "jacorb";
    public static final String DEFAULT_BINDING_JACORB_SSL = "jacorb-ssl";
    public static final String DEFAULT_BINDING_MESSAGING = "messaging";
    public static final String DEFAULT_BINDING_MESSAGING_THRUPUT = "messaging-throughput";
    public static final String DEFAULT_BINDING_OSGI_HTTP = "osgi-http";
    public static final String DEFAULT_BINDING_REMOTING = "remoting";
    public static final String DEFAULT_BINDING_TXN_RECOVERY_ENV = "txn-recovery-environment";
    public static final String DEFAULT_BINDING_TXN_STATUS_MGR = "txn-status-manager";

    public SocketBindingJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Sets the port offset for the socket bindings found in the standard socket binding group.
     * This will configure the offset to be allowed to be overridden by the standard
     * {@link #JBOSS_SYSPROP_PORT_OFFSET} system property.
     * 
     * @param offset the new port offset
     * @throws Exception 
     */
    public void setStandardPortOffset(int offset) throws Exception {
        setPortOffset(STANDARD_SOCKETS, JBOSS_SYSPROP_PORT_OFFSET, offset);
    }

    /**
     * Sets the port offset for the socket bindings found in the named socket binding group.
     * If sysPropName is null, this simply sets the offset explicitly to the given offset.
     * If sysPropName is not null, this sets the offset to the expression "${sysPropName:offset}".
     * You typically will want to use the standard {@link #JBOSS_SYSPROP_PORT_OFFSET} system property
     * name as the sysPropName to follow the normal out-of-box JBossAS configuration, though you don't have to.
     *
     * @param socketBindingGroupName name of the socket binding group whose port offset is to be set
     * @param sysPropName the name of the system property whose value is to be the port offset
     * @param offset the default port offset if the sysPropName is not defined
     * @throws Exception 
     */
    public void setPortOffset(String socketBindingGroupName, String sysPropName, int offset) throws Exception {
        String offsetValue;
        if (sysPropName != null) {
            offsetValue = "${" + sysPropName + ":" + offset + "}";
        } else {
            offsetValue = String.valueOf(offset);
        }

        Address addr = Address.root().add(SOCKET_BINDING_GROUP, socketBindingGroupName);
        ModelNode request = createWriteAttributeRequest(PORT_OFFSET, offsetValue, addr);
        ModelNode results = execute(request);
        if (!isSuccess(results)) {
            throw new FailureException(results);
        }
        return; // everything is OK
    }

    /**
     * Sets the port number for the named standard socket binding.
     * 
     * @param socketBindingName the name of the standard socket binding whose port is to be set
     * @param port the new port number
     * @throws Exception 
     */
    public void setStandardSocketBindingPort(String socketBindingName, int port) throws Exception {
        setStandardSocketBindingPortExpression(socketBindingName, null, port);
    }

    /**
     * Sets the port number for the named standard socket binding.
     * If sysPropName is null, this simply sets the port number explicitly to the given port number.
     * If sysPropName is not null, this sets the port to the expression "${sysPropName:port}".
     * 
     * @param socketBindingName the name of the standard socket binding whose port is to be set
     * @param sysPropName the name of the system property whose value is to be the port number
     * @param port the default port number if the sysPropName is not defined
     * @throws Exception 
     */
    public void setStandardSocketBindingPortExpression(String socketBindingName, String sysPropName, int port)
        throws Exception {
        setSocketBindingPortExpression(STANDARD_SOCKETS, socketBindingName, sysPropName, port);
    }

    /**
     * Sets the port number for the named socket binding found in the named socket binding group.
     * 
     * @param socketBindingGroupName the name of the socket binding group that has the named socket binding
     * @param socketBindingName the name of the socket binding whose port is to be set
     * @param port the new port number
     * @throws Exception 
     */
    public void setSocketBindingPort(String socketBindingGroupName, String socketBindingName, int port)
        throws Exception {
        setSocketBindingPortExpression(socketBindingGroupName, socketBindingName, null, port);
    }

    /**
     * Sets the port number for the named socket binding found in the named socket binding group.
     * If sysPropName is null, this simply sets the port number explicitly to the given port number.
     * If sysPropName is not null, this sets the port to the expression "${sysPropName:port}".
     * 
     * @param socketBindingGroupName the name of the socket binding group that has the named socket binding
     * @param socketBindingName the name of the socket binding whose port is to be set
     * @param sysPropName the name of the system property whose value is to be the port number
     * @param port the default port number if the sysPropName is not defined
     * @throws Exception 
     */
    public void setSocketBindingPortExpression(String socketBindingGroupName, String socketBindingName,
        String sysPropName, int port) throws Exception {

        String portValue;
        if (sysPropName != null) {
            portValue = "${" + sysPropName + ":" + port + "}";
        } else {
            portValue = String.valueOf(port);
        }

        Address addr = Address.root().add(SOCKET_BINDING_GROUP, socketBindingGroupName, SOCKET_BINDING,
            socketBindingName);
        ModelNode request = createWriteAttributeRequest(PORT, portValue, addr);
        ModelNode results = execute(request);
        if (!isSuccess(results)) {
            throw new FailureException(results);
        }
        return; // everything is OK
    }
}
