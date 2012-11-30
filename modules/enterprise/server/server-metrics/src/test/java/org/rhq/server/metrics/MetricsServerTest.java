/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.rhq.server.metrics.MetricsDAO.METRICS_INDEX_TABLE;
import static org.rhq.server.metrics.MetricsDAO.ONE_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.RAW_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.SIX_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.TWENTY_FOUR_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsServer.RAW_TTL;
import static org.rhq.server.metrics.MetricsServer.divide;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

import me.prettyprint.hector.api.Keyspace;

/**
 * @author John Sanda
 */
public class MetricsServerTest extends CassandraIntegrationTest {

    private static final boolean ENABLED = false;

    private final Log log = LogFactory.getLog(MetricsServerTest.class);

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final String RAW_METRIC_DATA_CF = "raw_metrics";

    private final String ONE_HOUR_METRIC_DATA_CF = "one_hour_metrics";

    private final String SIX_HOUR_METRIC_DATA_CF = "six_hour_metrics";

    private final String TWENTY_FOUR_HOUR_METRIC_DATA_CF = "twenty_four_hour_metrics";

    private final String METRICS_INDEX = "metrics_index";

    private final String TRAITS_CF = "traits";

    private final String RESOURCE_TRAITS_CF = "resource_traits";

    private MetricsServerStub metricsServer;

    private Keyspace keyspace;

    private MetricsDAO dao;

    private static class MetricsServerStub extends MetricsServer {
        private DateTime currentHour;

        public void setCurrentHour(DateTime currentHour) {
            this.currentHour = currentHour;
        }

        @Override
        protected DateTime getCurrentHour() {
            if (currentHour == null) {
                return super.getCurrentHour();
            }
            return currentHour;
        }
    }

    @BeforeMethod
    public void initServer() throws Exception {
        metricsServer = new MetricsServerStub();
        metricsServer.setSession(session);

        dao = new MetricsDAO(session);

        purgeDB();
    }

    private void purgeDB() throws Exception {
        session.execute("TRUNCATE " + RAW_METRICS_TABLE);
        session.execute("TRUNCATE " + ONE_HOUR_METRICS_TABLE);
        session.execute("TRUNCATE " + SIX_HOUR_METRICS_TABLE);
        session.execute("TRUNCATE " + TWENTY_FOUR_HOUR_METRICS_TABLE);
        session.execute("TRUNCATE " + METRICS_INDEX_TABLE);
    }

