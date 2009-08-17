package org.rhq.enterprise.server.webservices;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.jws.WebService;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.ChannelCriteria;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationUpdateStillInProgressException;
import org.rhq.enterprise.server.content.ChannelException;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.content.ChannelManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.report.DataAccessManagerLocal;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.support.SupportManagerLocal;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.system.ServerVersion;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.util.LookupUtil;

/** The purpose of this class is to aggregate all the EJB remote implementation into one
 *  class that can be annotated by JBossWS.  Each annotated SLSB causes a full WSDL compile and
 *  publish by JBossWS which is very costly in terms of time.  Deploy times went from 2 mins to 12 mins. 
 * 
 * @author Simeon Pinder
 *
 */
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.webservices.JonWebservicesRemote", targetNamespace = ServerVersion.namespace)
public class JonWebservicesManagerBean implements AlertManagerRemote, AlertDefinitionManagerRemote,
    AvailabilityManagerRemote, CallTimeDataManagerRemote, ChannelManagerRemote, ConfigurationManagerRemote,
    ContentManagerRemote, DataAccessManagerRemote, EventManagerRemote, MeasurementBaselineManagerRemote,
    MeasurementDataManagerRemote, MeasurementDefinitionManagerRemote, MeasurementProblemManagerRemote,
    MeasurementScheduleManagerRemote, OperationManagerRemote, ResourceManagerRemote, ResourceGroupManagerRemote,
    RoleManagerRemote, SubjectManagerRemote, SupportManagerRemote, SystemManagerRemote {

    //Lookup the required beans as local references
    private AlertManagerLocal alertManager = LookupUtil.getAlertManager();
    private AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
    private AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
    private CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();
    private ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private ContentManagerLocal contentManager = LookupUtil.getContentManager();
    private DataAccessManagerLocal dataAccessManager = LookupUtil.getDataAccessManager();
    private EventManagerLocal eventManager = LookupUtil.getEventManager();
    private MeasurementBaselineManagerLocal measurementBaselineManager = LookupUtil.getMeasurementBaselineManager();
    private MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();
    private MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
        .getMeasurementDefinitionManager();
    private MeasurementProblemManagerLocal measurementProblemManager = LookupUtil.getMeasurementProblemManager();
    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();
    private RoleManagerLocal roleManager = LookupUtil.getRoleManager();
    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
    private SupportManagerLocal supportManager = LookupUtil.getSupportManager();
    private SystemManagerLocal systemManager = LookupUtil.getSystemManager();

    //ALERTMANAGER: BEGIN ------------------------------------------
    @Override
    public PageList<Alert> findAlertsByCriteria(Subject subject, AlertCriteria criteria) {
        return alertManager.findAlertsByCriteria(subject, criteria);
    }

    //ALERTMANAGER: END --------------------------------------------

    //ALERTDEFINITIONMANAGER: BEGIN --------------------------------

    @Override
    public AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId) {
        return alertDefinitionManager.getAlertDefinition(subject, alertDefinitionId);
    }

    @Override
    public PageList<AlertDefinition> findAlertDefinitionsByCriteria(Subject subject, AlertDefinitionCriteria criteria) {
        return alertDefinitionManager.findAlertDefinitionsByCriteria(subject, criteria);
    }

    //ALERTDEFINITIONMANAGER: END ----------------------------------

    //AVAILABILITYMANAGER: BEGIN ----------------------------------
    @Override
    public PageList<Availability> findAvailabilityForResource(Subject subject, int resourceId, PageControl pc) {
        return availabilityManager.findAvailabilityForResource(subject, resourceId, pc);
    }

    @Override
    public Availability getCurrentAvailabilityForResource(Subject subject, int resourceId) {
        return availabilityManager.getCurrentAvailabilityForResource(subject, resourceId);
    }

    //AVAILABILITYMANAGER: END ----------------------------------

    //CALLTIMEDATAMANAGER: BEGIN ----------------------------------
    @Override
    public PageList<CallTimeDataComposite> findCallTimeDataForResource(Subject subject, int scheduleId, long beginTime,
        long endTime, PageControl pc) {
        return callTimeDataManager.findCallTimeDataForResource(subject, scheduleId, beginTime, endTime, pc);
    }

    //CALLTIMEDATAMANAGER: END ----------------------------------

    //CHANNELMANAGER: BEGIN ----------------------------------
    @Override
    public void addPackageVersionsToChannel(Subject subject, int channelId, int[] packageVersionIds) {
        channelManager.addPackageVersionsToChannel(subject, channelId, packageVersionIds);
    }

    @Override
    public Channel createChannel(Subject subject, Channel channel) throws ChannelException {
        return channelManager.createChannel(subject, channel);
    }

    @Override
    public void deleteChannel(Subject subject, int channelId) {
        channelManager.deleteChannel(subject, channelId);
    }

    @Override
    public PageList<Channel> findChannels(Subject subject, PageControl pc) {
        return channelManager.findChannels(subject, pc);
    }

    @Override
    public PageList<Channel> findChannelsByCriteria(Subject subject, ChannelCriteria criteria) {
        return channelManager.findChannelsByCriteria(subject, criteria);
    }

    @Override
    public PageList<PackageVersion> findPackageVersionsInChannel(Subject subject, int channelId, String filter,
        PageControl pc) {
        return channelManager.findPackageVersionsInChannel(subject, channelId, filter, pc);
    }

    @Override
    public PageList<PackageVersion> findPackageVersionsInChannelByCriteria(Subject subject,
        PackageVersionCriteria criteria) {
        return channelManager.findPackageVersionsInChannelByCriteria(subject, criteria);
    }

    @Override
    public PageList<Resource> findSubscribedResources(Subject subject, int channelId, PageControl pc) {
        return channelManager.findSubscribedResources(subject, channelId, pc);
    }

    @Override
    public Channel getChannel(Subject subject, int channelId) {
        return channelManager.getChannel(subject, channelId);
    }

    @Override
    public void subscribeResourceToChannels(Subject subject, int resourceId, int[] channelIds) {
        channelManager.subscribeResourceToChannels(subject, resourceId, channelIds);
    }

    @Override
    public void unsubscribeResourceFromChannels(Subject subject, int resourceId, int[] channelIds) {
        channelManager.unsubscribeResourceFromChannels(subject, resourceId, channelIds);
    }

    @Override
    public Channel updateChannel(Subject subject, Channel channel) throws ChannelException {
        return channelManager.updateChannel(subject, channel);
    }

    @Override
    public PageList<PackageVersion> findPackageVersionsByCriteria(Subject subject, PackageVersionCriteria criteria) {
        return channelManager.findPackageVersionsInChannelByCriteria(subject, criteria);
    }

    //CHANNELMANAGER: END ----------------------------------

    //CONFIGURATIONMANAGER: BEGIN ----------------------------------
    @Override
    public Configuration getConfiguration(Subject subject, int configurationId) {
        return configurationManager.getConfiguration(subject, configurationId);
    }

    @Override
    public GroupPluginConfigurationUpdate getGroupPluginConfigurationUpdate(Subject subject, int configurationUpdateId) {
        return configurationManager.getGroupPluginConfigurationUpdate(subject, configurationUpdateId);
    }

    @Override
    public GroupResourceConfigurationUpdate getGroupResourceConfigurationUpdate(Subject subject,
        int configurationUpdateId) {
        return configurationManager.getGroupResourceConfigurationUpdate(subject, configurationUpdateId);
    }

    @Override
    public PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject subject, int resourceId) {
        return configurationManager.getLatestPluginConfigurationUpdate(subject, resourceId);
    }

    @Override
    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId) {
        return configurationManager.getLatestResourceConfigurationUpdate(subject, resourceId);
    }

    @Override
    public Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst)
        throws Exception {
        return configurationManager.getLiveResourceConfiguration(subject, resourceId, pingAgentFirst);
    }

    @Override
    public Configuration getPluginConfiguration(Subject subject, int resourceId) {
        return configurationManager.getPluginConfiguration(subject, resourceId);
    }

    @Override
    public ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
        return configurationManager.getPluginConfigurationDefinitionForResourceType(subject, resourceTypeId);
    }

    @Override
    public Configuration getResourceConfiguration(Subject subject, int resourceId) {
        return configurationManager.getResourceConfiguration(subject, resourceId);
    }

    @Override
    public ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
        return configurationManager.getResourceConfigurationDefinitionForResourceType(subject, resourceTypeId);
    }

    @Override
    public ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(Subject subject,
        int resourceTypeId) {
        return configurationManager.getResourceConfigurationDefinitionWithTemplatesForResourceType(subject,
            resourceTypeId);
    }

    @Override
    public boolean isGroupResourceConfigurationUpdateInProgress(Subject subject, int resourceGroupId) {
        return configurationManager.isGroupResourceConfigurationUpdateInProgress(subject, resourceGroupId);
    }

    @Override
    public boolean isResourceConfigurationUpdateInProgress(Subject subject, int resourceId) {
        return configurationManager.isResourceConfigurationUpdateInProgress(subject, resourceId);
    }

    @Override
    public int scheduleGroupResourceConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> newResourceConfigurationMap) {
        return configurationManager.scheduleGroupResourceConfigurationUpdate(subject, compatibleGroupId,
            newResourceConfigurationMap);
    }

    @Override
    public PluginConfigurationUpdate updatePluginConfiguration(Subject subject, int resourceId,
        Configuration newConfiguration) throws ResourceNotFoundException {
        return configurationManager.updatePluginConfiguration(subject, resourceId, newConfiguration);
    }

    @Override
    public ResourceConfigurationUpdate updateResourceConfiguration(Subject subject, int resourceId,
        Configuration newConfiguration) throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException {
        return configurationManager.updateResourceConfiguration(subject, resourceId, newConfiguration);
    }

    //CONFIGURATIONMANAGER: END ----------------------------------

    //CONTENTMANAGER: BEGIN ----------------------------------
    @Override
    public PackageVersion createPackageVersion(Subject subject, String packageName, int packageTypeId, String version,
        int architectureId, byte[] packageBytes) {
        return contentManager.createPackageVersion(subject, packageName, packageTypeId, version, architectureId,
            packageBytes);
    }

    @Override
    public void deletePackages(Subject subject, int resourceId, int[] installedPackageIds, String requestNotes) {
        contentManager.deletePackages(subject, resourceId, installedPackageIds, requestNotes);
    }

    @Override
    public void deployPackages(Subject subject, int[] resourceIds, int[] packageVersionIds) {
        contentManager.deployPackages(subject, resourceIds, packageVersionIds);
    }

    @Override
    public List<Architecture> findArchitectures(Subject subject) {
        return contentManager.findArchitectures(subject);
    }

    @Override
    public PageList<InstalledPackage> findInstalledPackagesByCriteria(Subject subject, InstalledPackageCriteria criteria) {
        return contentManager.findInstalledPackagesByCriteria(subject, criteria);
    }

    @Override
    public List<PackageType> findPackageTypes(Subject subject, String resourceTypeName, String pluginName)
        throws ResourceTypeNotFoundException {
        return contentManager.findPackageTypes(subject, resourceTypeName, pluginName);
    }

    @Override
    public InstalledPackage getBackingPackageForResource(Subject subject, int resourceId) {
        return contentManager.getBackingPackageForResource(subject, resourceId);
    }

    @Override
    public byte[] getPackageBytes(Subject user, int resourceId, int installedPackageId) {
        return contentManager.getPackageBytes(user, resourceId, installedPackageId);
    }

    //CONTENTMANAGER: END ----------------------------------

    //DATAACCESSMANAGER: BEGIN ----------------------------------
    @Override
    public List<Object[]> executeQuery(Subject subject, String query) {
        return dataAccessManager.executeQuery(subject, query);
    }

    @Override
    public List<Object[]> executeQueryWithPageControl(Subject subject, String query, PageControl pageControl) {
        return dataAccessManager.executeQueryWithPageControl(subject, query, pageControl);
    }

    //DATAACCESSMANAGER: END ----------------------------------

    //EVENTMANAGER: BEGIN ----------------------------------
    @Override
    public PageList<Event> findEventsByCriteria(Subject subject, EventCriteria criteria) {
        return eventManager.findEventsByCriteria(subject, criteria);
    }

    @Override
    public EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets) {
        return eventManager.getSeverityBuckets(subject, resourceId, begin, end, numBuckets);
    }

    @Override
    public EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentResourceId, int resourceTypeId,
        long begin, long end, int numBuckets) {
        return eventManager.getSeverityBucketsForAutoGroup(subject, parentResourceId, resourceTypeId, begin, end,
            numBuckets);
    }

    @Override
    public EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int resourceGroupId, long begin, long end,
        int numBuckets) {
        return eventManager.getSeverityBucketsForCompGroup(subject, resourceGroupId, begin, end, numBuckets);
    }

    //EVENTMANAGER: END ----------------------------------

    //MEASUREMENTBASELINEMANAGER: BEGIN ----------------------------------
    @Override
    public List<MeasurementBaseline> findBaselinesForResource(Subject subject, int resourceId) {
        return measurementBaselineManager.findBaselinesForResource(subject, resourceId);
    }

    //MEASUREMENTBASELINEMANAGER: END ----------------------------------

    //MEASUREMENTDATAMANAGER: BEGIN ----------------------------------
    @Override
    public List<MeasurementDataTrait> findCurrentTraitsForResource(Subject subject, int resourceId,
        DisplayType displayType) {
        return measurementDataManager.findCurrentTraitsForResource(subject, resourceId, displayType);
    }

    @Override
    public List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(Subject subject, int groupId,
        int definitionId, long beginTime, long endTime, int numPoints, boolean groupAggregateOnly) {
        return measurementDataManager.findDataForCompatibleGroup(subject, groupId, definitionId, beginTime, endTime,
            numPoints, groupAggregateOnly);
    }

    @Override
    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(Subject subject, int resourceId,
        int[] definitionIds, long beginTime, long endTime, int numPoints) {
        return measurementDataManager.findDataForResource(subject, resourceId, definitionIds, beginTime, endTime,
            numPoints);
    }

    @Override
    public Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds) {
        return measurementDataManager.findLiveData(subject, resourceId, definitionIds);
    }

    @Override
    public List<MeasurementDataTrait> findTraits(Subject subject, int resourceId, int definitionId) {
        return measurementDataManager.findTraits(subject, resourceId, definitionId);
    }

    @Override
    public MeasurementAggregate getAggregate(Subject subject, int scheduleId, long startTime, long endTime) {
        return measurementDataManager.getAggregate(subject, scheduleId, startTime, endTime);
    }

    //MEASUREMENTDATAMANAGER: END ----------------------------------

    //MEASUREMENTDEFINITIONMANAGER: BEGIN ----------------------------------
    @Override
    public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(Subject subject,
        MeasurementDefinitionCriteria criteria) {
        return measurementDefinitionManager.findMeasurementDefinitionsByCriteria(subject, criteria);
    }

    @Override
    public MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId) {
        return measurementDefinitionManager.getMeasurementDefinition(subject, definitionId);
    }

    //MEASUREMENTDEFINITIONMANAGER: END ----------------------------------

    //MEASUREMENTPROBLEMMANAGER: BEGIN ----------------------------------
    @Override
    public PageList<ProblemResourceComposite> findProblemResources(Subject subject, long oldestDate, PageControl pc) {
        return measurementProblemManager.findProblemResources(subject, oldestDate, pc);
    }

    //MEASUREMENTPROBLEMMANAGER: END ----------------------------------

    //MEASUREMENTSCHEDULEMANAGER: BEGIN ----------------------------------
    @Override
    public void disableSchedules(Subject subject, int resourceId, int[] measurementDefinitionIds) {
        measurementScheduleManager.disableSchedules(subject, resourceId, measurementDefinitionIds);
    }

    @Override
    public void disableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds) {
        measurementScheduleManager.disableSchedulesForCompatibleGroup(subject, groupId, measurementDefinitionIds);
    }

    @Override
    public void enableSchedules(Subject subject, int resourceId, int[] measurementDefinitionIds) {
        measurementScheduleManager.enableSchedules(subject, resourceId, measurementDefinitionIds);
    }

    @Override
    public void enableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds) {
        measurementScheduleManager.enableSchedulesForCompatibleGroup(subject, groupId, measurementDefinitionIds);
    }

    @Override
    public PageList<MeasurementSchedule> getSchedulesByCriteria(Subject subject, MeasurementScheduleCriteria criteria) {
        return measurementScheduleManager.getSchedulesByCriteria(subject, criteria);
    }

    //MEASUREMENTSCHEDULEMANAGER: END ----------------------------------

    //OPERATIONMANAGER: BEGIN ----------------------------------
    @Override
    public void cancelOperationHistory(Subject subject, int operationHistoryId, boolean ignoreAgentErrors) {
        operationManager.cancelOperationHistory(subject, operationHistoryId, ignoreAgentErrors);
    }

    @Override
    public void deleteOperationHistory(Subject subject, int operationHistoryId, boolean purgeInProgress) {
        operationManager.deleteOperationHistory(subject, operationHistoryId, purgeInProgress);
    }

    @Override
    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(Subject subject,
        GroupOperationHistoryCriteria criteria) {
        return operationManager.findGroupOperationHistoriesByCriteria(subject, criteria);
    }

    @Override
    public List<OperationDefinition> findOperationDefinitionsByCriteria(Subject subject,
        OperationDefinitionCriteria criteria) {
        return operationManager.findOperationDefinitionsByCriteria(subject, criteria);
    }

    @Override
    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(Subject subject,
        ResourceOperationHistoryCriteria criteria) {
        return operationManager.findResourceOperationHistoriesByCriteria(subject, criteria);
    }

    @Override
    public List<GroupOperationSchedule> findScheduledGroupOperations(Subject subject, int groupId) throws Exception {
        return operationManager.findScheduledGroupOperations(subject, groupId);
    }

    @Override
    public List<ResourceOperationSchedule> findScheduledResourceOperations(Subject subject, int resourceId)
        throws Exception {
        return operationManager.findScheduledResourceOperations(subject, resourceId);
    }

    @Override
    public GroupOperationSchedule scheduleGroupOperation(Subject subject, int groupId, int[] executionOrderResourceIds,
        boolean haltOnFailure, String operationName, Configuration parameters, long delay, long repeatInterval,
        int repeatCount, int timeout, String description) throws ScheduleException {
        return operationManager.scheduleGroupOperation(subject, groupId, executionOrderResourceIds, haltOnFailure,
            operationName, parameters, delay, repeatInterval, repeatCount, timeout, description);
    }

    @Override
    public ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        long delay, long repeatInterval, int repeatCount, int timeout, Configuration parameters, String description)
        throws ScheduleException {
        return operationManager.scheduleResourceOperation(subject, resourceId, operationName, delay, repeatInterval,
            repeatCount, timeout, parameters, description);
    }

    @Override
    public void unscheduleGroupOperation(Subject subject, String jobId, int resourceGroupId) throws UnscheduleException {
        operationManager.unscheduleGroupOperation(subject, jobId, resourceGroupId);
    }

    @Override
    public void unscheduleResourceOperation(Subject subject, String jobId, int resourceId) throws UnscheduleException {
        operationManager.unscheduleResourceOperation(subject, jobId, resourceId);
    }

    //OPERATIONMANAGER: END ----------------------------------

    //RESOURCEMANAGER: BEGIN ----------------------------------
    @Override
    public PageList<ResourceComposite> findResourceComposites(Subject subject, ResourceCategory category,
        String typeName, int parentResourceId, String searchString, PageControl pageControl) {
        return resourceManager.findResourceComposites(subject, category, typeName, parentResourceId, searchString,
            pageControl);
    }

    @Override
    public List<Resource> findResourceLineage(Subject subject, int resourceId) {
        return resourceManager.findResourceLineage(subject, resourceId);
    }

    @Override
    public PageList<Resource> findResourcesByCriteria(Subject subject, ResourceCriteria criteria) {
        return resourceManager.findResourcesByCriteria(subject, criteria);
    }

    @Override
    public Resource getResource(Subject subject, int resourceId) {
        return resourceManager.getResource(subject, resourceId);
    }

    @Override
    public void uninventoryResources(Subject subject, int[] resourceIds) {
        resourceManager.uninventoryResources(subject, resourceIds);
    }

    //RESOURCEMANAGER: END ----------------------------------

    //RESOURCEGROUPMANAGER: BEGIN ----------------------------------
    @Override
    public void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds) {
        resourceGroupManager.addResourcesToGroup(subject, groupId, resourceIds);
    }

    @Override
    public ResourceGroup createResourceGroup(Subject subject, ResourceGroup resourceGroup) {
        return resourceGroupManager.createResourceGroup(subject, resourceGroup);
    }

    @Override
    public void deleteResourceGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException {
        resourceGroupManager.deleteResourceGroup(subject, groupId);
    }

    @Override
    public PageList<ResourceGroup> findResourceGroupsByCriteria(Subject subject, ResourceGroupCriteria criteria) {
        return resourceGroupManager.findResourceGroupsByCriteria(subject, criteria);
    }

    @Override
    public PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc) {
        return resourceGroupManager.findResourceGroupsForRole(subject, roleId, pc);
    }

    @Override
    public ResourceGroup getResourceGroup(Subject subject, int groupId) {
        return resourceGroupManager.getResourceGroup(subject, groupId);
    }

    @Override
    public ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId) {
        return resourceGroupManager.getResourceGroupComposite(subject, groupId);
    }

    @Override
    public void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds) {
        resourceGroupManager.removeResourcesFromGroup(subject, groupId, resourceIds);
    }

    @Override
    public void setRecursive(Subject subject, int groupId, boolean isRecursive) {
        resourceGroupManager.setRecursive(subject, groupId, isRecursive);
    }

    @Override
    public ResourceGroup updateResourceGroup(Subject subject, ResourceGroup newResourceGroup) {
        return resourceGroupManager.updateResourceGroup(subject, newResourceGroup);
    }

    //RESOURCEGROUPMANAGER: END ----------------------------------

    //ROLEMANAGER: BEGIN ----------------------------------
    @Override
    public void addResourceGroupsToRole(Subject subject, int roleId, int[] pendingGroupIds) {
        roleManager.addResourceGroupsToRole(subject, roleId, pendingGroupIds);
    }

    @Override
    public void addRolesToResourceGroup(Subject subject, int groupId, int[] roleIds) {
        roleManager.addRolesToResourceGroup(subject, groupId, roleIds);
    }

    @Override
    public void addRolesToSubject(Subject subject, int subjectId, int[] roleIds) {
        roleManager.addRolesToSubject(subject, subjectId, roleIds);
    }

    @Override
    public void addSubjectsToRole(Subject subject, int roleId, int[] subjectIds) {
        roleManager.addSubjectsToRole(subject, roleId, subjectIds);
    }

    @Override
    public PageList<Role> findRolesByCriteria(Subject subject, RoleCriteria criteria) {
        return roleManager.findRolesByCriteria(subject, criteria);
    }

    @Override
    public PageList<Role> findSubjectAssignedRoles(Subject subject, int subjectId, PageControl pc) {
        return roleManager.findSubjectAssignedRoles(subject, subjectId, pc);
    }

    @Override
    public PageList<Role> findSubjectUnassignedRoles(Subject subject, int subjectId, PageControl pc) {
        return roleManager.findSubjectUnassignedRoles(subject, subjectId, pc);
    }

    @Override
    public Role getRole(Subject subject, int roleId) {
        return roleManager.getRole(subject, roleId);
    }

    @Override
    public void removeResourceGroupsFromRole(Subject subject, int roleId, int[] groupIds) {
        roleManager.removeResourceGroupsFromRole(subject, roleId, groupIds);
    }

    @Override
    public void removeRolesFromResourceGroup(Subject subject, int groupId, int[] roleIds) {
        roleManager.removeRolesFromResourceGroup(subject, groupId, roleIds);
    }

    @Override
    public void removeRolesFromSubject(Subject subject, int subjectId, int[] roleIds) {
        roleManager.removeRolesFromSubject(subject, subjectId, roleIds);
    }

    @Override
    public void removeSubjectsFromRole(Subject subject, int roleId, int[] subjectIds) {
        roleManager.removeSubjectsFromRole(subject, roleId, subjectIds);
    }

    //ROLEMANAGER: END ----------------------------------

    //SUBJECT MANAGER: BEGIN ---------------------------------------
    @Override
    public void changePassword(Subject subject, String username, String password) {
        subjectManager.changePassword(subject, username, password);
    }

    @Override
    public void createPrincipal(Subject subject, String username, String password) throws SubjectException {
        subjectManager.createPrincipal(subject, username, password);
    }

    @Override
    public Subject createSubject(Subject subject, Subject subjectToCreate) throws SubjectException {
        return subjectManager.createSubject(subject, subjectToCreate);
    }

    @Override
    public void deleteSubjects(Subject subject, int[] subjectIds) {
        subjectManager.deleteSubjects(subject, subjectIds);
    }

    @Override
    public PageList<Subject> findSubjectsByCriteria(Subject subject, SubjectCriteria criteria) {
        return subjectManager.findSubjectsByCriteria(subject, criteria);
    }

    @Override
    public String getServerNamespaceVersion() {
        return subjectManager.getServerNamespaceVersion();
    }

    @Override
    public Subject getSubjectByName(String username) {
        return subjectManager.getSubjectByName(username);
    }

    @Override
    public Subject login(String username, String password) throws LoginException {
        return subjectManager.login(username, password);
    }

    @Override
    public void logout(Subject subject) {
        subjectManager.logout(subject);
    }

    @Override
    public Subject updateSubject(Subject subject, Subject subjectToModify) {
        return subjectManager.updateSubject(subject, subjectToModify);
    }

    //SUBJECTMANAGER: END ------------------------------------

    //SUPPORTMANAGER: BEGIN ------------------------------------
    @Override
    public URL getSnapshotReport(Subject subject, int resourceId, String name, String description) throws Exception {
        return supportManager.getSnapshotReport(subject, resourceId, name, description);
    }

    //SUPPORTMANAGER: END ------------------------------------

    //SYSTEMMANAGER: BEGIN ------------------------------------
    @Override
    public ServerVersion getServerVersion(Subject subject) throws Exception {
        return systemManager.getServerVersion(subject);
    }

    //SYSTEMMANAGER: END ------------------------------------

}
