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
package org.rhq.enterprise.server.resource.group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceIdFlyWeight;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * A manager that provides methods for creating, updating, deleting, and querying
 * {@link org.rhq.core.domain.resource.group.ResourceGroup}s.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class ResourceGroupManagerBean implements ResourceGroupManagerLocal {
    private final Log log = LogFactory.getLog(ResourceGroupManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    @IgnoreDependency
    private OperationManagerLocal operationManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    @IgnoreDependency
    private ResourceTypeManagerLocal resourceTypeManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;
    private DatabaseType dbType;

    @PostConstruct
    public void init() {
        Connection conn = null;
        try {
            conn = rhqDs.getConnection();
            dbType = DatabaseTypeFactory.getDatabaseType(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JDBCUtil.safeClose(conn);
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public int createResourceGroup(Subject user, ResourceGroup group) throws ResourceGroupNotFoundException,
        ResourceGroupAlreadyExistsException {
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_NAME);
        query.setParameter("name", group.getName());

        List<ResourceGroup> groups = query.getResultList();
        if (groups.size() != 0) {
            throw new ResourceGroupAlreadyExistsException("ResourceGroup with name " + group.getName()
                + " already exists");
        }

        long time = System.currentTimeMillis();
        group.setCtime(time);
        group.setMtime(time);
        group.setModifiedBy(user);

        entityManager.persist(group);

        return group.getId();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    public ResourceGroup updateResourceGroup(Subject user, ResourceGroup group)
        throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException {
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_NAME);
        query.setParameter("name", group.getName());

        try {
            ResourceGroup foundGroup = (ResourceGroup) query.getSingleResult();
            if (foundGroup.getId() == group.getId()) {
                // user is updating the same group and hasn't changed the name, this is OK
            } else {
                //  user is updating the group name to the name of an existing group - this is bad
                throw new ResourceGroupAlreadyExistsException("ResourceGroup with name " + group.getName()
                    + " already exists");
            }
        } catch (NoResultException e) {
            // user is changing the name of the group, this is OK
        }

        /*
         * The UI doesn't show the "recursive" checkbox, but this check prevents people from hitting the URL directly to
         * mess with the group
         */
        ResourceGroup attachedGroup = entityManager.find(ResourceGroup.class, group.getId());
        if (attachedGroup.isRecursive() != group.isRecursive()) {
            throw new ResourceGroupUpdateException("Can not change the " + (attachedGroup.isRecursive() ? "" : "non-")
                + "recursive nature of this group");
        }

        long time = System.currentTimeMillis();
        group.setMtime(time);
        group.setModifiedBy(user);

        return entityManager.merge(group);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteResourceGroup(Subject user, Integer groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException {
        ResourceGroup group = getResourceGroupById(user, groupId, null);

        // for compatible groups, first recursively remove any referring backing groups for auto-clusters
        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
            for (ResourceGroup referringGroup : group.getclusterBackingGroups()) {
                deleteResourceGroup(user, referringGroup.getId());
            }
        }

        // unschedule all jobs for this group (only compatible groups have operations, mixed do not)
        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
            Subject overlord = subjectManager.getOverlord();
            try {
                List<GroupOperationSchedule> ops = operationManager.getScheduledGroupOperations(overlord, groupId);

                for (GroupOperationSchedule schedule : ops) {
                    try {
                        operationManager.unscheduleGroupOperation(overlord, schedule.getJobId().toString(), groupId);
                    } catch (SchedulerException e) {
                        log.warn("Failed to unschedule job [" + schedule + "] for a group being deleted [" + group
                            + "]", e);
                    }
                }
            } catch (SchedulerException e1) {
                log.warn("Failed to get jobs for a group being deleted [" + group
                    + "]; will not attempt to unschedule anything", e1);
            }
        }

        for (Role doomedRoleRelationship : group.getRoles()) {
            group.removeRole(doomedRoleRelationship);
            entityManager.merge(doomedRoleRelationship);
        }

        group = entityManager.merge(group);

        // remove all resources in the group
        resourceGroupManager.removeAllResourcesFromGroup(user, groupId);

        // break resource and plugin configuration update links in order to preserve individual change history
        Query q = null;
        int numRows = -1;

        q = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE_BY_GROUP);
        q.setParameter("groupId", groupId);
        numRows = q.executeUpdate();

        q = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE_BY_GROUP);
        q.setParameter("groupId", groupId);
        numRows = q.executeUpdate();

        entityManager.remove(group);
    }

    public ResourceGroup getResourceGroupById(Subject user, int id, GroupCategory category)
        throws ResourceGroupNotFoundException {
        ResourceGroup group = entityManager.find(ResourceGroup.class, id);

        if (group == null) {
            throw new ResourceGroupNotFoundException("Resource group with specified id does not exist");
        }

        if (!authorizationManager.canViewGroup(user, group.getId())) {
            throw new PermissionException("You do not have permission to view this resource group");
        }

        // null category means calling context doesn't care about category
        if ((category != null) && (category != group.getGroupCategory())) {
            throw new ResourceGroupNotFoundException("Expected group to belong to '" + category + "' category, "
                + "it belongs to '" + group.getGroupCategory() + "' category instead");
        }

        initLazyFields(group);

        return group;
    }

    private void initLazyFields(ResourceGroup group) {
        /*
         * initialize modifiedBy field, which is now a lazily- loaded relationship to speed up the GroupHub stuff
         */
        if (group.getModifiedBy() != null) {
            group.getModifiedBy().getId();
        }
    }

    public int getResourceGroupCountByCategory(Subject subject, GroupCategory category) {
        Query queryCount;

        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_COUNT_admin);
        } else {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_BY_CATEGORY_COUNT);
            queryCount.setParameter("subject", subject);
        }

        queryCount.setParameter("category", category);

        long count = (Long) queryCount.getSingleResult();

        return (int) count;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup addResourcesToGroup(Subject subject, Integer groupId, Integer[] resourceIds)
        throws ResourceGroupNotFoundException, ResourceGroupUpdateException {
        long startTime = System.currentTimeMillis();

        ResourceGroup attachedGroup = getResourceGroupById(subject, groupId, null);
        if (resourceIds.length == 0) {
            return attachedGroup;
        }

        Set<Resource> explicitResources = new HashSet<Resource>(attachedGroup.getExplicitResources());

        // This is a convenience, because the logic already disallows duplicate items
        Set<Integer> uniqueResourceIds = new HashSet<Integer>();
        uniqueResourceIds.addAll(Arrays.asList(resourceIds));

        // list to hold the different types of errors
        List<Integer> alreadyMemberIds = new ArrayList<Integer>();

        List<ResourceIdFlyWeight> flyWeights = resourceManager.getFlyWeights(uniqueResourceIds
            .toArray(new Integer[uniqueResourceIds.size()]));

        for (ResourceIdFlyWeight fly : flyWeights) {
            // if resource is already in the explicit list, no work needs to be done
            if (explicitResources.contains(fly)) {
                // record this id that already exists in group's explicit list
                alreadyMemberIds.add(fly.getId());
                continue;
            }

            // updates explicit and implicit stuff
            addResourcesToGroupHelper(attachedGroup, fly);
        }

        ResourceGroup mergedResult = entityManager.merge(attachedGroup);

        if (alreadyMemberIds.size() != 0) {
            throw new ResourceGroupUpdateException(
                ((alreadyMemberIds.size() != 0) ? ("The following resources were already members of the group: " + alreadyMemberIds
                    .toString())
                    : ""));
        }

        long endTime = System.currentTimeMillis();

        log.debug("addResourcesToGroup took " + (endTime - startTime) + " millis");

        return mergedResult;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ResourceGroup removeResourcesFromGroup(Subject subject, Integer groupId, Integer[] resourceIds)
        throws ResourceGroupNotFoundException, ResourceGroupUpdateException {
        long startTime = System.currentTimeMillis();

        ResourceGroup attachedGroup = getResourceGroupById(subject, groupId, null);
        if (resourceIds.length == 0) {
            return attachedGroup;
        }

        // Proper operation insists that the passed group not contain dups
        Set<Integer> uniqueResourceIds = new HashSet<Integer>();
        uniqueResourceIds.addAll(Arrays.asList(resourceIds));

        // list to hold the different types of errors
        List<Integer> notValidMemberIds = new ArrayList<Integer>();

        List<ResourceIdFlyWeight> flyWeights = resourceManager.getFlyWeights(uniqueResourceIds
            .toArray(new Integer[uniqueResourceIds.size()]));
        // prepare structures for remove*Helper

        for (ResourceIdFlyWeight fly : flyWeights) {
            // no work needs to be done if the resource isn't in the explicit list
            if (!attachedGroup.getExplicitResources().contains(fly)) {
                // record this id that doesn't belong to the group's explicit list
                notValidMemberIds.add(fly.getId());
                continue;
            }

            // updates explicit and implicit stuff
            removeResourcesFromGroupHelper(attachedGroup, fly);
        }

        if (notValidMemberIds.size() != 0) {
            throw new ResourceGroupUpdateException(
                ((notValidMemberIds.size() != 0) ? ("The following resources are not members of the group["
                    + attachedGroup + "]: " + notValidMemberIds.toString()) : ""));
        }

        ResourceGroup mergedResult = entityManager.merge(attachedGroup);

        long endTime = System.currentTimeMillis();

        log.info("removeResourcesFromGroup took " + (endTime - startTime) + " millis");

        return mergedResult;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeAllResourcesFromGroup(Subject subject, Integer groupId) throws ResourceGroupDeleteException {
        Connection conn = null;
        PreparedStatement explicitStatement = null;
        PreparedStatement implicitStatement = null;
        try {
            conn = rhqDs.getConnection();

            explicitStatement = conn
                .prepareStatement("delete from rhq_resource_group_res_exp_map where resource_group_id = ?");
            implicitStatement = conn
                .prepareStatement("delete from rhq_resource_group_res_imp_map where resource_group_id = ?");

            explicitStatement.setInt(1, groupId);
            implicitStatement.setInt(1, groupId);

            explicitStatement.executeUpdate();
            implicitStatement.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error removing group resources", sqle);
            throw new ResourceGroupDeleteException("Error removing group resources: " + sqle.getMessage());
        } finally {
            JDBCUtil.safeClose(explicitStatement);
            JDBCUtil.safeClose(implicitStatement);
            JDBCUtil.safeClose(conn);
        }
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> getAvailableResourceGroupsForRole(Subject subject, Integer roleId,
        Integer[] excludeIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("rg.name");

        final String queryName;
        if ((excludeIds != null) && (excludeIds.length != 0)) {
            queryName = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE_WITH_EXCLUDES;
        } else {
            queryName = ResourceGroup.QUERY_GET_AVAILABLE_RESOURCE_GROUPS_FOR_ROLE;
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);

        if ((excludeIds != null) && (excludeIds.length != 0)) {
            List<Integer> excludeList = Arrays.asList(excludeIds);
            queryCount.setParameter("excludeIds", excludeList);
            query.setParameter("excludeIds", excludeList);
        }

        queryCount.setParameter("roleId", roleId);
        query.setParameter("roleId", roleId);

        long count = (Long) queryCount.getSingleResult();

        List<ResourceGroup> groups = query.getResultList();

        return new PageList<ResourceGroup>(groups, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> getResourceGroupByIds(Subject subject, Integer[] resourceGroupIds,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("rg.name");

        if ((resourceGroupIds == null) || (resourceGroupIds.length == 0)) {
            return new PageList<ResourceGroup>(pageControl);
        }

        Query queryCount = null;
        Query query = null;

        if (authorizationManager.isInventoryManager(subject)) {
            final String queryName = ResourceGroup.QUERY_FIND_BY_IDS_admin;
            queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);
        } else {
            final String queryName = ResourceGroup.QUERY_FIND_BY_IDS;
            queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        List<Integer> resourceGroupList = Arrays.asList(resourceGroupIds);
        queryCount.setParameter("ids", resourceGroupList);
        query.setParameter("ids", resourceGroupList);

        long count = (Long) queryCount.getSingleResult();

        List<ResourceGroup> groups = query.getResultList();

        return new PageList<ResourceGroup>(groups, (int) count, pageControl);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    public void updateImplicitGroupMembership(Subject subject, Resource resource) {
        /*
         * Get all the groups the parent of this resource is implicitly in. This will tell us which we need to update
         * (because we added new descendants).
         */
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_RESOURCE_ID);
        query.setParameter("id", resource.getParentResource().getId());
        List<ResourceGroup> groups = query.getResultList();

        // the resource isn't currently in a group -> no work to do
        if (groups.size() == 0) {
            return;
        }

        List<ResourceGroup> recursiveGroups = new ArrayList<ResourceGroup>(groups.size());
        for (ResourceGroup group : groups) {
            if (group.isRecursive()) {
                recursiveGroups.add(group);
            }
        }

        /*
         * BFS-construct the resource tree
         */
        List<Resource> resourceTree = new ArrayList<Resource>();
        List<Resource> toBeSearched = new LinkedList<Resource>();
        toBeSearched.add(resource);
        while (toBeSearched.size() > 0) {
            Resource next = toBeSearched.remove(0);
            resourceTree.add(next);
            toBeSearched.addAll(next.getChildResources());
        }

        /*
         * We should add this resource and all of its descendants to whatever recursive groups this resource's parent is
         * in
         */
        for (ResourceGroup implicitRecursiveGroup : recursiveGroups) {
            for (Resource implicitResource : resourceTree) {
                // cardinal rule, add the relationship in both directions
                implicitRecursiveGroup.addImplicitResource(implicitResource);
                implicitResource.getImplicitGroups().add(implicitRecursiveGroup);
            }

            try {
                /*
                 * when automatically updating recursive groups during inventory sync we need to make sure that we also
                 * update the resourceGroup; this will realistically only happen when a recursive group definition is
                 * created, but which initially creates one or more resource groups of size 1; if this happens, the
                 * group will be created as a compatible group, if resources are later added via an inventory sync, and
                 * if this compatible group's membership changes, we need to recalculate the group category
                 */
                setResourceType(implicitRecursiveGroup.getId());
            } catch (ResourceTypeNotFoundException rtnfe) {
                // fail gracefully, since this is a system side-effect, not user-initiated
                implicitRecursiveGroup.setResourceType(null);
            }
        }

        /*
         * Merge all the participants after the relationships are constructed, and save the return values so callers can
         * work with attached objects.
         */
        for (ResourceGroup implicitRecursiveGroup : recursiveGroups) {
            entityManager.merge(implicitRecursiveGroup);
        }

        for (Resource implicitResource : resourceTree) {
            entityManager.merge(implicitResource);
        }
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#getResourcesForAutoGroup(org.jboss.on.domain.auth.Subject,
     * int, int)
     */
    @SuppressWarnings("unchecked")
    public List<Resource> getResourcesForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId) {
        List<Resource> resources;
        try {
            Query q = entityManager.createNamedQuery(Resource.QUERY_FIND_FOR_AUTOGROUP);
            q.setParameter("type", autoGroupChildResourceTypeId);
            q.setParameter("parent", autoGroupParentResourceId);
            q.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
            //         q.setParameter("subject", subject);
            resources = q.getResultList();
        } catch (PersistenceException pe) {
            return new ArrayList<Resource>();
        }

        return resources;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#getResourcesForResourceGroup(org.jboss.on.domain.auth.Subject,
     * int)
     */
    public List<Resource> getResourcesForResourceGroup(Subject subject, int groupId, GroupCategory category) {
        ResourceGroup group = getResourceGroupById(subject, groupId, category);
        Set<Resource> res = group.getExplicitResources();
        List<Resource> ret = new ArrayList<Resource>(res.size());
        ret.addAll(res);

        return ret;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#getDefinitionsForAutoGroup(org.jboss.on.domain.auth.Subject,
     * int, int)
     */
    public int[] getDefinitionsForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId, boolean displayTypeSummaryOnly) {
        int[] ret;
        try {
            ResourceType rt = entityManager.find(ResourceType.class, autoGroupChildResourceTypeId);
            ret = getMeasurementDefinitionIdsForResourceType(rt, displayTypeSummaryOnly);
        } catch (EntityNotFoundException enfe) {
            ret = new int[0];
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal#getDefinitionsForCompatibleGroup(org.jboss.on.domain.auth.Subject,
     * int) TODO rework
     */
    public int[] getDefinitionsForCompatibleGroup(Subject subject, int groupId, boolean displayTypeSummaryOnly) {
        int[] ret = new int[0];
        try {
            ResourceGroup group = getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
            Set<Resource> resources = group.getExplicitResources();
            if ((resources != null) && (resources.size() > 0)) {
                Resource resource = resources.iterator().next();
                ResourceType type = resource.getResourceType();
                ret = getMeasurementDefinitionIdsForResourceType(type, displayTypeSummaryOnly);
            }
        } catch (ResourceGroupNotFoundException e) {
            log.debug("Resources for groupID: " + groupId + " not found " + e);
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public ResourceGroupComposite getResourceGroupWithAvailabilityById(Subject subject, int groupId) {
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_GROUP_COMPOSITE_BY_ID);
        query.setParameter("groupId", groupId);
        ResourceGroupComposite group = null;
        List<ResourceGroupComposite> groups = query.getResultList();

        //There should only be one record returned
        if ((groups != null) && (groups.size() == 1)) {
            group = groups.get(0);
            group.getResourceGroup().getModifiedBy().getFirstName();
        }

        return group;
    }

    @SuppressWarnings("unchecked")
    private int[] getMeasurementDefinitionIdsForResourceType(ResourceType type, boolean summariesOnly) {
        String queryString = "" //
            + "SELECT id " //
            + "  FROM MeasurementDefinition md " //
            + " WHERE md.resourceType.id = :resourceTypeId ";

        queryString += " AND md.dataType = :dataType";
        if (summariesOnly) {
            queryString += " AND md.displayType = :dispType";
        }

        // should respect the ordering
        queryString += " ORDER BY md.displayOrder, md.displayName";

        Query q = entityManager.createQuery(queryString);
        q.setParameter("resourceTypeId", type.getId());
        q.setParameter("dataType", DataType.MEASUREMENT);
        if (summariesOnly) {
            q.setParameter("dispType", DisplayType.SUMMARY);
        }

        List<Integer> res = q.getResultList();
        int[] ret = new int[res.size()];
        int i = 0;
        for (Integer r : res) {
            ret[i++] = r;
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public ResourceGroup findByGroupDefinitionAndGroupByClause(int groupDefinitionId, String groupByClause) {
        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_GROUP_DEFINITION_AND_EXPRESSION);

        /*
         * since oracle interprets empty strings as null, let's cleanse the
         * groupByClause so that the processing is identical on postgres
         */
        if (groupByClause.equals("")) {
            groupByClause = null;
        }
        query.setParameter("groupDefinitionId", groupDefinitionId);
        query.setParameter("groupByClause", groupByClause);

        List<ResourceGroup> groups = query.getResultList();

        if (groups.size() == 1) {
            // fyi, database constraints prevent dups on these two attributes
            ResourceGroup group = groups.get(0);
            group.getImplicitResources().size(); // initialize lazy set
            return group;
        } else // if ( groups.size() == 0 )
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void setResourceType(int resourceGroupId) throws ResourceTypeNotFoundException {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_GET_RESOURCE_TYPE_COUNTS_BY_GROUP);
        query.setParameter("groupId", resourceGroupId);

        Subject overlord = subjectManager.getOverlord();
        ResourceGroup resourceGroup = getResourceGroupById(overlord, resourceGroupId, null);

        List results = query.getResultList();
        if (results.size() == 1) {
            Object[] info = (Object[]) results.get(0);
            int resourceTypeId = (Integer) info[0];

            ResourceType type = resourceTypeManager.getResourceTypeById(overlord, resourceTypeId);

            resourceGroup.setResourceType(type);
        } else {
            resourceGroup.setResourceType(null);
        }
    }

    public int getImplicitGroupMemberCount(int resourceGroupId) {
        Query countQuery = entityManager
            .createNamedQuery(Resource.QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN);
        countQuery.setParameter("groupId", resourceGroupId);
        long count = (Long) countQuery.getSingleResult();
        return (int) count;
    }

    /*
     * This method constructs the implicit resource list based on an explicit resource passed to it.  If
     * <code>group.isRecursive()</code> is true, all of <code>resource</code>'s descendants will be added to the
     * implicit list. Otherwise, only <code>resource</code> will be added to the implicit list.
     *
     * This helper method simultaneously manages the explicit lists as well, so it is crucial to not make changes to the
     * explicit groups for a resource OR or explicit resources for a group outside of this method.
     */
    private void addResourcesToGroupHelper(ResourceGroup group, ResourceIdFlyWeight fly) {
        // both groups get the resource added to the explicit list
        group.addExplicitResource(fly);

        // implicit list mirrors explicit, if the group is not recursive
        if (!group.isRecursive()) {
            group.addImplicitResource(fly);
            return;
        }

        List<ResourceIdFlyWeight> toBeSearched = new LinkedList<ResourceIdFlyWeight>();
        toBeSearched.add(fly);

        // BFS the descendants of resource
        while (toBeSearched.size() > 0) {
            ResourceIdFlyWeight nextFly = toBeSearched.remove(0);

            // add to the collection we want to change
            group.addImplicitResource(nextFly);

            // and continue
            List<ResourceIdFlyWeight> children = resourceManager.getChildrenFlyWeights(nextFly.getId(),
                InventoryStatus.COMMITTED);
            toBeSearched.addAll(children);
        }
    }

    private void removeResourcesFromGroupHelper(ResourceGroup group, ResourceIdFlyWeight fly) {

        // groups always get the resources remove from the explicit list
        group.removeExplicitResource(fly);

        // non-recursive groups always get the resources removed from the explicit list
        if (!group.isRecursive()) {
            group.removeImplicitResource(fly);
            return;
        }

        /*
         * if some ancestor is in the explicit group, the implicit list will contain
         * the descendant subtree of resource - thus, no work has to be done here
         */
        List<Integer> lineage = resourceManager.getResourceIdLineage(fly.getId());
        for (Resource explicit : new HashSet<Resource>(group.getExplicitResources())) {
            Integer explicitId = explicit.getId();
            if (lineage.contains(explicitId)) {
                return;
            }
        }

        List<ResourceIdFlyWeight> toBeSearched = new LinkedList<ResourceIdFlyWeight>();

        // remove from the collection we want to change
        group.removeImplicitResource(fly);

        // BFS the descendants of resource - starting with the children
        toBeSearched.addAll(resourceManager.getChildrenFlyWeights(fly.getId(), InventoryStatus.COMMITTED));

        while (toBeSearched.size() > 0) {
            ResourceIdFlyWeight nextFly = toBeSearched.remove(0);

            /*
             * no need to remove the subtree from this relative root because we know it was also added explicitly to the
             * group
             */
            if (group.getExplicitResources().contains(nextFly)) {
                continue;
            }

            // remove from the collection we want to change
            group.removeImplicitResource(nextFly);

            // and continue
            toBeSearched.addAll(resourceManager.getChildrenFlyWeights(nextFly.getId(), InventoryStatus.COMMITTED));
        }
    }

    public PageList<ResourceGroupComposite> getResourceGroupsFiltered(Subject subject, GroupCategory groupCategory,
        ResourceCategory resourceCategory, ResourceType resourceType, String nameFilter, Integer resourceId,
        PageControl pc) {

        String query = ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER;
        if (authorizationManager.isInventoryManager(subject)) {
            query = query.replace("%SECURITY_FRAGMENT_JOIN%", "");
            query = query.replace("%SECURITY_FRAGMENT_WHERE%", "");
        } else {
            // add the security fragments when not the inventory manager
            query = query.replace("%SECURITY_FRAGMENT_JOIN%",
                ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER_SECURITY_FRAGMENT_JOIN);
            query = query.replace("%SECURITY_FRAGMENT_WHERE%",
                ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER_SECURITY_FRAGMENT_WHERE);
        }

        if (resourceId != null) {
            query = query.replace("%RESOURCE_FRAGMENT_WHERE%",
                ResourceGroup.QUERY_NATIVE_FIND_FILTERED_MEMBER_RESOURCE_FRAGMENT_WHERE);
        } else {
            query = query.replace("%RESOURCE_FRAGMENT_WHERE%", "");
        }

        pc.initDefaultOrderingField("rg.name");
        nameFilter = PersistenceUtility.formatSearchParameter(nameFilter);

        Connection conn = null;
        PreparedStatement stmt = null;

        List<Object[]> rawResults = new ArrayList<Object[]>();
        try {
            conn = rhqDs.getConnection();
            if (this.dbType instanceof PostgresqlDatabaseType) {
                query = query.replace("%IS_VISIBLE%", "TRUE");
                query = PersistenceUtility.addPostgresNativePagingSortingToQuery(query, pc);
            } else if (this.dbType instanceof OracleDatabaseType) {
                query = query.replace("%IS_VISIBLE%", "1");
                query = PersistenceUtility.addOracleNativePagingSortingToQuery(query, pc);
            } else {
                throw new RuntimeException("Unknown database type: " + this.dbType);
            }

            stmt = conn.prepareStatement(query);

            String search = nameFilter;
            Integer resourceTypeId = resourceType == null ? null : resourceType.getId();
            String resourceCategoryName = resourceCategory == null ? null : resourceCategory.name();
            String groupCategoryName = groupCategory == null ? null : groupCategory.name();

            int i = 0;
            if (resourceId != null) {
                stmt.setInt(++i, resourceId);
            }
            stmt.setString(++i, search);
            stmt.setString(++i, search);
            stmt.setString(++i, search);
            if (resourceTypeId == null) {
                stmt.setNull(++i, Types.INTEGER);
                stmt.setNull(++i, Types.INTEGER);
            } else {
                stmt.setInt(++i, resourceTypeId);
                stmt.setInt(++i, resourceTypeId);
            }
            stmt.setString(++i, resourceCategoryName);
            stmt.setString(++i, resourceCategoryName);
            if (resourceTypeId == null) {
                stmt.setNull(++i, Types.INTEGER);
            } else {
                stmt.setInt(++i, resourceTypeId);
            }
            stmt.setString(++i, resourceCategoryName);
            stmt.setString(++i, groupCategoryName);
            stmt.setString(++i, groupCategoryName);

            // set the 
            if (authorizationManager.isInventoryManager(subject) == false) {
                stmt.setInt(++i, subject.getId());
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long upCount = rs.getLong(1);
                long downCount = rs.getLong(2);
                int groupId = rs.getInt(3);
                Object[] next = new Object[] { upCount, downCount, groupId };
                rawResults.add(next);
            }
        } catch (Throwable t) {
            log.error("Could not execute groups query [ " + query + " ]: ", t);
            return new PageList<ResourceGroupComposite>();
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }

        Query queryCount = null;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT_ADMIN);
        } else {
            queryCount = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_ALL_FILTERED_COUNT);
            queryCount.setParameter("subject", subject);
        }

        queryCount.setParameter("groupCategory", groupCategory);
        queryCount.setParameter("category", resourceCategory);
        queryCount.setParameter("resourceType", resourceType);
        queryCount.setParameter("search", nameFilter);
        queryCount.setParameter("resourceId", resourceId);

        long count = (Long) queryCount.getSingleResult();

        List<Integer> groupIds = new ArrayList<Integer>();
        for (Object[] row : rawResults) {
            groupIds.add(((Number) row[2]).intValue());
        }
        Map<Integer, ResourceGroup> groupMap = getIdGroupMap(groupIds);

        List<ResourceGroupComposite> results = new ArrayList<ResourceGroupComposite>(rawResults.size());
        int i = 0;
        for (Object[] row : rawResults) {
            Long upCount = (Long) row[0];
            Long downCount = (Long) row[1];
            ResourceGroup group = groupMap.get(groupIds.get(i++));
            ResourceGroupComposite composite = new ResourceGroupComposite(upCount, downCount, group);
            results.add(composite);
        }

        return new PageList<ResourceGroupComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, ResourceGroup> getIdGroupMap(List<Integer> groupIds) {
        if (groupIds == null || groupIds.size() == 0) {
            return new HashMap<Integer, ResourceGroup>();
        }

        Query query = entityManager.createNamedQuery(ResourceGroup.QUERY_FIND_BY_IDS_admin);
        query.setParameter("ids", groupIds);
        List<ResourceGroup> groups = query.getResultList();

        Map<Integer, ResourceGroup> results = new HashMap<Integer, ResourceGroup>();
        for (ResourceGroup group : groups) {
            results.put(group.getId(), group);
        }
        return results;
    }
}