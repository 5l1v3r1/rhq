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
package org.rhq.core.system;

import java.util.Arrays;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides native information on a specific CPU.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class CpuInformation {
    private final Log log = LogFactory.getLog(this.getClass());

    private final int cpuIndex;
    private Cpu cpu;
    private CpuInfo cpuInfo;
    private CpuPerc cpuPercentage;
    private boolean enabled;

    public CpuInformation(int index) {
        cpuIndex = index;
        refresh();
    }

    public int getCpuIndex() {
        return cpuIndex;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public CpuInfo getCpuInfo() {
        return cpuInfo;
    }

    public CpuPerc getCpuPercentage() {
        return cpuPercentage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void refresh() {
        Sigar sigar = new Sigar();
        try {
            // This is supposed to return one CpuInfo per *socket*, but on some platforms, it will return one per *core*.
            // In either case, all CpuInfo's in the list should be identical.
            // NOTE: The results of getCpuInfoList() should be more consistent in SIGAR 1.5.1 and later
            //       (see http://jira.hyperic.com/browse/SIGAR-71).
            CpuInfo[] cpuInfoList = sigar.getCpuInfoList();
            if (cpuInfoList != null && cpuInfoList.length >= 1) {
                // Since all CpuInfo's in the list should be identical, we can always just grab the first one in the list.
                // We do *not* want to use this.cpuIndex as the index, because that is the *core* index, and this list
                // may be a list of *sockets*.
                this.cpuInfo = cpuInfoList[0];
            }
            else {
                log.error("Sigar.getCpuInfoList() returned null or empty array: "
                        + ((cpuInfoList != null) ? Arrays.asList(cpuInfoList) : cpuInfoList));
                this.cpuInfo = null;
            }

            // This should return one Cpu per *core*.
            Cpu[] cpuList = sigar.getCpuList();
            if (cpuList != null && this.cpuIndex < cpuList.length) {
                this.cpu = cpuList[this.cpuIndex];
            }
            else {
                log.debug("Sigar.getCpuList() returned null or array with size smaller than or equal to this CPU's index ("
                        + this.cpuIndex + "): " + ((cpuList != null) ? Arrays.asList(cpuList) : cpuList));
                this.cpu = null;
            }

            // This should return one CpuPerc per *core*.
            CpuPerc[] cpuPercentageList = sigar.getCpuPercList();
            if (cpuPercentageList != null && this.cpuIndex < cpuPercentageList.length) {
                this.cpuPercentage = cpuPercentageList[this.cpuIndex];
            }
            else {
                log.debug("Sigar.getCpuPercList() returned null or array with size smaller than or equal to this CPU's index ("
                        + this.cpuIndex + "): " + ((cpuPercentageList != null) ? Arrays.asList(cpuPercentageList) : cpuPercentageList));
                this.cpuPercentage = null;
            }            
        } catch (Exception e) {
            throw new SystemInfoException("Cannot refresh the native CPU information", e);
        } finally {
            sigar.close();
        }

        this.enabled = ((this.cpuInfo != null) && (this.cpu != null) && (this.cpuPercentage != null));

        return;
    }
}