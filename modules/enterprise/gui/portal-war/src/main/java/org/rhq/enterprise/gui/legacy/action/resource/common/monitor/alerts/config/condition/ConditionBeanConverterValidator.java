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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.condition;

import org.apache.struts.action.ActionErrors;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.alert.AlertCondition;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;

public interface ConditionBeanConverterValidator {
    public abstract void exportProperties(Subject subject, ConditionBean fromBean, AlertCondition toCondition);

    public abstract void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean);

    public abstract boolean validate(ConditionBean bean, ActionErrors errors, int index);

    public abstract String getTriggerName();
}