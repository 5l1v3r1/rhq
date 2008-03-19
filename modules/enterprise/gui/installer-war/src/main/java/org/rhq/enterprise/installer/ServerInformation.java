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
package org.rhq.enterprise.installer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.server.ServerConfig;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.db.setup.DBSetup;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * Finds out information about the RHQ Server and the JBossAS server it is running in. It can also be used to modify
 * things about the RHQ Server, such as the names of the deployment artifacts.
 *
 * @author John Mazzitelli
 */
public class ServerInformation {
    private static final Log LOG = LogFactory.getLog(ServerInformation.class);

    private static final String DEPLOYED_EAR_FILENAME = "rhq.ear";
    private static final String UNDEPLOYED_EAR_FILENAME = DEPLOYED_EAR_FILENAME + ".rej";
    private static final String DEPLOYED_DATASOURCE_FILENAME = "rhq-ds.xml";
    private static final String UNDEPLOYED_DATASOURCE_FILENAME = DEPLOYED_DATASOURCE_FILENAME + ".rej";
    private static final String DEPLOYED_EMBEDDED_AGENT_FILENAME = "rhq-agent.sar";
    private static final String UNDEPLOYED_EMBEDDED_AGENT_FILENAME = DEPLOYED_EMBEDDED_AGENT_FILENAME + ".rej";
    private static final String DEPLOYED_MAIL_SERVICE_FILENAME = "mail-service.xml";
    private static final String UNDEPLOYED_MAIL_SERVICE_FILENAME = DEPLOYED_MAIL_SERVICE_FILENAME + ".rej";
    private static final String DEPLOYED_ALERT_CACHE_SERVICE_FILENAME = "alert-cache-service.xml";
    private static final String UNDEPLOYED_ALERT_CACHE_SERVICE_FILENAME = DEPLOYED_ALERT_CACHE_SERVICE_FILENAME
        + ".rej";
    private static final String DEPLOYED_JMS_FILENAME = "jms";
    private static final String UNDEPLOYED_POSTGRES_JMS_FILENAME = "jms-postgres.rej";
    private static final String UNDEPLOYED_ORACLE_JMS_FILENAME = "jms-oracle.rej";
    private static final String SERVER_PROPERTIES_FILENAME = "rhq-server.properties";

    private MBeanServer mbeanServer = null;
    private File deployDirectory = null;
    private File binDirectory = null;
    private File logDirectory = null;

    /**
     * Returns <code>true</code> if the given set of properties provides settings that allow for a successful database
     * connection. If <code>props</code> is <code>null</code>, it will use the server properties from
     * {@link #getServerProperties()}.
     *
     * @param  props set of properties where the connection information is found
     *
     * @return <code>true</code> if the database can be connected to
     */
    public boolean isDatabaseConnectionValid(Properties props) {
        if (props == null) {
            props = getServerProperties();
        }

        String jdbcUrl = props.getProperty(ServerProperties.PROP_DATABASE_CONNECTION_URL, "-unknown-");
        String userName = props.getProperty(ServerProperties.PROP_DATABASE_USERNAME, "-unknown-");
        String password = props.getProperty(ServerProperties.PROP_DATABASE_PASSWORD, "-unknown-");

        return DbUtil.ping(jdbcUrl, userName, password);
    }

    /**
     * Returns <code>true</code> if the database already has the database schema created for it. It will not be known
     * what version of schema or if its the latest, all this method tells you is that some RHQ database schema exists.
     *
     * <p>The given set of properties provides settings that allow for a successful database connection. If <code>
     * props</code> is <code>null</code>, it will use the server properties from {@link #getServerProperties()}.</p>
     *
     * <p>Do not call this method unless {@link #isDatabaseConnectionValid(Properties)} is <code>true</code>.</p>
     *
     * @param  props set of properties where the connection information is found
     *
     * @return <code>true</code> if the database can be connected to
     *
     * @throws Exception if failed to communicate with the database
     */
    public boolean isDatabaseSchemaExist(Properties props) throws Exception {
        Connection conn = getDatabaseConnection(props);
        DatabaseType db = DatabaseTypeFactory.getDatabaseType(conn);

        try {
            return db.checkTableExists(conn, "RHQ_PRINCIPAL");
        } finally {
            db.closeConnection(conn);
        }
    }

