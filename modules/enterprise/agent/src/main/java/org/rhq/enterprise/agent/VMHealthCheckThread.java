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
package org.rhq.enterprise.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

import mazz.i18n.Logger;

import org.rhq.enterprise.agent.AgentRestartCounter.AgentRestartReason;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * This is a thread that will periodically check the health of the VM
 * (e.g. check the memory usage within the VM to detect if
 * memory is critically low), and if the health is poor it will put the
 * agent into hibernate mode, which will essentially shutdown the agent,
 * let it pause for some amount of time, then restart the agent. This
 * will hopefully clear up the poor VM condition.
 * 
 * @author John Mazzitelli
 */
public class VMHealthCheckThread extends Thread {
    private static final Logger LOG = AgentI18NFactory.getLogger(VMHealthCheckThread.class);

    /**
     * Will be <code>true</code> when this thread is told to stop checking. Note that this does not necessarily mean the
     * thread is stopped, it just means this thread was told to stop. See {@link #stopped}.
     */
    private boolean stop;

    /**
     * Will be <code>true</code> when this thread is stopped or will be stopped shortly.
     */
    private boolean stopped;

    /**
     * The agent that will be hibernated if the VM is critically sick.
     */
    private final AgentMain agent;

    /**
     * The amount of time in milliseconds that this thread will sleep in between checks
     */
    private final long interval;

    /**
     * If the amount of used heap memory is larger than this percentage of max heap memory
     * then the VM will be considered critically low on heap.
     */

    private final float heapThreshold;

    /**
     * If the amount of used non-heap memory is larger than this percentage of max non-heap memory,
     * then the VM will be considered critically low on heap.
     */
    private final float nonheapThreshold;

    /**
     * If <code>true</code>, the thread will explicitly ask for garbage collection to occur when
     * memory is critical. If <code>false</code>, the thread will merely report when memory is critical,
     * but it will not attempt to correct the situation itself - it will assume the garabage collector
     * will trigger at the appropriate time.
     */
    private final boolean performGC;

    /**
     * These are names used to identify MemoryPoolMXBeans that are to be monitored.
     */
    private final List<String> memoryPoolsToMonitor;

    public VMHealthCheckThread(AgentMain agent) {
        super("RHQ VM Health Check Thread");
        setDaemon(false);
        this.stop = false;
        this.stopped = true;
        this.agent = agent;

        AgentConfiguration config = agent.getConfiguration();
        if (config != null) {
            this.interval = config.getVMHealthCheckIntervalMsecs();
            this.heapThreshold = config.getVMHealthCheckLowHeapMemThreshold();
            this.nonheapThreshold = config.getVMHealthCheckLowNonHeapMemThreshold();
        } else { // this should never happen, but I'm paranoid
            this.interval = 5000L;
            this.heapThreshold = 0.90f;
            this.nonheapThreshold = 0.90f;
        }

        // TODO: put these in agent configuration
        this.memoryPoolsToMonitor = new ArrayList<String>();
        String memoryPoolNames = System.getProperty("rhq.agent.vm-health-check.mem-pools-to-check", "perm gen");
        for (String memoryPoolName : memoryPoolNames.split(",")) {
            this.memoryPoolsToMonitor.add(memoryPoolName.toLowerCase()); // lowercase so our checks are case-insensitive
        }

        String gcProp = System.getProperty("rhq.agent.vm-health-check.perform-gc", "true");
        this.performGC = Boolean.parseBoolean(gcProp);

        return;
    }

    /**
     * Tells this thread to stop checking. This will block and wait for the thread to die.
     */
    public void stopChecking() {
        this.stop = true;

        // tell the thread that we flipped the stop flag in case it is waiting in a sleep interval
        synchronized (this) {
            while (!this.stopped) {
                try {
                    notifyAll();
                    wait(5000L);
                } catch (InterruptedException e) {
                }
            }
        }

        return;
    }

