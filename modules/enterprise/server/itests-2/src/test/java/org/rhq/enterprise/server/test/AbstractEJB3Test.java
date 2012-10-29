package org.rhq.enterprise.server.test;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScanner;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceMBean;
import org.rhq.enterprise.server.scheduler.SchedulerService;
import org.rhq.enterprise.server.scheduler.SchedulerServiceMBean;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;
import org.rhq.test.MatchResult;
import org.rhq.test.PropertyMatchException;
import org.rhq.test.PropertyMatcher;

public abstract class AbstractEJB3Test extends Arquillian {

    protected static final String JNDI_RHQDS = "java:jboss/datasources/RHQDS";

    private SchedulerService schedulerService;
    private ServerPluginService serverPluginService;
    private PluginDeploymentScannerMBean pluginScannerService;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @ArquillianResource
    protected InitialContext initialContext;

    // We originally deployed the jar as a JavaArchive (JAR file). But this ran into problems because
    // of the dependent 3rd party jars.  There isn't an obvious way to deploy the dependent jars of a jar.
    // Also, AS7 makes it hard to put them in a globally accessibly /lib directory due to
    // its classloader isolation and module approach. So, we now deploy an EnterpriseArchive (ear)
    // where the ejb jar is deployed as a module and the 3rd party libraries are put in the ear's /lib.

    @Deployment
    protected static EnterpriseArchive getBaseDeployment() {

        // depending on the db in use, set up the necessary datasource 
        String dialect = System.getProperty("hibernate.dialect");
        if (dialect == null) {
            System.out.println("!!! hibernate.dialect is not set! Assuming you want to test on postgres");
            dialect = "postgres";
        }

        @SuppressWarnings("unused")
        String dataSourceXml;
        if (dialect.toLowerCase().contains("postgres")) {
            dataSourceXml = "jbossas-postgres-ds.xml";
        } else {
            dataSourceXml = "jbossas-oracle-ds.xml";
        }

        MavenDependencyResolver jarResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        //resolver.loadMetadataFromPom("pom.xml");

        // Create the ejb jar which will subsequently be packaged in the ear deployment   
        //        JavaArchive serverJar = ShrinkWrap.create(JavaArchive.class, "rhq-enterprise-server-ejb3.jar") //
        //            .addAsManifestResource(dataSourceXml) // the datasource
        //            .addAsManifestResource("ejb-jar.xml") // the empty ejb jar descriptor
        //            .addAsManifestResource("test-persistence.xml", "persistence.xml") //  the test persistence context            
        //            .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml") // add CDI injection (needed by arquillian injection)
        //            );

        // server jar classes
        //        serverJar = addClasses(serverJar, new File("../jar/target/classes/org"), null);
        //        JavaArchive serverJar = ShrinkWrap.create(JavaArchive.class, "rhq-enterprise-server-ejb3.jar");
        //        JavaArchive artifact = jarResolver.artifact("org.rhq:rhq-enterprise-server:4.6.0-SNAPSHOT")
        //            .resolveAs(JavaArchive.class).iterator().next();
        //        serverJar.merge(artifact, Filters.exclude(".*META-INF.*"));
        //        serverJar.addAsManifestResource(dataSourceXml) // the datasource
        //            .addAsManifestResource("ejb-jar.xml") // the empty ejb jar descriptor
        //            .addAsManifestResource("test-persistence.xml", "persistence.xml") //  the test persistence context            
        //            .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml")); // add CDI injection (needed by arquillian injection)

        //        System.out.println("** The Deployment EJB JAR: " + serverJar.toString(true) + "\n");

        JavaArchive testClassesJar = ShrinkWrap.create(JavaArchive.class, "test-classes.jar");
        testClassesJar = addClasses(testClassesJar, new File("target/test-classes/org"), null);

        // non itests-2 RHQ classes in use by test classes
        testClassesJar.addClass(ThrowableUtil.class);
        testClassesJar.addClass(MessageDigestGenerator.class);
        testClassesJar.addClass(StreamUtil.class);
        testClassesJar.addClass(AssertUtils.class);
        testClassesJar.addClasses(PropertyMatcher.class, MatchResult.class, PropertyMatchException.class);
        testClassesJar.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml")); // add CDI injection (needed by arquillian injection);
        testClassesJar.addAsResource("test-scheduler.properties");

        // create test ear
        MavenDependencyResolver earResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        // this must be named rhq.ear because the "rhq" portion is used in the jndi names
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "rhq.ear");
        EnterpriseArchive ear2 = earResolver.artifact("org.rhq:rhq-enterprise-server-ear:ear:4.6.0-SNAPSHOT")
            .resolveAs(EnterpriseArchive.class).iterator().next();
        //ear = ear.merge(ear2, Filters.exclude(".*\\.war.*|.*application.xml.*|.*rhq-enterprise-server-ejb3.*"));
        ear = ear.merge(ear2, Filters.include("/lib.*|/rhq.*ejb3\\.jar.*|/rhq-enterprise-server-services-sar.*"));