    /**
     * This will create the database schema in the database. <code>props</code> define the connection to the database -
     * see {@link #isDatabaseConnectionValid(Properties)}.
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist(Properties) schema already exists}, it will be purged of all
     * data/tables and recreated.</p>
     *
     * @param  props
     * 
     * @throws Exception if failed to create the new schema for some reason
     */
    public void createNewDatabaseSchema(Properties props) throws Exception {
        if (props == null) {
            props = getServerProperties();
        }

        String jdbcUrl = props.getProperty(ServerProperties.PROP_DATABASE_CONNECTION_URL, "-unknown-");
        String userName = props.getProperty(ServerProperties.PROP_DATABASE_USERNAME, "-unknown-");
        String password = props.getProperty(ServerProperties.PROP_DATABASE_PASSWORD, "-unknown-");
        File logfile = new File(getLogDirectory(), "rhq-installer-dbsetup.log");

        logfile.delete(); // do not keep logs from previous dbsetup runs

        try {
            // extract the dbsetup files which are located in the dbutils jar
            String dbsetupSchemaXmlFile = extractDatabaseXmlFile("db-schema-combined.xml", props);
            String dbsetupDataXmlFile = extractDatabaseXmlFile("db-data-combined.xml", props);

            // first uninstall any old existing schema, then create the tables then insert the data
            DBSetup dbsetup = new DBSetup(jdbcUrl, userName, password, logfile.getAbsolutePath());
            dbsetup.uninstall(dbsetupSchemaXmlFile);
            dbsetup.setup(dbsetupSchemaXmlFile);
            dbsetup.setup(dbsetupDataXmlFile, null, true, false);
        } catch (Exception e) {
            LOG.fatal("Cannot install the database schema - RHQ Server will not run properly", e);
            throw e;
        }

        return;
    }

    /**
     * This will update an existing database schema so it can be upgraded to the latest schema version. <code>
     * props</code> define the connection to the database - see {@link #isDatabaseConnectionValid(Properties)}.
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist(Properties) schema does not already exist}, errors will
     * occur.</p>
     *
     * @param  props
     * 
     * @throws Exception if the upgrade failed for some reason
     */
    public void upgradeExistingDatabaseSchema(Properties props) throws Exception {
        if (props == null) {
            props = getServerProperties();
        }

        String jdbcUrl = props.getProperty(ServerProperties.PROP_DATABASE_CONNECTION_URL, "-unknown-");
        String userName = props.getProperty(ServerProperties.PROP_DATABASE_USERNAME, "-unknown-");
        String password = props.getProperty(ServerProperties.PROP_DATABASE_PASSWORD, "-unknown-");
        File logfile = new File(getLogDirectory(), "rhq-installer-dbupgrade.log");

        logfile.delete(); // do not keep logs from previous dbupgrade runs

        try {
            // extract the dbupgrade ANT script which is located in the dbutils jar
            String dbupgradeXmlFile = extractDatabaseXmlFile("db-upgrade.xml", props);

            Properties antProps = new Properties();
            antProps.setProperty("jdbc.url", jdbcUrl);
            antProps.setProperty("jdbc.user", userName);
            antProps.setProperty("jdbc.password", password);
            antProps.setProperty("target.schema.version", "LATEST");

            startAnt(new File(dbupgradeXmlFile), "db-ant-tasks.properties", antProps, logfile);
        } catch (Exception e) {
            LOG.fatal("Cannot upgrade the database schema - RHQ Server will not run properly", e);
            throw e;
        }

        return;
    }