    @Test//(enabled = ENABLED)
    public void insertMultipleRawNumericDataForOneSchedule() throws Exception {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        long timestamp = System.currentTimeMillis();
        metricsServer.addNumericData(data);

        List<RawNumericMetric> actual = dao.findRawMetrics(scheduleId, hour0.plusHours(4), hour0.plusHours(5));
        List<RawNumericMetric> expected = asList(
            new RawNumericMetric(scheduleId, threeMinutesAgo.getMillis(), 3.2),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9),
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6)
        );

        assertEquals(actual, expected, "Failed to retrieve raw metric data");
        assertColumnMetadataEquals(scheduleId, hour0.plusHours(4), hour0.plusHours(5), RAW_TTL, timestamp);

        List<MetricsIndexEntry> expectedIndex = asList(new MetricsIndexEntry(ONE_HOUR_METRIC_DATA_CF,
            hour0.plusHours(4), scheduleId));
            assertMetricsIndexEquals(ONE_HOUR_METRIC_DATA_CF, expectedIndex, "Failed to update index for " +
                ONE_HOUR_METRIC_DATA_CF);
    }

    @Test//(enabled = ENABLED)
    public void calculateAggregatesForOneScheduleWhenDBIsEmpty() {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour6 = hour0.plusHours(6);
        DateTime lastHour = hour6.minusHours(1);
        DateTime firstMetricTime = hour6.minusMinutes(3);
        DateTime secondMetricTime = hour6.minusMinutes(2);
        DateTime thirdMetricTime = hour6.minusMinutes(1);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(firstMetricTime.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(secondMetricTime.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(thirdMetricTime.getMillis(), scheduleId, 2.6));

        metricsServer.setCurrentHour(hour6);
        metricsServer.addNumericData(data);
        metricsServer.calculateAggregates();

        // verify that one hour metric data is updated
        List<AggregatedNumericMetric> expected = asList(new AggregatedNumericMetric(scheduleId,
            divide((3.9 + 3.2 + 2.6), 3), 2.6, 3.9, lastHour.getMillis()));
        assert1HourDataEquals(scheduleId, expected);

        // verify that 6 hour metric data is updated
        assert6HourDataEquals(scheduleId, asList(new AggregatedNumericMetric(scheduleId, divide((3.9 + 3.2 + 2.6), 3),
            2.6, 3.9, hour0.getMillis())));

        // TODO verify that 24 hour data is *not* updated
        // TODO verify metrics index for 24 hour data is updated
    }

    @Test//(enabled = ENABLED)
    public void aggregateRawDataDuring9thHour() {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour9 = hour0.plusHours(9);
        DateTime hour8 = hour9.minusHours(1);

        DateTime firstMetricTime = hour8.plusMinutes(5);
        DateTime secondMetricTime = hour8.plusMinutes(10);
        DateTime thirdMetricTime = hour8.plusMinutes(15);

        double firstValue = 1.1;
        double secondValue = 2.2;
        double thirdValue = 3.3;

        Set<MeasurementDataNumeric> rawMetrics = new HashSet<MeasurementDataNumeric>();
        rawMetrics.add(new MeasurementDataNumeric(firstMetricTime.getMillis(), scheduleId, firstValue));
        rawMetrics.add(new MeasurementDataNumeric(secondMetricTime.getMillis(), scheduleId, secondValue));
        rawMetrics.add(new MeasurementDataNumeric(thirdMetricTime.getMillis(), scheduleId, thirdValue));

        Set<MeasurementDataNumeric> insertedRawMetrics = dao.insertRawMetrics(rawMetrics, RAW_TTL);
        metricsServer.updateMetricsIndex(insertedRawMetrics);

        metricsServer.setCurrentHour(hour9);
        metricsServer.calculateAggregates();

        // verify that the 1 hour aggregates are calculated
        assert1HourDataEquals(scheduleId, asList(new AggregatedNumericMetric(scheduleId, divide((1.1 + 2.2 + 3.3), 3),
            firstValue, thirdValue, hour8.getMillis())));

        // verify that the 6 hour index is updated
        DateTimeService dateTimeService = new DateTimeService();
        List<MetricsIndexEntry> expected6HourIndex = asList(new MetricsIndexEntry(SIX_HOUR_METRICS_TABLE,
            dateTimeService.getTimeSlice(hour9, Minutes.minutes(60 * 6)), scheduleId));

        assertMetricsIndexEquals(SIX_HOUR_METRICS_TABLE, expected6HourIndex, "Failed to update index for " +
            SIX_HOUR_METRICS_TABLE);

        // The 6 hour data should not get aggregated since the current 6 hour time slice
        // has not passed yet. More specifically, the aggregation job is running at 09:00
        // which means that the current 6 hour slice is from 06:00 to 12:00.
        assert6HourDataEmpty(scheduleId);

        // verify that the 24 hour index is empty
        assert24HourMetricsIndexEmpty(scheduleId);

        // verify that the 1 hour queue has been purged
        assert1HourMetricsIndexEmpty(scheduleId);
    }

    @Test//(enabled = ENABLED)
    public void aggregate1HourDataDuring12thHour() {
        // set up the test fixture
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour7 = hour0.plusHours(7);
        DateTime hour8 = hour0.plusHours(8);

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 9.9;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert one hour data to be aggregated
        List<AggregatedNumericMetric> oneHourMetrics = asList(
            new AggregatedNumericMetric(scheduleId, avg1, min1, max1, hour7.getMillis()),
            new AggregatedNumericMetric(scheduleId, avg2, min2, max2, hour8.getMillis())
        );
        dao.insertAggregates(ONE_HOUR_METRICS_TABLE, oneHourMetrics, DateTimeService.TWO_WEEKS);

        // update the 6 hour queue
        Map<Integer, DateTime> indexUpdates = new HashMap<Integer, DateTime>();
        indexUpdates.put(scheduleId, hour6);
        dao.updateMetricsIndex(SIX_HOUR_METRICS_TABLE, indexUpdates);

        // execute the system under test
        metricsServer.setCurrentHour(hour12);
        metricsServer.calculateAggregates();

        // verify the results
        // verify that the one hour data has been aggregated
        assert6HourDataEquals(scheduleId, asList(new AggregatedNumericMetric(scheduleId, divide((avg1 + avg2), 2), min1,
            max1, hour6.getMillis())));

        // verify that the 6 hour queue has been updated
        assert6HourMetricsIndexEmpty(scheduleId);

        // verify that the 24 hour queue is updated
        assertMetricsIndexEquals(TWENTY_FOUR_HOUR_METRICS_TABLE, asList(new MetricsIndexEntry(
            TWENTY_FOUR_HOUR_METRICS_TABLE, hour0, scheduleId)), "Failed to update index for " +
            TWENTY_FOUR_HOUR_METRICS_TABLE);

        // verify that 6 hour data is not rolled up into the 24 hour bucket
        assert24HourDataEmpty(scheduleId);
    }

    @Test//(enabled = ENABLED)
    public void aggregate6HourDataDuring24thHour() {
        // set up the test fixture
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour24 = hour0.plusHours(24);

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 3.3;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert 6 hour data to be aggregated
        List<AggregatedNumericMetric> sixHourMetrics = asList(
            new AggregatedNumericMetric(scheduleId, avg1, min1, max1, hour6.getMillis()),
            new AggregatedNumericMetric(scheduleId, avg2, min2, max2, hour12.getMillis())
        );
        dao.insertAggregates(SIX_HOUR_METRICS_TABLE, sixHourMetrics, DateTimeService.ONE_MONTH);

        // update the 24 queue
        Map<Integer, DateTime> indexUpdates = new HashMap<Integer, DateTime>();
        indexUpdates.put(scheduleId, hour0);
        dao.updateMetricsIndex(TWENTY_FOUR_HOUR_METRICS_TABLE, indexUpdates);

        // execute the system under test
        metricsServer.setCurrentHour(hour24);
        metricsServer.calculateAggregates();

        // verify the results
        // verify that the 6 hour data is aggregated
        assert24HourDataEquals(scheduleId, asList(new AggregatedNumericMetric(scheduleId, divide(avg1 + avg2, 2),
            min1, max2, hour0.getMillis())));

        // verify that the 24 hour queue is updated
        assert24HourMetricsIndexEmpty(scheduleId);
    }

    @Test//(enabled = ENABLED)
    public void findRawDataComposites() {
        DateTime beginTime = now().minusHours(4);
        DateTime endTime = now();
        Buckets buckets = new Buckets(beginTime, endTime);
        int scheduleId = 123;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId, 1.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId, 2.2));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId, 3.3));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId, 4.4));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId, 5.5));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId, 6.6));

        // add some data outside the range
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId, 1.23));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId,
            4.56));

        metricsServer.addNumericData(data);
        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForResource(scheduleId,
            beginTime.getMillis(), endTime.getMillis());

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(1.1 + 2.2 + 3.3, 3), 3.3, 1.1);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(4.4 + 5.5 + 6.6, 3), 6.6, 4.4);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test//(enabled = ENABLED)
    public void find1HourDataComposites() {
        DateTime beginTime = now().minusDays(11);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduleId, 2.0, 1.0, 3.0, bucket0Time.getMillis()),
            new AggregatedNumericMetric(scheduleId, 5.0, 4.0, 6.0, bucket0Time.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduleId, 3.0, 3.0, 3.0, bucket0Time.plusHours(2).getMillis()),
            new AggregatedNumericMetric(scheduleId, 5.0, 2.0, 9.0, bucket59Time.getMillis()),
            new AggregatedNumericMetric(scheduleId, 5.0, 4.0, 6.0, bucket59Time.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduleId, 3.0, 3.0, 3.0, bucket59Time.plusHours(2).getMillis())
        );
        dao.insertAggregates(ONE_HOUR_METRICS_TABLE, metrics, DateTimeService.TWO_WEEKS);

        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForResource(scheduleId,
            beginTime.getMillis(), endTime.getMillis());

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(2.0 + 5.0 + 3.0, 3), 5.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(5.0 + 5.0 + 3.0, 3), 5.0, 3.0);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test//(enabled = ENABLED)
    public void find6HourDataComposites() {
        DateTime beginTime = now().minusDays(20);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduleId, 2.0, 1.0, 3.0, bucket0Time.getMillis()),
            new AggregatedNumericMetric(scheduleId, 5.0, 4.0, 6.0, bucket0Time.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduleId, 3.0, 3.0, 3.0, bucket0Time.plusHours(2).getMillis()),
            new AggregatedNumericMetric(scheduleId, 5.0, 2.0, 9.0, bucket59Time.getMillis()),
            new AggregatedNumericMetric(scheduleId, 5.0, 4.0, 6.0, bucket59Time.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduleId, 3.0, 3.0, 3.0, bucket59Time.plusHours(2).getMillis())
        );
        dao.insertAggregates(SIX_HOUR_METRICS_TABLE, metrics, DateTimeService.ONE_MONTH);

        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForResource(scheduleId,
            beginTime.getMillis(), endTime.getMillis());

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(2.0 + 5.0 + 3.0, 3), 5.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(5.0 + 5.0 + 3.0, 3), 5.0, 3.0);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    private void assertColumnMetadataEquals(int scheduleId, DateTime startTime, DateTime endTime, Integer ttl,
        long timestamp) {
        List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, startTime, endTime, true);
        for (RawNumericMetric metric : metrics) {
            assertEquals(metric.getColumnMetadata().getTtl(), ttl, "The TTL does not match the expected value for " +
                metric);
            assertTrue(metric.getColumnMetadata().getWriteTime() >= timestamp, "The column timestamp for " + metric +
                " should be >= " + timestamp + " but it is " + metric.getColumnMetadata().getWriteTime());
        }
    }

    private void assertMetricsIndexEquals(String columnFamily, List<MetricsIndexEntry> expected, String msg) {
        List<MetricsIndexEntry> actual = dao.findMetricsIndexEntries(columnFamily);
        assertCollectionMatchesNoOrder("Failed to retrieve raw metric data", expected, actual, msg + ": " +
            columnFamily + " index not match expected values.");
    }

    private void assert1HourDataEquals(int scheduleId, List<AggregatedNumericMetric> expected) {
        assertMetricDataEquals(ONE_HOUR_METRICS_TABLE, scheduleId, expected);
    }

    private void assert6HourDataEquals(int scheduleId, List<AggregatedNumericMetric> expected) {
        assertMetricDataEquals(SIX_HOUR_METRICS_TABLE, scheduleId, expected);
    }

    private void assert24HourDataEquals(int scheduleId, List<AggregatedNumericMetric> expected) {
        assertMetricDataEquals(TWENTY_FOUR_HOUR_METRICS_TABLE, scheduleId, expected);
    }

    private void assertMetricDataEquals(String columnFamily, int scheduleId, List<AggregatedNumericMetric> expected) {
        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(columnFamily, scheduleId);
        assertCollectionMatchesNoOrder(expected, actual, "Metric data for schedule id " + scheduleId +
            " in table " + columnFamily + " does not match expected values");
    }

    private void assert6HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, SIX_HOUR_METRICS_TABLE);
    }

    private void assert24HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, TWENTY_FOUR_HOUR_METRIC_DATA_CF);
    }

    private void assertMetricDataEmpty(int scheduleId, String columnFamily) {
        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(columnFamily, scheduleId);
        assertEquals(metrics.size(), 0, "Expected " + columnFamily + " to be empty for schedule id " + scheduleId +
            " but found " + metrics);
    }

    private void assert1HourMetricsIndexEmpty(int scheduleId) {
        assertMetricsIndexEmpty(scheduleId, ONE_HOUR_METRICS_TABLE);
    }

    private void assert6HourMetricsIndexEmpty(int scheduleId) {
        assertMetricsIndexEmpty(scheduleId, SIX_HOUR_METRICS_TABLE);
    }

    private void assert24HourMetricsIndexEmpty(int scheduleId) {
        assertMetricsIndexEmpty(scheduleId, TWENTY_FOUR_HOUR_METRICS_TABLE);
    }

    private void assertMetricsIndexEmpty(int scheduleId, String table) {
        List<MetricsIndexEntry> index = dao.findMetricsIndexEntries(table);
        assertEquals(index.size(), 0, "Expected metrics index for " + table + " to be empty but found " + index);
    }

}
