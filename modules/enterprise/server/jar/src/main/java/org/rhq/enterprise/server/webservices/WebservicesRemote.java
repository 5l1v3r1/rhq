package org.rhq.enterprise.server.webservices;

import javax.ejb.Remote;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

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
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.system.ServerVersion;
import org.rhq.enterprise.server.system.SystemManagerRemote;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface WebservicesRemote extends //
    AlertManagerRemote, //
    AlertDefinitionManagerRemote, //
    AvailabilityManagerRemote,//
    CallTimeDataManagerRemote,//
    ChannelManagerRemote,//
    ConfigurationManagerRemote,//
    ContentManagerRemote,//
    DataAccessManagerRemote,//
    DiscoveryBossRemote,//
    EventManagerRemote,//
    MeasurementBaselineManagerRemote,//
    MeasurementDataManagerRemote,//
    MeasurementDefinitionManagerRemote,//
    MeasurementProblemManagerRemote,//
    MeasurementScheduleManagerRemote,//
    OperationManagerRemote,//
    ResourceGroupManagerRemote,//
    ResourceManagerRemote,//
    ResourceTypeManagerRemote,//
    RoleManagerRemote,//
    SubjectManagerRemote,//   
    SupportManagerRemote,//
    SystemManagerRemote {
}
