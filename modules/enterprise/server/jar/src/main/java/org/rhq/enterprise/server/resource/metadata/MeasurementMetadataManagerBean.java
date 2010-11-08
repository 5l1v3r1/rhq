package org.rhq.enterprise.server.resource.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Stateless
public class MeasurementMetadataManagerBean implements MeasurementMetadataManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private MeasurementScheduleManagerLocal scheduleMgr;

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionMgr;

    @Override
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        Set<MeasurementDefinition> existingDefinitions = existingType.getMetricDefinitions();
        if (existingDefinitions.isEmpty()) {
            // They're all new.
            for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                if (newDefinition.getDefaultInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                    newDefinition.setDefaultInterval(MeasurementSchedule.MINIMUM_INTERVAL);
                    log.info("Definition [" + newDefinition
                            + "] has too short of a default interval, setting to minimum");
                }
                existingType.addMetricDefinition(newDefinition);
                entityMgr.persist(newDefinition);

                // Now create schedules for already existing resources
                scheduleMgr.createSchedulesForExistingResources(existingType, newDefinition);
            }
        } else {
            // Update existing or add new metrics
            for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                boolean found = false;
                for (MeasurementDefinition existingDefinition : existingDefinitions) {
                    if (existingDefinition.getName().equals(newDefinition.getName())
                            && (existingDefinition.isPerMinute() == newDefinition.isPerMinute())) {
                        found = true;

                        existingDefinition.update(newDefinition, false);

                        // we normally do not want to touch interval in case a user changed it,
                        // but we cannot allow too-short of an interval, so override it if necessary
                        if (existingDefinition.getDefaultInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                            existingDefinition.setDefaultInterval(MeasurementSchedule.MINIMUM_INTERVAL);
                            log.info("Definition [" + existingDefinition
                                    + "] has too short of a default interval, setting to minimum");
                        }

                        entityMgr.merge(existingDefinition);

                        // There is nothing in the schedules that need to be updated.
                        // We do not want to change schedules (such as collection interval)
                        // because the user might have customized them. So leave them be.

                        break;
                    }
                }

                if (!found) {
                    // Its new, create it
                    existingType.addMetricDefinition(newDefinition);
                    entityMgr.persist(newDefinition);

                    // Now create schedules for already existing resources
                    scheduleMgr.createSchedulesForExistingResources(existingType, newDefinition);
                }
            }

            /*
            * Now delete outdated measurement definitions. First find them ...
            */
            List<MeasurementDefinition> definitionsToDelete = new ArrayList<MeasurementDefinition>();
            for (MeasurementDefinition existingDefinition : existingDefinitions) {
                if (!newType.getMetricDefinitions().contains(existingDefinition)) {
                    definitionsToDelete.add(existingDefinition);
                }
            }

            // ... and remove them
            existingDefinitions.removeAll(definitionsToDelete);
            for (MeasurementDefinition definitionToDelete : definitionsToDelete) {
                measurementDefinitionMgr.removeMeasurementDefinition(definitionToDelete);
            }
            if (!definitionsToDelete.isEmpty() && log.isDebugEnabled()) {
                log.debug("Metadata update: Measurement definitions deleted from resource type ["
                        + existingType.getName() + "]:" + definitionsToDelete);
            }

            entityMgr.flush();
        }
        // TODO what if they are null? --> delete everything from existingType
        // not needed see JBNADM-1639
    }

    @Override
    public void deleteMetadata(ResourceType existingType) {
        // Remove the type's metric definitions. We do this separately, rather than just relying on cascade
        // upon deletion of the ResourceType, because the removeMeasurementDefinition() will also take care
        // of removing any associated schedules and those schedules' OOBs.
        Set<MeasurementDefinition> definitions = existingType.getMetricDefinitions();
        if (definitions != null) {
            Iterator<MeasurementDefinition> defIter = definitions.iterator();
            while (defIter.hasNext()) {
                MeasurementDefinition def = defIter.next();
                if (entityMgr.contains(def)) {
                    entityMgr.refresh(def);
                    measurementDefinitionMgr.removeMeasurementDefinition(def);
                }
                defIter.remove();
            }
        }
    }
}
