package org.rhq.plugins.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class StorageNodeConfigDelegate implements ConfigurationFacet {

    private File jvmOptsFile;
    private File wrapperEnvFile;

    public StorageNodeConfigDelegate(File basedir) {
        File confDir = new File(basedir, "conf");
        jvmOptsFile = new File(confDir, "cassandra-jvm.properties");

        // for windows, config props also get propagated to the wrapper env
        if (isWindows()) {
            File wrapperDir = new File(basedir, "../bin/wrapper");
            wrapperEnvFile = new File(wrapperDir, "rhq-storage-wrapper.env");
        }
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream(jvmOptsFile));

        Configuration config = new Configuration();

        String heapDumpOnOOMError = properties.getProperty("heap_dump_on_OOMError");
        String heapDumpDir = properties.getProperty("heap_dump_dir");

        config.put(new PropertySimple("minHeapSize", getHeapMinProp(properties)));
        config.put(new PropertySimple("maxHeapSize", getHeapMaxProp(properties)));
        config.put(new PropertySimple("heapNewSize", getHeapNewProp(properties)));
        config.put(new PropertySimple("threadStackSize", getStackSizeProp(properties)));

        if (!StringUtil.isEmpty(heapDumpOnOOMError)) {
            config.put(new PropertySimple("heapDumpOnOOMError", true));
        } else {
            config.put(new PropertySimple("heapDumpOnOOMError", false));
        }

        if (!StringUtil.isEmpty(heapDumpDir)) {
            config.put(new PropertySimple("heapDumpDir", heapDumpDir));
        } else {
            File basedir = jvmOptsFile.getParentFile().getParentFile();
            config.put(new PropertySimple("heapDumpDir", new File(basedir, "bin").getAbsolutePath()));
        }

        return config;
    }

    /**
     * Ensure that the path uses only forward slash.
     * @param path
     * @return forward-slashed path, or null if path is null
     */
    private static String useForwardSlash(String path) {

        return (null != path) ? path.replace('\\', '/') : null;
    }

    private String getHeapMinProp(Properties properties) {
        String value = properties.getProperty("heap_min");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!value.startsWith("-Xms")) {
            return value;
        }

        return value.substring(4);
    }

    private String getHeapMaxProp(Properties properties) {
        String value = properties.getProperty("heap_max");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!value.startsWith("-Xmx")) {
            return value;
        }

        return value.substring(4);
    }

    private String getHeapNewProp(Properties properties) {
        String value = properties.getProperty("heap_new");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!value.startsWith("-Xmn")) {
            return value;
        }

        return value.substring(4);
    }

    private String getStackSizeProp(Properties properties) {
        String value = properties.getProperty("thread_stack_size");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!(value.startsWith("-Xss") || value.endsWith("k") || value.length() > 5)) {
            return value;
        }

        return value.substring(4, value.length() - 1);
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        try {
            Configuration config = configurationUpdateReport.getConfiguration();

            updateCassandraJvmProps(config);

            if (isWindows()) {
                updateWrapperEnv(config);
            }

            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            configurationUpdateReport.setErrorMessage("No configuration update was applied: " + e.getMessage());
        } catch (IOException e) {
            configurationUpdateReport.setErrorMessageFromThrowable(e);
        }
    }

    private void updateCassandraJvmProps(Configuration newConfig) throws IOException {
        PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(jvmOptsFile.getAbsolutePath());
        Properties properties = propertiesUpdater.loadExistingProperties();

        String jmxPort = newConfig.getSimpleValue("jmxPort");
        if (!StringUtil.isEmpty(jmxPort)) {
            validateIntegerArg("jmx_port", jmxPort);
            properties.setProperty("jmx_port", jmxPort);
        }

        String maxHeapSize = newConfig.getSimpleValue("maxHeapSize");
        if (!StringUtil.isEmpty(maxHeapSize)) {
            validateHeapArg("maxHeapSize", maxHeapSize);
            // We want min and max heap to be the same
            properties.setProperty("heap_min", "-Xms" + maxHeapSize);
            properties.setProperty("heap_max", "-Xmx" + maxHeapSize);
        }

        String heapNewSize = newConfig.getSimpleValue("heapNewSize");
        if (!StringUtil.isEmpty(heapNewSize)) {
            validateHeapArg("heapNewSize", heapNewSize);
            properties.setProperty("heap_new", "-Xmn" + heapNewSize);
        }

        String threadStackSize = newConfig.getSimpleValue("threadStackSize");
        if (!StringUtil.isEmpty(threadStackSize)) {
            validateIntegerArg("threadStackSize", threadStackSize);
            properties.setProperty("thread_stack_size", "-Xss" + threadStackSize + "k");
        }

        PropertySimple heapDumpOnOMMError = newConfig.getSimple("heapDumpOnOOMError");
        if (heapDumpOnOMMError != null) {
            if (heapDumpOnOMMError.getBooleanValue()) {
                properties.setProperty("heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
            } else {
                properties.setProperty("heap_dump_on_OOMError", "");
            }
        }

        String heapDumpDir = useForwardSlash(newConfig.getSimpleValue("heapDumpDir"));
        if (!StringUtil.isEmpty(heapDumpDir)) {
            properties.setProperty("heap_dump_dir", heapDumpDir);
        }

        propertiesUpdater.update(properties);
    }

    private void updateWrapperEnv(Configuration config) throws IOException {
        PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(wrapperEnvFile.getAbsolutePath());
        Properties properties = propertiesUpdater.loadExistingProperties();

        String maxHeapSize = config.getSimpleValue("maxHeapSize");
        if (!StringUtil.isEmpty(maxHeapSize)) {
            validateHeapArg("maxHeapSize", maxHeapSize);
            // We want min and max heap to be the same
            properties.setProperty("set.heap_min", "-Xms" + maxHeapSize);
            properties.setProperty("set.heap_max", "-Xmx" + maxHeapSize);
        }

        String heapNewSize = config.getSimpleValue("heapNewSize");
        if (!StringUtil.isEmpty(heapNewSize)) {
            validateHeapArg("heapNewSize", heapNewSize);
            properties.setProperty("set.heap_new", "-Xmn" + heapNewSize);
        }

        String threadStackSize = config.getSimpleValue("threadStackSize");
        if (!StringUtil.isEmpty(threadStackSize)) {
            validateIntegerArg("threadStackSize", threadStackSize);
            properties.setProperty("set.thread_stack_size", "-Xss" + threadStackSize + "k");
        }

        PropertySimple heapDumpOnOMMError = config.getSimple("heapDumpOnOOMError");
        if (heapDumpOnOMMError != null) {
            if (heapDumpOnOMMError.getBooleanValue()) {
                properties.setProperty("set.heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
            } else {
                properties.setProperty("set.heap_dump_on_OOMError", "");
            }
        }

        String heapDumpDir = useForwardSlash(config.getSimpleValue("heapDumpDir"));
        if (!StringUtil.isEmpty(heapDumpDir)) {
            properties.setProperty("set.heap_dump_dir", heapDumpDir);
        }

        propertiesUpdater.update(properties);
    }

    private void validateHeapArg(String name, String value) {
        if (value.length() < 2) {
            throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
        }

        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length - 1; ++i) {
            if (!Character.isDigit(chars[i])) {
                throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
            }
        }

        char lastChar = Character.toUpperCase(chars[chars.length - 1]);
        if (!(lastChar == 'M' || lastChar == 'G')) {
            throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
        }
    }

    private void validateIntegerArg(String name, String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
        }
    }

    private boolean isWindows() {
        return File.separatorChar == '\\';
    }
}