        //        ear.addAsModule((resolver.artifact("org.rhq:rhq-enterprise-server:4.6.0-SNAPSHOT").resolveAsFiles())[0],
        //            "rhq-core-enterprise-server-ejb3.jar");
        //        ear.addAsModule((resolver.artifact("org.rhq:rhq-core-domain:4.6.0-SNAPSHOT").resolveAsFiles())[0],
        //            "rhq-core-domain-ejb3.jar");
        //ear.getAsType(JavaArchive.class, "rhq-enterprise-server-ejb3.jar")
        //ear = ear.addAsModule(serverJar); // the jar under test
        ear = ear.addAsLibrary(testClassesJar); // the actual test classes
        ear = ear.addAsManifestResource("jboss-deployment-structure.xml");
        ear = ear.setApplicationXML("application.xml"); // the application xml declaring the ejb jar
        //ear = ear.addAsLibrary("persistence.jar");

        //System.out.println("** The Deployment EAR: " + ear.toString(true) + "\n");

        //ear.delete("rhq-portal.war");
        //ear.delete("coregui.war");

        //        ear.merge(resolver.artifact("org.rhq:rhq-enterprise-server-ear:4.6.0-SNAPSHOT")
        //            .resolveAs(EnterpriseArchive.class).iterator().next());
        //            .addAsModule(ejbJar) // the jar under test
        //            .addAsLibrary(testClassesJar) // the actual test classes
        //            .setApplicationXML("application.xml"); // the application xml declaring the ejb jar

        // Adding the 3rd party jars is not easy.  There were basically two approaches I could think of:
        //
        // 1) Use the MavenDependencyResolver to figure out and add all of the test deps and put them in lib
        // 
        // This immediately ran into trouble as there were way more jars sucked in than were actually necessary.
        // Furthermore, it included arquillian, shrinkwrap, jboss, etc.. lots of jars that actually caused
        // issues like locking and other horrible things due, I suppose, to just stepping all over the test env
        // set up by Arquillian.  So, the next step was to try and start excluding the unwanted jars.  This was
        // tedious and difficult and I didn't really get it close to working before giving up and going with
        // option 2.
        //
        // 2) Use the MavenDependencyResolver to locate and pull just the artifacts we need.
        //
        // This is annoying because it's basically duplicating the same sort of effort that we already
        // use maven to do for us. It involves running, failing on NoClassDefFound, fixing it, repeat. It
        // does pull in necessary transitive deps but sometimes it pulls in unwanted transitive deps. So,
        // we still end up having to do some exclusion filtering.  Since Shrinkwrap has weak and buggy         
        // filtering, we have some homegrown filtering methods below. 
        // TODO: Is there any way to not have to specify the versions for the transitive deps? This is brittle as is.

