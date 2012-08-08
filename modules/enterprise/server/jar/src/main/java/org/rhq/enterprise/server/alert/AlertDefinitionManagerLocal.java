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

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * all methods that aren't getters appropriately update the contents of the AlertConditionCache
 *
 * @author Joseph Marques
 */
@Local
public interface AlertDefinitionManagerLocal {
    PageList<AlertDefinition> findAlertDefinitions(Subject subject, int resourceId, PageControl pageControl);

    AlertDefinition getAlertDefinitionById(Subject subject, int alertDefinitionId);

    List<IntegerOptionItem> findAlertDefinitionOptionItemsForResource(Subject subject, int resourceId);

    List<IntegerOptionItem> findAlertDefinitionOptionItemsForGroup(Subject subject, int groupId);

    int createAlertDefinition(Subject subject, AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException;

    /**
     * This is exactly the same as {@link #createAlertDefinition(Subject, AlertDefinition, Integer)} but
     * assumes the resource is part of a group (or has given resource type for templates) for which 
     * a group or template alert definition is being created.
     * <p>
     * This method assumes the caller already checked the subject has permissions to create a group or template alert
     * definition on a group / resource type the resource is member of.
     * <p>
     * In another words this method is a helper to 
     * {@link GroupAlertDefinitionManagerLocal#createGroupAlertDefinitions(Subject, AlertDefinition, Integer)} and
     * {@link AlertTemplateManagerLocal#createAlertTemplate(Subject, AlertDefinition, Integer)}.
     * 
     * @param subject the user that is creating the group or template alert definition
     * @param alertDefinition the alert definition on the resource
     * @param resourceId the resource
     * @return the id of the newly created alert definition
     */
    int createDependentAlertDefinition(Subject subject, AlertDefinition alertDefinition, int resourceId);
    
    boolean isEnabled(Integer definitionId);

    boolean isTemplate(Integer definitionId);

    boolean isGroupAlertDefinition(Integer definitionId);

    boolean isResourceAlertDefinition(Integer definitionId);

    List<AlertDefinition> findAllRecoveryDefinitionsById(Subject subject, Integer alertDefinitionId);

    void copyAlertDefinitions(Subject subject, Integer[] alertDefinitionIds);

    AlertDefinition updateAlertDefinition(Subject subject, int alertDefinitionId, AlertDefinition alertDefinition,
        boolean updateInternals) throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;

    int purgeUnusedAlertDefinitions();

    void purgeInternals(int alertDefinitionId);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId);

    PageList<AlertDefinition> findAlertDefinitionsByCriteria(Subject subject, AlertDefinitionCriteria criteria);

    int enableAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    int disableAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    int removeAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    String[] getAlertNotificationConfigurationPreview(Subject sessionSubject, AlertNotification[] notifications);
}