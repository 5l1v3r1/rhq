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
package org.rhq.enterprise.server.alert.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionAvailabilityCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionBaselineCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionChangesCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionControlCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionEventCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionScheduleCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionTraitCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerLocal;
import org.rhq.enterprise.server.alert.engine.mbean.AlertConditionCacheMonitor;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityCacheElement;
import org.rhq.enterprise.server.alert.engine.model.EventCacheElement;
import org.rhq.enterprise.server.alert.engine.model.InvalidCacheElementException;
import org.rhq.enterprise.server.alert.engine.model.MeasurementBaselineCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementNumericCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementTraitCacheElement;
import org.rhq.enterprise.server.alert.engine.model.NumericDoubleCacheElement;
import org.rhq.enterprise.server.alert.engine.model.OutOfBoundsCacheElement;
import org.rhq.enterprise.server.alert.engine.model.ResourceOperationCacheElement;
import org.rhq.enterprise.server.alert.engine.model.UnsupportedAlertConditionOperatorException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The singleton that contains the actual caches for the alert subsystem.
 * 
 * @author Joseph Marques
 * @author John Mazzittelli
 */
public final class AlertConditionCache {
    private static final Log log = LogFactory.getLog(AlertConditionCache.class);

    /**
     * the batch size of conditions into the cache at a time
     */
    final int PAGE_SIZE = 250;

    /**
     * The actual cache - this is a singleton.
     */
    private static final AlertConditionCache instance = new AlertConditionCache();

    /**
     * For concurrency control into the cache.
     */
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private enum CacheName {
        MeasurementDataCache, //
        MeasurementTraitCache, //
        MeasurementOutOfBoundsCache, //
        ResourceOperationCache, //
        AvailabilityCache, //
        Inverse;
    }

    /*
     * Instead of coming up with an overly complicated uber-map that can hold everything in the world, separate the
     * cache out so that cache management and access is simplified.  This allows for each cache to potentially take on a
     * different structure, a shape that would be most appropriate to the subsystem of information it is supposed to be
     * modeling and caching.
     *
     * TODO: jmarques - think about resourceGroup-level alert processing; not touching this topic right now
     */

    /*
     * structure: 
     *      map< measurementScheduleId, list < NumericDoubleCacheElement > >    (MeasurementDataNumeric cache)
     *      map< measurementScheduleId, list < MeasurementTraitCacheElement > > (MeasurementDataTrait   cache)
     *      map< measurementScheduleId, list < OutOfBoundsCacheElement > >      (MeasurementOutOfBounds cache)
     *
     * algorithm: 
     *      Use the measurementScheduleId to find a list of cached conditions for measurement numerics.
     */
    private Map<Integer, List<NumericDoubleCacheElement>> measurementDataCache;
    private Map<Integer, List<MeasurementTraitCacheElement>> measurementTraitCache;
    private Map<Integer, List<OutOfBoundsCacheElement>> measurementOutOfBoundsCache;

    /*
     * structure: 
     *      map< resourceId, map< operationDefinitionId, list< ResourceOperationCacheElement > > >
     *
     * algorithm: 
     *      Finding matching conditions are simple because a ResourceOperationHistory element already contains
     *      references back to its OperationDefinition element as well as the Resource it was executed against.
     *
     *      Using these two pieces of information, it's possible to lookup the list of states that should cause a
     *      firing.
     */
    private Map<Integer, Map<Integer, List<ResourceOperationCacheElement>>> resourceOperationCache;

    /*
     * structure: 
     *      map< resourceId, list< AvailabilityCacheElement > > >
     *
     * algorithm: 
     *      When an Availability object comes across the line, use the Resource object associated/attached with it
     *      to get the list of cache elements representing conditions created against this resource's availability.
     */
    private Map<Integer, List<AvailabilityCacheElement>> availabilityCache;

    /*
     * structure:
     *      map< resourceId, list< EventCacheElement > >
     * 
     * algorithm:
     *      When an EventReport comes across the line, use the Resource object it is coming from to get the list
     *      of cache elements representing conditions created against that resource's list of incoming Events  
     */
    private Map<Integer, List<EventCacheElement>> eventsCache;

    /*
     * structure: 
     *      map< alertConditionId, list< tuple< AbstractCacheElement, list< AbstractCacheElement > > > >
     *
     * algorithm: 
     *      Updating an AlertDefinition is an expensive operation.  If the definition is deleted or disabled, all of
     *      the corresponding cache elements that were constructed against any of the definition's nested conditions
     *      now need to be removed.  This would normally require iterating over the entirety of the cache.  This may
     *      not seem so bad for an individual update, but let's pretend that the user wants to disable all alerts on
     *      a particular resource (perhaps for a maintenance window) or, worse, disable all alerts across several
     *      resources.  Now we have to iterate over the ENTIRE cache multiple times, once for each AlertDefinition
     *      updated.  This is unacceptable.
     *
     *      To remedy this, we keep an inverse map to mark the locations where particular AlertCondition ids have been
     *      used to create cache elements.  This way, when an AlertCondition is updated, we can go to this map to learn
     *      which cache elements were created from this condition as well as their precise location(s) across all
     *      internal cache structures.
     *
     * note: 
     *      A list of tuples was used as the type for the map's value objects because a nested Map was insufficient.
     *      The cache for measurement data uses multiple lists, and that particular implementation actually references
     *      the same NumericDoubleCacheElement in these multiple lists.  Thus, a map would have been insufficient
     *      because it would not have been able to represent both lists that the cache element was a member of.
     *      A list of tuples can do this.
     *
     *      Furthermore, a map, even if it could have represented the data properly, wouldn't have been the most
     *      appropriate structure since the value objects in this structure need to be iterated over in full.  In
     *      other words, when this map is used to access bookkeeping information about a particular alert condition id,
     *      the entire list of corresponding bookkeeping entries needs to be visited; so there would be no benefit to
     *      using the nested map anyway.
     */
    private Map<Integer, List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>> inverseAlertConditionMap;
    private Map<Integer, List<Integer>> inverseAgentConditionMap;
    private Map<Integer, List<Integer>> inverseAgentOutOfBoundsMap;

