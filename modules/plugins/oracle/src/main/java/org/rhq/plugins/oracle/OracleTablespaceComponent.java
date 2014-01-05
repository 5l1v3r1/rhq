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

package org.rhq.plugins.oracle;

import static org.rhq.plugins.database.DatabasePluginUtil.getConnectionFromComponent;
import static org.rhq.plugins.database.DatabasePluginUtil.getNumericQueryValues;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.AbstractDatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * Oracle Tablespace Component.
 * 
 * @author Richard Hensman
 */
public class OracleTablespaceComponent extends AbstractDatabaseComponent implements MeasurementFacet {
    private static final Log LOG = LogFactory.getLog(OracleTablespaceComponent.class);

    private static final String SQL_AVAILABLE = "SELECT COUNT(*) FROM dba_tablespaces WHERE tablespace_name = ?";
    private static final String SQL_VALUES = "SELECT USED_SPACE usedSpace, TABLESPACE_SIZE totalSize, (USED_PERCENT/100) usedPercent "
        + "FROM dba_tablespace_usage_metrics where tablespace_name = ?";

    public AvailabilityType getAvailability() {
        Connection jdbcConnection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getConnectionFromComponent(this);
            statement = jdbcConnection.prepareStatement(SQL_AVAILABLE);
            statement.setString(1, this.resourceContext.getResourceKey());
            resultSet = statement.executeQuery();
            if (resultSet.next() && (resultSet.getInt(1) == 1)) {
                return AvailabilityType.UP;
            }
        } catch (SQLException e) {
            LOG.debug("unable to query", e);
        } finally {
            DatabasePluginUtil.safeClose(null, statement, resultSet);
            if (supportsConnectionPooling()) {
                DatabasePluginUtil.safeClose(jdbcConnection);
            }
        }

        return AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Map<String, Double> values = getNumericQueryValues(this, SQL_VALUES, this.resourceContext.getResourceKey());
        for (MeasurementScheduleRequest request : metrics) {
            Double d = values.get(request.getName().toUpperCase(Locale.US));
            if (d != null) {
                report.addData(new MeasurementDataNumeric(request, d));
            }
        }
    }
}
