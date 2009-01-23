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
package org.rhq.enterprise.gui.common.metric;

import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;

public class MetricComponentUtilityUIBean extends PagedDataTableUIBean {

    private final Log log = LogFactory.getLog(this.getClass());

    public static final String MANAGED_BEAN_NAME = "MetricComponentUtilityUIBean";

    private boolean readOnly;

    public MetricComponentUtilityUIBean() {

    }

    public boolean isReadOnly() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        this.readOnly = rangePreferences.readOnly;
        return readOnly;
    }

    @Override
    public DataModel getDataModel() {
        return null;
    }

}
