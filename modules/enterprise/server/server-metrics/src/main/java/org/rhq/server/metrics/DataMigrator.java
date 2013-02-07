/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;

import org.rhq.core.domain.measurement.MeasurementDataNumeric1D;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementDataNumeric6H;
import org.rhq.core.domain.measurement.MeasurementDataNumericAggregateInterface;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author Stefan Negrea
 *
 */
public class DataMigrator {

    private static final int MAX_RECORDS_TO_MIGRATE = 1000;
    private static final int MAX_NUMBER_OF_FAILURES = 5;

    private final EntityManager entityManager;
    private final Session session;

    private boolean deleteDataImmediatelyAfterMigration;
    private boolean deleteAllDataAtEndOfMigration;
    private boolean runRawDataMigration;
    private boolean run1HAggregateDataMigration;
    private boolean run6HAggregateDataMigration;
    private boolean run1DAggregateDataMigration;

    public DataMigrator(EntityManager entityManager, Session session) {
        this.entityManager = entityManager;
        this.session = session;

        this.deleteDataImmediatelyAfterMigration = true;
        this.deleteAllDataAtEndOfMigration = false;
        this.runRawDataMigration = true;
        this.run1HAggregateDataMigration = true;
        this.run6HAggregateDataMigration = true;
        this.run1DAggregateDataMigration = true;
    }

    public void run1HAggregateDataMigration(boolean value) {
        this.run1HAggregateDataMigration = value;
    }

    public void run6HAggregateDataMigration(boolean value) {
        this.run6HAggregateDataMigration = value;
    }

    public void run1DAggregateDataMigration(boolean value) {
        this.run1DAggregateDataMigration = value;
    }

    public void deleteDataImmediatelyAfterMigration(boolean value) {
        this.deleteDataImmediatelyAfterMigration = value;
        this.deleteAllDataAtEndOfMigration = !value;
    }

    public void deleteAllDataAtEndOfMigration(boolean value) {
        this.deleteAllDataAtEndOfMigration = value;
        this.deleteDataImmediatelyAfterMigration = !value;
    }