    /*
     * The SLBSs used by the cache.
     */
    private AlertConditionManagerLocal alertConditionManager;
    private MeasurementBaselineManagerLocal measurementBaselineManager;
    private MeasurementDataManagerLocal measurementDataManager;
    private SubjectManagerLocal subjectManager;
    private CachedConditionProducerLocal cachedConditionProducer;
    private EntityManagerFacadeLocal entityManagerFacade;

    private AlertConditionCache() {
        measurementDataCache = new HashMap<Integer, List<NumericDoubleCacheElement>>();
        measurementTraitCache = new HashMap<Integer, List<MeasurementTraitCacheElement>>();
        measurementOutOfBoundsCache = new HashMap<Integer, List<OutOfBoundsCacheElement>>();
        resourceOperationCache = new HashMap<Integer, Map<Integer, List<ResourceOperationCacheElement>>>();
        availabilityCache = new HashMap<Integer, List<AvailabilityCacheElement>>();
        eventsCache = new HashMap<Integer, List<EventCacheElement>>();

        // bookkeeping constructs for fast agent-by-agent removal of cache elements
        inverseAlertConditionMap = new HashMap<Integer, List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>>();
        inverseAgentConditionMap = new HashMap<Integer, List<Integer>>();
        inverseAgentOutOfBoundsMap = new HashMap<Integer, List<Integer>>();

        alertConditionManager = LookupUtil.getAlertConditionManager();
        measurementBaselineManager = LookupUtil.getMeasurementBaselineManager();
        measurementDataManager = LookupUtil.getMeasurementDataManager();
        subjectManager = LookupUtil.getSubjectManager();
        cachedConditionProducer = LookupUtil.getCachedConditionProducerLocal();
        entityManagerFacade = LookupUtil.getEntityManagerFacade();
    }

    public static AlertConditionCache getInstance() {
        return instance;
    }

