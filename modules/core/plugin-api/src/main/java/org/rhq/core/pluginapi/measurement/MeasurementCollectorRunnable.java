package org.rhq.core.pluginapi.measurement;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * Provides assistance for resource components whose measurement reports take a
 * long time to complete. Although each measurement can be configured to only be
 * polled infrequently, in the case of a complex calculation (database query,
 * hourly reports, etc.) that returns multiple values, this collector ensures
 * they are not collected in separate calls which may put unnecessary load on
 * the measured system. This class also effectively creates a lower bound on the
 * frequency of these calculations.
 *
 * Typically, measurement reports are fast (a few seconds). However, the plugin
 * container is blocked waiting for a plugin's resource component to return
 * measurements from calls to
 * {@link MeasurementFacet#getValues(MeasurementReport, Set)()}. Report data is
 * stored and periodically polled instead from a separate thread.
 *
 * @see AvailabilityCollectorRunnable for a similar implementation
 *
 * @author Elias Ross
 */
public class MeasurementCollectorRunnable implements Runnable {
    private static final Log log = LogFactory.getLog(MeasurementCollectorRunnable.class);

    /**
     * The minimum interval allowed between collections, in milliseconds.
     */
    public static final long MIN_INTERVAL = 60000L;

    /**
     * The thread pool to give this runnable a thread to run in when it needs to collect measurements.
     */
    private final ScheduledExecutorService threadPool;

    /**
     * If <code>true</code>, this collector runnable should be actively collection measurements.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Data collection future.
     */
    private Future<?> task = new FutureTask<Void>(this, null);

    /**
     * The classloader to be used when collecting measurements.
     */
    private final ClassLoader contextClassloader;

    /**
     * The object that is used to collect measurements from the managed resource.
     */
    private final MeasurementFacet measured;

    /**
     * The time, in milliseconds, that this collector will pause in between collections.
     */
    private final long interval;

    /**
     * The time, in milliseconds, that this collector wait before performing the first, initial collection.
     * Once this initial delay expires, and the initial collection is performed, the {@link #interval} period
     * must then expire before the next collection is performed.
     */
    private final long initialDelay;

    /**
     * Holds a reference to the last measurements obtained from the measurement facet.
     * This reference is reset when new measurements are obtained.
     */
    private AtomicReference<MeasurementReport> cachedReportHolder = new AtomicReference<MeasurementReport>(new MeasurementReport());

    /**
     * Requested metrics and the date they were last requested.
     * Eventually entries in this are expired.
     */
    private Map<MeasurementScheduleRequest, Date> requestedMetrics = new ConcurrentHashMap<MeasurementScheduleRequest, Date>();

    /**
     * Just a cache of the facet toString used in log messages. We don't want to keep calling toString on the
     * facet for fear we might get some odd blocking or exceptions thrown. So we call it once and cache it here.
     */
    private final String facetId;

    /**
     * Creates a collector instance that will perform measurement reporting for a particular managed resource.
     *
     * The interval is the time, in milliseconds, this collector will wait between reports.
     * A typically value should be something around 30 minutes, but its minimum allowed value is 60 seconds.
     *
     * @param measured the object that is used to periodically check the managed resource (must not be <code>null</code>)
     * @param interval the initial delay, in millis, before the first collection is performed.
     * @param interval the interval, in millis, between measurement collections
     * @param contextClassloader the context classloader that will be used when collection measurements
     * @param threadPool the thread pool to be used to submit this runnable when it needs to start
     */
    public MeasurementCollectorRunnable(MeasurementFacet measured, long initialDelay, long interval,
            ClassLoader contextClassloader, ScheduledExecutorService threadPool) {

        if (measured == null) {
            throw new IllegalArgumentException("measurement facet is null");
        }

        if (threadPool == null) {
            throw new IllegalArgumentException("threadPool is null");
        }

        if (interval < MIN_INTERVAL) {
            log.info("Interval is too short [" + interval + "] - setting to minimum of [" + MIN_INTERVAL + "]");
            interval = MIN_INTERVAL;
        }

        if (contextClassloader == null) {
            contextClassloader = Thread.currentThread().getContextClassLoader();
        }

        this.measured = measured;
        this.contextClassloader = contextClassloader;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.threadPool = threadPool;
        this.facetId = measured.toString();
    }

    /**
     * Adds the last measured report data to the passed in report.
     * This will not perform measurements for the managed resource
     * For those resource components using this measurement collector utility,
     * their {@link MeasurementFacet#getValues()} method should simply be calling this method.
     */
    public void getLastValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest metric : metrics)
            requestedMetrics.put(metric, new Date());
        // For all metrics being requested, take their cached values last collected and transfer them to the given report.
        // Note that we only remove the metrics that were being requested, leaving any cached data intact so they can
        // be retrieved later when they are requested.
        MeasurementReport cachedReport = cachedReportHolder.get();
        report.add(cachedReport, metrics, true);
    }

    /**
     * For those resource components using this measurement collector utility,
     * their {@link ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)} method must call this
     * to start the measurement checking that this object performs.
     */
    public void start() {
        boolean isStarted = started.getAndSet(true);
        if (!isStarted) {
            task.cancel(true);
            task = threadPool.scheduleWithFixedDelay(this, initialDelay, interval, TimeUnit.MILLISECONDS);
            log.debug("measurement collector started: " + facetId);
        }
    }

    /**
     * For those resource components using this measurement collector utility,
     * their {@link ResourceComponent#stop()} method must call this
     * to stop the measurement checking that this object performs.
     */
    public void stop() {
        started.set(false);
        task.cancel(true);
        cachedReportHolder.set(new MeasurementReport());
        requestedMetrics.clear();

        log.debug("measurement collector stopped: " + facetId);
    }

    /**
     * Performs the actual measurements. This is the method that is invoked
     * after this runnable is {@link #start() submitted to the thread pool}.
     * You should not be calling this method directly - use {@link #start()} instead.
     */
    public void run() {
        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug("measurement collector is collecting now: " + facetId);
        }

        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(contextClassloader);

        try {
            expireRequested();
            // collect the new data for all metrics previous requested in the past
            MeasurementReport cachedReport = new MeasurementReport();
            if (requestedMetrics.isEmpty()) {
                log.debug("no cached values");
            } else {
                measured.getValues(cachedReport, requestedMetrics.keySet());
            }
            if (debug) {
                log.debug("measurement collector latest data: " + cachedReport);
            }
            cachedReportHolder.set(cachedReport);
        } catch (Exception e) {
            log.warn("measurement collector failed to get values", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }
    }

    private void expireRequested() {
        long now = System.currentTimeMillis();
        for (Iterator<Entry<MeasurementScheduleRequest, Date>> i = requestedMetrics.entrySet().iterator(); i.hasNext(); ) {
            Entry<MeasurementScheduleRequest, Date> me = i.next();
            long until = me.getValue().getTime() + me.getKey().getInterval() * 2;
            if (now > until) {
                log.debug("no longer requesting measurement " + me);
                i.remove();
            }
        }
    }
}
