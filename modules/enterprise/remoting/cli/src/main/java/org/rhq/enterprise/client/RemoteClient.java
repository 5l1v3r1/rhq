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
package org.rhq.enterprise.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ChannelManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.system.ServerVersion;
import org.rhq.enterprise.server.system.SystemManagerRemote;

/**
 * A remote access client with transparent proxies to RHQ servers.
 *
 * @author Greg Hinkle
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class RemoteClient {

    public enum Manager {
        AlertManager(AlertManagerRemote.class), //
        AlertDefinitionManager(AlertDefinitionManagerRemote.class), //
        AvailabilityManager(AvailabilityManagerRemote.class), //
        CallTimeDataManager(CallTimeDataManagerRemote.class), // 
        ChannelManager(ChannelManagerRemote.class), //
        ConfigurationManager(ConfigurationManagerRemote.class), //
        //ContentHelperManager(ContentHelperRemote.class), //
        ContentManager(ContentManagerRemote.class), //
        DataAccessManager(DataAccessManagerRemote.class), //
        DiscoveryBoss(DiscoveryBossRemote.class), //
        EventManager(EventManagerRemote.class), //
        MeasurementBaselineManager(MeasurementBaselineManagerRemote.class), //
        MeasurementDataManager(MeasurementDataManagerRemote.class), //
        MeasurementDefinitionManager(MeasurementDefinitionManagerRemote.class), // 
        MeasurementScheduleManager(MeasurementScheduleManagerRemote.class), //
        OperationManager(OperationManagerRemote.class), //
        ResourceManager(ResourceManagerRemote.class), //
        ResourceFactoryManager(ResourceFactoryManagerRemote.class), //
        ResourceGroupManager(ResourceGroupManagerRemote.class), //
        ResourceTypeManager(ResourceTypeManagerRemote.class), //
        RoleManager(RoleManagerRemote.class), //
        SubjectManager(SubjectManagerRemote.class), //
        SupportManager(SupportManagerRemote.class), //
        SystemManager(SystemManagerRemote.class) //
        //        RemoteInstallManager(RemoteInstallManagerRemote.class),
        ;

        private Class<?> remote;
        private String remoteName;
        private String beanName;

        private Manager(Class<?> remote) {
            this.remote = remote;
            this.beanName = this.name() + "Bean";
            this.remoteName = this.name() + "Remote";
        }

        Class<?> remote() {
            return this.remote;
        }

        public String beanName() {
            return this.beanName;
        }

        public String remoteName() {
            return this.remoteName;
        }
    };

    public static final String NONSECURE_TRANSPORT = "servlet";
    public static final String SECURE_TRANSPORT = "sslservlet";

    private String transport;
    private final String host;
    private final int port;
    private boolean loggedIn;
    private Map<String, Object> managers;
    private Subject subject;
    private Client remotingClient;
    private String subsystem = null;

    /**
     * Creates a client that will communicate with the server running on the given host
     * listening on the given port. This constructor will not attempt to connect or login
     * to the remote server - use {@link #login(String, String)} for that.
     * 
     * @param host
     * @param port
     */
    public RemoteClient(String host, int port) {
        this(null, host, port);
    }

    /**
     * Creates a client that will communicate with the server running on the given host
     * listening on the given port over the given transport.
     * This constructor will not attempt to connect or login
     * to the remote server - use {@link #login(String, String)} for that.
     * 
     * @param transport valid values are "servlet" and "sslservlet" - if <code>null</code>,
     *                  sslservlet will be used for ports that end with "443", servlet otherwise
     * @param host
     * @param port
     */
    public RemoteClient(String transport, String host, int port) {
        this.transport = transport;
        this.host = host;
        this.port = port;
    }

    public RemoteClient(String transport, String host, int port, String subsystem) {
        this(null, host, port);
        this.subsystem = subsystem;
    }

    /**
     * Connects to the remote server and logs in with the given credentials.
     * After successfully executing this, {@link #isConnected()} will be <code>true</code>
     * and {@link #getSubject()} will return the subject that this method returns.
     * 
     * @param user
     * @param password
     *
     * @return the logged in user
     *
     * @throws Exception if failed to connect to the server or log in
     */
    public Subject login(String user, String password) throws Exception {

        logout();
        connect();

        this.subject = getSubjectManagerRemote().login(user, password);
        this.loggedIn = true;

        ServerVersion version = getSystemManagerRemote().getServerVersion(this.subject);
        // TODO: what to do with this?
        System.out.println("Remote server version is: " + version);

        return this.subject;
    }

    /**
     * Logs out from the server and disconnects this client.
     */
    public void logout() {
        try {
            if (this.loggedIn && this.subject != null) {
                getSubjectManagerRemote().logout(this.subject);
            }
        } catch (Exception e) {
            // just keep going so we can disconnect this client
        }

        disconnect();

        this.subject = null;
        this.loggedIn = false;
    }

    /**
     * Returns <code>true</code> if and only if this client successfully connected
     * to the remote server and the user successfully logged in.
     * 
     * @return if the user was able to connect and log into the server
     */
    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    /**
     * Returns the information on the user that is logged in.
     * May be <code>null</code> if the user never logged in successfully.
     * 
     * @return user information or <code>null</code>
     */
    public Subject getSubject() {
        return this.subject;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getTransport() {
        if (this.transport != null) {
            return this.transport;
        } else if (String.valueOf(this.port).endsWith("443")) {
            return SECURE_TRANSPORT;
        } else {
            return NONSECURE_TRANSPORT;
        }
    }

    /**
     * Sets the underlying transport to use to communicate with the server.
     * Available transports are "servlet" and "sslservlet".
     * If you set it to <code>null</code>, then the transport to be used will
     * be set appropriately for the {@link #getPort()} (e.g. a secure transport
     * will be used for ports that end with 443, a non-secure transport will be
     * used for all other ports).
     * 
     * @param transport
     */
    public void setTransport(String transport) {
        this.transport = transport;
    }

    public AlertManagerRemote getAlertManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.AlertManager);
    }

    public AlertDefinitionManagerRemote getAlertDefinitionManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.AlertDefinitionManager);
    }

    public AvailabilityManagerRemote getAvailabilityManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.AvailabilityManager);
    }

    public CallTimeDataManagerRemote getCallTimeDataManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.CallTimeDataManager);
    }

    public ChannelManagerRemote getChannelManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ChannelManager);
    }

    public ConfigurationManagerRemote getConfigurationManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ConfigurationManager);
    }

    public ContentManagerRemote getContentManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ContentManager);
    }

    public DataAccessManagerRemote getDataAccessManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.DataAccessManager);
    }

    public DiscoveryBossRemote getDiscoveryBossRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.DiscoveryBoss);
    }

    public EventManagerRemote getEventManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.EventManager);
    }

    public MeasurementBaselineManagerRemote getMeasurementBaselineManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementBaselineManager);
    }

    public MeasurementDataManagerRemote getMeasurementDataManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementDataManager);
    }

    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementDefinitionManager);
    }

    public MeasurementScheduleManagerRemote getMeasurementScheduleManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementScheduleManager);
    }

    public OperationManagerRemote getOperationManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.OperationManager);
    }

    public ResourceManagerRemote getResourceManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceManager);
    }

    public ResourceFactoryManagerRemote getResourceFactoryManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceFactoryManager);
    }

    public ResourceGroupManagerRemote getResourceGroupManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceGroupManager);
    }

    public ResourceTypeManagerRemote getResourceTypeManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceTypeManager);
    }

    public RoleManagerRemote getRoleManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.RoleManager);
    }

    public SubjectManagerRemote getSubjectManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.SubjectManager);
    }

    public SupportManagerRemote getSupportManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.SupportManager);
    }

    public SystemManagerRemote getSystemManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.SystemManager);
    }

    //    public RemoteInstallManagerRemote getRemoteInstallManagerRemote() {
    //        return RemoteClientProxy.getProcessor(this, Manager.RemoteInstallManager);
    //    }

    /**
     * Returns the map of all remote managers running in the server that this
     * client can talk to.
     * 
     * @return Map K=manager name V=remote proxy
     */
    public Map<String, Object> getManagers() {
        if (this.managers == null) {

            this.managers = new HashMap<String, Object>();

            for (Manager manager : Manager.values()) {
                try {
                    Method m = this.getClass().getMethod("get" + manager.remoteName());
                    this.managers.put(manager.name(), m.invoke(this));
                } catch (Throwable e) {
                    System.out.println("Failed to load manager " + manager + " due to missing class: " + e);
                }
            }
        }

        return this.managers;
    }

    /**
     * Returns the internal JBoss/Remoting client used to perform the low-level
     * comm with the server.
     * 
     * This is package-scoped so the proxy can use it.
     * 
     * @return remoting client used to talk to the server
     */
    Client getRemotingClient() {
        return this.remotingClient;
    }

    private void disconnect() {
        try {
            if (this.remotingClient != null && this.remotingClient.isConnected()) {
                this.remotingClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO what to do here?
        } finally {
            this.remotingClient = null;
        }
    }

    private void connect() throws Exception {
        String locatorURI = getTransport() + "://" + this.host + ":" + this.port
            + "/jboss-remoting-servlet-invoker/ServerInvokerServlet";
        InvokerLocator locator = new InvokerLocator(locatorURI);

        String subsystem = "REMOTEAPI";
        if ((this.subsystem != null) && (this.subsystem.trim().equalsIgnoreCase("WSREMOTEAPI"))) {
            subsystem = "WSREMOTEAPI";
        }
        Map<String, String> remotingConfig = buildRemotingConfig(locatorURI);
        this.remotingClient = new Client(locator, subsystem, remotingConfig);
        this.remotingClient.connect();
    }

    private Map<String, String> buildRemotingConfig(String locatorURI) {
        Map<String, String> config = new HashMap<String, String>();
        if (SecurityUtil.isTransportSecure(locatorURI)) {
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "data/keystore.dat");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "password");
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_PASSWORD, "password");
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_SSL_PROTOCOL, null);
            setConfigProp(config, SSLSocketBuilder.REMOTING_KEY_ALIAS, "self");
            setConfigProp(config, SSLSocketBuilder.REMOTING_SERVER_AUTH_MODE, "false");
            config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");

            // since we do not know the server's client-auth mode, assume we need a keystore and let's make sure we have one
            SSLSocketBuilder dummy_sslbuilder = new SSLSocketBuilder(); // just so we can test finding our keystore
            try {
                // this allows the configured keystore file to be a URL, file path or a resource relative to our classloader
                dummy_sslbuilder.setKeyStoreURL(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH));
            } catch (Exception e) {
                // this probably is due to the fact that the keystore doesn't exist yet - let's prepare one now
                SecurityUtil.createKeyStore(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH), config
                    .get(SSLSocketBuilder.REMOTING_KEY_ALIAS), "CN=RHQ, OU=RedHat, O=redhat.com, C=US", config
                    .get(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD), config
                    .get(SSLSocketBuilder.REMOTING_KEY_PASSWORD), "DSA", 36500);

                // now try to set it again, if an exception is still thrown, it's an unrecoverable error
                dummy_sslbuilder.setKeyStoreURL(config.get(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH));
            }

            // in case the transport floats over https - we want to make sure a hostname verifier is installed and allows all hosts
            config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
        }
        return config;
    }

    /**
     * Looks up the prop name in system properties and puts the value in the map. If the property
     * isn't set, the given default is used. If the given default is null and the property isn't
     * set, then the map is not populated.
     * 
     * @param configMap
     * @param propName
     * @param defaultValue
     */
    private void setConfigProp(Map<String, String> configMap, String propName, String defaultValue) {
        String propValue = System.getProperty(propName, defaultValue);
        if (propValue != null) {
            configMap.put(propName, propValue);
        }
        return;
    }
}