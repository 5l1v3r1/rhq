package org.rhq.storage.installer;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.util.ConfigEditor;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class StorageInstallerTest {

    private MessageDigestGenerator digestGenerator;

    private File basedir;

    private File serverDir;

    private File storageDir;

    private StorageInstaller installer;

    @BeforeMethod
    public void initDirs(Method test) throws Exception {
        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

        File dir = new File(getClass().getResource(".").toURI());
        basedir = new File(dir, getClass().getSimpleName() + "/" + test.getName());
        FileUtil.purge(basedir, true);
        basedir.mkdirs();

        serverDir = new File(basedir, "rhq-server");

        System.setProperty("rhq.server.basedir", serverDir.getAbsolutePath());

        File serverPropsFile = new File(serverDir, "rhq-server.properties");
        FileUtils.touch(serverPropsFile);
        System.setProperty("rhq.server.properties-file", serverPropsFile.getAbsolutePath());

        storageDir = new File(serverDir, "rhq-storage");

        installer = new StorageInstaller();
    }

    @AfterMethod
    public void shutdownStorageNode() throws Exception {
        if (FileUtils.getFile(storageDir, "bin", "cassandra.pid").exists()) {
            CassandraClusterManager ccm = new CassandraClusterManager();
            ccm.killNode(storageDir);
        }
    }

    @Test
    public void performDefaultInstall() throws Exception {
        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = parser.parse(installer.getOptions(), new String[] {});

        int status = installer.run(cmdLine);

        String address = InetAddress.getLocalHost().getHostAddress();

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful default install");
        assertNodeIsRunning();
        assertRhqServerPropsUpdated(address);

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File baseDataDir = new File(basedir, "rhq-data");

        File commitLogDir = new File(baseDataDir, "commit_log");
        assertTrue(commitLogDir.exists(), "Expected to find commit_log directory at " + commitLogDir);

        File dataDir = new File(baseDataDir, "data");
        assertTrue(dataDir.exists(), "Expected to find data directory at " + dataDir);

        File savedCachesDir = new File(baseDataDir, "saved_caches");
        assertTrue(savedCachesDir.exists(), "Expected to find saved_caches directory at " + savedCachesDir);

        File log4jFile = new File(confDir, "log4j-server.properties");
        assertTrue(log4jFile.exists(), log4jFile + " does not exist");

        File logsDir = new File(serverDir, "logs");
        File logFile = new File(logsDir, "rhq-storage.log");

        Properties log4jProps = new Properties();
        log4jProps.load(new FileInputStream(log4jFile));
        assertEquals(log4jProps.getProperty("log4j.appender.R.File"), logFile.getAbsolutePath(),
            "The log file is wrong");

        File yamlFile = new File(confDir, "cassandra.yaml");
        ConfigEditor yamlEditor = new ConfigEditor(yamlFile);
        yamlEditor.load();

        assertEquals(yamlEditor.getInternodeAuthenticator(), "org.rhq.cassandra.auth.RhqInternodeAuthenticator",
            "Failed to set the internode_authenticator property in " + yamlFile);
        assertEquals(yamlEditor.getAuthenticator(), "org.apache.cassandra.auth.PasswordAuthenticator",
            "The authenticator property is wrong");
        assertEquals(yamlEditor.getListenAddress(), address, "The listen_address property is wrong");
        assertEquals(yamlEditor.getNativeTransportPort(), (Integer) 9142,  "The native_transport_port property is wrong");
        assertEquals(yamlEditor.getRpcAddress(), address, "The rpc_address property is wrong");
        assertEquals(yamlEditor.getStoragePort(), (Integer) 7100, "The storage_port property is wrong");

        File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(cassandraJvmPropsFile));

        assertEquals(properties.getProperty("jmx_port"), "7299", "The jmx_port property is wrong");
        assertEquals(properties.getProperty("heap_min"), "-Xms512M", "The heap_min property is wrong");
        assertEquals(properties.getProperty("heap_max"), "-Xmx512M", "The heap_max property is wrong");
        assertEquals(properties.getProperty("heap_new"), "-Xmn128M", "The heap_new property is wrong");
        assertEquals(properties.getProperty("thread_stack_size"), "-Xss256k", "The thread_stack_size property is wrong");
    }

    @Test
    public void performValidInstall() throws Exception {
        CommandLineParser parser = new PosixParser();

        String[] args = {
            "--dir", storageDir.getAbsolutePath(),
            "--commitlog", new File(storageDir, "commit_log").getAbsolutePath(),
            "--data", new File(storageDir, "data").getAbsolutePath(),
            "--saved-caches", new File(storageDir, "saved_caches").getAbsolutePath(),
            "--heap-size", "256M",
            "--heap-new-size", "64M",
            "--hostname", "127.0.0.1"
        };

        CommandLine cmdLine = parser.parse(installer.getOptions(), args);
        int status = installer.run(cmdLine);

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful install");
        assertNodeIsRunning();
        assertRhqServerPropsUpdated();

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File commitLogDir = new File(storageDir, "commit_log");
        assertTrue(commitLogDir.exists(), "Expected to find commit_log directory at " + commitLogDir);

        File dataDir = new File(storageDir, "data");
        assertTrue(dataDir.exists(), "Expected to find data directory at " + dataDir);

        File savedCachesDir = new File(storageDir, "saved_caches");
        assertTrue(savedCachesDir.exists(), "Expected to find saved_caches directory at " + savedCachesDir);
    }

    @Test
    public void upgradeFromRHQ48Install() throws Exception {
        File rhq48ServerDir = new File(basedir, "rhq48-server");
        File rhq48StorageDir = new File(rhq48ServerDir, "rhq-storage");
        File rhq48StorageConfDir = new File(rhq48StorageDir, "conf");

        File oldCassandraYamlFile = new File(rhq48StorageConfDir, "cassandra.yaml");
        File oldCassandraEnvFile = new File(rhq48StorageConfDir, "cassandra-env.sh");
        File oldLog4JFile = new File(rhq48StorageConfDir, "log4j-server.properties");

        rhq48StorageConfDir.mkdirs();
        StreamUtil.copy(getClass().getResourceAsStream("/rhq48/storage/conf/cassandra.yaml"),
            new FileOutputStream(oldCassandraYamlFile), true);
        StreamUtil.copy(getClass().getResourceAsStream("/rhq48/storage/conf/cassandra-env.sh"),
            new FileOutputStream(oldCassandraEnvFile));
        StreamUtil.copy(getClass().getResourceAsStream("/rhq48/storage/conf/log4j-server.properties"),
            new FileOutputStream(oldLog4JFile));

        CommandLineParser parser = new PosixParser();

        String[] args = {
            "--upgrade", rhq48ServerDir.getAbsolutePath(),
            "--dir", storageDir.getAbsolutePath()
        };

        CommandLine cmdLine = parser.parse(installer.getOptions(), args);
        int status = installer.run(cmdLine);

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful upgrade");
        assertNodeIsRunning();

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File newCassandraYamlFile = new File(confDir, "cassandra.yaml");
        assertTrue(newCassandraYamlFile.exists(), newCassandraYamlFile + " does not exist");

        File newLog4JFile = new File(confDir, "log4j-server.properties");
        assertTrue(newLog4JFile.exists(), newLog4JFile + " does not exist");

        File logsDir = new File(serverDir, "logs");
        File logFile = new File(logsDir, "rhq-storage.log");

        Properties log4jProps = new Properties();
        log4jProps.load(new FileInputStream(newLog4JFile));
        assertEquals(log4jProps.getProperty("log4j.appender.R.File"), logFile.getAbsolutePath(),
            "The log file is wrong");

        assertFalse(new File(confDir, "cassandra-env.sh").exists(),
            "cassandra-env.sh should not be used after RHQ 4.8.0");

        File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(cassandraJvmPropsFile));

        // If this check fails, make sure that the expected value matches the value in
        // src/test/resources/rhq48/storage/conf/cassandra-env.sh
        assertEquals(properties.getProperty("jmx_port"), "7399", "Failed to update the JMX port in " +
            cassandraJvmPropsFile);

        File yamlFile = new File(confDir, "cassandra.yaml");
        ConfigEditor newYamlEditor = new ConfigEditor(yamlFile);
        newYamlEditor.load();

        ConfigEditor oldYamlEditor = new ConfigEditor(oldCassandraYamlFile);
        oldYamlEditor.load();

        assertEquals(newYamlEditor.getInternodeAuthenticator(), "org.rhq.cassandra.auth.RhqInternodeAuthenticator",
            "Failed to set the internode_authenticator property in " + yamlFile);
        assertEquals(newYamlEditor.getAuthenticator(), oldYamlEditor.getAuthenticator(), "The authenticator property " +
            "is wrong");
        assertEquals(newYamlEditor.getCommitLogDirectory(), oldYamlEditor.getCommitLogDirectory(),
            "The commit_log property is wrong");
        assertEquals(newYamlEditor.getDataFileDirectories(), oldYamlEditor.getDataFileDirectories(),
            "The data_files property is wrong");
        assertEquals(newYamlEditor.getListenAddress(), oldYamlEditor.getListenAddress(),
            "The listen_address property is wrong");
        assertEquals(newYamlEditor.getNativeTransportPort(), oldYamlEditor.getNativeTransportPort(),
            "The native_transport_port property is wrong");
        assertEquals(newYamlEditor.getRpcAddress(), oldYamlEditor.getRpcAddress(),
            "The rpc_address property is wrong");
        assertEquals(newYamlEditor.getSavedCachesDirectory(), oldYamlEditor.getSavedCachesDirectory(),
            "The saved_caches_directory property is wrong");
        assertEquals(newYamlEditor.getStoragePort(), oldYamlEditor.getStoragePort(),
            "The storage_port property is wrong");
    }

    private void assertNodeIsRunning() {
        try {
            installer.verifyNodeIsUp("127.0.0.1", 7299, 3, 1000);
        } catch (Exception e) {
            fail("Failed to verify that node is up", e);
        }
    }

    private void assertRhqServerPropsUpdated() {
        File serverPropsFile = new File(serverDir, "rhq-server.properties");
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(serverPropsFile));
        } catch (IOException e) {
            fail("Failed to verify that " + serverPropsFile + " was updated", e);
        }

        assertEquals(properties.getProperty("rhq.storage.nodes"), "127.0.0.1");
        assertEquals(properties.getProperty("rhq.storage.cql-port"), "9142");
    }

    private String sha256(File file) {
        try {
            return digestGenerator.calcDigestString(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash for " + file.getPath(), e);
        }
    }

}
