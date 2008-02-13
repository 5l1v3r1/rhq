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

import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.event.alert.AlertCondition;
import org.rhq.core.domain.event.alert.AlertConditionCategory;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.AlertDefinitionEvent;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
@Stateless
public class AlertDefinitionManagerBean implements AlertDefinitionManagerLocal {
    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(AlertDefinitionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    AuthorizationManagerLocal authorizationManager;
    @EJB
    AlertConditionCacheManagerLocal alertConditionCacheManager;

    private boolean checkPermission(Subject subject, AlertDefinition alertDefinition) {
        /*
         * system side-effects are primarily call-outs from the template manager to the definition manager, when the
         * template operation is being cascaded
         */
        if (authorizationManager.isOverlord(subject)) {
            return true;
        }

        if (alertDefinition.getResourceType() != null) // an alert template
        {
            return authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_INVENTORY);
        } else // an alert definition
        {
            return authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS, alertDefinition
                .getResource().getId());
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> getAllAlertDefinitionsWithConditions() {
        Query query = entityManager.createNamedQuery(AlertDefinition.QUERY_FIND_ALL_WITH_CONDITIONS);
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, PageControl.getUnlimitedInstance());
    }

    @SuppressWarnings("unchecked")
    public PageList<AlertDefinition> getAlertDefinitions(Subject user, int resourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, AlertDefinition.QUERY_FIND_BY_RESOURCE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, AlertDefinition.QUERY_FIND_BY_RESOURCE,
            pageControl);

        queryCount.setParameter("id", resourceId);
        query.setParameter("id", resourceId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<AlertDefinition> list = query.getResultList();

        return new PageList<AlertDefinition>(list, (int) totalCount, pageControl);
    }

    public AlertDefinition getAlertDefinitionById(Subject user, int alertDefinitionId) {
        LOG.debug("AlertDefinitionManager.getAlertDefinitionById(" + user + ", " + alertDefinitionId + ")");
        AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefinitionId);
        if (alertDefinition != null) {
            // avoid NPEs if the caller passed an invalid id
            alertDefinition.getConditions().size();
            alertDefinition.getAlerts().size();
            alertDefinition.getAlertNotifications().size();
        }

