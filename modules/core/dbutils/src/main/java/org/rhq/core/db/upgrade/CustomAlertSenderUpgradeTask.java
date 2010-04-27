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
package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.rhq.core.db.DatabaseType;

/**
 * The introduction of custom alert senders brought with it the denormalization of the AlertNotification schema.
 * Instead of the AlertNotification entity storing the notification-related data itself (through referential integrity)
 * it has been subsumed inside of configuration objects, which are then associated back to the entity.
 * 
 * Each custom alert sender has full control over the structure within that configuration object.  This task represents
 * the work necessary to translate the first-class notification data (previously stored in the rhq_alert_notification 
 * table itself) into appropriate configuration objects to be used by the custom senders that will be shipped with the 
 * product by default.
 * 
 * In particular, this task handles the upgrade tasks for alert notifications setup against the following types of data:
 * 
 * <ul>
 *   <li>RHQ Subjects</li>
 *   <li>RHQ Roles</li>
 *   <li>Direct Email Addresses</li>
 *   <li>SNMP Trap Receivers</li>
 *   <li>Resource Operations</li>
 * </ul> 
 * 
 * @author Joseph Marques
 */
public class CustomAlertSenderUpgradeTask implements DatabaseUpgradeTask {

    private DatabaseType databaseType;
    private Connection connection;

    private final long NOW = System.currentTimeMillis();

    @Override
    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        this.databaseType = databaseType;
        this.connection = connection;

        upgradeSubjectNotifications();
        upgradeRoleNotifications();
        upgradeEmailNotifications();
        upgradeSNMPNotifications();
        upgradeOperationNotifications();
    }

    private void upgradeSubjectNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.subject_id "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'SUBJECT' "//
            + "ORDER BY notif.alert_definition_id";
        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        String propertyName = "subjectId";
        String senderName = "System Users";

        persist(data, propertyName, senderName);
    }

    private void upgradeRoleNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.role_id "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'ROLE' "//
            + "ORDER BY notif.alert_definition_id";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        String propertyName = "roleId";
        String senderName = "System Roles";

        persist(data, propertyName, senderName);
    }

    private void upgradeEmailNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.email_address "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'EMAIL' "//
            + "ORDER BY notif.alert_definition_id";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        String propertyName = "emailAddress";
        String senderName = "Direct Emails";

        persist(data, propertyName, senderName);
    }

    private void upgradeSNMPNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.snmp_host, notif.snmp_port, notif.snmp_oid "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'SNMP' "//
            + "ORDER BY notif.alert_definition_id";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        for (Object[] next : data) {
            int alertDefinitionId = ((Number) next[0]).intValue();
            String host = (String) next[1];
            String port = ((Number) next[2]).toString();
            String oid = (String) next[3];

            // buffer will be 0 the very first time, since definitionId is initially -1
            int configId = persistConfiguration("host", host, "port", port, "oid", oid);
            persistNotification(alertDefinitionId, configId, "SNMP Traps");
        }
    }

    private void upgradeOperationNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT def.id, def.operation_def_id" //
            + "    FROM rhq_alert_definition def";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        for (Object[] next : data) {
            int alertDefinitionId = ((Number) next[0]).intValue();
            String operationDefinitionId = ((Number) next[1]).toString();

            // buffer will be 0 the very first time, since definitionId is initially -1
            int configId = persistConfiguration("operation-definition-id", operationDefinitionId, "selection-mode",
                "SELF");
            persistNotification(alertDefinitionId, configId, "Resource Operations");
        }
    }

    private void persist(List<Object[]> data, String propertyName, String sender) throws SQLException {
        int definitionId = -1;
        StringBuilder buffer = new StringBuilder();
        for (Object[] next : data) {
            int nextDefinitionId = ((Number) next[0]).intValue();
            String nextData = String.valueOf(next[1]);
            if (nextDefinitionId != definitionId) {
                definitionId = nextDefinitionId;
                if (buffer.length() != 0) {
                    // buffer will be 0 the very first time, since definitionId is initially -1
                    int configId = persistConfiguration(propertyName, buffer.toString());
                    persistNotification(definitionId, configId, sender);
                }
                buffer = new StringBuilder(); // reset for the next definitionId
            }

            if (buffer.length() != 0) {
                // elements are already in the list
                buffer.append(',');
            }
            buffer.append(nextData);
        }

        if (buffer.length() != 0) {
            int configId = persistConfiguration(propertyName, buffer.toString());
            persistNotification(definitionId, configId, sender);
        }
    }

    private int persistConfiguration(String... propertyNameValues) throws SQLException {
        int configId = databaseType.getNextSequenceValue(connection, "rhq_config", "id");
        String insertConfigSQL = getInsertConfigSQL(configId);
        databaseType.executeSql(connection, insertConfigSQL);

        for (int i = 0; i < propertyNameValues.length; i += 2) {
            String propertyName = propertyNameValues[i];
            String propertyValue = propertyNameValues[i + 1];

            int propertyId = databaseType.getNextSequenceValue(connection, "rhq_config_property", "id");
            String insertPropertySQL = getInsertPropertySQL(propertyId, configId, propertyName, propertyValue);
            databaseType.executeSql(connection, insertPropertySQL);
        }

        return configId;
    }

    private void persistNotification(int definitionId, int configId, String sender) throws SQLException {
        int notificationId = databaseType.getNextSequenceValue(connection, "rhq_alert_notification", "id");
        String insertNotificationSQL = getInsertNotificationSQL(notificationId, definitionId, configId, sender);

        databaseType.executeSql(connection, insertNotificationSQL);
    }

    private String getInsertConfigSQL(int id) {
        return "INSERT INTO rhq_config ( id, ctime, mtime )" //
            + "      VALUES ( " + id + ", " + NOW + ", " + NOW + " ) ";
    }

    private String getInsertPropertySQL(int id, int configId, String name, String value) {
        return "INSERT INTO rhq_config_property ( id, configuration_id, name, string_value, dtype )" //
            + "      VALUES ( " + id + ", " + configId + ", '" + name + "', '" + value + "', 'property' ) ";
    }

    private String getInsertNotificationSQL(int id, int definitionId, int configId, String sender) {
        return "INSERT INTO rhq_alert_notification ( id, alert_definition_id, sender_config_id, sender_name )" //
            + "      VALUES ( " + id + ", " + definitionId + ", " + configId + ", '" + sender + "' ) ";
    }

}
