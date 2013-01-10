/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * Alert Definition
 * @author Heiko W. Rupp
 */
@ApiClass("Representation of an AlertDefinition")
@XmlRootElement
public class AlertDefinitionRest {

    int id;
    String name;
    boolean enabled;
    String priority = "LOW";
    int recoveryId;
    String conditionMode = "ANY"; // ANY, ALL
    List<AlertConditionRest> conditions = new ArrayList<AlertConditionRest>();
    List<AlertNotificationRest> notifications = new ArrayList<AlertNotificationRest>();


    public AlertDefinitionRest() {
    }

    public AlertDefinitionRest(int id) {
        this.id = id;
    }

    @ApiProperty("The id of the definition")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiProperty("The name of the definition")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiProperty("Is the definition enabled(=active)?")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @ApiProperty(value = "The priority of the definition",
        allowableValues = "LOW, MEDIUM, HIGH")
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getRecoveryId() {
        return recoveryId;
    }

    public void setRecoveryId(int recoveryId) {
        this.recoveryId = recoveryId;
    }

    @ApiProperty(value = "Expression to use for condition logic",
        allowableValues = "ALL, ANY")
    public String getConditionMode() {
        return conditionMode;
    }

    public void setConditionMode(String conditionMode) {
        this.conditionMode = conditionMode;
    }

    @ApiProperty(value = "List of Conditions. Only sent if explicitly requested.")
    public List<AlertConditionRest> getConditions() {
        return conditions;
    }

    public void setConditions(List<AlertConditionRest> conditions) {
        this.conditions = conditions;
    }

    @ApiProperty(value = "List of notifications. Only sent if explicitly requested.")
    public List<AlertNotificationRest> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<AlertNotificationRest> notifications) {
        this.notifications = notifications;
    }
}
