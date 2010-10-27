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
package org.rhq.enterprise.server.subsystem;

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.composite.AlertDefinitionComposite;
import org.rhq.core.domain.alert.composite.AlertHistoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */
@Local
public interface AlertSubsystemManagerLocal {

    PageList<AlertHistoryComposite> getAlertHistories(Subject subject, String resourceFilter, String parentFilter,
        Long startTime, Long endTime, AlertConditionCategory category, PageControl pc);

    PageList<AlertDefinitionComposite> getAlertDefinitions(Subject subject, String resourceFilter, String parentFilter,
        Long startTime, Long endTime, AlertConditionCategory category, PageControl pc);

    void deleteAlertHistories(Subject subject, Integer[] historyIds);

    int purgeAllAlertHistories(Subject subject);
}