        return alertDefinition;
    }

    public int createAlertDefinition(Subject user, AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException {
        checkAlertDefinition(alertDefinition);

        // if this is an alert definition, set up the link to a resource
        if (resourceId != null) {
            // don't attach an alert template to any particular resource
            // the template should have already been attached to the resourceType by the template manager
            Resource resource = LookupUtil.getResourceManager().getResourceById(user, resourceId);

            alertDefinition.setResource(resource);
        }

        // after the resource is set up (in the case of non-templates), we can use the checkPermission on it
        if (checkPermission(user, alertDefinition) == false) {
            if (resourceId != null) {
                throw new PermissionException("User [" + user.getName()
                    + "] does not have permission to create alert definitions" + "for resource ["
                    + alertDefinition.getResource() + "]");
            } else {
                throw new PermissionException("User [" + user.getName()
                    + "] does not have permission to create alert templates");
            }
        }

        entityManager.persist(alertDefinition);
        entityManager.flush();

        // Only add to the cache if the alert definition was created as active
        if ((resourceId != null) && alertDefinition.getEnabled()) {
            // don't notify on an alert template, only for those that get attached to a resource
            notifyAlertConditionCacheManager("createAlertDefinition", alertDefinition, AlertDefinitionEvent.CREATED);
        }

        return alertDefinition.getId();
    }

    public int removeAlertDefinitions(Subject user, Integer[] alertDefinitionIds) {
        int modifiedCount = 0;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(user, alertDefinition)) {
                alertDefinition.setDeleted(true);
                modifiedCount++;

                notifyAlertConditionCacheManager("removeAlertDefinitions", alertDefinition,
                    AlertDefinitionEvent.DELETED);
            }
        }

        return modifiedCount;
    }

    public int enableAlertDefinitions(Subject user, Integer[] alertDefinitionIds) {
        int modifiedCount = 0;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(user, alertDefinition)) {
                // only enable an alert if it's not currently enabled
                if (alertDefinition.getEnabled() == false) {
                    alertDefinition.setEnabled(true);
                    modifiedCount++;

                    // thus, add it to the cache since it (shouldn't) already exist
                    notifyAlertConditionCacheManager("enableAlertDefinitions", alertDefinition,
                        AlertDefinitionEvent.ENABLED);
                }
            }
        }

        return modifiedCount;
    }

    public int disableAlertDefinitions(Subject user, Integer[] alertDefinitionIds) {
        int modifiedCount = 0;
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(user, alertDefinition)) {
                // only disable an alert if it's currently enabled
                if (alertDefinition.getEnabled() == true) {
                    alertDefinition.setEnabled(false);
                    modifiedCount++;

                    // thus, remove it from the cache since it (should) already exist
                    notifyAlertConditionCacheManager("disableAlertDefinitions", alertDefinition,
                        AlertDefinitionEvent.DISABLED);
                }
            }
        }

        return modifiedCount;
    }

    public void copyAlertDefinitions(Subject user, Integer[] alertDefinitionIds) {
        for (int alertDefId : alertDefinitionIds) {
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefId);

            // TODO GH: Can be more efficient
            if (checkPermission(user, alertDefinition)) {
                AlertDefinition newAlertDefinition = new AlertDefinition(alertDefinition);
                newAlertDefinition.setEnabled(false);

                // this is a "true" copy, so update parentId, resource, and resourceType
                newAlertDefinition.setParentId(alertDefinition.getParentId());
                newAlertDefinition.setResource(alertDefinition.getResource());
                newAlertDefinition.setResourceType(alertDefinition.getResourceType());

                entityManager.persist(newAlertDefinition);

                notifyAlertConditionCacheManager("copyAlertDefinitions", alertDefinition, AlertDefinitionEvent.CREATED);
            }
        }
    }

    public AlertDefinition updateAlertDefinition(Subject user, AlertDefinition alertDefinition)
        throws InvalidAlertDefinitionException, AlertDefinitionUpdateException {
        boolean isAlertTemplate = (alertDefinition.getResourceType() != null);

        if (checkPermission(user, alertDefinition) == false) {
            if (isAlertTemplate) {
                throw new PermissionException("You do not have permission to modify this alert template");
            } else {
                throw new PermissionException("You do not have permission to modify this alert definition");
            }
        }

        /*
         * Method for catching ENABLE / DISABLE changes will use switch logic off of the delta instead of calling out to
         * the enable/disable functions
         */
        AlertDefinition oldAlertDefinition = getAlertDefinitionById(user, alertDefinition.getId());

        /*
         * Should not be able to update an alert definition if the old alert definition is in an invalid state
         */
        if (alertDefinition.getDeleted()) {
            throw new AlertDefinitionUpdateException("Can not update deleted " + alertDefinition.toSimpleString());
        }

        /*
         * only need to check the validity of the new alert definition if the authz checks pass *and* the old definition
         * is not currently deleted
         */
        checkAlertDefinition(alertDefinition);

        AlertDefinitionUpdateType updateType = AlertDefinitionUpdateType.get(oldAlertDefinition, alertDefinition);

        if ((isAlertTemplate == false)
            && ((updateType == AlertDefinitionUpdateType.JUST_DISABLED) || (updateType == AlertDefinitionUpdateType.STILL_ENABLED))) {
            /*
             * if you were JUST_DISABLED or STILL_ENABLED, you are coming from the ENABLED state, which means you need
             * to be removed from the cache as the first half of this update
             */
            notifyAlertConditionCacheManager("updateAlertDefinition", oldAlertDefinition, AlertDefinitionEvent.DELETED);
        }

        AlertDefinition newAlertDefinition = entityManager.merge(alertDefinition);

        if ((isAlertTemplate == false)
            && ((updateType == AlertDefinitionUpdateType.JUST_ENABLED) || (updateType == AlertDefinitionUpdateType.STILL_ENABLED))) {
            /*
             * if you were JUST_ENABLED or STILL_ENABLED, you are moving to the ENABLED state, which means you need to
             * be added to the cache as the last half of this update
             */
            notifyAlertConditionCacheManager("updateAlertDefinition", newAlertDefinition, AlertDefinitionEvent.CREATED);
        }

        /*
         * note, nothing is done to the cache in the STILL_DISABLED case because nothing should've been in the cache to
         * begin with, and nothing needs to be added to the cache as a result
         */

        return newAlertDefinition;
    }

    /*
     * A helper enum to make for cleaner logic in updateAlertDefinition( Subject, AlertDefinition, boolean )
     */
    enum AlertDefinitionUpdateType {
        JUST_ENABLED, JUST_DISABLED, STILL_ENABLED, STILL_DISABLED;

        public static AlertDefinitionUpdateType get(AlertDefinition oldDefinition, AlertDefinition newDefinition) {
            if ((oldDefinition.getEnabled() == false) && (newDefinition.getEnabled() == true)) {
                return AlertDefinitionUpdateType.JUST_ENABLED;
            } else if ((oldDefinition.getEnabled() == true) && (newDefinition.getEnabled() == false)) {
                return AlertDefinitionUpdateType.JUST_DISABLED;
            } else if ((oldDefinition.getEnabled() == true) && (newDefinition.getEnabled() == true)) {
                return AlertDefinitionUpdateType.STILL_ENABLED;
            } else {
                return AlertDefinitionUpdateType.STILL_DISABLED;
            }
        }
    }

    private void checkAlertDefinition(AlertDefinition alertDefinition) throws InvalidAlertDefinitionException {
        for (AlertCondition alertCondition : alertDefinition.getConditions()) {
            AlertConditionCategory alertConditionCategory = alertCondition.getCategory();
            if (alertConditionCategory == AlertConditionCategory.ALERT) {
                throw new InvalidAlertDefinitionException(
                    "AlertDefinitionManager does not yet support condition category: " + alertConditionCategory);
            }
        }

        return;
    }

    private void notifyAlertConditionCacheManager(String methodName, AlertDefinition alertDefinition,
        AlertDefinitionEvent alertDefinitionEvent) {
        AlertConditionCacheStats stats = alertConditionCacheManager.updateConditions(alertDefinition,
            alertDefinitionEvent);

        LOG.debug(methodName + ": " + stats.toString());
    }
}