    public void migrateData() throws Exception {
        if (runRawDataMigration) {
            retryOnFailure(new RawDataMigrator());
        }

        if (run1HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.ONE_HOUR));
        }

        if (run6HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.SIX_HOUR));
        }

        if (run1DAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.TWENTY_FOUR_HOUR));
        }

        if (deleteAllDataAtEndOfMigration) {
            retryOnFailure(new DeleteAllData());
        }
    }

    /**
     * Retries the migration {@link #MAX_NUMBER_OF_FAILURES} times before
     * failing the migration operation.
     *
     * @param migrator
     * @throws Exception
     */
    private void retryOnFailure(CallableMigrationWorker migrator) throws Exception {
        int numberOfFailures = 0;
        Exception caughtException = null;

        while (numberOfFailures < MAX_NUMBER_OF_FAILURES) {
            try {
                migrator.work();
                return;
            } catch (Exception e) {
                caughtException = e;
                numberOfFailures++;
            }
        }

        throw caughtException;
    }

    /**
     * Returns a list of all the raw SQL metric tables.
     * There is no equivalent in Cassandra, all raw data is stored in a single column family.
     *
     * @return SQL raw metric tables
     */
    private String[] getRawDataTables() {
        int tableCount = 15;
        String tablePrefix = "RHQ_MEAS_DATA_NUM_R";

        String[] tables = new String[tableCount];
        for (int i = 0; i < tableCount; i++) {
            if (i < 10) {
                tables[i] = tablePrefix + "0" + i;
            } else {
                tables[i] = tablePrefix + i;
            }
        }

        return tables;
    }

    private interface CallableMigrationWorker {
        void work() throws Exception;
    }


    private class AggregateDataMigrator implements CallableMigrationWorker {

        private final String query;
        private final MetricsTable metricsTable;

        /**
         * @param query
         * @param metricsTable
         */
        public AggregateDataMigrator(MetricsTable metricsTable) {
            this.metricsTable = metricsTable;

            if (MetricsTable.ONE_HOUR.equals(this.metricsTable)) {
                this.query = MeasurementDataNumeric1H.QUERY_FIND_ALL;
            } else if (MetricsTable.ONE_HOUR.equals(this.metricsTable)) {
                this.query = MeasurementDataNumeric6H.QUERY_FIND_ALL;
            } else if (MetricsTable.TWENTY_FOUR_HOUR.equals(this.metricsTable)) {
                this.query = MeasurementDataNumeric1D.QUERY_FIND_ALL;
            } else {
                this.query = null;
            }
        }

        public void work() throws Exception {
            if (deleteDataImmediatelyAfterMigration) {
                performedBatchedMigration();
            } else {
                performFullMigration();
            }
        }

        @SuppressWarnings("unchecked")
        private void performedBatchedMigration() throws Exception {
            List<MeasurementDataNumericAggregateInterface> existingData;

            while (true) {
                Query q = entityManager.createNamedQuery(query);
                q.setMaxResults(MAX_RECORDS_TO_MIGRATE);
                existingData = (List<MeasurementDataNumericAggregateInterface>) q.getResultList();

                if (existingData.size() == 0) {
                    break;
                }

                insertDataToCassandra(existingData);

                for (Object entity : existingData) {
                    entityManager.remove(entity);
                }
                entityManager.flush();
            }
        }

        @SuppressWarnings("unchecked")
        private void performFullMigration() throws Exception {
            Query q = entityManager.createNamedQuery(query);
            List<MeasurementDataNumericAggregateInterface> existingData = (List<MeasurementDataNumericAggregateInterface>) q
                .getResultList();

            insertDataToCassandra(existingData);
        }

        private void insertDataToCassandra(List<MeasurementDataNumericAggregateInterface> existingData)
            throws Exception {
            String cql = "INSERT INTO " + metricsTable
                + " (schedule_id, time, type, value) VALUES (?, ?, ?, ?) USING TTL " + metricsTable.getTTL();
            PreparedStatement statement = session.prepare(cql);

            List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();

            for (MeasurementDataNumericAggregateInterface measurement : existingData) {
                BoundStatement boundStatement = statement.bind(measurement.getScheduleId(),
                    new Date(measurement.getTimestamp()), AggregateType.MIN.ordinal(), measurement.getMin());
                resultSetFutures.add(session.executeAsync(boundStatement));

                boundStatement = statement.bind(measurement.getScheduleId(), new Date(measurement.getTimestamp()),
                    AggregateType.MAX.ordinal(), measurement.getMax());
                resultSetFutures.add(session.executeAsync(boundStatement));

                boundStatement = statement.bind(measurement.getScheduleId(), new Date(measurement.getTimestamp()),
                    AggregateType.AVG.ordinal(), Double.parseDouble(measurement.getValue().toString()));
                resultSetFutures.add(session.executeAsync(boundStatement));
            }

            for (ResultSetFuture future : resultSetFutures) {
                future.get();
            }
        }
    }


    private class RawDataMigrator implements CallableMigrationWorker {

        public void work() throws Exception {
            if (deleteDataImmediatelyAfterMigration) {
                performBatchedMigration();
            } else {
                performFullMigration();
            }
        }

        @SuppressWarnings("unchecked")
        private void performBatchedMigration() throws Exception {
            List<Object[]> existingData = null;

            for (String table : getRawDataTables()) {
                String selectQuery = "SELECT schedule_id, value, time_stamp FROM " + table;
                String deleteQuery = "DELETE FROM " + table + " WHERE schedule_id = ?";

                while (true) {
                    Query query = entityManager.createNativeQuery(selectQuery);
                    query.setMaxResults(MAX_RECORDS_TO_MIGRATE);
                    existingData = query.getResultList();

                    if (existingData.size() == 0) {
                        break;
                    }

                    insertDataToCassandra(existingData);

                    query = entityManager.createNativeQuery(deleteQuery);

                    for (Object[] rawDataPoint : existingData) {
                        query.setParameter(0, Integer.parseInt(rawDataPoint[0].toString()));
                        query.executeUpdate();
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void performFullMigration() throws Exception {
            List<Object[]> existingData = null;

            for (String table : getRawDataTables()) {
                String selectQuery = "SELECT schedule_id, value, time_stamp FROM " + table;
                Query query = entityManager.createNativeQuery(selectQuery);
                existingData = query.getResultList();
                insertDataToCassandra(existingData);
            }
        }

        private void insertDataToCassandra(List<Object[]> existingData) throws Exception {
            String cql = "INSERT INTO " + MetricsTable.RAW + " (schedule_id, time, value) VALUES (?, ?, ?) USING TTL "
                + MetricsTable.RAW.getTTL();
            PreparedStatement statement = session.prepare(cql);

            List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();

            for (Object[] rawDataPoint : existingData) {
                BoundStatement boundStatement = statement.bind(Integer.parseInt(rawDataPoint[0].toString()),
                    new Date(Long.parseLong(rawDataPoint[1].toString())),
                    Double.parseDouble(rawDataPoint[2].toString()));
                resultSetFutures.add(session.executeAsync(boundStatement));
            }

            for (ResultSetFuture future : resultSetFutures) {
                future.get();
            }
        }
    }


    private class DeleteAllData implements CallableMigrationWorker {

        public void work() {
            Query q = entityManager.createNamedQuery(MeasurementDataNumeric1H.QUERY_DELETE_ALL);
            q.executeUpdate();

            q = entityManager.createNamedQuery(MeasurementDataNumeric6H.QUERY_DELETE_ALL);
            q.executeUpdate();

            q = entityManager.createNamedQuery(MeasurementDataNumeric1D.QUERY_DELETE_ALL);
            q.executeUpdate();

            for (String table : getRawDataTables()) {
                String deleteAllData = "DELETE FROM " + table;
                q = entityManager.createNativeQuery(deleteAllData);
                q.executeUpdate();
            }
        }
    }
}
