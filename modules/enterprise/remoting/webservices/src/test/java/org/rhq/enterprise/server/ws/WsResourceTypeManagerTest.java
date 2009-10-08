package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * These tests can not be executed in our standard unit test fashion as they
 * require a running RHQ Server with our web services deployed.
 * 
 * This is still in development and has the current restrictions: - add
 * [dev_root
 * ]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices
 * -{version}.jar to TOP of eclipse classpath to run from your IDE(actually need
 * to use classpath setup from bin/jbossas/bin/wsrunclient.sh to take advantage
 * of type substitution correctly) - Server running on localhost. - ws-test user
 * defined in database with full permissions - Non RHQ Server JBossAS in
 * inventory. - The -Ptest-ws profile specified when running mvn test from
 * webservices dir - Perftest plugin installed and agent started as described in
 * modules/enterprise/remoting/scripts/README.txt
 * 
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsResourceTypeManagerTest extends AssertJUnit implements
		TestPropertiesInterface {

	private static ObjectFactory WS_OBJECT_FACTORY;
	private static WebservicesRemote WEBSERVICE_REMOTE;
	private static Subject subject = null;

	@BeforeClass
	public void init() throws ClassNotFoundException, MalformedURLException,
			SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, LoginException_Exception {

		// build reference variable bits
		URL gUrl = WsUtility.generateRemoteWebserviceURL(
				WebservicesManagerBeanService.class, host, port, useSSL);
		QName gQName = WsUtility
				.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
		WebservicesManagerBeanService jws = new WebservicesManagerBeanService(
				gUrl, gQName);

		WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
		WS_OBJECT_FACTORY = new ObjectFactory();
		WsSubjectTest.checkForWsTestUserAndRole();
		subject = WEBSERVICE_REMOTE.login(credentials, credentials);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithFiltering() {
		ResourceType resourceType = WEBSERVICE_REMOTE
				.getResourceTypeByNameAndPlugin(subject, "service-alpha",
						"PerfTest");

		ResourceTypeCriteria criteria = new ResourceTypeCriteria();
		criteria.setFilterName("service-alpha");
		criteria.setFilterDescription(resourceType.description);
		criteria.setFilterCategory(ResourceCategory.SERVICE);
		criteria.setFilterPluginName("PerfTest");
		criteria
				.setFilterCreationDataType(ResourceCreationDataType.CONFIGURATION);
		criteria.setFilterCreateDeletePolicy(CreateDeletePolicy.NEITHER);
		criteria.setFilterSupportsManualAdd(false);

		List<ResourceType> resourceTypes = WEBSERVICE_REMOTE
				.findResourceTypesByCriteria(subject, criteria);

		assertEquals("Failed to find resource type when filtering",
				resourceTypes.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithFetchingAssociations() {
		ResourceType resourceType = WEBSERVICE_REMOTE
				.getResourceTypeByNameAndPlugin(subject, "service-alpha",
						"PerfTest");

		ResourceTypeCriteria criteria = new ResourceTypeCriteria();
		criteria.setFilterId(resourceType.id);
		criteria.setFetchSubCategory(true);
		criteria.setFetchChildResourceTypes(true);
		criteria.setFetchParentResourceTypes(true);
		criteria.setFetchPluginConfigurationDefinition(true);
		criteria.setFetchResourceConfigurationDefinition(true);
		criteria.setFetchMetricDefinitions(true);
		criteria.setFetchEventDefinitions(true);
		criteria.setFetchOperationDefinitions(true);
		criteria.setFetchProcessScans(true);
		criteria.setFetchPackageTypes(true);
		criteria.setFetchSubCategories(true);
		criteria.setFetchProductVersions(true);

		List<ResourceType> resourceTypes = WEBSERVICE_REMOTE
				.findResourceTypesByCriteria(subject, criteria);

		assertEquals("Failed to find resource type when fetching associations",
				resourceTypes.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithSorting() {
		ResourceTypeCriteria criteria = new ResourceTypeCriteria();
		criteria.setSortName(PageOrdering.ASC);
		criteria.setSortCategory(PageOrdering.DESC);
		criteria.setSortPluginName(PageOrdering.ASC);

		List<ResourceType> resourceTypes = WEBSERVICE_REMOTE
				.findResourceTypesByCriteria(subject, criteria);

		assertTrue("Failed to find resource types when sorting", resourceTypes
				.size() > 0);
	}

}
