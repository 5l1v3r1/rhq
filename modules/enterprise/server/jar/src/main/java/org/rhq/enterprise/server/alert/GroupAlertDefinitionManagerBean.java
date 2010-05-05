/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.alert;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;

/**
 * @author Joseph Marques
 */

@Stateless
public class GroupAlertDefinitionManagerBean implements GroupAlertDefinitionManagerLocal {

    private static final Log LOG = LogFactory.getLog(GroupAlertDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    private SubjectManagerLocal subjectManager;

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> findGroupAlertDefinitions(Subject subject, int resourceGroupId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            AlertDefinition.QUERY_FIND_BY_RESOURCE_GROUP);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AlertDefinition.QUERY_FIND_BY_RESOURCE_GROUP, pageControl);

        queryCount.setParameter("groupId", resourceGroupId);
        query.setParameter("groupId", resourceGroupId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getChildrenAlertDefinitionIds(Subject subject, int groupAlertDefinitionId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_BY_GROUP_ALERT_DEFINITION_ID);
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);

        List<Integer> list = query.getResultList();
        return list;
    }

    public int removeGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds) {
        if (groupAlertDefinitionIds == null || groupAlertDefinitionIds.length == 0) {
            return 0;
        }

        int modified = 0;
        List<Integer> allChildDefinitionIds = new ArrayList<Integer>();
        Subject overlord = subjectManager.getOverlord();
        for (Integer groupAlertDefinitionId : groupAlertDefinitionIds) {
            List<Integer> childDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinitionId);
            allChildDefinitionIds.addAll(childDefinitions);
            modified += alertDefinitionManager
                .removeAlertDefinitions(subject, new Integer[] { groupAlertDefinitionId });
            alertDefinitionManager.removeAlertDefinitions(overlord, childDefinitions
                .toArray(new Integer[childDefinitions.size()]));
        }

        /*
         * break the Hibernate relationships used for navigating between the groupAlertDefinition and the
         * children alertDefinitions so that the async deletion mechanism can delete without FK violations
         */
        if (allChildDefinitionIds.size() > 0) {
            Query breakLinksQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_UPDATE_SET_PARENTS_NULL);
            breakLinksQuery.setParameter("childrenDefinitionIds", allChildDefinitionIds);
            breakLinksQuery.executeUpdate();
        }

        return modified;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(Subject subject,
        int groupAlertDefinitionId, int resourceGroupId) {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_RESOURCE_IDS_NEEDING_GROUP_APPLICATION);
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);
        query.setParameter("resourceGroupId", resourceGroupId);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<Integer> list = query.getResultList();
        return list;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int createGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        Integer resourceGroupId) throws InvalidAlertDefinitionException, AlertDefinitionCreationException {
        ResourceGroup group = entityManager.find(ResourceGroup.class, resourceGroupId);
        groupAlertDefinition.setResourceGroup(group);
        int groupAlertDefinitionId = alertDefinitionManager.createAlertDefinition(subject, groupAlertDefinition, null);
        group = entityManager.merge(group);

        Subject overlord = subjectManager.getOverlord();
        List<Integer> resourceIdsForGroup = getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(subject,
            groupAlertDefinitionId, resourceGroupId);

        for (Integer resourceId : resourceIdsForGroup) {
            try {
                // construct the child
                AlertDefinition childAlertDefinition = new AlertDefinition(groupAlertDefinition);
                childAlertDefinition.setGroupAlertDefinition(groupAlertDefinition);

                // persist the child
                alertDefinitionManager.createAlertDefinition(overlord, childAlertDefinition, resourceId);
            } catch (Throwable t) {
                throw new AlertDefinitionCreationException("Could not create alert definition child for Resource[id="
                    + resourceId + "] with group " + groupAlertDefinition.toSimpleString());
            }
        }

        return groupAlertDefinitionId;

    }

    public int disableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds) {
        if (groupAlertDefinitionIds == null || groupAlertDefinitionIds.length == 0) {
            return 0;
        }

        int modified = 0;
        Subject overlord = subjectManager.getOverlord();
        for (Integer groupAlertDefinitionId : groupAlertDefinitionIds) {
            List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinitionId);

            modified += alertDefinitionManager.disableAlertDefinitions(subject,
                new Integer[] { groupAlertDefinitionId });
            alertDefinitionManager.disableAlertDefinitions(overlord, alertDefinitions
                .toArray(new Integer[alertDefinitions.size()]));
        }
        return modified;
    }

    public int enableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds) {
        if (groupAlertDefinitionIds == null || groupAlertDefinitionIds.length == 0) {
            return 0;
        }

        int modified = 0;
        Subject overlord = subjectManager.getOverlord();
        for (Integer groupAlertDefinitionId : groupAlertDefinitionIds) {
            List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinitionId);

            modified += alertDefinitionManager
                .enableAlertDefinitions(subject, new Integer[] { groupAlertDefinitionId });
            alertDefinitionManager.enableAlertDefinitions(overlord, alertDefinitions
                .toArray(new Integer[alertDefinitions.size()]));
        }
        return modified;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AlertDefinition updateGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        boolean purgeInternals) throws InvalidAlertDefinitionException, AlertDefinitionUpdateException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("updateGroupAlertDefinition: " + groupAlertDefinition);
        }

        // first update the actual alert group alert definition
        AlertDefinition updated = null;
        try {
            updated = alertDefinitionManager.updateAlertDefinition(subject, groupAlertDefinition.getId(),
                groupAlertDefinition, purgeInternals); // do not allow direct undeletes of an alert definition
        } catch (Throwable t) {
            throw new AlertDefinitionUpdateException("Failed to update a GroupAlertDefinition: "
                + groupAlertDefinition.toSimpleString(), t);
        }

        // overlord will be used for all system-side effects as a result of updating this alert template
        Subject overlord = subjectManager.getOverlord();

        // update all of the definitions that were spawned from this group alert definition
        List<Integer> alertDefinitions = getChildrenAlertDefinitionIds(overlord, groupAlertDefinition.getId());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Need to update the following children alert definition ids: " + alertDefinitions);
        }
        for (Integer alertDefinitionId : alertDefinitions) {
            try {
                alertDefinitionManager.updateAlertDefinition(overlord, alertDefinitionId, groupAlertDefinition,
                    purgeInternals);
            } catch (Throwable t) {
                throw new AlertDefinitionUpdateException("Failed to update child AlertDefinition[id="
                    + alertDefinitionId + "] with group " + groupAlertDefinition.toSimpleString(), t);
            }
        }

        // if the subject deleted the alertDefinition spawned from a groupAlertDefinition, cascade update will recreate it
        List<Integer> resourceIds = getCommittedResourceIdsNeedingGroupAlertDefinitionApplication(overlord,
            groupAlertDefinition.getId(), getResourceGroupIdForAlertDefinitionId(groupAlertDefinition.getId()));
        for (Integer resourceId : resourceIds) {
            try {
                // construct the child
                AlertDefinition childAlertDefinition = new AlertDefinition(groupAlertDefinition);
                childAlertDefinition.setGroupAlertDefinition(groupAlertDefinition);

                // persist the child
                alertDefinitionManager.createAlertDefinition(overlord, childAlertDefinition, resourceId);
            } catch (Throwable t) {
                throw new AlertDefinitionUpdateException("Failed to re-create child AlertDefinition for Resource[id="
                    + resourceId + "] with group" + groupAlertDefinition.toSimpleString(), t);
            }
        }

        return updated;
    }

    public void addGroupAlertDefinitions(Subject subject, int resourceGroupId, int[] resourcesIdsToAdd) {
        if (resourcesIdsToAdd == null || resourcesIdsToAdd.length == 0) {
            return;
        }

        List<AlertDefinition> groupAlertDefinitions = findGroupAlertDefinitions(subject, resourceGroupId, PageControl
            .getUnlimitedInstance());

        Subject overlord = subjectManager.getOverlord();
        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            for (Integer resourceId : resourcesIdsToAdd) {
                try {
                    // construct the child
                    AlertDefinition childAlertDefinition = new AlertDefinition(groupAlertDefinition);
                    childAlertDefinition.setGroupAlertDefinition(groupAlertDefinition);

                    // persist the child
                    alertDefinitionManager.createAlertDefinition(overlord, childAlertDefinition, resourceId);
                } catch (Throwable t) {
                    throw new AlertDefinitionCreationException(
                        "Could not create child alert definition for Resource[id=" + resourceId
                            + "] under ResourceGroup[id=" + resourceGroupId + "] with group"
                            + groupAlertDefinition.toSimpleString());
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void purgeAllGroupAlertDefinitions(Subject subject, int resourceGroupId) {
        List<AlertDefinition> groupAlertDefinitions = findGroupAlertDefinitions(subject, resourceGroupId, PageControl
            .getUnlimitedInstance());

        int i = 0;
        Integer[] groupAlertDefinitionIds = new Integer[groupAlertDefinitions.size()];
        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            groupAlertDefinitionIds[i++] = groupAlertDefinition.getId();
        }

        removeGroupAlertDefinitions(subject, groupAlertDefinitionIds);
    }

    public void removeGroupAlertDefinitions(Subject subject, int resourceGroupId, int[] resourceIdsToRemove) {
        if (resourceIdsToRemove == null || resourceIdsToRemove.length == 0) {
            return;
        }

        List<AlertDefinition> groupAlertDefinitions = findGroupAlertDefinitions(subject, resourceGroupId, PageControl
            .getUnlimitedInstance());

        List<Integer> allChildrenDefinitionIds = new ArrayList<Integer>();
        Subject overlord = subjectManager.getOverlord();
        for (AlertDefinition groupAlertDefinition : groupAlertDefinitions) {
            List<Integer> childDefinitions = getChildrenAlertDefinitionIds(subject, groupAlertDefinition.getId());
            allChildrenDefinitionIds.addAll(childDefinitions);
            alertDefinitionManager.removeAlertDefinitions(overlord, childDefinitions
                .toArray(new Integer[childDefinitions.size()]));
        }

        /*
         * break the Hibernate relationships used for navigating between the groupAlertDefinition and the
         * children alertDefinitions so that the async deletion mechanism can delete without FK violations
         */
        if (allChildrenDefinitionIds.size() > 0) {
            Query breakLinksQuery = entityManager.createNamedQuery(AlertDefinition.QUERY_UPDATE_SET_PARENTS_NULL);
            breakLinksQuery.setParameter("childrenDefinitionIds", allChildrenDefinitionIds);
            breakLinksQuery.executeUpdate();
        }
    }

    private int getResourceGroupIdForAlertDefinitionId(int groupAlertDefinitionId) {
        Query query = entityManager.createQuery("" //
            + "SELECT groupAlertDefinition.resourceGroup.id " //
            + "  FROM AlertDefinition groupAlertDefinition " //
            + " WHERE groupAlertDefinition.id = :groupAlertDefinitionId");
        query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);
        int groupId = ((Number) query.getSingleResult()).intValue();
        return groupId;
    }
}