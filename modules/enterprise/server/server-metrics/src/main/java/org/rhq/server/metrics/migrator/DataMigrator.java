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

package org.rhq.server.metrics.migrator;

import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StatelessSession;

import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;


/**
 * @author Stefan Negrea
 *
 */
public class DataMigrator {

    public enum DatabaseType {
        Postgres, Oracle
    }

    private final Log log = LogFactory.getLog(DataMigrator.class);


    private static final int MAX_RECORDS_TO_LOAD_FROM_SQL = 30000;
    private static final int MAX_RAW_BATCH_TO_CASSANDRA = 100;
    private static final int MAX_AGGREGATE_BATCH_TO_CASSANDRA = 50;
    private static final int MAX_NUMBER_OF_FAILURES = 5;
    private static final int NUMBER_OF_BATCHES_FOR_ESTIMATION = 4;
    private static final double UNDER_ESTIMATION_FACTOR = .15;
    public static final int SQL_TIMEOUT = 6000000;


    private enum MigrationQuery {
        SELECT_1H_DATA("SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_1H ORDER BY schedule_id, time_stamp"),
        SELECT_6H_DATA("SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_6H ORDER BY schedule_id, time_stamp"),
        SELECT_1D_DATA("SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_1D ORDER BY schedule_id, time_stamp"),

        DELETE_1H_DATA("DELETE FROM RHQ_MEASUREMENT_DATA_NUM_1H"),
        DELETE_6H_DATA("DELETE FROM RHQ_MEASUREMENT_DATA_NUM_6H"),
        DELETE_1D_DATA("DELETE FROM RHQ_MEASUREMENT_DATA_NUM_1D"),

        COUNT_1H_DATA("SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_1H"),
        COUNT_6H_DATA("SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_6H"),
        COUNT_1D_DATA("SELECT COUNT(*) FROM RHQ_MEASUREMENT_DATA_NUM_1D"),

        COUNT_RAW("SELECT COUNT(*) FROM %s"),
        SELECT_RAW_DATA("SELECT schedule_id, time_stamp, value FROM %s ORDER BY schedule_id, time_stamp"),
        DELETE_RAW_ALL_DATA("DELETE FROM %s"),
        DELETE_RAW_ENTRY("DELETE FROM %s WHERE schedule_id = ?");

        public static final int SCHEDULE_INDEX = 0;
        public static final int TIMESTAMP_INDEX = 1;
        public static final int VALUE_INDEX = 2;
        public static final int MIN_VALUE_INDEX = 3;
        public static final int MAX_VALUE_INDEX = 4;

        private String query;

        private MigrationQuery(String query){
            this.query = query;
        }

        /**
         * @return the query
         */
        public String getQuery() {
            return query;
        }

        @Override
        public String toString() {
            return query;
        }
    }

    private final EntityManager entityManager;
    private final Session session;
    private final DatabaseType databaseType;
    private final boolean experimentalDataSource;

    private boolean deleteDataImmediatelyAfterMigration;
    private boolean deleteAllDataAtEndOfMigration;

    private boolean runRawDataMigration;
    private boolean run1HAggregateDataMigration;
    private boolean run6HAggregateDataMigration;
    private boolean run1DAggregateDataMigration;

    private long estimation;

    public DataMigrator(EntityManager entityManager, Session session, DatabaseType databaseType) {
        this(entityManager, session, databaseType, false);
    }

    public DataMigrator(EntityManager entityManager, Session session, DatabaseType databaseType,
        boolean experimentalDataSource) {
        this.entityManager = entityManager;
        this.session = session;
        this.databaseType = databaseType;

        this.experimentalDataSource = experimentalDataSource;

        this.deleteDataImmediatelyAfterMigration = false;
        this.deleteAllDataAtEndOfMigration = false;
        this.runRawDataMigration = true;
        this.run1HAggregateDataMigration = true;
        this.run6HAggregateDataMigration = true;
        this.run1DAggregateDataMigration = true;
    }

