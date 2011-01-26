/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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


/**
 * Thrown when alert definition notifications validation fails during creation or update of
 * alert definition.
 *
 * @author Lukas Krejci
 */
public class AlertNotificationValidationException extends AlertDefinitionException {

    private static final long serialVersionUID = 1L;

    public AlertNotificationValidationException() {
        super();
    }

    public AlertNotificationValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlertNotificationValidationException(String message) {
        super(message);
    }

    public AlertNotificationValidationException(Throwable cause) {
        super(cause);
    }

}
