/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.importers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.sync.entity.MetricTemplate;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.sync.ExportReader;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class MetricTemplateImporter implements Importer<MeasurementDefinition, MetricTemplate> {

    private static final Log LOG = LogFactory.getLog(MetricTemplateImporter.class);

    private static final boolean UPDATE_SCHEDULES_DEFAULT = false;
    public static final String UPDATE_ALL_SCHEDULES_PROPERTY = "updateAllSchedules";
    public static final String METRIC_NAME_PROPERTY = "metricName";
    public static final String RESOURCE_TYPE_NAME_PROPERTY = "resourceTypeName";
    public static final String RESOURCE_TYPE_PLUGIN_PROPERTY = "resourceTypePlugin";
    public static final String UPDATE_SCHEDULES_PROPERTY = "updateSchedules";
    public static final String METRIC_UPDATE_OVERRIDES_PROPERTY = "metricUpdateOverrides";
    public static final String METRIC_UPDATE_OVERRIDE_PROPERTY = "metricUpdateOverride";

    private static class CollectionIntervalAndUpdateSchedules {
        public final long collectionInterval;
        public final boolean updateSchedules;

        public CollectionIntervalAndUpdateSchedules(long collectionInterval, boolean updateSchedules) {
            this.collectionInterval = collectionInterval;
            this.updateSchedules = updateSchedules;
        }

        @Override
        public int hashCode() {
            return (int) collectionInterval * (updateSchedules ? 31 : 1);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof CollectionIntervalAndUpdateSchedules)) {
                return false;
            }

            CollectionIntervalAndUpdateSchedules o = (CollectionIntervalAndUpdateSchedules) other;

            return o.collectionInterval == collectionInterval && o.updateSchedules == updateSchedules;
        }
    }

    private Subject subject;
    private EntityManager entityManager;
    private List<MeasurementDefinition> definitionsToEnable = new ArrayList<MeasurementDefinition>();
    private List<MeasurementDefinition> definitionsToDisable = new ArrayList<MeasurementDefinition>();
    private Map<CollectionIntervalAndUpdateSchedules, List<MeasurementDefinition>> definitionsByCollectionIntervalAndUpdateSchedules =
        new HashMap<MetricTemplateImporter.CollectionIntervalAndUpdateSchedules, List<MeasurementDefinition>>();
    private Configuration importConfiguration;
    private Unmarshaller unmarshaller;
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    public MetricTemplateImporter(Subject subject, EntityManager entityManager) {
        this(subject, entityManager, LookupUtil.getMeasurementScheduleManager());
    }

    public MetricTemplateImporter(Subject subject, EntityManager entityManager,
        MeasurementScheduleManagerLocal measurementScheduleManager) {
        try {
            this.subject = subject;
            this.entityManager = entityManager;
            JAXBContext context = JAXBContext.newInstance(MetricTemplate.class);
            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize JAXB marshaller for MetricTemplate.", e);
        }
        this.measurementScheduleManager = measurementScheduleManager;
    }

    @Override
    public ConfigurationDefinition getImportConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("MetricTemplateImportConfiguration", null);
        PropertyDefinitionSimple updateAllSchedules =
            new PropertyDefinitionSimple(
                UPDATE_ALL_SCHEDULES_PROPERTY,
                "If set to true, all the metric templates will update all the existing schedules on corresponding resources.",
                true, PropertySimpleType.BOOLEAN);
        updateAllSchedules.setDefaultValue(Boolean.toString(UPDATE_SCHEDULES_DEFAULT));
        def.put(updateAllSchedules);

        PropertyDefinitionSimple metricName =
            new PropertyDefinitionSimple(METRIC_NAME_PROPERTY, "The name of the metric", true,
                PropertySimpleType.STRING);
        PropertyDefinitionSimple resourceTypeName =
            new PropertyDefinitionSimple(RESOURCE_TYPE_NAME_PROPERTY,
                "The name of the resource type defining the metric", true, PropertySimpleType.STRING);
        PropertyDefinitionSimple resourceTypePlugin =
            new PropertyDefinitionSimple(RESOURCE_TYPE_PLUGIN_PROPERTY,
                "The name of the plugin defining the resource type that defines the metric", true,
                PropertySimpleType.STRING);
        PropertyDefinitionSimple updateSchedules =
            new PropertyDefinitionSimple(UPDATE_SCHEDULES_PROPERTY,
                "Whether to update the schedules of this metric on existing resources", true,
                PropertySimpleType.BOOLEAN);

        PropertyDefinitionMap metricUpdateOverride =
            new PropertyDefinitionMap(METRIC_UPDATE_OVERRIDE_PROPERTY, null, true, metricName, resourceTypeName,
                resourceTypePlugin, updateSchedules);
        PropertyDefinitionList metricUpdateOverrides =
            new PropertyDefinitionList(METRIC_UPDATE_OVERRIDES_PROPERTY, "Per metric settings", false,
                metricUpdateOverride);

        def.put(metricUpdateOverrides);

        ConfigurationUtility.initializeDefaultTemplate(def);

        return def;
    }

    @Override
    public void configure(Configuration configuration) {
        this.importConfiguration = configuration;
    }

    @Override
    public ExportedEntityMatcher<MeasurementDefinition, MetricTemplate> getExportedEntityMatcher() {
        return new ExportedEntityMatcher<MeasurementDefinition, MetricTemplate>() {

            @Override
            public MeasurementDefinition findMatch(MetricTemplate object) {
                Query q =
                    entityManager
                        .createNamedQuery(MeasurementDefinition.FIND_RAW_OR_PER_MINUTE_BY_NAME_AND_RESOURCE_TYPE_NAME);
                q.setParameter("name", object.getMetricName());
                q.setParameter("resourceTypeName", object.getResourceTypeName());
                q.setParameter("resourceTypePlugin", object.getResourceTypePlugin());
                q.setParameter("perMinute", object.isPerMinute() ? 1 : 0);

                List<?> results = q.getResultList();

                if (results.isEmpty()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Failed to find a measurement definition corresponding to "
                            + object
                            + ". This means that the plugins in the source RHQ install were different than in this RHQ install "
                            + "but the DeployedAgentPluginsValidator failed to catch that. This most probably means that the "
                            + "export file has been tampered with. Letting the import continue because that might have been intentional change by the user.");
                    }
                    return null;
                } else if (results.size() > 1) {
                    throw new IllegalStateException(
                        "Found more than one measurement definition with the same name and resource type. This should never happen. "
                            + results);
                } else {
                    return (MeasurementDefinition) results.get(0);
                }
            }
        };
    }

    @Override
    public void update(MeasurementDefinition entity, MetricTemplate exportedEntity) {
        if (entity == null) {
            return;
        }

        if (exportedEntity.isEnabled()) {
            definitionsToEnable.add(entity);
        } else {
            definitionsToDisable.add(entity);
        }

        addToCollectionIntervalMap(exportedEntity, entity);
    }

    @Override
    public MetricTemplate unmarshallExportedEntity(ExportReader reader) throws XMLStreamException {
        try {
            return (MetricTemplate) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to unmarshal metric template.", e);
        }
    }

    @Override
    public void finishImport() {
        measurementScheduleManager.enableMeasurementTemplates(subject, getIdsFromDefs(definitionsToEnable));
        measurementScheduleManager.disableMeasurementTemplates(subject, getIdsFromDefs(definitionsToDisable));

        for (Map.Entry<CollectionIntervalAndUpdateSchedules, List<MeasurementDefinition>> e : definitionsByCollectionIntervalAndUpdateSchedules
            .entrySet()) {
            measurementScheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(subject,
                getIdsFromDefs(e.getValue()), e.getKey().collectionInterval, e.getKey().updateSchedules);
        }
    }

    private static int[] getIdsFromDefs(Collection<MeasurementDefinition> defs) {
        int[] ids = new int[defs.size()];

        int i = 0;
        for (MeasurementDefinition d : defs) {
            ids[i++] = d.getId();
        }

        return ids;
    }

    private void addToCollectionIntervalMap(MetricTemplate metricTemplate, MeasurementDefinition def) {
        CollectionIntervalAndUpdateSchedules key =
            new CollectionIntervalAndUpdateSchedules(metricTemplate.getDefaultInterval(),
                shouldUpdateSchedules(metricTemplate));

        List<MeasurementDefinition> defs = definitionsByCollectionIntervalAndUpdateSchedules.get(key);

        if (defs == null) {
            defs = new ArrayList<MeasurementDefinition>();
            definitionsByCollectionIntervalAndUpdateSchedules.put(key, defs);
        }

        defs.add(def);
    }

    private boolean shouldUpdateSchedules(MetricTemplate def) {
        if (importConfiguration == null) {
            return UPDATE_SCHEDULES_DEFAULT;
        }

        String updateAll = importConfiguration.getSimpleValue(UPDATE_ALL_SCHEDULES_PROPERTY, null);
        if (updateAll != null) {
            if (Boolean.parseBoolean(updateAll)) {
                return true;
            }
        }

        PropertyList perMetricOverrides = importConfiguration.getList(METRIC_UPDATE_OVERRIDES_PROPERTY);
        if (perMetricOverrides == null) {
            return UPDATE_SCHEDULES_DEFAULT;
        }

        PropertySimple override = findOverrideForMetric(def, perMetricOverrides);

        if (override == null) {
            return UPDATE_SCHEDULES_DEFAULT;
        }

        return override.getBooleanValue();
    }

    PropertySimple findOverrideForMetric(MetricTemplate def, PropertyList overrides) {
        for (Property p : overrides.getList()) {
            PropertyMap map = (PropertyMap) p;

            String metricName = map.getSimpleValue(METRIC_NAME_PROPERTY, null);
            String resourceTypeName = map.getSimpleValue(RESOURCE_TYPE_NAME_PROPERTY, null);
            String resourceTypePlugin = map.getSimpleValue(RESOURCE_TYPE_PLUGIN_PROPERTY, null);
            PropertySimple updateSchedules = map.getSimple(UPDATE_SCHEDULES_PROPERTY);

            if (metricName == null || resourceTypeName == null || resourceTypePlugin == null || updateSchedules == null) {
                continue;
            }

            if (metricName.equals(def.getMetricName()) && resourceTypeName.equals(def.getResourceTypeName())
                && resourceTypePlugin.equals(def.getResourceTypePlugin())) {

                return updateSchedules;
            }
        }

        return null;
    }
}
