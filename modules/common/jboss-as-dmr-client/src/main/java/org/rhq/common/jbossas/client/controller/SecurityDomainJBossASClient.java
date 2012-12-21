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

import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convenience methods associated with security domain management.
 *
 * @author John Mazzitelli
 */
public class SecurityDomainJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_SECURITY = "security";
    public static final String SECURITY_DOMAIN = "security-domain";
    public static final String CACHE_TYPE = "cache-type";
    public static final String AUTHENTICATION = "authentication";
    public static final String LOGIN_MODULES = "login-modules";
    public static final String CLASSIC = "classic";
    public static final String CODE = "code";
    public static final String FLAG = "flag";
    public static final String MODULE_OPTIONS = "module-options";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DS_JNDI_NAME = "dsJndiName";
    public static final String PRINCIPALS_QUERY = "principalsQuery";
    public static final String ROLES_QUERY = "rolesQuery";
    public static final String HASH_ALGORITHM = "hashAlgorithm";
    public static final String HASH_ENCODING = "hashEncoding";

    public SecurityDomainJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if there is already a security domain with the given name.
     *
     * @param securityDomainName the name to check
     * @return true if there is a security domain with the given name already in existence
     */
    public boolean isSecurityDomain(String securityDomainName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        String haystack = SECURITY_DOMAIN;
        return null != findNodeInList(addr, haystack, securityDomainName);
    }

    /**
     * Create a new security domain using the SecureIdentity authentication method.
     * This is used when you want to obfuscate a database password in the configuration.
     *
     * @param securityDomainName the name of the new security domain
     * @param username the username associated with the security domain
     * @param password the value of the password to store in the configuration (e.g. the obfuscated password itself)
     *
     * @throws Exception if failed to create security domain
     */
    public void createNewSecureIdentitySecurityDomain(String securityDomainName, String username, String password)
        throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        ModelNode addAuthNode = createRequest(ADD, addr.clone().add(AUTHENTICATION, CLASSIC));
        ModelNode loginModulesNode = addAuthNode.get(LOGIN_MODULES);
        ModelNode loginModule = new ModelNode();
        loginModule.get(CODE).set("SecureIdentity");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.setEmptyList();
        // TODO: we really want to use addExpression (e.g. ${rhq.server.database.user-name})
        // for username and password so rhq-server.properties can be used to set these.
        // However, AS7.1 doesn't support this yet - see https://issues.jboss.org/browse/AS7-5177
        moduleOptions.add(USERNAME, username);
        moduleOptions.add(PASSWORD, password);
        loginModulesNode.add(loginModule);

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * Create a new security domain using the database server authentication method.
     * This is used when you want to directly authenticate against a db entry.
     *
     * @param securityDomainName the name of the new security domain
     * @param dsJndiName the jndi name for the datasource to query against
     * @param principalsQuery the SQL query for selecting password info for a principal
     * @param rolesQuery the SQL query for selecting role info for a principal
     * @param hashAlgorithm if null defaults to "MD5"
     * @param hashEncoding if null defaults to "base64"
     * @throws Exception if failed to create security domain
     */
    public void createNewDatabaseServerSecurityDomain(String securityDomainName, String dsJndiName,
        String principalsQuery, String rolesQuery, String hashAlgorithm, String hashEncoding) throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        ModelNode addAuthNode = createRequest(ADD, addr.clone().add(AUTHENTICATION, CLASSIC));
        ModelNode loginModulesNode = addAuthNode.get(LOGIN_MODULES);
        ModelNode loginModule = new ModelNode();
        loginModule.get(CODE).set("Database");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.setEmptyList();
        moduleOptions.add(DS_JNDI_NAME, dsJndiName);
        moduleOptions.add(PRINCIPALS_QUERY, principalsQuery);
        moduleOptions.add(ROLES_QUERY, rolesQuery);
        moduleOptions.add(HASH_ALGORITHM, (null == hashAlgorithm ? "MD5" : hashAlgorithm));
        moduleOptions.add(HASH_ENCODING, (null == hashEncoding ? "base64" : hashEncoding));
        loginModulesNode.add(loginModule);

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * Convenience method that removes a security domain by name. Useful when changing the characteristics of the
     * login modules.
     *
     * @param securityDomainName the name of the new security domain
     * @throws Exception if failed to remove the security domain
     */
    public void removeSecurityDomain(String securityDomainName) throws Exception {

        // If not there just return
        if (!isSecurityDomain(securityDomainName)) {
            return;
        }

        final Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode removeSecurityDomainNode = createRequest(REMOVE, addr);

        final ModelNode results = execute(removeSecurityDomainNode);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to remove security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * Creates a new security domain including one or more login modules.
     * The security domain will be replaced if it exists.
     *
     * @param securityDomainName the name of the new security domain
     * @param loginModules an array of login modules to place in the security domain. They are ordered top-down in the
     * same index order of the array. 
     * @throws Exception if failed to create security domain
     */
    public void createNewSecurityDomain(String securityDomainName, LoginModuleRequest... loginModules)
        throws Exception {

        if (isSecurityDomain(securityDomainName)) {
            removeSecurityDomain(securityDomainName);
        }

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);

        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        ModelNode addAuthNode = createRequest(ADD, addr.clone().add(AUTHENTICATION, CLASSIC));
        ModelNode loginModulesNode = addAuthNode.get(LOGIN_MODULES);

        ModelNode[] loginModuleNodes = new ModelNode[loginModules.length];

        for (int i = 0, len = loginModules.length; i < len; ++i) {
            ModelNode loginModule = new ModelNode();
            loginModule.get(CODE).set(loginModules[i].getLoginModuleFQCN());
            loginModule.get(FLAG).set(loginModules[i].getFlagString());
            ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
            moduleOptions.setEmptyList();

            Map<String, String> moduleOptionProperties = loginModules[i].getModuleOptionProperties();
            if (null != moduleOptionProperties) {
                for (String key : moduleOptionProperties.keySet()) {
                    String value = moduleOptionProperties.get(key);
                    if (null != value) {
                        moduleOptions.add(key, value);
                    }
                }
            }

            loginModulesNode.add(loginModule);
        }

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    /** Immutable helper */
    public static class LoginModuleRequest {
        private AppConfigurationEntry entry;

        /**
         * @param loginModuleFQCN fully qualified class name to be set as the login-module "code".
         * @param flag constant, one of required|requisite|sufficient|optional
         * @param moduleOptionProperties map of propName->propValue mappings to to bet as module options
         */
        public LoginModuleRequest(String loginModuleFQCN, AppConfigurationEntry.LoginModuleControlFlag flag,
            Map<String, String> moduleOptionProperties) {

            this.entry = new AppConfigurationEntry(loginModuleFQCN, flag, moduleOptionProperties);
        }

        public String getLoginModuleFQCN() {
            return entry.getLoginModuleName();
        }

        public AppConfigurationEntry.LoginModuleControlFlag getFlag() {
            return entry.getControlFlag();
        }

        public String getFlagString() {
            return entry.getControlFlag().toString().split(" ")[1];
        }

        public Map<String, String> getModuleOptionProperties() {
            return (Map<String, String>) entry.getOptions();
        }

        @Override
        public String toString() {
            return "LoginModuleRequest [loginModuleFQCN=" + getLoginModuleFQCN() + ", flag=" + getFlag()
                + ", moduleOptionProperties=" + getModuleOptionProperties() + "]";
        }
    }
}
