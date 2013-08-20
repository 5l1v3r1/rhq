package org.rhq.cassandra.util;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.SeedProviderDef;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

/**
 * @author John Sanda
 */
public class ConfigEditorTest {

    private File basedir;

    private File configFile;

    @BeforeMethod
    public void initTestDir(Method test) throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        basedir = new File(dir, getClass().getSimpleName() + "/" + test.getName());
        purge(basedir, true);
        basedir.mkdirs();

        configFile = new File(basedir, "cassandra.yaml");

        InputStream inputStream = getClass().getResourceAsStream("/cassandra.yaml");
        FileOutputStream outputStream = new FileOutputStream(configFile);
        copyStreams(inputStream, outputStream);
    }

    @Test
    public void updateSeeds() throws Exception {
        ConfigEditor editor = new ConfigEditor(configFile);
        editor.load();
        editor.setSeeds("127.0.0.1", "127.0.0.2", "127.0.0.3");
        editor.save();

        Config config = loadConfig();

        assertEquals(config.seed_provider.parameters.get("seeds"), "127.0.0.1,127.0.0.2,127.0.0.3",
            "Failed to update seeds property.");
    }

    @Test
    public void updateNativeTransportPort() throws Exception {
        ConfigEditor editor = new ConfigEditor(configFile);
        editor.load();
        editor.setNativeTransportPort(9393);
        editor.save();

        Config config = loadConfig();

        assertEquals(config.native_transport_port, (Integer) 9393, "Failed to update native_transport_port");

        editor.load();

        assertEquals(editor.getNativeTransportPort(), config.native_transport_port,
            "Failed to fetch native_transport_port");
    }

    @Test
    public void updateStoragePort() throws Exception {
        ConfigEditor editor = new ConfigEditor(configFile);
        editor.load();
        editor.setStoragePort(6767);
        editor.save();

        Config config = loadConfig();

        assertEquals(config.storage_port, (Integer) 6767, "Failed to update storage_port");

        editor.load();

        assertEquals(editor.getStoragePort(), config.storage_port, "Failed to fetch storage_port");
    }

    @Test
    public void updateDataFilesDirectories() throws Exception {
        ConfigEditor editor = new ConfigEditor(configFile);
        editor.load();
        editor.setDataFileDirectories(asList("/data/dir1", "/data/dir2", "data/dir3"));
        editor.save();

        Config config = loadConfig();

        assertEquals(config.data_file_directories, new String[] {"/data/dir1", "/data/dir2", "data/dir3"},
            "Failed to update data_file_directories");

        editor.load();

        assertEquals(editor.getDataFileDirectories().toArray(new String[3]), config.data_file_directories,
            "Failed to fetch data_file_directories");
    }

    private Config loadConfig() throws Exception {
        FileInputStream inputStream = new FileInputStream(configFile);
        org.yaml.snakeyaml.constructor.Constructor constructor =
            new org.yaml.snakeyaml.constructor.Constructor(Config.class);
        TypeDescription seedDesc = new TypeDescription(SeedProviderDef.class);
        seedDesc.putMapPropertyType("parameters", String.class, String.class);
        constructor.addTypeDescription(seedDesc);
        Yaml yaml = new Yaml(new Loader(constructor));

        return (Config) yaml.load(inputStream);
    }

    private static void purge(File dir, boolean deleteIt) {
        if (dir != null) {
            if (dir.isDirectory()) {
                File[] doomedFiles = dir.listFiles();
                if (doomedFiles != null) {
                    for (File doomedFile : doomedFiles) {
                        purge(doomedFile, true); // call this method recursively
                    }
                }
            }

            if (deleteIt) {
                dir.delete();
            }
        }

        return;
    }

    public static void copyStreams(InputStream is, OutputStream os) throws FileNotFoundException, IOException {
        int bufferSize = 32768;
        try {
            is = new BufferedInputStream(is, bufferSize);
            byte[] buffer = new byte[bufferSize];
            for (int bytesRead = is.read(buffer); bytesRead != -1; bytesRead = is.read(buffer)) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be copied", ioe);
        } finally {
            os.close();
            is.close();
        }
    }

}