    @Override
    public void run() {
        this.stopped = false;

        LOG.debug(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_STARTED, this.interval);

        final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        final List<MemoryPoolMXBean> memoryPoolMxBeans = getMemoryPoolMXBeansToMonitor();

        try {
            // perform an initial sleep to prevent us from trying to stop while agent is still starting
            synchronized (this) {
                wait(this.interval);
            }

            while (!this.stop) {
                try {
                    if (checkMemory(memoryMxBean)) {
                        LOG.fatal(AgentI18NResourceKeys.VM_HEALTH_CHECK_SEES_MEM_PROBLEM);
                        restartAgent(60000L);
                        continue;
                    }

                    if (checkPoolMemories(memoryPoolMxBeans, memoryMxBean)) {
                        LOG.fatal(AgentI18NResourceKeys.VM_HEALTH_CHECK_SEES_MEM_PROBLEM);
                        restartAgent(60000L);
                        continue;
                    }

                    // TODO: if our memory is good, we might have to check and make sure we are
                    //       the only thread running.  Under an odd and rare circumstance (if
                    //       restartAgent fails to completely start the agent but did manage to
                    //       start another VM check thread and failed to "re-shutdown" the agent)
                    //       there will end up being more than one of these threads running.
                    //       We'll need to make sure we kill all threads but one.

                    // go to sleep before we check again
                    synchronized (this) {
                        wait(this.interval);
                    }
                } catch (VirtualMachineError vme) {
                    // We're too late - OOM probably happening now.
                    // Try to do as little as possible here (no logging, no creating objects)
                    // and immediately try to shutdown our agent and restart it.
                    restartAgent(0L);
                } catch (InterruptedException e) {
                    this.stop = true;
                }
            }
        } catch (Throwable t) {
            LOG.error(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_EXCEPTION, t);
        }

        LOG.debug(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_STOPPED);
        this.stopped = true;

        return;
    }

    /**
     * This will {@link AgentMain#shutdown()} the agent, pause for the given number of milliseconds, then
     * {@link AgentMain#start()} the agent again.
     * 
     * @param pause number of milliseconds before restarting the agent after shutting down
     */
    private void restartAgent(long pause) throws Exception {
        // this method is going to kill our thread by calling stopChecking; to avoid deadlock, set our flags now
        this.stop = true;
        this.stopped = true;

        // immediately attempt to shutdown the agent which should free up alot of VM resources (memory/threads)
        try {
            this.agent.shutdown();
        } catch (Throwable t) {
            // this is bad, we can't even shutdown the agent.
            // but this thread is our only hope to recover, so do not stop the thread now
            // let it continue and see if we can recover the next time
            this.stop = false;
            this.stopped = false;
            Thread.interrupted(); // clear the interrupted status to ensure our thread doesn't abort
            Thread.sleep(30000L); // give our thread time to breath - do avoid fast infinite looping that might occur
            return;
        }

        // If we are told to wait before restarting, do so here. We want to wait because its possible
        // some external influence (downed server or downed managed resource) is causing our agent
        // to misbehave. In that case, we'll want to wait a bit to give time for that external resource
        // to correct itself and thus allow the agent to get back to normal itself.
        if (pause > 0) {
            Thread.sleep(pause);
        }

        // now that the agent is shutdown and we've paused a bit, let's try to restart it
        try {
            this.agent.start();
        } catch (Throwable t) {
            // uh-oh, we can't start the agent for some reason; our thread is our last and only hope to recover

            // first try to shutdown again, in case start() got half way there but couldn't finish
            // do NOT set stop flags to false yet as this would cause a deadlock
            try {
                this.agent.shutdown();
                // TODO: purging spool: agentConfig.getDataDirectory() + agentConfig.getClientSenderCommandSpoolFileName()
            } catch (Throwable ignore) {
                // at this point, we may (or may not) have two VM check threads running, what should we do?
            }

            // do not stop the thread - let it continue and see if we can recover the next time
            this.stop = false;
            this.stopped = false;
            Thread.interrupted(); // clear the interrupted status to ensure our thread doesn't abort
            return;
        }

        // At this point, we have "rebooted" the agent - our memory usage should be back to normal.
        this.agent.getAgentRestartCounter().restartedAgent(AgentRestartReason.VM_HEALTH_CHECK);

        return;
    }

