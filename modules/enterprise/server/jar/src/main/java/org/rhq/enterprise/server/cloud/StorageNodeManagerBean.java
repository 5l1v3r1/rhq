/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package org.rhq.enterprise.server.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.rest.reporting.MeasurementConverter;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.server.metrics.CQLException;

@Stateless
public class StorageNodeManagerBean implements StorageNodeManagerLocal, StorageNodeManagerRemote {

    private static final String RESOURCE_TYPE_NAME = "RHQ Storage Node";
    private static final String PLUGIN_NAME = "RHQStorage";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private MeasurementDataManagerLocal measurementManager;

    @EJB
    private MeasurementScheduleManagerLocal scheduleManager;

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @PostConstruct
    @Override
    public void scanForStorageNodes() {
        try {
            String seedProp = System.getProperty("rhq.cassandra.seeds");
            if (seedProp == null) {
                throw new CQLException("The rhq.cassandra.seeds property is null. Cannot create session.");
            }

            List<StorageNode> storageNodes = this.getStorageNodes();
            if (storageNodes == null) {
                storageNodes = new ArrayList<StorageNode>();
            }

            for (StorageNode storageNode : storageNodes) {
                storageNode.setOperationMode(OperationMode.DOWN);
            }

            Map<String, StorageNode> storageNodeMap = new HashMap<String, StorageNode>();
            for (StorageNode storageNode : storageNodes) {
                storageNodeMap.put(storageNode.getAddress(), storageNode);
            }

            String[] seeds = seedProp.split(",");
            for (int i = 0; i < seeds.length; ++i) {
                StorageNode discoveredStorageNode = new StorageNode();
                discoveredStorageNode.parseNodeInformation(seeds[i]);

                //Mark the node as down for now since no information is available.
                //This will change to the correct value once a corresponding resource
                //is found in the inventory
                discoveredStorageNode.setOperationMode(OperationMode.DOWN);

                if (storageNodeMap.containsKey(discoveredStorageNode.getAddress())) {
                    StorageNode existingStorageNode = storageNodeMap.get(discoveredStorageNode.getAddress());
                    existingStorageNode.setJmxPort(discoveredStorageNode.getJmxPort());
                    existingStorageNode.setCqlPort(discoveredStorageNode.getCqlPort());
                } else {
                    storageNodeMap.put(discoveredStorageNode.getAddress(), discoveredStorageNode);
                }
            }

            this.discoverResourceInformation(storageNodeMap);

            this.updateStorageNodeList(storageNodeMap.values());
        } catch (Exception e) {
            throw new CQLException("Unable to create session", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void discoverResourceInformation(Map<String, StorageNode> storageNodeMap) {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", RESOURCE_TYPE_NAME).setParameter("plugin", PLUGIN_NAME);
        List<ResourceType> resourceTypes = (List<ResourceType>) query.getResultList();

        if (resourceTypes.isEmpty()) {
            return;
        }

        query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_TYPE_ADMIN).setParameter("type",
            resourceTypes.get(0));
        List<Resource> cassandraResources = (List<Resource>) query.getResultList();

        for (Resource resource : cassandraResources) {
            Configuration resourceConfiguration = resource.getPluginConfiguration();
            String host = resourceConfiguration.getSimpleValue("host");

            if (host != null && storageNodeMap.containsKey(host)) {
                StorageNode storageNode = storageNodeMap.get(host);

                storageNode.setResource(resource);
                if (resource.getInventoryStatus() == InventoryStatus.NEW) {
                    storageNode.setOperationMode(OperationMode.INSTALLED);
                } else if (resource.getInventoryStatus() == InventoryStatus.COMMITTED
                    && resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP) {
                    storageNode.setOperationMode(OperationMode.NORMAL);
                }
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public StorageNodeLoadComposite getLoad(Subject subject, StorageNode node, long beginTime, long endTime) {
        StorageNodeLoadComposite result = new StorageNodeLoadComposite(node, beginTime, endTime);
        final String tokensMetric = "Tokens", ownershipMetric = "Ownership", loadMetric = "Load";
        final String heapComittedMetric = "{HeapMemoryUsage.committed}", heapUsedMetric = "{HeapMemoryUsage.used}";

        int resourceId;
        if (node.getResource() == null) {
            resourceId = entityManager.merge(node).getResource().getId();
        } else {
            resourceId = node.getResource().getId();
        }

        // get the schedule ids for Storage Service resource
        TypedQuery<Object[]> query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, Object[].class);
        query.setParameter("parrentId", resourceId).setParameter("metricNames",
            Arrays.asList(tokensMetric, ownershipMetric, loadMetric));
        List<Object[]> scheduleIds = query.getResultList();
        Map<String, Integer> scheduleIdsMap = new HashMap<String, Integer>(4);
        for (Object[] pair : scheduleIds) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }

        // get the schedule ids for Memory Subsystem resource
        query = entityManager.<Object[]> createNamedQuery(
            StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES,
            Object[].class);
        query.setParameter("grandparrentId", resourceId).setParameter("metricNames",
            Arrays.asList(heapComittedMetric, heapUsedMetric));
        scheduleIds = query.getResultList();
        for (Object[] pair : scheduleIds) {
            scheduleIdsMap.put((String) pair[0], (Integer) pair[1]);
        }

        // find the aggregates and enrich the result instance
        if (!scheduleIdsMap.isEmpty()) {
            if (scheduleIdsMap.get(tokensMetric) != null) {
                MeasurementAggregate tokensAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(tokensMetric), beginTime, endTime);
                result.setTokens(tokensAggregate);
            }
            if (scheduleIdsMap.get(ownershipMetric) != null) {
                MeasurementAggregate ownershipAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(ownershipMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits ovnershipAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    ownershipAggregate, MeasurementUnits.PERCENTAGE);
                ovnershipAggregateWithUnits.setFormattedValue(getSummaryString(ownershipAggregate,
                    MeasurementUnits.PERCENTAGE));
                result.setActuallyOwns(ovnershipAggregateWithUnits);
            }
            if (scheduleIdsMap.get(loadMetric) != null) {
                MeasurementAggregate loadAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(loadMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits loadAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    loadAggregate, MeasurementUnits.BYTES);
                loadAggregateWithUnits.setFormattedValue(getSummaryString(loadAggregate, MeasurementUnits.BYTES));
                result.setLoad(loadAggregateWithUnits);
            }

            if (scheduleIdsMap.get(heapComittedMetric) != null) {
                MeasurementAggregate heapCommitedAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(heapComittedMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapCommitedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    heapCommitedAggregate, MeasurementUnits.BYTES);
                heapCommitedAggregateWithUnits.setFormattedValue(getSummaryString(heapCommitedAggregate,
                    MeasurementUnits.BYTES));
                result.setHeapCommited(heapCommitedAggregateWithUnits);
            }
            if (scheduleIdsMap.get(heapUsedMetric) != null) {
                MeasurementAggregate heapUsedAggregate = measurementManager.getAggregate(subject,
                    scheduleIdsMap.get(heapUsedMetric), beginTime, endTime);
                StorageNodeLoadComposite.MeasurementAggregateWithUnits heapUsedAggregateWithUnits = new StorageNodeLoadComposite.MeasurementAggregateWithUnits(
                    heapUsedAggregate, MeasurementUnits.BYTES);
                heapUsedAggregateWithUnits
                    .setFormattedValue(getSummaryString(heapUsedAggregate, MeasurementUnits.BYTES));
                result.setHeapUsed(heapUsedAggregateWithUnits);
            }
        }

        return result;
    }

    @Nullable
    public List<StorageNode> getStorageNodes() {
        TypedQuery<StorageNode> query = entityManager.<StorageNode> createNamedQuery(StorageNode.QUERY_FIND_ALL,
            StorageNode.class);
        return query.getResultList();
    }

    public void updateStorageNodeList(Collection<StorageNode> storageNodes) {
        for (StorageNode storageNode : storageNodes) {
            entityManager.persist(storageNode);
        }
        entityManager.flush();
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<StorageNode> findStorageNodesByCriteria(Subject subject, StorageNodeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<StorageNode> runner = new CriteriaQueryRunner<StorageNode>(criteria, generator,
            entityManager);
        return runner.execute();
    }
    
    private String getSummaryString(MeasurementAggregate aggregate, MeasurementUnits units) {
        String formattedValue = "Min: "
            + MeasurementConverter.format(aggregate.getMin(), units, true)
            + ", Max: "
            + MeasurementConverter.format(aggregate.getMax(), units, true)
            + ", Avg: "
            + MeasurementConverter.format(aggregate.getAvg(), units, true);
        return formattedValue;
    }
}