    /**
     * Gets the current set of properties currently configured for the RHQ Server.
     *
     * @return current configuration properties
     *
     * @throws RuntimeException if failed to read the properties file
     */
    public Properties getServerProperties() {
        File file = getServerPropertiesFile();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Properties props = new Properties();
            props.load(fis);
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load configuration from [" + file + "]. Cause:" + e, e);
        } finally {
            JDBCUtil.safeClose(fis);
        }
    }

    /**
     * Writes the given properties to the RHQ Server properties file and also sets them as system properties.
     *
     * @param  props the new properties
     *
     * @throws RuntimeException if failed to write the file
     */
    public void setServerProperties(Properties props) {
        clearNullProperties(props);

        File file = getServerPropertiesFile();

        try {
            PropertiesFileUpdate updater = new PropertiesFileUpdate(file.getAbsolutePath());
            updater.update(props);
        } catch (Exception e) {
            throw new RuntimeException("Cannot store configuration to [" + file + "]. Cause:" + e, e);
        }

        // we need to put them as system properties now so when we hot deploy,
        // the replacement variables in the config files pick up the new values
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            System.setProperty(entry.getKey().toString(), entry.getValue().toString());
        }

        return;
    }

    /**
     * This will move the RHQ Server deployment artifacts such that they either get hot-deployed or hot-undeployed in
     * the JBossAS server. If <code>deploy</code> is <code>true</code>, this ensures the RHQ Server deployment artifacts
     * (e.g. the EAR and its data source) are deployed so they can run. If <code>deploy</code> is <code>false</code>,
     * the caller is saying he wants the RHQ Server to be undeployed so it doesn't run anymore.
     *
     * @param  deploy <code>true</code> means the RHQ Server should be deployed; otherwise, its deployment artifacts
     *                should be undeployed
     *
     * @throws RuntimeException if failed to move one or more artifacts
     */
    public void moveDeploymentArtifacts(boolean deploy) {
        try {
            // order is important here - do agent and data source first, then JMS, then alert cache, and then ear last

            // MAIL SERVICE
            File mail = getMailServiceFile(!deploy);
            File mailRenameTo = getMailServiceFile(deploy);
            if (!mailRenameTo.exists()) {
                mail.renameTo(mailRenameTo);
            }

            // EMBEDDED AGENT
            // if we are to deploy, then we need to rename the undeployed files; and vice versa
            File agent = getEmbeddedAgentFile(!deploy);
            File agentRenameTo = getEmbeddedAgentFile(deploy);
            if (!agentRenameTo.exists()) {
                agent.renameTo(agentRenameTo);
            }

            // DATA SOURCE
            File ds = getDataSourceFile(!deploy);
            File dsRenameTo = getDataSourceFile(deploy);
            if (!dsRenameTo.exists()) {
                // purpose of this is to copy the .rej so we can modify it (the connection checker string is db-specific)
                // we only need to do this if we are deploying; if we are undeploying, just do the rename
                if (deploy) {
                    String replacement;
                    String db = getServerProperties().getProperty(ServerProperties.PROP_DATABASE_TYPE);
                    if (db.toLowerCase().indexOf("postgres") > -1) {
                        replacement = ";";
                    } else {
                        replacement = "select 1 from dual";
                    }

                    File dsTmp = new File(ds.getAbsolutePath() + ".rej"); // makes a .rej.rej tmp file in the same location
                    Reader reader = new FileReader(ds);
                    BufferedReader bufR = new BufferedReader(reader);
                    Writer writer = new FileWriter(dsTmp);
                    BufferedWriter bufW = new BufferedWriter(writer);
                    String line;
                    while ((line = bufR.readLine()) != null) {
                        line = line.replace("@@@rhq-server-connection-checker-sql@@@", replacement);
                        bufW.write(line);
                        bufW.newLine();
                    }

                    bufW.flush();
                    bufW.close();
                    writer.close();
                    bufR.close();
                    reader.close();
                    if (dsTmp.renameTo(dsRenameTo)) {
                        ds.delete();
                    }
                } else {
                    ds.renameTo(dsRenameTo);
                }
            }

            // JMS
            // we leave the undeployed versions - there is one per supported database
            // to undeploy, we just delete the deployed JMS directory
            // to deploy, we copy one of the undeployed version for the DB to be used
            File jms = getJmsFile(true);
            if (deploy) {
                File undeployedJms = getJmsFile(false); // gets the DB specific one
                copyDirectory(undeployedJms, jms);
            } else {
                // being told to undeploy - delete the deployed JMS directory if exists
                deleteFile(jms);
            }

            // ALERT CACHE SERVICE
            File alertCache = getAlertCacheServiceFile(!deploy);
            File alertCacheRenameTo = getAlertCacheServiceFile(deploy);
            if (!alertCacheRenameTo.exists()) {
                alertCache.renameTo(alertCacheRenameTo);
            }

            // EAR
            File ear = getEarFile(!deploy);
            File earRenameTo = getEarFile(deploy);
            if (!earRenameTo.exists()) {
                ear.renameTo(earRenameTo);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to move deployment artifacts. Cause: " + e, e);
        }

        return;
    }

    /**
     * Returns <code>true</code> if the RHQ Server deployment artifacts are fully deployed. <code>false</code> if the
     * installer needs to perform some work to finish the installation.
     *
     * @return installation status of RHQ Server
     */
    public boolean isFullyDeployed() {
        File ear = getEarFile(true);
        File agent = getEmbeddedAgentFile(true);
        File jms = getJmsFile(true);
        File ds = getDataSourceFile(true);
        File mail = getMailServiceFile(true);
        File alert = getAlertCacheServiceFile(true);

        return ds.exists() && jms.exists() && ear.exists() && agent.exists() && mail.exists() && alert.exists();
    }

    private File getEarFile(boolean deployed) {
        File deployDir = getDeployDirectory();
        File file = new File(deployDir, deployed ? DEPLOYED_EAR_FILENAME : UNDEPLOYED_EAR_FILENAME);
        return file;
    }

    private File getMailServiceFile(boolean deployed) {
        File deployDir = getDeployDirectory();
        File file = new File(deployDir, deployed ? DEPLOYED_MAIL_SERVICE_FILENAME : UNDEPLOYED_MAIL_SERVICE_FILENAME);
        return file;
    }

    private File getAlertCacheServiceFile(boolean deployed) {
        File deployDir = getDeployDirectory();
        File file = new File(deployDir, deployed ? DEPLOYED_ALERT_CACHE_SERVICE_FILENAME
            : UNDEPLOYED_ALERT_CACHE_SERVICE_FILENAME);
        return file;
    }

    private File getEmbeddedAgentFile(boolean deployed) {
        File deployDir = getDeployDirectory();
        File file = new File(deployDir, deployed ? DEPLOYED_EMBEDDED_AGENT_FILENAME
            : UNDEPLOYED_EMBEDDED_AGENT_FILENAME);
        return file;
    }

    private File getDataSourceFile(boolean deployed) {
        File deployDir = getDeployDirectory();
        File file = new File(deployDir, deployed ? DEPLOYED_DATASOURCE_FILENAME : UNDEPLOYED_DATASOURCE_FILENAME);
        return file;
    }

    private File getJmsFile(boolean deployed) {
        File deployDir = getDeployDirectory();
        File file;

        if (deployed) {
            file = new File(deployDir, DEPLOYED_JMS_FILENAME);
        } else {
            String db = getServerProperties().getProperty(ServerProperties.PROP_DATABASE_TYPE);
            if (db.toLowerCase().indexOf("postgres") > -1) {
                file = new File(deployDir, UNDEPLOYED_POSTGRES_JMS_FILENAME);
            } else if (db.toLowerCase().indexOf("oracle") > -1) {
                file = new File(deployDir, UNDEPLOYED_ORACLE_JMS_FILENAME);
            } else {
                throw new RuntimeException("Unsupported database: " + db);
            }
        }

        return file;
    }

    private void clearNullProperties(Properties props) {
        // some properties, if blank, must be removed so their defaults are picked up

        String bindAddr = props.getProperty(ServerProperties.PROP_SERVER_BIND_ADDRESS);
        if ((bindAddr != null) && (bindAddr.trim().length() == 0)) {
            props.remove(ServerProperties.PROP_SERVER_BIND_ADDRESS);
        }

        return;
    }

    private File getServerPropertiesFile() {
        File binDir = getBinDirectory();
        File file = new File(binDir, SERVER_PROPERTIES_FILENAME);
        return file;
    }

    private File getDeployDirectory() {
        if (deployDirectory == null) {
            MBeanServer mbs = getMBeanServer();
            ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
            Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);

            deployDirectory = new File(((ServerConfig) mbean).getServerHomeDir(), "deploy");
        }

        return deployDirectory;
    }

    private File getBinDirectory() {
        if (binDirectory == null) {
            MBeanServer mbs = getMBeanServer();
            ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
            Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);

            File homeDir = ((ServerConfig) mbean).getHomeDir();
            binDirectory = new File(homeDir.getParentFile(), "bin");
        }

        return binDirectory;
    }

    private File getLogDirectory() {
        if (logDirectory == null) {
            MBeanServer mbs = getMBeanServer();
            ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
            Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);

            File homeDir = ((ServerConfig) mbean).getHomeDir();
            logDirectory = new File(homeDir.getParentFile(), "logs");
            logDirectory.mkdirs(); // just in case it doesn't exist yet, let's create it now
        }

        return logDirectory;
    }

    private MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = MBeanServerLocator.locateJBoss();
        }

        return mbeanServer;
    }

    private static void deleteFile(File file) {
        if (file != null) {
            if (file.isDirectory()) {
                File[] doomedFiles = file.listFiles();
                if (doomedFiles != null) {
                    for (File doomedFile : doomedFiles) {
                        deleteFile(doomedFile); // call this method recursively
                    }
                }
            } else {
                file.delete();
            }
        }

        return;
    }

    private void copyDirectory(File from, File to) throws Exception {
        to.mkdirs();

        File[] children = from.listFiles();

        for (File fromChild : children) {
            File toChild = new File(to, fromChild.getName());
            if (fromChild.isDirectory()) {
                copyDirectory(fromChild, toChild);
            } else {
                copySingleFile(fromChild, toChild);
            }
        }

        return;
    }

    private void copySingleFile(File from, File to) throws Exception {
        FileInputStream fis = new FileInputStream(from);
        FileOutputStream fos = new FileOutputStream(to);

        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } finally {
            JDBCUtil.safeClose(fis);
            JDBCUtil.safeClose(fos);
        }

        return;
    }

    /**
     * Returns a database connection with the given set of properties providing the settings that allow for a successful
     * database connection. If <code>props</code> is <code>null</code>, it will use the server properties from
     * {@link #getServerProperties()}.
     *
     * @param  props set of properties where the connection information is found
     *
     * @return the database connection
     *
     * @throws SQLException if cannot successfully connect to the database
     */
    public Connection getDatabaseConnection(Properties props) throws SQLException {
        if (props == null) {
            props = getServerProperties();
        }

        String jdbcUrl = props.getProperty(ServerProperties.PROP_DATABASE_CONNECTION_URL, "-unknown-");
        String userName = props.getProperty(ServerProperties.PROP_DATABASE_USERNAME, "-unknown-");
        String password = props.getProperty(ServerProperties.PROP_DATABASE_PASSWORD, "-unknown-");

        return DbUtil.getConnection(jdbcUrl, userName, password);
    }

    /**
     * Takes the named XML file from the classloader and writes the file to the log directory. This is meant to extract
     * the schema/data xml files from the dbutils jar file. It can also be used to extract the db upgrade XML file.
     *
     * @param  xmlFileName the name of the XML file, as found in the classloader
     * @param  props       properties whose values are used to replace the replacement strings found in the XML file
     *
     * @return the absolute path to the extracted file
     *
     * @throws IOException if failed to extract the file to the log directory
     */
    private String extractDatabaseXmlFile(String xmlFileName, Properties props) throws IOException {
        // first slurp the file contents in memory
        InputStream resourceInStream = this.getClass().getClassLoader().getResourceAsStream(xmlFileName);
        ByteArrayOutputStream contentOutStream = new ByteArrayOutputStream();
        copyStreamData(resourceInStream, contentOutStream);

        // now replace their replacement strings with values from the properties
        String content = contentOutStream.toString();
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_DATA@@@", "DEFAULT");
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_INDEX@@@", "DEFAULT");
        content = content.replaceAll("@@@ADMINUSERNAME@@@", "rhqadmin");
        content = content.replaceAll("@@@ADMINPASSWORD@@@", "x1XwrxKuPvYUILiOnOZTLg=="); // rhqadmin
        content = content.replaceAll("@@@ADMINEMAIL@@@", props.getProperty(ServerProperties.PROP_EMAIL_FROM_ADDRESS));
        content = content.replaceAll("@@@BASEURL@@@", "http://" + ServerProperties.getValidServerBindAddress(props)
            + ":" + ServerProperties.getHttpPort(props) + "/");
        content = content.replaceAll("@@@JAASPROVIDER@@@", "JDBC");
        content = content.replaceAll("@@@LDAPURL@@@", "ldap://localhost/");
        content = content.replaceAll("@@@LDAPPROTOCOL@@@", "");
        content = content.replaceAll("@@@LDAPLOGINPROP@@@", "cn");
        content = content.replaceAll("@@@LDAPBASEDN@@@", "o=JBoss,c=US");
        content = content.replaceAll("@@@LDAPSEARCHFILTER@@@", "");
        content = content.replaceAll("@@@LDAPBINDDN@@@", "");
        content = content.replaceAll("@@@LDAPBINDPW@@@", "");
        content = content.replaceAll("@@@MULTICAST_ADDR@@@", "");
        content = content.replaceAll("@@@MULTICAST_PORT@@@", "");

        // we now have the finished XML content - write out the file to the log directory
        File xmlFile = new File(getLogDirectory(), xmlFileName);
        FileOutputStream xmlFileOutStream = new FileOutputStream(xmlFile);
        ByteArrayInputStream contentInStream = new ByteArrayInputStream(content.getBytes());
        copyStreamData(contentInStream, xmlFileOutStream);

        return xmlFile.getAbsolutePath();
    }

    private void copyStreamData(InputStream input, OutputStream output) throws IOException {
        int bufferSize = 32768;

        try {
            // make sure we buffer the input
            input = new BufferedInputStream(input, bufferSize);

            byte[] buffer = new byte[bufferSize];

            for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
                output.write(buffer, 0, bytesRead);
            }

            output.flush();
        } finally {
            JDBCUtil.safeClose(output);
            JDBCUtil.safeClose(input);
        }

        return;
    }

    /**
     * Launches ANT and runs the default target in the given build file.
     *
     * @param  buildFile      the build file that ANT will run
     * @param  customTaskDefs the properties file found in classloader that contains all the taskdef definitions
     * @param  properties     set of properties to set for the ANT task to access
     * @param  logFile        where ANT messages will be logged (in addition to the app server's log file)
     *
     * @throws RuntimeException
     */
    private void startAnt(File buildFile, String customTaskDefs, Properties properties, File logFile) {
        PrintWriter logFileOutput = null;

        try {
            logFileOutput = new PrintWriter(new FileOutputStream(logFile));

            ClassLoader classLoader = getClass().getClassLoader();

            Properties taskDefs = new Properties();
            InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDefs);
            try {
                taskDefs.load(taskDefsStream);
            } finally {
                taskDefsStream.close();
            }

            Project project = new Project();
            project.setCoreLoader(classLoader);
            project.init();

            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                project.setProperty(property.getKey().toString(), property.getValue().toString());
            }

            // notice we add our listener after we set the properties - we do not want the password to be in the log file
            // our dbupgrade script will echo the property settings, so we can still get the other values
            project.addBuildListener(new LoggerAntBuildListener(logFileOutput));

            for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
                project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(),
                    true, classLoader));
            }

            new ProjectHelper2().parse(project, buildFile);
            project.executeTarget(project.getDefaultTarget());
        } catch (Exception e) {
            throw new RuntimeException("Cannot run ANT on script [" + buildFile + "]. Cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
    }
}