    /**
     * Checks the VM's memory subsystem and if it detects the VM is critically
     * low on memory, <code>true</code> will be returned.
     * 
     * @param bean the platform MBean that contains the memory statistics
     * 
     * @return <code>true</code> if the VM is critically low on memory
     */
    private boolean checkMemory(MemoryMXBean bean) {
        boolean heapCritical = false;
        boolean nonheapCritical = false;

        try {
            heapCritical = isCriticallyLow(bean.getHeapMemoryUsage(), this.heapThreshold, "VM heap");
            nonheapCritical = isCriticallyLow(bean.getNonHeapMemoryUsage(), this.nonheapThreshold, "VM nonheap");

            if (heapCritical || nonheapCritical) {
                // uh-oh, we are low on memory, before we say we are truly critical, try to GC
                try {
                    if (this.performGC) {
                        LOG.warn(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_GC);
                        bean.gc();
                    }

                    // let see what our memory usage is now
                    heapCritical = isCriticallyLow(bean.getHeapMemoryUsage(), this.heapThreshold, "VM heap");
                    nonheapCritical = isCriticallyLow(bean.getNonHeapMemoryUsage(), this.nonheapThreshold, "VM nonheap");
                } catch (Throwable t) {
                    // something bad is happening, let's return true and see if we can recover
                    return true;
                }
            }
        } catch (Throwable t) {
            // this should never happen unless something odd occurred.
            // let's return true only if we have previously detected critically low memory
        }

        return heapCritical || nonheapCritical;
    }

    /**
     * Checks the given pools' memories and if it detects the pool is critically
     * low on memory, <code>true</code> will be returned.
     * 
     * @param memoryPoolMxBeans the MBeans that contain the memory statistics
     * @param memoryMxBean the memory MX bean, used to perform GC if we need to
     * 
     * @return <code>true</code> if one of the pools is critically low on memory
     */

    private boolean checkPoolMemories(List<MemoryPoolMXBean> memoryPoolMxBeans, MemoryMXBean memoryMxBean) {
        boolean critical = false;
        boolean allValid = true;

        try {
            for (MemoryPoolMXBean bean : memoryPoolMxBeans) {
                if (bean.isValid()) {
                    critical = isCriticallyLow(bean.getUsage(), this.heapThreshold, bean.getName());

                    if (critical) {
                        // uh-oh, we are low on memory, before we say we are truly critical, try to GC
                        try {
                            if (this.performGC) {
                                LOG.warn(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_GC);
                                memoryMxBean.gc();
                            }

                            // let see what our memory usage is now
                            critical = isCriticallyLow(bean.getUsage(), this.heapThreshold, bean.getName());
                        } catch (Throwable t) {
                            // something bad is happening, let's return true and see if we can recover
                            return true;
                        }
                    }
                } else {
                    allValid = false;
                }
            }
        } catch (Throwable t) {
            // this should never happen unless something odd occurred.
            // let's return true only if we have previously detected critically low memory
        }

        // we aren't critical, but for some reason, one of our MBeans aren't valid anymore, re-obtain them
        if (!critical && !allValid) {
            memoryPoolMxBeans.clear();
            memoryPoolMxBeans.addAll(getMemoryPoolMXBeansToMonitor());
        }

        return critical;
    }

    /**
     * Returns <code>true</code> if the given memory usage indicates that
     * memory is critically low.
     * 
     * @param memoryUsage
     * @param d the percentage of used memory to max available memory that is
     *          the threshold to be considered critical. e.g. If this is 0.9, that means
     *          if the used memory is 90% or higher of the max, then there is
     *          a critical shortest of free memory and true will be returned
     * @param type the type of memory
     *          
     * @return <code>true</code> if the amount of used memory is over the threshold
     */
    private boolean isCriticallyLow(MemoryUsage memoryUsage, float thresholdPercentage, String type) {
        final long used = memoryUsage.getUsed();
        final long max = memoryUsage.getMax();

        if ((max > -1) && (used > (max * thresholdPercentage))) {
            LOG.warn(AgentI18NResourceKeys.VM_HEALTH_CHECK_THREAD_MEM_LOW, type, thresholdPercentage, memoryUsage);
            return true;
        }

        return false;
    }

    /**
     * Gets a list of all the memory pool MBeans that are to be monitored.
     *  
     * @return the list of MBeans that need to be monitored
     */
    private List<MemoryPoolMXBean> getMemoryPoolMXBeansToMonitor() {
        final List<MemoryPoolMXBean> memoryPoolMxBeansToMonitor = new ArrayList<MemoryPoolMXBean>();

        if (!this.memoryPoolsToMonitor.isEmpty()) {
            final List<MemoryPoolMXBean> allMemoryPoolMxBeans = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean memoryPoolMXBean : allMemoryPoolMxBeans) {
                if (this.memoryPoolsToMonitor.contains(memoryPoolMXBean.getName().toLowerCase())) {
                    memoryPoolMxBeansToMonitor.add(memoryPoolMXBean);
                }
            }
        }

        return memoryPoolMxBeansToMonitor;
    }
}
