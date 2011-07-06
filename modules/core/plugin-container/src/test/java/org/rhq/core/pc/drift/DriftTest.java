package org.rhq.core.pc.drift;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.writeLines;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;

/**
 * A base test class that provides a framework for drift related tests. DriftTest sets up
 * directories for change sets and for fake resources. Before each test method is run, a
 * uniquely named resource directory is created. Files to be included in a change set for
 * example can be placed under this directory. DriftTest also provides a number of helper
 * methods for things like generating a SHA-256 hash, accessing a change set file, and
 * obtaining a unique resource id.
 * <br/>
 * <br/>
 * DriftTest writes all output to a directory named after the test class name. Suppose your
 * test class name is MyDriftTest. DriftTest creates the following directories:
 *
 * <ul>
 *   <li><b>target/MyDriftTest</b> - the base directory to which all output will be written</li>
 *   <li><b>target/MyDriftTest/resources</b> - directory in which fake resources are created for
 *   each test method.</li>
 *   <li><b>target/MyDriftTest/changesets</b> - directory to which change set files are written</li>
 * </ul>
 */
public class DriftTest {

    /**
     * The base directory to which change sets are written to and read from. This directory
     * is deleted and recreated before any test methods are run. It is not deleted after
     * individual test methods are run so that output is available for inspection after tests
     * run.
     */
    protected File changeSetsDir;

    /**
     * The base directory to which resources are written/stored. This directory is deleted
     * and recreated before any test methods are run. It is not deleted after individual
     * test methods are run so that output is available for inspection after tests run.
     */
    protected File resourcesDir;

    /**
     * A {@link ChangeSetManager} to use in tests for reading, writing, and finding change
     * sets.
     */
    protected ChangeSetManager changeSetMgr;

    /**
     * This is basically a counter used to generate unique resource ids across test methods.
     * The current id is obtained from {@link #resourceId()}. The next (or a new) id is
     * obtained from {@link #nextResourceId()}.
     */
    private int resourceId;

    /**
     * Resource files for a given tests are to be written to this directory (or
     * subdirectories). This directory is created before each test method runs. Its name is
     * of the form:
     * <br/>
     * <pre>
     *         &lt;test_method_name&gt;-id
     * </pre>
     * where test_method_name is the name of the current test method, and id is unique
     * integer obtained from {@link #nextResourceId()}.
     */
    protected File resourceDir;

    private MessageDigestGenerator digestGenerator;

    /**
     * Deletes the base output directory (which is the name of the test class), removing
     * output from a previous run. The output directories (i.e., change sets and resources)
     * are then recreated.
     *
     * @throws Exception
     */
    @BeforeClass
    public void initResourcesAndChangeSetsDirs() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);
        basedir.mkdir();

        changeSetsDir = mkdir(basedir, "changesets");
        resourcesDir = mkdir(basedir, "resources");

        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
    }

    /**
     * Generates a uniquely named resource directory for the test method about to run. The
     * directory name is &lt;test_method_name&gt;-&lt;id&gt; where id is an integer id
     * generated from {@link #nextResourceId()}. The member variable, {@link #resourceDir},
     * is initialized to this directory and is accessible to subclasses.
     *
     * @param test
     */
    @BeforeMethod
    public void setUp(Method test) {
        resourceDir = mkdir(resourcesDir, test.getName() + "-" + nextResourceId());
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
    }

    /** @return The current or last resource id generated */
    protected int resourceId() {
        return resourceId;
    }

    /** @return Generates and returns the next resource id */
    protected int nextResourceId() {
        return ++resourceId;
    }

    /**
     * Creates and returns the specified directory. Any nonexistent parent directories are
     * created as well.
     *
     * @param parent The parent directory
     * @param name The name of the directory to be created
     * @return The directory
     */
    protected File mkdir(File parent, String name) {
        File dir = new File(parent, name);
        dir.mkdirs();
        return dir;
    }

    /**
     * Returns the change set file for the specified drift configuration for the resource
     * with the id that can be obtained from {@link #resourceId}. The type argument
     * determines whether a coverage or drift change set file is returned.
     *
     * @param config The drift configuration name
     * @param type Determines whether a coverage or drift change set file is to be returned
     * @return The change set file
     * @throws IOException
     */
    protected File changeSet(String config, DriftChangeSetCategory type) throws IOException {
        return changeSetMgr.findChangeSet(resourceId(), config, type);
    }

    protected File changeSetDir(String driftConfigName) throws Exception {
        File dir = new File(new File(changeSetsDir, Integer.toString(resourceId)), driftConfigName);
        dir.mkdirs();
        return dir;
    }

    /**
     * Generates a SHA-256 hash
     * @param file The file for which the hash will be generated
     * @return The SHA-256 hash as a string
     * @throws IOException
     */
    protected String sha256(File file) throws IOException {
        return digestGenerator.calcDigestString(file);
    }

    protected void writeChangeSet(File changeSetDir, String... changeSet) throws Exception {
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(changeSetDir,
            "changeset.txt")));
        writeLines(asList(changeSet), "\n", stream);
        stream.close();
    }

    /**
     * Creates a {@link DriftConfiguration} with the specified basedir. The file system is
     * used as the context for the basedir which means the path specified is used as is.
     *
     * @param name The configuration name
     * @param basedir An absolute path of the base directory
     * @return The drift configuration object
     */
    protected DriftConfiguration driftConfiguration(String name, String basedir) {
        DriftConfiguration config = new DriftConfiguration(new Configuration());
        config.setName(name);
        config.setBasedir(new DriftConfiguration.BaseDirectory(fileSystem, basedir));

        return config;
    }
}