        //load 3rd party deps explicitly
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class);
        resolver.loadMetadataFromPom("pom.xml");
        Collection<JavaArchive> dependencies = new HashSet<JavaArchive>();
        dependencies.addAll(resolver.artifact("org.powermock:powermock-api-mockito").resolveAs(JavaArchive.class));
        dependencies.addAll(resolver.artifact("org.liquibase:liquibase-core").resolveAs(JavaArchive.class));
        dependencies.addAll(resolver.artifact("org.rhq:test-utils").resolveAs(JavaArchive.class));
        dependencies.addAll(resolver.artifact("org.rhq.helpers:perftest-support").resolveAs(JavaArchive.class));
        //dependencies.addAll(resolver.artifact("commons-io:commons-io").resolveAs(JavaArchive.class));
        //dependencies.addAll(resolver.artifact("org.unitils:unitils-testng:3.1").resolveAs(JavaArchive.class));

        //dependencies.addAll(resolver.artifact("org.rhq:rhq-core-domain").resolveAs(JavaArchive.class));
        String[] excludeFilters = { "testng.*jdk", "rhq-core-domain.*jar" };
        //
        dependencies = exclude(dependencies, excludeFilters);
        ear = ear.addAsLibraries(dependencies);
        //        ear.addAsModule((resolver.artifact("org.rhq:rhq-core-domain").resolveAsFiles())[0], "rhq-core-domain-ejb3.jar");

        //System.out.println("** The Deployment EAR: " + ear.toString(true) + "\n");

        try {
            ZipExporter exporter = new ZipExporterImpl(ear);
            exporter.exportTo(new File("/home/jshaughn/temp/test-ear.ear"), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ear;
    }

    /**
     * @param dependencies The current set of deps - THIS IS FILTERED, not copied
     * @param filters regex filters
     * @return the filtered set of deps
     */
    public static Collection<JavaArchive> exclude(Collection<JavaArchive> dependencies, String... filters) {

        for (String filter : filters) {
            Pattern p = Pattern.compile(filter);

            for (Iterator<JavaArchive> i = dependencies.iterator(); i.hasNext();) {
                JavaArchive dependency = i.next();
                if (p.matcher(dependency.getName()).find()) {
                    i.remove();
                }
            }
        }

        return dependencies;
    }

    /**
     * @param dependencies The current set of deps - this is not changed
     * @param filters regex filters
     * @return set set of JavaArchives in dependencies that matched an include filter
     */
    public static Collection<JavaArchive> include(Collection<JavaArchive> dependencies, String... filters) {

        Collection<JavaArchive> result = new HashSet<JavaArchive>();

        for (String filter : filters) {
            Pattern p = Pattern.compile(filter);

            for (Iterator<JavaArchive> i = dependencies.iterator(); i.hasNext();) {
                JavaArchive dependency = i.next();
                if (p.matcher(dependency.getName()).find()) {
                    result.add(dependency);
                }
            }
        }

        return result;
    }

    public static JavaArchive addClasses(JavaArchive archive, File dir, String packageName) {

        packageName = (null == packageName) ? "" + dir.getName() : packageName + "." + dir.getName();

        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                archive = addClasses(archive, file, packageName);
            } else if (fileName.endsWith(".class")) {
                int dot = fileName.indexOf('.');
                try {
                    //Class<?> clazz = Class.forName(packageName + "." + fileName.substring(0, dot));
                    archive.addClass(packageName + "." + fileName.substring(0, dot));
                } catch (Exception e) {
                    System.out.println("WARN: Could not add class:" + e);
                }
            }
        }

        return archive;
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @BeforeMethod</p>
     * 
     * Instead, override {@link #beforeMethod()}.  If you must override, for example, if you
     * need to use special attributes on your annotation, then ensure you protect the code with
     * and {@link #inContainer()} call.
     */
    @BeforeMethod
    protected void __beforeMethod() throws Throwable {
        // Note that Arquillian calls the testng BeforeMethod twice (as of 1.0.2.Final, once 
        // out of container and once in container. In general the expectation is to execute it 
        // one time, and doing it in container allows for the expected injections and context.
        if (inContainer()) {
            try {
                beforeMethod();
            } catch (Throwable t) {
                // Arquillian is eating these, make sure they show up in some way
                System.out.println("BEFORE METHOD FAILURE, TEST DID NOT RUN!!! " + t.getMessage());
                throw t;
            }
        }
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @AfterMethod</p>
     * 
     * Instead, override {@link #afterMethod()}.
     */
    @AfterMethod
    protected void __afterMethod() throws Exception {
        if (inContainer()) {
            afterMethod();
        }
    }

    protected boolean inContainer() {
        // If the injection is done we're running in the container. 
        return (null != initialContext);
    }

    /**
     * Override Point!  Do not implement a @BeforeMethod, instead override this method. 
     */
    protected void beforeMethod() throws Exception {
        // do nothing if we're not overridden
    }

    /**
     * Override Point!  Do not implement an @AfterMethod, instead override this method. 
     */
    protected void afterMethod() throws Exception {
        // do nothing if we're not overridden
    }

    protected void startTransaction() throws Exception {
        getTransactionManager().begin();
    }

    protected void commitTransaction() throws Exception {
        getTransactionManager().commit();
    }

    protected void rollbackTransaction() throws Exception {
        getTransactionManager().rollback();
    }

    protected InitialContext getInitialContext() {
        // may be null if not yet injected (as of 1.0.1.Final, only injected inside @Test)
        if (null != initialContext) {
            return initialContext;
        }

        InitialContext result = null;

        try {
            Properties jndiProperties = new Properties();
            jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
            result = new InitialContext(jndiProperties);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get InitialContext", e);
        }

        return result;
    }

    public TransactionManager getTransactionManager() {
        TransactionManager result = null;

        try {
            result = (TransactionManager) getInitialContext().lookup("java:jboss/TransactionManager");
            if (null == result) {
                result = (TransactionManager) getInitialContext().lookup("TransactionManager");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to get TransactionManager", e);
        }

        return result;
    }

    protected EntityManager getEntityManager() {
        // may be null if not yet injected (as of 1.0.1.Final, only injected inside @Test)
        if (null != em) {
            return em;
        }

        EntityManager result = null;

        try {
            EntityManagerFactory emf = (EntityManagerFactory) getInitialContext().lookup(
                "java:jboss/RHQEntityManagerFactory");
            result = emf.createEntityManager();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get EntityManagerFactory", e);
        }

        return result;
    }

    /**
     * Equivalent to <pre>executeInTransaction(true, callback)</pre>.
     * @param callback
     */
    protected void executeInTransaction(TransactionCallback callback) {
        executeInTransaction(true, callback);
    }

    protected void executeInTransaction(boolean rollback, final TransactionCallback callback) {
        executeInTransaction(rollback, new TransactionCallbackReturnable<Void>() {

            public Void execute() throws Exception {
                callback.execute();
                return null;
            }
        });
    }

    protected <T> T executeInTransaction(TransactionCallbackReturnable<T> callback) {
        return executeInTransaction(true, callback);
    }

    protected <T> T executeInTransaction(boolean rollback, TransactionCallbackReturnable<T> callback) {
        try {
            startTransaction();
            T result = callback.execute();
            return result;

        } catch (Throwable t) {
            rollback = true;
            RuntimeException re = new RuntimeException(ThrowableUtil.getAllMessages(t), t);
            throw re;

        } finally {
            try {
                if (!rollback) {
                    commitTransaction();
                } else {
                    rollbackTransaction();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to " + (rollback ? "rollback" : "commit") + " transaction", e);
            }
        }
    }

    /* The old AbstractEJB3Test impl extended AssertJUnit. Continue to support the used methods
     * with various call-thru methods. 
     */

    protected void assertNotNull(Object o) {
        AssertJUnit.assertNotNull(o);
    }

    protected void assertNotNull(String msg, Object o) {
        AssertJUnit.assertNotNull(msg, o);
    }

    protected void assertNull(Object o) {
        AssertJUnit.assertNull(o);
    }

    protected void assertNull(String msg, Object o) {
        AssertJUnit.assertNull(msg, o);
    }

    protected void assertFalse(boolean b) {
        AssertJUnit.assertFalse(b);
    }

    protected void assertFalse(String msg, boolean b) {
        AssertJUnit.assertFalse(msg, b);
    }

    protected void assertTrue(boolean b) {
        AssertJUnit.assertTrue(b);
    }

    protected void assertTrue(String msg, boolean b) {
        AssertJUnit.assertTrue(msg, b);
    }

    protected void assertEquals(int expected, int actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, int expected, int actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(long expected, long actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, long expected, long actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(String expected, String actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, String expected, String actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(boolean expected, boolean actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, boolean expected, boolean actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(Object expected, Object actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, Object expected, Object actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void fail() {
        AssertJUnit.fail();
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    private final long DEFAULT_OFFSET = 50;
    private long referenceTime = new Date().getTime();

    public Date getAnotherDate() {
        return getAnotherDate(DEFAULT_OFFSET);
    }

    public Date getAnotherDate(long offset) {
        referenceTime += offset;
        return new Date(referenceTime);
    }

    /**
     * If you need to test server plugins, you must first prepare the server plugin service.
     * After this returns, the caller must explicitly start the PC by using the appropriate API
     * on the given mbean; this method will only start the service, it will NOT start the master PC.
     *
     * @param testServiceMBean the object that will house your test server plugins
     *
     * @throws RuntimeException
     */
    public void prepareCustomServerPluginService(ServerPluginService testServiceMBean) {
        try {
            // first, unregister the real service...
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(ServerPluginService.OBJECT_NAME)) {
                mbs.unregisterMBean(ServerPluginService.OBJECT_NAME);
            }

            // Now replace with the test service...
            testServiceMBean.start();
            mbs.registerMBean(testServiceMBean, ServerPluginServiceMBean.OBJECT_NAME);
            serverPluginService = testServiceMBean;
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unprepareServerPluginService() throws Exception {
        unprepareServerPluginService(false);
    }

    public void unprepareServerPluginService(boolean beanOnly) throws Exception {
        if (serverPluginService != null) {
            serverPluginService.stopMasterPluginContainer();
            serverPluginService.stop();
            MBeanServer mbs = getPlatformMBeanServer();
            if (beanOnly) {
                if (mbs.isRegistered(ServerPluginService.OBJECT_NAME)) {
                    mbs.unregisterMBean(ServerPluginService.OBJECT_NAME);
                }

            } else {
                //MBeanServerFactory.releaseMBeanServer(mbs);
            }
            serverPluginService = null;
        }
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public void prepareScheduler() {
        try {
            if (schedulerService != null) {
                return;
            }

            // first, unregister the real service...
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(SchedulerService.SCHEDULER_MBEAN_NAME)) {
                mbs.unregisterMBean(SchedulerService.SCHEDULER_MBEAN_NAME);
            }

            // Now replace with the test service...
            Properties quartzProps = new Properties();
            quartzProps.load(this.getClass().getClassLoader().getResourceAsStream("test-scheduler.properties"));

            schedulerService = new SchedulerService();
            schedulerService.setQuartzProperties(quartzProps);
            schedulerService.start();
            mbs.registerMBean(schedulerService, SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
            schedulerService.startQuartzScheduler();

            //            MBeanServer mbs = getPlatformMBeanServer();
            //            schedulerService = MBeanServerInvocationHandler.newProxyInstance(mbs,
            //                SchedulerServiceMBean.SCHEDULER_MBEAN_NAME, SchedulerServiceMBean.class, false);
            //
            //            Properties quartzProps = new Properties();
            //            quartzProps.load(this.getClass().getClassLoader().getResourceAsStream("test-scheduler.properties"));
            //            //schedulerService = LookupUtil.getSchedulerBean();
            //            //schedulerService = new SchedulerService();
            //            schedulerService.setQuartzProperties(quartzProps);
            //            //schedulerService.start();
            //            //getJBossMBeanServer().registerMBean(schedulerService, SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
            //            schedulerService.startQuartzScheduler();
            //            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unprepareScheduler() throws Exception {
        unprepareScheduler(false);
    }

    public void unprepareScheduler(boolean beanOnly) throws Exception {
        if (schedulerService != null) {
            //schedulerService.stop();
            schedulerService = null;

            MBeanServer mbs = getPlatformMBeanServer();
            if (beanOnly) {
                if (mbs.isRegistered(SchedulerService.SCHEDULER_MBEAN_NAME)) {
                    mbs.unregisterMBean(SchedulerService.SCHEDULER_MBEAN_NAME);
                }
            } else {
                //MBeanServerFactory.releaseMBeanServer(mbs);
            }

        }
    }

    public PluginDeploymentScannerMBean getPluginScannerService() {
        return pluginScannerService;
    }

    protected void preparePluginScannerService() {
        preparePluginScannerService(null);
    }

    public void preparePluginScannerService(PluginDeploymentScannerMBean scannerService) {
        try {
            if (scannerService == null) {
                scannerService = new PluginDeploymentScanner();
            }
            //            MBeanServer mbs = getJBossMBeanServer();
            //            mbs.registerMBean(scannerService, PluginDeploymentScannerMBean.OBJECT_NAME);
            //            pluginScannerService = scannerService;
            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unpreparePluginScannerService() throws Exception {
        unpreparePluginScannerService(false);
    }

    public void unpreparePluginScannerService(boolean beanOnly) throws Exception {
        if (pluginScannerService != null) {
            pluginScannerService.stop();
            if (beanOnly) {
                //                MBeanServer mbs = getJBossMBeanServer();
                //                if (mbs.isRegistered(PluginDeploymentScannerMBean.OBJECT_NAME)) {
                //                    getJBossMBeanServer().unregisterMBean(PluginDeploymentScannerMBean.OBJECT_NAME);
            }
        } else {
            //                releaseJBossMBeanServer();
        }

        pluginScannerService = null;
    }

    /**
     * If you need to test round trips from server to agent and back, you first must install the server communications
     * service that houses all the agent clients. Call this method and add your test agent services to the public fields
     * in the returned object.
     *
     * @return the object that will house your test agent service impls and the agent clients.
     *
     * @throws RuntimeException
     */
    public TestServerCommunicationsService prepareForTestAgents() {
        try {
            //            MBeanServer mbs = getJBossMBeanServer();
            //            if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
            //                mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
            //            }
            TestServerCommunicationsService testAgentContainer = new TestServerCommunicationsService();
            //            mbs.registerMBean(testAgentContainer, ServerCommunicationsServiceMBean.OBJECT_NAME);
            return testAgentContainer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call this after your tests have finished. You only need to call this if your test previously called
     * {@link #prepareForTestAgents()}.
     */
    public void unprepareForTestAgents() {
        unprepareForTestAgents(false);
    }

    public void unprepareForTestAgents(boolean beanOnly) {
        try {
            if (beanOnly) {
                //                MBeanServer mbs = getJBossMBeanServer();
                //                if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
                //                    mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
                //                }
            } else {
                //                releaseJBossMBeanServer();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MBeanServer getPlatformMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * This creates a session for the given user and associates that session with the subject. You can test the security
     * annotations by creating sessions for different users with different permissions.
     *
     * @param subject a JON subject
     * @return the session activated subject, a copy of the subject passed in. 
     */
    public Subject createSession(Subject subject) {
        return SessionManager.getInstance().put(subject);
    }

    public static Connection getConnection() throws SQLException {
        return LookupUtil.getDataSource().getConnection();
    }

}