    public void reloadCachesForAgent(int agentId) {
        rwLock.writeLock().lock();

        try {
            AlertConditionCacheStats unloadStats = unloadCachesForAgent(agentId);
            AlertConditionCacheStats reloadStats = loadCachesForAgent(agentId);
            log.debug("UnloadStats for agent[id=" + agentId + "]: " + unloadStats);
            log.debug("ReloadStats for agent[id=" + agentId + "]: " + reloadStats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private AlertConditionCacheStats unloadCachesForAgent(int agentId) {
        AlertConditionCacheStats stats = new AlertConditionCacheStats();

        // first unload the conditions
        List<Integer> agentConditions = inverseAgentConditionMap.get(agentId);

        if (agentConditions == null) {
            log.debug("Found no alert condition elements to remove for agent[id=" + agentId + "]");
        } else {
            log.debug("Found " + agentConditions.size() + " alert conditions to remove for agent[id=" + agentId + "]");
            log.trace("Conditions were: " + agentConditions);

            for (Integer alertConditionId : agentConditions) {
                removeAlertCondition(alertConditionId, stats);
            }

            // reverse mappings are no longer needed once we unload all of the elements
            agentConditions.clear();
        }

        // then unload the baselines
        List<Integer> agentOutOfBounds = inverseAgentOutOfBoundsMap.get(agentId);

        if (agentOutOfBounds == null) {
            log.debug("Found no OOB elements to remove for agent[id=" + agentId + "]");
        } else {
            log.debug("Found " + agentOutOfBounds.size() + " OOB elements to remove for agent[id=" + agentId + "]");
            log.trace("Baselines were: " + agentOutOfBounds);

            for (Integer scheduleId : agentOutOfBounds) {
                removeOutOfBoundsCondition(scheduleId, stats);
            }

            // reverse mappings are no longer needed once we unload all of the elements
            agentOutOfBounds.clear();
        }

        return stats;
    }

    /**
     * This method is used to do the initial loading from the database for a particular agent. In the high availability
     * infrastructure each server instance in the cloud will only be responsible for monitoring a select number of 
     * agents at any given point in time. When an agent makes a connection to this server instance, the caches for that
     * agent will be loaded at that time. 
     *
     * @return the number of conditions that re/loaded
     */
    private AlertConditionCacheStats loadCachesForAgent(int agentId) {
        rwLock.writeLock().lock();

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            log.debug("Loading Alert Condition Caches for agent[id=" + agentId + "]...");

            Subject overlord = subjectManager.getOverlord();

            EnumSet<AlertConditionCategory> supportedCategories = EnumSet.of(AlertConditionCategory.BASELINE,
                AlertConditionCategory.CHANGE, AlertConditionCategory.TRAIT, AlertConditionCategory.AVAILABILITY,
                AlertConditionCategory.CONTROL, AlertConditionCategory.THRESHOLD, AlertConditionCategory.EVENT);

            for (AlertConditionCategory nextCategory : supportedCategories) {
                // page thru all alert definitions
                int rowsProcessed = 0;
                PageControl pc = new PageControl();
                pc.setPageNumber(0);
                pc.setPageSize(PAGE_SIZE); // condition composites are small so we can grab alot; use the setter, constructor limits this to 100

                while (true) {
                    PageList<? extends AbstractAlertConditionCategoryComposite> alertConditions = null;
                    alertConditions = alertConditionManager.getAlertConditionComposites(overlord, nextCategory, pc);

                    if (alertConditions.isEmpty()) {
                        break; // didn't get any rows back, must not have any data or no more rows left to process
                    }

                    for (AbstractAlertConditionCategoryComposite nextComposite : alertConditions) {
                        insertAlertConditionComposite(agentId, nextComposite, stats);
                    }

                    entityManagerFacade.flush();
                    entityManagerFacade.clear();

                    rowsProcessed += alertConditions.size();
                    if (rowsProcessed >= alertConditions.getTotalSize()) {
                        break; // we've processed all data, we can stop now
                    }

                    pc.setPageNumber(pc.getPageNumber() + 1);
                }
                log.debug("Loaded " + rowsProcessed + " Alert Condition Composites of type '" + nextCategory + "'");
            }

            // page thru all dynamic baselines
            int rowsProcessed = 0;
            PageControl pc = new PageControl();
            pc.setPageNumber(0);
            pc.setPageSize(PAGE_SIZE); // baseline composites are small so we can grab alot; use the setter, constructor limits this to 100

            while (true) {
                PageList<MeasurementBaselineComposite> baselines = measurementBaselineManager
                    .getAllDynamicMeasurementBaselines(agentId, overlord, pc);

                if (baselines.isEmpty()) {
                    break; // didn't get any rows back, must not have any data or no more rows left to process
                }

                for (MeasurementBaselineComposite baseline : baselines) {
                    // don't insert borked baselines: only when min/max is correctly non-null, non-NAN, non-INF
                    if (isInvalidDouble(baseline.getMin()) || isInvalidDouble(baseline.getMax())) {
                        continue; // process the next baseline
                    }

                    // safe auto-unboxing here, because min/max both guaranteed to be valid by this point
                    insertOutOfBoundsBaseline(agentId, baseline.getId(), baseline.getMin(), baseline.getMax(), baseline
                        .getScheduleId(), stats);
                }

                entityManagerFacade.flush();
                entityManagerFacade.clear();

                rowsProcessed += baselines.size();
                if (rowsProcessed >= baselines.getTotalSize()) {
                    break; // we've processed all data, we can stop now
                }

                pc.setPageNumber(pc.getPageNumber() + 1);
            }

            log.debug("Loaded " + rowsProcessed + " Measurement Baseline Composites (for OOBs)");

            log.debug("Loaded Alert Condition Caches for agent[id=" + agentId + "]");
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error loading cache for agent[id=" + agentId + "]", t);
        } finally {
            rwLock.writeLock().unlock();
        }
        return stats;
    }

    /**
     * This will empty all internal caches. Be very careful calling this method as it will make all caches out of sync
     * from the database data. You'll probably want to call {@link #loadCachesForAgent()} after making a call to this method.
     */
    public void clearCaches() {
        rwLock.writeLock().lock();

        try {
            measurementDataCache.clear();
            measurementTraitCache.clear();
            resourceOperationCache.clear();
            availabilityCache.clear();
            inverseAlertConditionMap.clear();
            AlertConditionCacheMonitor.getMBean().resetAllCacheElementCounts();
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        if ((measurementData == null) || (measurementData.length == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.readLock().lock();

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            for (MeasurementData datum : measurementData) {
                int scheduleId = datum.getScheduleId();

                if (datum instanceof MeasurementDataNumeric) {
                    // check two caches, one for measurement-based alert condition, the other strictly for OOBs
                    List<? extends NumericDoubleCacheElement> conditionCacheElements = lookupMeasurementDataCacheElements(scheduleId);
                    List<OutOfBoundsCacheElement> outOfBoundsCacheElements = lookupOutOfBoundsCacheElements(scheduleId);

                    Double providedValue = ((MeasurementDataNumeric) datum).getValue();

                    processCacheElements(conditionCacheElements, providedValue, datum.getTimestamp(), stats);
                    processCacheElements(outOfBoundsCacheElements, providedValue, datum.getTimestamp(), stats);
                } else if (datum instanceof MeasurementDataTrait) {
                    List<MeasurementTraitCacheElement> cacheElements = lookupMeasurementTraitCacheElements(scheduleId);

                    processCacheElements(cacheElements, ((MeasurementDataTrait) datum).getValue(),
                        datum.getTimestamp(), stats);
                } else {
                    log.error(getClass().getSimpleName() + " does not support " + "checking conditions against "
                        + datum.getClass().getSimpleName() + " types");
                }
            }

            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementMeasurementProcessingTime(stats.getAge());
            log.debug("Check Measurements[size=" + measurementData.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.readLock().unlock();
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        rwLock.readLock().lock();

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            if (operationHistory instanceof ResourceOperationHistory) {
                ResourceOperationHistory resourceOperationHistory = (ResourceOperationHistory) operationHistory;

                Resource resource = resourceOperationHistory.getResource();
                OperationDefinition operationDefinition = resourceOperationHistory.getOperationDefinition();
                OperationRequestStatus operationStatus = resourceOperationHistory.getStatus();

                List<ResourceOperationCacheElement> cacheElements = lookupResourceOperationHistoryCacheElements(
                    resource.getId(), operationDefinition.getId());

                processCacheElements(cacheElements, operationStatus, resourceOperationHistory.getModifiedTime(), stats);
            } else {
                log.debug(getClass().getSimpleName() + " does not support checking conditions against "
                    + operationHistory.getClass().getSimpleName() + " types");
            }

            AlertConditionCacheMonitor.getMBean().incrementOperationCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementOperationProcessingTime(stats.getAge());
            log.debug("Check OperationHistory[size=1] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.readLock().unlock();
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(EventSource source, Event... events) {
        if ((events == null) || (events.length == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.readLock().lock();

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            Resource resource = source.getResource();
            List<EventCacheElement> cacheElements = lookupEventCacheElements(resource.getId());

            for (Event event : events) {
                processCacheElements(cacheElements, event.getSeverity(), event.getTimestamp(), stats, event.getDetail());
            }

            AlertConditionCacheMonitor.getMBean().incrementEventCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementEventProcessingTime(stats.getAge());
            log.debug("Check Events[size=" + events.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.readLock().unlock();
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        if ((availabilities == null) || (availabilities.length == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.readLock().lock();

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            for (Availability availability : availabilities) {
                Resource resource = availability.getResource();
                AvailabilityType availabilityType = availability.getAvailabilityType();

                List<AvailabilityCacheElement> cacheElements = lookupAvailabilityCacheElements(resource.getId());

                processCacheElements(cacheElements, availabilityType, availability.getStartTime().getTime(), stats);
            }

            AlertConditionCacheMonitor.getMBean().incrementAvailabilityCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementAvailabilityProcessingTime(stats.getAge());
            log.debug("Check Availability[size=" + availabilities.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.readLock().unlock();
        }
        return stats;
    }

    private <T extends AbstractCacheElement<S>, S> void processCacheElements(List<T> cacheElements, S providedValue,
        long timestamp, AlertConditionCacheStats stats, Object... extraParams) {
        if (cacheElements == null) {
            return; // nothing to do
        }

        int errors = 0;

        for (T cacheElement : cacheElements) {
            boolean matched = cacheElement.process(providedValue, extraParams);

            if (matched) // send positive event in case of a match
            {
                try {
                    if (OutOfBoundsCacheElement.class.isInstance(cacheElement)) {
                        // don't bother with active property for OutOfBoundsCacheElement types
                        double diff = ((Double) providedValue).doubleValue()
                            - ((Double) cacheElement.getAlertConditionValue()).doubleValue();
                        cachedConditionProducer.sendOutOfBoundsConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), Double.valueOf(diff), timestamp);

                    } else {
                        /*
                         * Set the active property for alertCondition-based cache elements, and send it on its way;
                         * Thus, even if the element is already active, we're going to send another message with the new
                         * value
                         */
                        cacheElement.setActive(true); // no harm to always set active (though, technically, STATELESS operators don't need it)
                        cachedConditionProducer.sendActivateAlertConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), timestamp, providedValue, extraParams);
                    }

                    stats.matched++;
                } catch (Exception e) {
                    log.error("Error processing matched cache element '" + cacheElement + "': " + e.getMessage());
                    errors++;
                }
            } else // no match, negative event
            {
                /*
                 * but only send negative events if we're, 1) a type of operator that supports STATEFUL events, and
                 * 2) currently active
                 */
                if (cacheElement.isType(AlertConditionOperator.Type.STATEFUL) && cacheElement.isActive()) {
                    cacheElement.setActive(false);

                    try {
                        // send negative message
                        cachedConditionProducer.sendDeactivateAlertConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), timestamp);
                    } catch (Exception e) {
                        log.error("Error sending deactivation message for cache element '" + cacheElement + "': "
                            + e.getMessage());
                        errors++;
                    }
                } else {
                    /*
                     * negative message, but nothing was active...so do nothing.
                     *
                     * this will occur in the overwhelming majority of cases.  in theory, since most of the time
                     * conditions exist to alert people of non-ideal system state, it will not fire in the POSITIVE very
                     * often.  thus, we suppress the firing of negative events unless we know we've already sent a
                     * POSITIVE event that we need to compensate for.
                     */
                }
            }
        }

        if (errors != 0) {
            log.error("There were " + errors + " alert conditions that did not fire. "
                + "Please check the configuration of the JMS subsystem and try again. ");
        }
    }

    private boolean addToOutOfBoundsCache(Integer scheduleId, OutOfBoundsCacheElement outOfBoundsCacheElement,
        Integer agentId, AlertConditionCacheStats stats) {
        List<OutOfBoundsCacheElement> outOfBoundsCacheElements = measurementOutOfBoundsCache.get(scheduleId);

        if (outOfBoundsCacheElements == null) {
            outOfBoundsCacheElements = new ArrayList<OutOfBoundsCacheElement>();
            measurementOutOfBoundsCache.put(scheduleId, outOfBoundsCacheElements);
        }

        if (log.isTraceEnabled()) {
            log.trace("Inserted '" + OutOfBoundsCacheElement.class.getSimpleName() + "' element: " + "key="
                + scheduleId + ", " + "value=" + outOfBoundsCacheElement);
        }

        // make sure to add bookkeeping information to the inverse cache too
        addToInverseAlertOutOfBoundsMap(scheduleId, agentId);

        // and finally update stats and return whether it was success
        boolean success = outOfBoundsCacheElements.add(outOfBoundsCacheElement);
        if (success) {
            stats.created++;
        }

        return success;
    }

    private <T extends AbstractCacheElement<?>> boolean addTo(String mapName, Map<Integer, List<T>> cache, Integer key,
        T cacheElement, int alertConditionId, Integer agentId, AlertConditionCacheStats stats) {
        List<T> cacheElements = cache.get(key);

        if (cacheElements == null) {
            cacheElements = new ArrayList<T>();
            cache.put(key, cacheElements);
        }

        if (log.isTraceEnabled()) {
            log.trace("Inserted '" + mapName + "' element: " + "key=" + key + ", " + "value=" + cacheElement);
        }

        // make sure to add bookkeeping information to the inverse cache too
        addToInverseAlertConditionMap(alertConditionId, agentId, cacheElement, cacheElements);

        // and finally update stats and return whether it was success
        boolean success = cacheElements.add(cacheElement);
        if (success) {
            stats.created++;
        }

        return success;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractCacheElement<?>> boolean addToInverseAlertConditionMap(Integer alertConditionId,
        Integer agentId, T inverseCacheElement, List<T> inverseList) {
        List<Integer> agentConditions = inverseAgentConditionMap.get(agentId);

        if (agentConditions == null) {
            agentConditions = new ArrayList<Integer>();
            inverseAgentConditionMap.put(agentId, agentConditions);
            log.trace("Adding new inverseAgentConditionMap for agent[id=" + agentId + "]");
        }

        agentConditions.add(alertConditionId);
        log.trace("Adding entry for inverseAgentConditionMap for agent[id=" + agentId + "]: alertConditionId="
            + alertConditionId);

        List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>> tuples = inverseAlertConditionMap
            .get(alertConditionId);

        if (tuples == null) {
            tuples = new ArrayList<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>();
            inverseAlertConditionMap.put(alertConditionId, tuples);
        }

        if (log.isDebugEnabled()) {
            log.trace("Inserted inverse agent element: " + "key=" + agentId + ", " + "value=" + alertConditionId);
            log.trace("Inserted inverse condition element: " + "key=" + alertConditionId + ", " + "value="
                + inverseCacheElement);
        }

        Tuple inverseTuple = new Tuple<T, List<T>>(inverseCacheElement, inverseList);
        return tuples.add(inverseTuple);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractCacheElement<?>> boolean addToInverseAlertOutOfBoundsMap(Integer scheduleId,
        Integer agentId) {
        List<Integer> agentOutOfBounds = inverseAgentOutOfBoundsMap.get(agentId);

        if (agentOutOfBounds == null) {
            agentOutOfBounds = new ArrayList<Integer>();
            inverseAgentOutOfBoundsMap.put(agentId, agentOutOfBounds);
            log.trace("Adding new inverseAgentOutOfBoundsMap for agent[id=" + agentId + "]");
        }

        log
            .trace("Adding entry for inverseAgentOutOfBoundsMap for agent[id=" + agentId + "]: scheduleId="
                + scheduleId);
        return agentOutOfBounds.add(scheduleId);
    }

    private boolean addToResourceOperationCache(Integer resourceId, Integer operationDefinitionId,
        ResourceOperationCacheElement cacheElement, Integer alertConditionId, Integer agentId,
        AlertConditionCacheStats stats) {
        // resourceOperationCache = new HashMap< Integer, Map< Integer, List < ResourceOperationCacheElement > > >();
        Map<Integer, List<ResourceOperationCacheElement>> operationDefinitionMap = resourceOperationCache
            .get(resourceId);

        if (operationDefinitionMap == null) {
            operationDefinitionMap = new HashMap<Integer, List<ResourceOperationCacheElement>>();
            resourceOperationCache.put(resourceId, operationDefinitionMap);
        }

        return addTo("operationDefinitionMap", operationDefinitionMap, operationDefinitionId, cacheElement,
            alertConditionId, agentId, stats);
    }

    // TODO: jmarques - update model to replace comparator strings with AlertConditionOperator enum
    @Deprecated
    private AlertConditionOperator getAlertConditionOperator(AlertConditionCategory category, String comparator,
        String conditionOption) {
        if (category == AlertConditionCategory.CONTROL) {
            // the UI currently only supports one operator for control
            return AlertConditionOperator.EQUALS;
        }

        if (category == AlertConditionCategory.EVENT) {
            // the UI currently only supports one operator for events
            return AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO;
        }

        if ((category == AlertConditionCategory.CHANGE) || (category == AlertConditionCategory.TRAIT)) {
            // the model currently supports CHANGE as a category type instead of a comparator
            return AlertConditionOperator.CHANGES;
        }

        if (category == AlertConditionCategory.AVAILABILITY) {
            AvailabilityType conditionOptionType = AvailabilityType.valueOf(conditionOption.toUpperCase());
            if (conditionOptionType == AvailabilityType.DOWN) {
                /*
                 * UI phrases this as "Goes DOWN", but we're going to store the cache element as CHANGES_FROM:UP
                 *
                 * This way, it'll work when the agent's goes suspect and null is persisted for AvailabilityType
                 */
                return AlertConditionOperator.CHANGES_FROM;
            } else if (conditionOptionType == AvailabilityType.UP) {
                /*
                 * UI phrases this as "Goes UP", but we're going to store the cache element as CHANGES_TO:UP
                 *
                 * This way, it'll work when the agent's comes back from being suspect, where it had null for its
                 * AvailabilityType
                 */
                return AlertConditionOperator.CHANGES_TO;
            } else {
                throw new UnsupportedAlertConditionOperatorException("Invalid alertCondition for AVAILABILITY category");
            }
        }

        if (comparator.equals("<")) {
            return AlertConditionOperator.LESS_THAN;
        } else if (comparator.equals(">")) {
            return AlertConditionOperator.GREATER_THAN;
        } else if (comparator.equals("=")) {
            return AlertConditionOperator.EQUALS;
        } else {
            throw new UnsupportedAlertConditionOperatorException("Comparator '" + comparator + "' "
                + "is not supported for ArtifactConditionCategory." + category.name());
        }
    }

    private List<OutOfBoundsCacheElement> lookupOutOfBoundsCacheElements(int scheduleId) {
        return measurementOutOfBoundsCache.get(scheduleId); // yup, might be null
    }

    private List<? extends NumericDoubleCacheElement> lookupMeasurementDataCacheElements(int scheduleId) {
        return measurementDataCache.get(scheduleId); // yup, might be null
    }

    private List<MeasurementTraitCacheElement> lookupMeasurementTraitCacheElements(int scheduleId) {
        return measurementTraitCache.get(scheduleId); // yup, might be null
    }

    private List<ResourceOperationCacheElement> lookupResourceOperationHistoryCacheElements(int resourceId,
        int definitionId) {
        Map<Integer, List<ResourceOperationCacheElement>> operationDefinitionMap = resourceOperationCache
            .get(resourceId);

        if (operationDefinitionMap == null) {
            return null;
        }

        return operationDefinitionMap.get(definitionId); // yup, might be null
    }

    private List<AvailabilityCacheElement> lookupAvailabilityCacheElements(int resourceId) {
        return availabilityCache.get(resourceId); // yup, might be null
    }

    private List<EventCacheElement> lookupEventCacheElements(int resourceId) {
        return eventsCache.get(resourceId); // yup, might be null
    }

    private void insertAlertConditionComposite(int agentId, AbstractAlertConditionCategoryComposite composite,
        AlertConditionCacheStats stats) {

        AlertCondition alertCondition = composite.getCondition();
        int alertConditionId = alertCondition.getId(); // auto-unboxing is safe here because as the PK it's guaranteed to be non-null

        AlertConditionCategory alertConditionCategory = alertCondition.getCategory();
        AlertConditionOperator alertConditionOperator = getAlertConditionOperator(alertConditionCategory,
            alertCondition.getComparator(), alertCondition.getOption());

        if (alertConditionCategory == AlertConditionCategory.BASELINE) {
            AlertConditionBaselineCategoryComposite baselineComposite = (AlertConditionBaselineCategoryComposite) composite;
            // option status for baseline gets set to "mean", but it's rather useless since the UI
            // current doesn't allow alerting off of other baseline properties such as "min" and "max"
            Double threshold = alertCondition.getThreshold();
            String optionStatus = alertCondition.getOption();

            /* 
             * yes, calculatedValue may be null, but that's OK because the match 
             * method for MeasurementBaselineCacheElement handles nulls just fine
             */
            Double calculatedValue = getCalculatedBaselineValue(alertConditionId, baselineComposite, optionStatus,
                threshold);

            try {
                MeasurementBaselineCacheElement cacheElement = new MeasurementBaselineCacheElement(
                    alertConditionOperator, calculatedValue, alertConditionId, optionStatus);

                // auto-boxing (of alertConditionId) is always safe
                addTo("measurementDataCache", measurementDataCache, baselineComposite.getScheduleId(), cacheElement,
                    alertConditionId, agentId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumericDoubleCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, calculatedValue));
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(2);
        } else if (alertConditionCategory == AlertConditionCategory.CHANGE) {
            AlertConditionChangesCategoryComposite changesComposite = (AlertConditionChangesCategoryComposite) composite;
            int scheduleId = changesComposite.getScheduleId();

            MeasurementDataNumeric numeric = measurementDataManager.getCurrentNumericForSchedule(scheduleId);

            try {
                MeasurementNumericCacheElement cacheElement = new MeasurementNumericCacheElement(
                    alertConditionOperator, (numeric == null) ? null : numeric.getValue(), alertConditionId);

                addTo("measurementDataCache", measurementDataCache, scheduleId, cacheElement, alertConditionId,
                    agentId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumericDoubleCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, numeric));
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(1);
        } else if (alertConditionCategory == AlertConditionCategory.TRAIT) {
            AlertConditionTraitCategoryComposite traitsComposite = (AlertConditionTraitCategoryComposite) composite;
            String value = traitsComposite.getValue();

            try {
                /* 
                 * don't forget special defensive handling to allow for null trait calculation;
                 * this might happen if a newly committed resource has some alert template applied to
                 * it for some trait that it has not yet gotten from the agent
                 */
                MeasurementTraitCacheElement cacheElement = new MeasurementTraitCacheElement(alertConditionOperator,
                    value, alertConditionId);

                addTo("measurementTraitCache", measurementTraitCache, traitsComposite.getScheduleId(), cacheElement,
                    alertConditionId, agentId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create StringCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, value));
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(1);
        } else if (alertConditionCategory == AlertConditionCategory.AVAILABILITY) {
            /*
             * This is a hack, because we're not respecting the persist alertCondition option, we're instead overriding
             * it with AvailabilityType.UP to satisfy the desired semantics.
             *
             * TODO: jmarques - should associate a specific operator with availability UI selections to make biz
             * processing more consistent with the model
             */
            AlertConditionAvailabilityCategoryComposite availabilityComposite = (AlertConditionAvailabilityCategoryComposite) composite;

            try {
                AvailabilityCacheElement cacheElement = new AvailabilityCacheElement(alertConditionOperator,
                    AvailabilityType.UP, availabilityComposite.getAvailabilityType(), alertConditionId);
                addTo("availabilityCache", availabilityCache, availabilityComposite.getResourceId(), cacheElement,
                    alertConditionId, agentId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create AvailabilityCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, availabilityComposite
                        .getAvailabilityType(), AvailabilityType.UP));
            }
            AlertConditionCacheMonitor.getMBean().incrementAvailabilityCacheElementCount(1);
        } else if (alertConditionCategory == AlertConditionCategory.CONTROL) {
            AlertConditionControlCategoryComposite controlComposite = (AlertConditionControlCategoryComposite) composite;
            String option = alertCondition.getOption();
            OperationRequestStatus operationRequestStatus = OperationRequestStatus.valueOf(option.toUpperCase());

            try {
                ResourceOperationCacheElement cacheElement = new ResourceOperationCacheElement(alertConditionOperator,
                    operationRequestStatus, alertConditionId);

                // auto-boxing always safe
                addToResourceOperationCache(controlComposite.getResourceId(), controlComposite
                    .getOperationDefinitionId(), cacheElement, alertConditionId, agentId, stats);
            } catch (InvalidCacheElementException icee) {
                log
                    .info("Failed to create ResourceOperationCacheElement with parameters: "
                        + getCacheElementErrorString(alertConditionId, alertConditionOperator, null,
                            operationRequestStatus));
            }
            AlertConditionCacheMonitor.getMBean().incrementOperationCacheElementCount(1);
        } else if (alertConditionCategory == AlertConditionCategory.THRESHOLD) {
            AlertConditionScheduleCategoryComposite thresholdComposite = (AlertConditionScheduleCategoryComposite) composite;
            Double thresholdValue = alertCondition.getThreshold();

            MeasurementNumericCacheElement cacheElement = null;
            try {
                cacheElement = new MeasurementNumericCacheElement(alertConditionOperator, thresholdValue,
                    alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumberDoubleCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, thresholdValue));
            }

            if (cacheElement != null) {
                addTo("measurementDataCache", measurementDataCache, thresholdComposite.getScheduleId(), cacheElement,
                    alertConditionId, agentId, stats);

            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(1);
        } else if (alertConditionCategory == AlertConditionCategory.EVENT) {
            AlertConditionEventCategoryComposite eventComposite = (AlertConditionEventCategoryComposite) composite;
            EventSeverity eventSeverity = EventSeverity.valueOf(alertCondition.getName());
            String eventDetails = alertCondition.getOption();

            EventCacheElement cacheElement = null;
            try {
                if (eventDetails == null) {
                    cacheElement = new EventCacheElement(alertConditionOperator, eventSeverity, alertConditionId);
                } else {
                    cacheElement = new EventCacheElement(alertConditionOperator, eventDetails, eventSeverity,
                        alertConditionId);
                }
            } catch (InvalidCacheElementException icee) {
                log
                    .info("Failed to create EventCacheElement with parameters: "
                        + getCacheElementErrorString(alertConditionId, alertConditionOperator, eventDetails,
                            eventSeverity));
            }

            addTo("eventsCache", eventsCache, eventComposite.getResourceId(), cacheElement, alertConditionId, agentId,
                stats);
            AlertConditionCacheMonitor.getMBean().incrementEventCacheElementCount(1);
        }
    }

    private Double getCalculatedBaselineValue(int conditionId, AlertConditionBaselineCategoryComposite composite,
        String optionStatus, Double threshold) {
        int baselineId = composite.getBaselineId();

        if (isInvalidDouble(threshold)) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: threshold was null");
        }

        // auto-unboxing of threshold is safe here 
        double percentage = threshold / 100.0;
        Double baselineValue = 0.0;

        if (optionStatus == null) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: optionStatus string was null");
        } else if (optionStatus.equals("min")) {
            baselineValue = composite.getMinValue();
        } else if (optionStatus.equals("mean")) {
            baselineValue = composite.getMeanValue();
        } else if (optionStatus.equals("max")) {
            baselineValue = composite.getMaxValue();
        } else {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: unrecognized optionStatus string of '" + optionStatus + "'");
            return null;
        }

        if (isInvalidDouble(baselineValue)) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: optionStatus string was '" + optionStatus + "', but the corresponding baseline value was null");
        }

        return percentage * baselineValue;
    }

    private void insertOutOfBoundsBaseline(int agentId, int baselineId, double baselineMin, double baselineMax,
        int scheduleId, AlertConditionCacheStats stats) {
        List<OutOfBoundsCacheElement> oobCacheElements = lookupOutOfBoundsCacheElements(scheduleId);

        /*
         * as a sanity check, make sure we never insert duplicate OOB cache elements; this will
         * prevent upstream logic from passing data that puts the OOB portion of the cache into
         * an inconsistent state, which upon processing would create duplicate OOBs that blow up
         * later during out-of-band processing / insertion due to database constraint violations
         */

        if (oobCacheElements == null || oobCacheElements.isEmpty()) {
            // all is well in the land of OOBs
        } else {
            if (log.isDebugEnabled()) {
                /*
                 * this should never happen if the upstream logic is correct, but let's be lenient 
                 * during cache load and prevent it; showing it at debug level only so that it's
                 * not confused as an error in the logic, since the CORRECT logic *is* to prevent
                 * duplicate elements (i.e., only two OOB elements per scheduleId - one high, one low) 
                 */
                log.debug("Will not insert duplicate " + OutOfBoundsCacheElement.class.getSimpleName()
                    + " for schedule[id=" + scheduleId + "]");
            }
            return;
        }

        /*
         * First, do some calculations to compute the cache elements from the raw data
         */
        double lowOOB = baselineMin * MeasurementConstants.LOW_OOB_THRESHOLD;
        double highOOB = baselineMax * MeasurementConstants.HIGH_OOB_THRESHOLD;

        /*
         * when dealing with negative numbers, the LOW_OOB_THRESHOLD will create a **larger** lowOOB than the
         * highOOB...so, after multiplying by the thresholds, swap if necessary to correct the issue
         */
        if (lowOOB > highOOB) {
            double temp = highOOB;

            highOOB = lowOOB;
            lowOOB = temp;
        }

        OutOfBoundsCacheElement lowCacheElement = null;
        OutOfBoundsCacheElement highCacheElement = null;

        try {
            lowCacheElement = new OutOfBoundsCacheElement(AlertConditionOperator.LESS_THAN, lowOOB, scheduleId);
        } catch (InvalidCacheElementException icee) {
            log.info("Failed to create OutOfBoundsCacheElement with parameters: "
                + getCacheElementErrorString(scheduleId, AlertConditionOperator.LESS_THAN, null, lowOOB));
        }

        try {
            highCacheElement = new OutOfBoundsCacheElement(AlertConditionOperator.GREATER_THAN, highOOB, scheduleId);
        } catch (InvalidCacheElementException icee) {
            log.info("Failed to create OutOfBoundsCacheElement with parameters: "
                + getCacheElementErrorString(scheduleId, AlertConditionOperator.GREATER_THAN, null, highOOB));
        }

        if ((lowCacheElement != null) && (highCacheElement != null)) {
            /*
             * Next, add the calculated values to the measurementDataCache
             */
            addToOutOfBoundsCache(scheduleId, lowCacheElement, agentId, stats);
            addToOutOfBoundsCache(scheduleId, highCacheElement, agentId, stats);

            /* 
             * can't pass (stats.created-stats.deleted) to the monitor because this methods called in a loop, which
             * means the first time this is called the count will increase by 2, next time by 4, etc
             */
            AlertConditionCacheMonitor.getMBean().incrementOOBCacheElementCount(2);
        }
    }

    private String getCacheElementErrorString(int conditionId, AlertConditionOperator operator, Object option,
        Object value) {
        return "id=" + conditionId + ", " + "operator=" + operator + ", "
            + ((option != null) ? ("option=" + option + ", ") : "") + "value=" + value;
    }

    private void removeAlertCondition(int alertConditionId, AlertConditionCacheStats stats) {
        /*
         * remove the map bound to the alertCondition id being removed from the cache; we no longer need bookkeeping
         * information about it after we're done using it
         */
        List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>> inverseCacheElements = inverseAlertConditionMap
            .remove(alertConditionId);

        /*
         * if we have no inverse data for this alertCondition, we have no work to do; this should never happen, but
         * let's not fail with NPE if it does for some reason
         */
        if (inverseCacheElements == null) {
            if (log.isDebugEnabled()) {
                log.debug("There were no inverseCacheElements for " + alertConditionId
                    + ", but all alertConditions should be in the cache");
            }
            return;
        }

        for (Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>> inverseCacheElement : inverseCacheElements) {
            // it's possible this might leave empty lists in the various other caches and maps - not a big deal
            if (log.isTraceEnabled()) {
                log.trace("Removing " + inverseCacheElement.lefty + " from the cache");
            }
            inverseCacheElement.righty.remove(inverseCacheElement.lefty);
            stats.deleted++;

            // OOBs elements are no longer removed here, see removeOutOfBoundsCondition(int, AlertConditionCacheStats)
            if (inverseCacheElement.lefty instanceof AvailabilityCacheElement) {
                AlertConditionCacheMonitor.getMBean().incrementAvailabilityCacheElementCount(-1);
            } else if (inverseCacheElement.lefty instanceof EventCacheElement) {
                AlertConditionCacheMonitor.getMBean().incrementEventCacheElementCount(-1);
            } else if (inverseCacheElement.lefty instanceof ResourceOperationCacheElement) {
                AlertConditionCacheMonitor.getMBean().incrementOperationCacheElementCount(-1);
            } else if (inverseCacheElement.lefty instanceof MeasurementTraitCacheElement) {
                AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(-1);
            } else if (inverseCacheElement.lefty instanceof MeasurementNumericCacheElement) {
                AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(-1);
            } else {
                log.info("AlertConditionCacheMonitor does not yet remove/reset cache counts for elements of type '"
                    + inverseCacheElement.lefty.getClass() + "'");
            }
        }
    }

    private void removeOutOfBoundsCondition(int scheduleId, AlertConditionCacheStats stats) {
        // purposefully NOT removing the List for this scheduleId, cause it'll be reloaded in a few moments anyway
        List<OutOfBoundsCacheElement> outOfBoundsCacheElements = lookupOutOfBoundsCacheElements(scheduleId);
        int toDeleteCount = outOfBoundsCacheElements.size();
        stats.deleted += toDeleteCount;
        AlertConditionCacheMonitor.getMBean().incrementOOBCacheElementCount(-toDeleteCount);
        outOfBoundsCacheElements.clear(); // but we do need to clear it
    }

    public String[] getCacheNames() {
        CacheName[] caches = CacheName.values();
        String[] results = new String[caches.length];

        int i = 0;
        for (CacheName cache : caches) {
            results[i++] = cache.name();
        }

        return results;
    }

    public void printCache(String cacheName) {
        rwLock.readLock().lock();

        try {
            if (CacheName.MeasurementDataCache.name().equals(cacheName)) {
                printListCache(CacheName.MeasurementDataCache.name(), measurementDataCache);
            } else if (CacheName.MeasurementTraitCache.name().equals(cacheName)) {
                printListCache(CacheName.MeasurementTraitCache.name(), measurementTraitCache);
            } else if (CacheName.MeasurementOutOfBoundsCache.name().equals(cacheName)) {
                printListCache(CacheName.MeasurementOutOfBoundsCache.name(), measurementOutOfBoundsCache);
            } else if (CacheName.ResourceOperationCache.name().equals(cacheName)) {
                printNestedCache(CacheName.ResourceOperationCache.name(), resourceOperationCache);
            } else if (CacheName.AvailabilityCache.name().equals(cacheName)) {
                printListCache(CacheName.AvailabilityCache.name(), availabilityCache);
            } else if (CacheName.Inverse.name().equals(cacheName)) {
                printInverseCache();
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void printAllCaches() {
        rwLock.readLock().lock();

        try {
            if (log.isDebugEnabled()) {
                log.debug(""); // visually separate logs better
                printListCache(CacheName.MeasurementDataCache.name(), measurementDataCache);
                printListCache(CacheName.MeasurementTraitCache.name(), measurementTraitCache);
                printListCache(CacheName.MeasurementOutOfBoundsCache.name(), measurementOutOfBoundsCache);
                printNestedCache(CacheName.ResourceOperationCache.name(), resourceOperationCache);
                printListCache(CacheName.AvailabilityCache.name(), availabilityCache);
                printInverseCache();
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private <T extends AbstractCacheElement<?>> void printListCache(String cacheName, Map<Integer, List<T>> cache) {
        log.debug("Printing " + cacheName + "...");

        for (Map.Entry<Integer, List<T>> cacheElement : cache.entrySet()) {
            log.debug("key=" + cacheElement.getKey() + ", " + "value=" + cacheElement.getValue());
        }
    }

    private <T extends AbstractCacheElement<?>> void printNestedCache(String cacheName,
        Map<Integer, Map<Integer, List<T>>> nestedCache) {
        log.debug("Printing " + cacheName + "...");
        for (Map.Entry<Integer, Map<Integer, List<T>>> cache : nestedCache.entrySet()) {
            for (Map.Entry<Integer, List<T>> cacheElement : cache.getValue().entrySet()) {
                log.debug("key1=" + cache.getKey() + ", " + "key2=" + cacheElement.getKey() + ", " + "value="
                    + cacheElement.getValue());
            }
        }
    }

    private void printInverseCache() {
        log.debug("Printing inverseAgentConditionMap...");
        for (Map.Entry<Integer, List<Integer>> inverseEntry : inverseAgentConditionMap.entrySet()) {
            log.debug("agentId=" + inverseEntry.getKey() + " has the following mappings: ");
            List<Integer> conditions = inverseEntry.getValue();
            log.debug("conditionIds=" + conditions);
        }

        log.debug("Printing inverseAlertConditionMap...");
        for (Map.Entry<Integer, List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>> inverseEntry : inverseAlertConditionMap
            .entrySet()) {
            log.debug("alertConditionId=" + inverseEntry.getKey() + " has the following mappings: ");
            for (Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>> tupleElement : inverseEntry.getValue()) {
                AbstractCacheElement<?> inverseElement = tupleElement.lefty;
                log.debug("abstractCacheElement=" + inverseElement);
            }
        }
    }

    private boolean isInvalidDouble(Double d) {
        return (d == null || Double.isNaN(d) || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY);
    }

}