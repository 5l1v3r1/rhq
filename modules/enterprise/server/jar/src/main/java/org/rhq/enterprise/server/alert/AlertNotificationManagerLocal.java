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
import org.rhq.core.domain.alert.notification.AlertNotificationTemplate;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;

/**
 * @author Joseph Marques
 */

@Local
public interface AlertNotificationManagerLocal {

    /**
     * Remove the passed notifications from the passed alert definition (all identified by their id)
     * @param subject Caller
     * @param alertDefinitionId alert definition to modify
     * @param notificationIds Notifications to remove
     * @return number of notifications removed
     */
    int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds);

    int purgeOrphanedAlertNotifications();

    public Configuration getAlertPropertiesConfiguration(AlertNotification notification);

    /**
     * Return a list of all available AlertSenders in the system by their shortname.
     * @return list of senders.
     */
    List<String> listAllAlertSenders();

    ConfigurationDefinition getConfigurationDefinitionForSender(String shortName);

    /**
     * Add a new AlertNotification to the passed definition
     * @param user subject of the caller
     * @param alertDefinitionId Id of the alert definition
     * @param senderName shortName of the {@link AlertSender}
     * @param alertName name of the new {@link AlertNotification}
     * @param configuration Properties for this alert sender.
     */
    AlertNotification addAlertNotification(Subject user, int alertDefinitionId, String senderName, String alertName,
        Configuration configuration);

    /**
     * Return notifications for a certain alertDefinitionId
     *
     * NOTE: this only returns notifications that have an AlertSender defined.
     *
     * @param user Subject of the caller
     * @param alertDefinitionId Id of the alert definition
     * @return list of defined notification of the passed alert definition
     *
     *
     */
    List<AlertNotification> getNotificationsForAlertDefinition(Subject user, int alertDefinitionId);

    /**
     * Persist changes to the passed {@link AlertNotification}
     *
     * @param notification
     */
    void updateAlertNotification(AlertNotification notification);

    AlertSenderInfo getAlertInfoForSender(String shortName);

    /**
     * Take the passed NotificationTemplate and apply its Notifications to the passed AlertDefinition
     * @param template NotificationTemplate to apply
     * @param def AlertDefinition  to apply the template to
     * @param removeOldNotifications Shall old Notifications on the Definition be removed?
     */
    void applyNotificationTemplateToAlertDefinition(AlertNotificationTemplate template, AlertDefinition def,
        boolean removeOldNotifications);

    /**
     * Return the backing bean for the AlertSender with the passed shortNama
     * @param shortName name of a sender
     * @param alertNotificationId
     * @return an initialized BackingBean or null in case of error
     */
    CustomAlertSenderBackingBean getBackingBeanForSender(String shortName, Integer alertNotificationId);

    String getBackingBeanNameForSender(String shortName);

    /**
     * Add the passed 'transient' notifications onto the alert definitions contained. The old
     * notifications are removed.
     * This method is mainly used when migrating alerts from an old format to the current.
     * @param subject Subject of the caller
     * @param notifications list of AlertNotifications that have the alert definition id encoded in a transient field
     */
    void mergeTransientAlertNotifications(Subject subject, List<AlertNotification> notifications);

    /**
     * Create a new NotificationTemplate from the passed parameters. The passed AlertNotification objects need to have the
     * name and sender and any configuration properties already set; alert definitions must not be set.
     * @param name name of this notification template. Must be unique
     * @param description description of the template
     * @param notifications notifications that make up the template
     * @param copyNotifications
     * @return the newly created template
     * @throws IllegalArgumentException when a template with the passed name already exists
     */
    AlertNotificationTemplate createNotificationTemplate(String name, String description,
        List<AlertNotification> notifications, boolean copyNotifications) throws IllegalArgumentException;

    void updateNotificationTemplate(Subject subject, int templateId, String newName, String newDescription);

    /**
     * Get all defined notification templates in the system along with their AlertNotifications
     * @param user Subject of the caller
     * @return List of all defined alert notification templates
     */
    List<AlertNotificationTemplate> listNotificationTemplates(Subject user);

    AlertNotificationTemplate getAlertNotificationTemplate(Subject user, int alertNotificationTemplateId);

    /**
     * Take the passed NotificationTemplate and apply its Notifications to the passed AlertDefinition
     * @param templateName name of a pre-defined alert NotificationTemplate
     * @param alertDefinitionId id of an AlertDefinition on which the template should be applied
     * @param removeOldNotifications Shall old Notifications on the Definition be removed?
     */
    void applyNotificationTemplateToAlertDefinition(String templateName, int alertDefinitionId,
        boolean removeOldNotifications);

    /**
     * Add a new alert Notification to a template
     * @param user subject of the caller
     * @param templateName name of the NotificationTemplate to use
     * @param sender the alert sender to use
     * @param notificationName the name of this notification
     * @param notificationConfiguration the configuration of this AlertNotification
     * @return the new AlertNotification
     */
    AlertNotification addAlertNotificationToTemplate(Subject user, int templateId, String sender,
        String notificationName, Configuration notificationConfiguration);

    int removeNotificationsFromTemplate(Subject subject, int templateId, Integer[] notificationIds);

    List<AlertNotification> getNotificationsForTemplate(Subject subject, int templateId);

    /**
     * Delete the passed Notification Templates
     * @param subject subject of the caller
     * @param templateIds ids of the templates to delete
     * @return number of templates deleted
     */
    int deleteNotificationTemplates(Subject subject, Integer[] templateIds);
}
