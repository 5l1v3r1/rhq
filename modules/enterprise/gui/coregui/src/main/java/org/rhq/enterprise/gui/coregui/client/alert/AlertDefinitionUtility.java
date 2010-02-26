/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.alert;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;

import java.util.ArrayList;
import java.util.List;

/**
 * The methods in this class are ported from AlertDefUtil from portal-war and MeasurementFormatter from
 * server-jar.
 *
 * @author Ian Springer
 */
public class AlertDefinitionUtility {
    private static final String BASELINE_OPT_MEAN = "mean";
    private static final String BASELINE_OPT_MIN = "min";
    private static final String BASELINE_OPT_MAX = "max";

    private static final String MEASUREMENT_BASELINE_MIN_TEXT = "Min Value";
    private static final String MEASUREMENT_BASELINE_MEAN_TEXT = "Baseline Value";
    private static final String MEASUREMENT_BASELINE_MAX_TEXT = "Max Value";

    @SuppressWarnings("deprecation")
    public static String formatAlertConditionForDisplay(AlertCondition condition) {
        AlertConditionCategory category = condition.getCategory();

        StringBuilder textValue = new StringBuilder();

        // first format the LHS of the operator
        if (category == AlertConditionCategory.CONTROL) {
            try {
                String operationName = condition.getName();
                /*Integer resourceTypeId = condition.getAlertDefinition().getResource().getResourceType().getId();
                OperationManagerLocal operationManager = LookupUtil.getOperationManager();
                OperationDefinition definition = operationManager.getOperationDefinitionByResourceTypeAndName(
                    resourceTypeId, operationName, false);
                String operationDisplayName = definition.getDisplayName();*/
                textValue.append(operationName).append(' ');
            } catch (Exception e) {
                textValue.append(condition.getName()).append(' ');
            }
        } else if (category == AlertConditionCategory.RESOURCE_CONFIG) {
            textValue.append("Resource Configuration").append(' ');
        } else {
            textValue.append(condition.getName()).append(' ');
        }

        // next format the RHS
        if (category == AlertConditionCategory.CONTROL) {
            textValue.append(condition.getOption());
        } else if ((category == AlertConditionCategory.THRESHOLD) || (category == AlertConditionCategory.BASELINE)) {
            textValue.append(condition.getComparator());
            textValue.append(' ');

            MeasurementSchedule schedule = null;

            MeasurementUnits units;
            double value = condition.getThreshold();
            if (category == AlertConditionCategory.THRESHOLD) {
                units = condition.getMeasurementDefinition().getUnits();
            } else // ( category == AlertConditionCategory.BASELINE )
            {
                units = MeasurementUnits.PERCENTAGE;
            }

            String formatted = MeasurementConverterClient.format(value, units, true);
            textValue.append(formatted);

            if (category == AlertConditionCategory.BASELINE) {
                textValue.append(" of ");
                textValue.append(getBaselineText(condition.getOption(), schedule));
            }
        } else if (category == AlertConditionCategory.RESOURCE_CONFIG || category == AlertConditionCategory.CHANGE
            || category == AlertConditionCategory.TRAIT) {
            textValue.append("Value Changed");
        } else if (category == AlertConditionCategory.EVENT) {
            String msgKey = "alert.config.props.CB.EventSeverity";
            List<String> args = new ArrayList<String>(2);

            args.add(condition.getName());
            if ((condition.getOption() != null) && (condition.getOption().length() > 0)) {
                msgKey += ".RegexMatch";
                args.add(condition.getOption());
            }
            // TODO
            textValue.append("TODO ").append(args);
        } else if (category == AlertConditionCategory.AVAILABILITY) {
            // TODO
            textValue.append("Availability ").append(condition.getOption());
        } else {
            // do nothing
        }

        return textValue.toString();
    }

    public static String getAlertRecoveryInfo(Alert alert) {
        String recoveryInfo;
        AlertDefinition recoveryAlertDefinition = alert.getRecoveryAlertDefinition();
        if (recoveryAlertDefinition != null && recoveryAlertDefinition.getId() != 0) {
            int resourceId = alert.getAlertDefinition().getResource().getId();
            recoveryInfo = "Triggered '<a href=\"/alerts/Config.do?mode=viewRoles&id=" + resourceId + "&ad=" + recoveryAlertDefinition.getId()
                + "\">" + recoveryAlertDefinition.getName() + "</a>' to be re-enabled.";
        } else if (alert.getWillRecover()) {
            recoveryInfo = "This alert caused its alert definition to be disabled.";
        } else {
            recoveryInfo = "N/A";
        }
        return recoveryInfo;
    }

    private static String getBaselineText(String baselineOption, MeasurementSchedule schedule) {
        if ((null != schedule) && (null != schedule.getBaseline())) {
            MeasurementBaseline baseline = schedule.getBaseline();

            String lookupText = null;
            Double value = null;

            if (baselineOption.equals(BASELINE_OPT_MIN)) {
                lookupText = MEASUREMENT_BASELINE_MIN_TEXT;
                value = baseline.getMin();
            } else if (baselineOption.equals(BASELINE_OPT_MEAN)) {
                lookupText = MEASUREMENT_BASELINE_MEAN_TEXT;
                value = baseline.getMean();
            } else if (baselineOption.equals(BASELINE_OPT_MAX)) {
                lookupText = MEASUREMENT_BASELINE_MAX_TEXT;
                value = baseline.getMax();
            }

            if (value != null) {
                try {
                    String formatted = MeasurementConverterClient.scaleAndFormat(value, schedule, true);
                    return formatted + " (" + lookupText + ")";
                } catch (MeasurementConversionException mce) {
                    return lookupText;
                }
            }
            /*
             * will need a fall-through here because the value was null; this can happen when the user requests to view
             * the formatted baseline before the first time it has been calculated
             */
        }

        // here is the fall-through
        if (BASELINE_OPT_MIN.equals(baselineOption)) {
            return MEASUREMENT_BASELINE_MIN_TEXT;
        } else if (BASELINE_OPT_MAX.equals(baselineOption)) {
            return MEASUREMENT_BASELINE_MAX_TEXT;
        } else {
            return MEASUREMENT_BASELINE_MEAN_TEXT;
        }
    }

    private AlertDefinitionUtility() {
    }
}
