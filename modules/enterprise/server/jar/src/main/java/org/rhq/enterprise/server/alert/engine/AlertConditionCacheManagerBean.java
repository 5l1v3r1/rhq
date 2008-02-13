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
package org.rhq.enterprise.server.alert.engine;

import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.resource.Resource;

@Stateless
public class AlertConditionCacheManagerBean implements AlertConditionCacheManagerLocal {
    private static final Log log = LogFactory.getLog(AlertConditionCacheManagerBean.class);

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void beforeBaselineCalculation() {
        AlertConditionCache.getInstance().beforeBaselineCalculation();
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void afterBaselineCalculation() {
        AlertConditionCache.getInstance().afterBaselineCalculation();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(measurementData);
        return stats;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(operationHistory);
        return stats;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().checkConditions(availabilities);
        return stats;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AlertConditionCacheStats updateConditions(Resource deletedResource) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().updateConditions(deletedResource);
        return stats;
    }

    // this could potentially take really long, but we don't need to be in a transactional scope anyway
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public AlertConditionCacheStats updateConditions(List<MeasurementBaselineComposite> measurementBaselines) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().updateConditions(measurementBaselines);
        return stats;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AlertConditionCacheStats updateConditions(AlertDefinition alertDefinition,
        AlertDefinitionEvent alertDefinitionEvent) {
        AlertConditionCacheStats stats;
        stats = AlertConditionCache.getInstance().updateConditions(alertDefinition, alertDefinitionEvent);
        return stats;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isCacheValid() {
        boolean valid;
        valid = AlertConditionCache.getInstance().isCacheValid();
        return valid;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String[] getCacheNames() {
        String[] names;
        names = AlertConditionCache.getInstance().getCacheNames();
        return names;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void printCache(String cacheName) {
        AlertConditionCache.getInstance().printCache(cacheName);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void printAllCaches() {
        AlertConditionCache.getInstance().printAllCaches();
    }
}