    public void runRawDataMigration(boolean value) {
        this.runRawDataMigration = value;
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


    public void deleteDataImmediatelyAfterMigration() {
        this.deleteDataImmediatelyAfterMigration = true;
        this.deleteAllDataAtEndOfMigration = false;
    }

    public void deleteAllDataAtEndOfMigration() {
        this.deleteAllDataAtEndOfMigration = true;
        this.deleteDataImmediatelyAfterMigration = false;
    }

    public void preserveData() {
        this.deleteAllDataAtEndOfMigration = false;
        this.deleteDataImmediatelyAfterMigration = false;
    }

    public long estimate() throws Exception {
        this.estimation = 0;
        if (runRawDataMigration) {
            retryOnFailure(new RawDataMigrator(), Task.Estimate);
        }

        if (run1HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.ONE_HOUR), Task.Estimate);
        }

        if (run6HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.SIX_HOUR), Task.Estimate);
        }

        if (run1DAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.TWENTY_FOUR_HOUR), Task.Estimate);
        }

        if (deleteAllDataAtEndOfMigration) {
            retryOnFailure(new DeleteAllData(), Task.Estimate);
        }

        estimation = (long) (estimation + estimation * UNDER_ESTIMATION_FACTOR);

        return estimation;
    }

    public void migrateData() throws Exception {
        if (runRawDataMigration) {
            retryOnFailure(new RawDataMigrator(), Task.Migrate);
        }

        if (run1HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.ONE_HOUR), Task.Migrate);
        }

        if (run6HAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.SIX_HOUR), Task.Migrate);
        }

        if (run1DAggregateDataMigration) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.TWENTY_FOUR_HOUR), Task.Migrate);
        }

        if (deleteAllDataAtEndOfMigration) {
            retryOnFailure(new DeleteAllData(), Task.Migrate);
        }
    }

    public void deleteOldData() throws Exception {
        if (deleteAllDataAtEndOfMigration) {
            retryOnFailure(new DeleteAllData(), Task.Migrate);
        }
    }

    /**
     * Retries the migration {@link #MAX_NUMBER_OF_FAILURES} times before
     * failing the migration operation.
     *
     * @param migrator
     * @throws Exception
     */
    private Thread retryOnFailure(final CallableMigrationWorker migrator, final Task task)
        throws Exception {

        RunnableWithException runnable = new RunnableWithException() {
            private Exception exception;

            @Override
            public void run() {
                int numberOfFailures = 0;
                Exception caughtException = null;

                log.info(migrator.getClass());

                while (numberOfFailures < MAX_NUMBER_OF_FAILURES) {
                    try {
                        if (task == Task.Estimate) {
                            estimation += migrator.estimate();
                        } else {
                            migrator.migrate();
                        }
                        return;
                    } catch (Exception e) {
                        log.error("Migrator " + migrator.getClass() + " failed. Retrying!", e);

                        caughtException = e;
                        numberOfFailures++;
                    }
                }

                this.exception = caughtException;
            }

            @Override
            public Exception getException() {
                return this.exception;
            }
        };

        Thread localThread = new Thread(runnable);
        localThread.start();
        localThread.join();

        if (runnable.getException() != null) {
            throw runnable.getException();
        }

        return localThread;
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

    private ExistingDataSource getExistingDataSource(EntityManager entityManager, String query, Task task) {

        if (Task.Migrate.equals(task)) {
            if (this.databaseType == DatabaseType.Oracle) {
                return new ScrollableDataSource(entityManager, query);
            } else {
                if (!experimentalDataSource) {
                    return new ScrollableDataSource(entityManager, query);
                } else {
                    return new ExistingPostgresDataBulkExportSource(entityManager, query);
                }
            }
        } else if (Task.Estimate.equals(task)) {
            int limit = MAX_RECORDS_TO_LOAD_FROM_SQL * (NUMBER_OF_BATCHES_FOR_ESTIMATION + 1);

            if (this.databaseType == DatabaseType.Oracle) {
                return new ScrollableDataSource(entityManager, query, limit);
            } else {
                if (!experimentalDataSource) {
                    return new ScrollableDataSource(entityManager, query, limit);
                } else {
                    return new ExistingPostgresDataBulkExportSource(entityManager, query, limit);
                }
            }
        }

        return new ScrollableDataSource(entityManager, query);
    }

    private enum Task {
        Migrate, Estimate
    }

    private class Telemetry {
        private StopWatch generalTimer;
        private StopWatch migrationTimer;

        public Telemetry() {
            this.generalTimer = new StopWatch();
            this.migrationTimer = new StopWatch();
        }

        public StopWatch getGeneralTimer() {
            return generalTimer;
        }

        public StopWatch getMigrationTimer() {
            return migrationTimer;
        }

        public long getMigrationTime() {
            return migrationTimer.getTime();
        }

        public long getGeneralTime() {
            return generalTimer.getTime();
        }

        public long getNonMigrationTime() {
            return this.getGeneralTime() - this.getMigrationTime();
        }
    }

    private interface CallableMigrationWorker {


        long estimate() throws Exception;

        void migrate() throws Exception;
    }

    private interface RunnableWithException extends Runnable {
        Exception getException();
    }

    private class AggregateDataMigrator implements CallableMigrationWorker {

        private final String selectQuery;
        private final String deleteQuery;
        private final String countQuery;
        private final MetricsTable metricsTable;

        /**
         * @param query
         * @param metricsTable
         */
        public AggregateDataMigrator(MetricsTable metricsTable) throws Exception {
            this.metricsTable = metricsTable;

            if (MetricsTable.ONE_HOUR.equals(this.metricsTable)) {
                this.selectQuery = MigrationQuery.SELECT_1H_DATA.toString();
                this.deleteQuery = MigrationQuery.DELETE_1H_DATA.toString();
                this.countQuery = MigrationQuery.COUNT_1H_DATA.toString();
            } else if (MetricsTable.SIX_HOUR.equals(this.metricsTable)) {
                this.selectQuery = MigrationQuery.SELECT_6H_DATA.toString();
                this.deleteQuery = MigrationQuery.DELETE_6H_DATA.toString();
                this.countQuery = MigrationQuery.COUNT_6H_DATA.toString();
            } else if (MetricsTable.TWENTY_FOUR_HOUR.equals(this.metricsTable)) {
                this.selectQuery = MigrationQuery.SELECT_1D_DATA.toString();
                this.deleteQuery = MigrationQuery.DELETE_1D_DATA.toString();
                this.countQuery = MigrationQuery.COUNT_1D_DATA.toString();
            } else {
                throw new Exception("MetricsTable " + metricsTable.toString() + " not supported by this migrator.");
            }
        }

        @Override
        public long estimate() throws Exception {
            long recordCount = this.getRowCount(this.countQuery);

            Telemetry telemetry = this.performMigration(Task.Estimate);
            long estimatedTimeToMigrate = telemetry.getMigrationTime();

            long estimation = (recordCount / (long) MAX_RECORDS_TO_LOAD_FROM_SQL / (long) NUMBER_OF_BATCHES_FOR_ESTIMATION)
                * estimatedTimeToMigrate;

            estimation += telemetry.getNonMigrationTime();

            return estimation;
        }

        public void migrate() throws Exception {
            performMigration(Task.Migrate);
            if (deleteDataImmediatelyAfterMigration) {
                deleteTableData();
            }
        }

        private long getRowCount(String countQuery) {
            StatelessSession session = ((org.hibernate.Session) entityManager.getDelegate())
                .getSessionFactory().openStatelessSession();

            org.hibernate.Query query = session.createSQLQuery(countQuery);
            query.setReadOnly(true);
            query.setTimeout(SQL_TIMEOUT);

            return Long.parseLong(query.uniqueResult().toString());
        }

        private void deleteTableData() throws Exception {
            int failureCount = 0;
            while (failureCount < MAX_NUMBER_OF_FAILURES) {
                try {
                    entityManager.getTransaction().begin();
                    Query nativeQuery = entityManager.createNativeQuery(this.deleteQuery);
                    nativeQuery.executeUpdate();
                    entityManager.getTransaction().commit();
                    log.info("- " + metricsTable.toString() + " - Cleaned -");
                } catch (Exception e) {
                    log.error("Failed to delete " + metricsTable.toString()
                        + " data. Attempting to delete data one more time...");

                    failureCount++;
                    if (failureCount == MAX_NUMBER_OF_FAILURES) {
                        throw e;
                    }
                }
            }
        }

        private Telemetry performMigration(Task task) throws Exception {
            Telemetry telemetry = new Telemetry();
            telemetry.getGeneralTimer().start();

            long numberOfBatchesMigrated = 0;

            List<Object[]> existingData;
            int failureCount;

            int lastMigratedRecord = 0;
            ExistingDataSource dataSource = getExistingDataSource(entityManager, selectQuery, task);
            dataSource.initialize();

            telemetry.getMigrationTimer().start();
            while (true) {
                existingData = dataSource.getData(lastMigratedRecord, MAX_RECORDS_TO_LOAD_FROM_SQL);

                if (existingData.size() == 0) {
                    break;
                }

                lastMigratedRecord += existingData.size();

                failureCount = 0;
                while (failureCount < MAX_NUMBER_OF_FAILURES) {
                    try {
                        insertDataToCassandra(existingData);
                        break;
                    } catch (Exception e) {
                        log.error("Failed to insert " + metricsTable.toString()
                            + " data. Attempting to insert the current batch of data one more time");
                        log.error(e);

                        failureCount++;
                        if (failureCount == MAX_NUMBER_OF_FAILURES) {
                            throw e;
                        }
                    }
                }

                log.info("- " + metricsTable + " - " + lastMigratedRecord + " -");

                numberOfBatchesMigrated++;
                if (Task.Estimate.equals(task) && numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                    break;
                }
            }
            telemetry.getMigrationTimer().stop();

            dataSource.close();
            telemetry.getGeneralTimer().stop();

            return telemetry;
        }

        private void insertDataToCassandra(List<Object[]> existingData)
            throws Exception {
            List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();
            Batch batch = QueryBuilder.batch();
            int batchSize = 0;

            //only need approximate TTL to speed up processing
            //given that each batch is processed within seconds, getting the
            //system time once per batch has minimal impact on the record retention
            long creationTimeMillis;
            long itemTTLSeconds;
            long currentTimeMillis = System.currentTimeMillis();
            long expectedTTLMillis = metricsTable.getTTLinMilliseconds();


            for (Object[] rawMeasurement : existingData) {
                creationTimeMillis = Long.parseLong(rawMeasurement[MigrationQuery.TIMESTAMP_INDEX].toString());
                itemTTLSeconds = (expectedTTLMillis - currentTimeMillis + creationTimeMillis) / 1000l;

                if(itemTTLSeconds > 0 ){
                    batch.add(QueryBuilder.insertInto(metricsTable.toString())
                        .value("schedule_id",Integer.parseInt(rawMeasurement[MigrationQuery.SCHEDULE_INDEX].toString()))
                        .value("time", new Date(creationTimeMillis))
                        .value("type", AggregateType.AVG.ordinal())
                        .value("value", Double.parseDouble(rawMeasurement[MigrationQuery.VALUE_INDEX].toString()))
                        .using(ttl((int) itemTTLSeconds)));

                    batch.add(QueryBuilder.insertInto(metricsTable.toString())
                        .value("schedule_id", Integer.parseInt(rawMeasurement[MigrationQuery.SCHEDULE_INDEX].toString()))
                        .value("time", new Date(creationTimeMillis))
                        .value("type", AggregateType.MIN.ordinal())
                        .value("value", Double.parseDouble(rawMeasurement[MigrationQuery.MIN_VALUE_INDEX].toString()))
                        .using(ttl((int) itemTTLSeconds)));

                    batch.add(QueryBuilder.insertInto(metricsTable.toString())
                        .value("schedule_id", Integer.parseInt(rawMeasurement[MigrationQuery.SCHEDULE_INDEX].toString()))
                        .value("time", new Date(creationTimeMillis))
                        .value("type", AggregateType.MAX.ordinal())
                        .value("value", Double.parseDouble(rawMeasurement[MigrationQuery.MAX_VALUE_INDEX].toString()))
                        .using(ttl((int) itemTTLSeconds)));

                    batchSize += 3;
                }

                if (batchSize >= MAX_AGGREGATE_BATCH_TO_CASSANDRA) {
                    resultSetFutures.add(session.executeAsync(batch));
                    batch = QueryBuilder.batch();
                    batchSize = 0;
                }
            }

            if (batchSize != 0) {
                resultSetFutures.add(session.executeAsync(batch));
            }

            for (ResultSetFuture future : resultSetFutures) {
                future.get();
            }
        }
    }


    private class RawDataMigrator implements CallableMigrationWorker {

        Queue<String> tablesNotProcessed = new LinkedList<String>(Arrays.asList(getRawDataTables()));

        public long estimate() throws Exception {
            long recordCount = 0;
            for (String table : getRawDataTables()) {
                String countQuery = String.format(MigrationQuery.COUNT_RAW.toString(), table);
                recordCount += this.getRowCount(countQuery);
            }

            Telemetry telemetry = this.performMigration(Task.Estimate);
            long estimatedTimeToMigrate = telemetry.getMigrationTime();
            long estimation = (recordCount / (long) MAX_RECORDS_TO_LOAD_FROM_SQL / (long) NUMBER_OF_BATCHES_FOR_ESTIMATION)
                * estimatedTimeToMigrate;
            estimation += telemetry.getNonMigrationTime();

            return estimation;
        }

        public void migrate() throws Exception {
            performMigration(Task.Migrate);
        }

        private long getRowCount(String countQuery) {
            StatelessSession session = ((org.hibernate.Session) entityManager.getDelegate()).getSessionFactory()
                .openStatelessSession();

            org.hibernate.Query query = session.createSQLQuery(countQuery);
            query.setReadOnly(true);
            query.setTimeout(SQL_TIMEOUT);

            return Long.parseLong(query.uniqueResult().toString());
        }

        private Telemetry performMigration(Task task) throws Exception {
            Telemetry telemetry = new Telemetry();
            telemetry.getGeneralTimer().start();

            long numberOfBatchesMigrated = 0;

            List<Object[]> existingData;
            int failureCount;

            telemetry.getMigrationTimer().start();
            telemetry.getMigrationTimer().suspend();

            while (!tablesNotProcessed.isEmpty()) {
                String table = tablesNotProcessed.peek();

                String selectQuery = String.format(MigrationQuery.SELECT_RAW_DATA.toString(), table);

                ExistingDataSource dataSource = getExistingDataSource(entityManager, selectQuery, task);
                dataSource.initialize();

                log.info("Start migrating raw table: " + table);

                telemetry.getMigrationTimer().resume();
                int lastMigratedRecord = 0;
                while (true) {
                    existingData = dataSource.getData(lastMigratedRecord, MAX_RECORDS_TO_LOAD_FROM_SQL);

                    if (existingData.size() == 0) {
                        break;
                    }

                    lastMigratedRecord += existingData.size();

                    failureCount = 0;
                    while (failureCount < MAX_NUMBER_OF_FAILURES) {
                        try {
                            insertDataToCassandra(existingData);
                            break;
                        } catch (Exception e) {
                            log.error("Failed to insert " + MetricsTable.RAW.toString()
                                + " data. Attempting to insert the current batch of data one more time");
                            log.error(e);


                            failureCount++;
                            if (failureCount == MAX_AGGREGATE_BATCH_TO_CASSANDRA) {
                                throw e;
                            }
                        }
                    }

                    log.info("- " + table + " - " + lastMigratedRecord + " -");

                    numberOfBatchesMigrated++;
                    if (Task.Estimate.equals(task) && numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                        break;
                    }
                }
                telemetry.getMigrationTimer().suspend();

                if (Task.Migrate.equals(task)) {
                    log.info("Done migrating raw table" + table + "---------------------");

                    if (deleteDataImmediatelyAfterMigration) {
                        deleteTableData(table);
                    }
                } else if (numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                    break;
                }

                dataSource.close();
                tablesNotProcessed.poll();
            }

            telemetry.getGeneralTimer().stop();
            return telemetry;
        }

        private void deleteTableData(String table) throws Exception {
            String deleteQuery = String.format(MigrationQuery.DELETE_RAW_ENTRY.toString(), table);
            int failureCount = 0;
            while (failureCount < MAX_NUMBER_OF_FAILURES) {
                try {
                    entityManager.getTransaction().begin();
                    Query nativeQuery = entityManager.createNativeQuery(deleteQuery);
                    nativeQuery.executeUpdate();
                    entityManager.getTransaction().commit();
                    log.info("- " + table + " - Cleaned -");
                } catch (Exception e) {
                    log.error("Failed to delete " + table + " data. Attempting to delete data one more time...");

                    failureCount++;
                    if (failureCount == MAX_NUMBER_OF_FAILURES) {
                        throw e;
                    }
                }
            }
        }

        private void insertDataToCassandra(List<Object[]> existingData) throws Exception {
            List<ResultSetFuture> resultSetFutures = new ArrayList<ResultSetFuture>();
            Batch batch = QueryBuilder.batch();
            int batchSize = 0;

            //only need approximate TTL to speed up processing
            //given that each batch is processed within seconds, getting the
            //system time once per batch has minimal impact on the record retention
            long creationTimeMillis;
            long itemTTLSeconds;
            long currentTimeMillis = System.currentTimeMillis();
            long expectedTTLMillis = MetricsTable.RAW.getTTLinMilliseconds();


            for (Object[] rawDataPoint : existingData) {
                creationTimeMillis = Long.parseLong(rawDataPoint[MigrationQuery.TIMESTAMP_INDEX].toString());
                itemTTLSeconds = (expectedTTLMillis - currentTimeMillis + creationTimeMillis) / 1000l;

                if (itemTTLSeconds > 0) {
                    batch.add(QueryBuilder.insertInto(MetricsTable.RAW.toString())
                        .value("schedule_id", Integer.parseInt(rawDataPoint[MigrationQuery.SCHEDULE_INDEX].toString()))
                        .value("time", new Date(creationTimeMillis))
                        .value("value", Double.parseDouble(rawDataPoint[MigrationQuery.VALUE_INDEX].toString()))
                        .using(ttl((int) itemTTLSeconds)));
                    batchSize++;
                }

                if (batchSize >= MAX_RAW_BATCH_TO_CASSANDRA) {
                    resultSetFutures.add(session.executeAsync(batch));
                    batch = QueryBuilder.batch();
                    batchSize = 0;
                }
            }

            if (batchSize != 0) {
                resultSetFutures.add(session.executeAsync(batch));
            }

            for (ResultSetFuture future : resultSetFutures) {
                future.get();
            }
        }
    }


    private class DeleteAllData implements CallableMigrationWorker {

        public void migrate() {
            Query nativeQuery;

            if (run1HAggregateDataMigration) {
                entityManager.getTransaction().begin();
                nativeQuery = entityManager.createNativeQuery(MigrationQuery.DELETE_1H_DATA.toString());
                nativeQuery.executeUpdate();
                entityManager.getTransaction().commit();
                log.info("- RHQ_MEASUREMENT_DATA_NUM_1H - Cleaned -");
            }

            if (run6HAggregateDataMigration) {
                entityManager.getTransaction().begin();
                nativeQuery = entityManager.createNativeQuery(MigrationQuery.DELETE_6H_DATA.toString());
                nativeQuery.executeUpdate();
                entityManager.getTransaction().commit();
                log.info("- RHQ_MEASUREMENT_DATA_NUM_6H - Cleaned -");
            }

            if (run1DAggregateDataMigration) {
                entityManager.getTransaction().begin();
                nativeQuery = entityManager.createNativeQuery(MigrationQuery.DELETE_1D_DATA.toString());
                nativeQuery.executeUpdate();
                entityManager.getTransaction().commit();
                log.info("- RHQ_MEASUREMENT_DATA_NUM_1D - Cleaned -");
            }

            if (runRawDataMigration) {
                for (String table : getRawDataTables()) {
                    entityManager.getTransaction().begin();
                    String deleteAllData = String.format(MigrationQuery.DELETE_RAW_ALL_DATA.toString(), table);
                    nativeQuery = entityManager.createNativeQuery(deleteAllData);
                    nativeQuery.executeUpdate();
                    entityManager.getTransaction().commit();
                    log.info("- " + table + " - Cleaned -");
                }
            }
        }

        @Override
        public long estimate() throws Exception {
            return 300000; // return return 5 minutes for now without any database side checks.
        }
    }
}


