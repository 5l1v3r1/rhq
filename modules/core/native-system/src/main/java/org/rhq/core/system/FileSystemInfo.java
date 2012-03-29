/*
* RHQ Management Platform
* Copyright (C) 2005-2012 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.core.system;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

/**
 * Information about a mounted file system.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class FileSystemInfo {
    private final Log log = LogFactory.getLog(this.getClass());

    private final String mountPoint;
    private FileSystem fs;
    private FileSystemUsage fsUsage;

    public FileSystemInfo(String mountPoint) {
        this.mountPoint = mountPoint;
        refresh();
    }

    public void refresh() {
        SigarProxy sigar = SigarAccess.getSigar();

        try {
            // this only needs to be loaded once - it will never change during the lifetime of the file system
            if (this.fs == null) {
                this.fs = sigar.getFileSystemMap().getFileSystem(this.mountPoint);
            }
        } catch (Exception e) {
            throw new SystemInfoException("Cannot refresh file system mounted at [" + this.mountPoint + "]", e);
        }

        try {
            // this is the usage data and therefore should be refreshed
            this.fsUsage = sigar.getMountedFileSystemUsage(this.mountPoint);
        } catch (SigarException e) {
            // this happens when the file system is not available (e.g. if it's a CD-ROM without a CD loaded in it) or
            // if we don't have permission to access the filesystem. we can ignore it and set the usage data to null.
            this.fsUsage = null;
            if (log.isTraceEnabled()) {
                log.trace("Cannot refresh the usage data for file system mounted at [" + this.mountPoint + "].", e);
            } else {
                log.debug("Cannot refresh the usage data for file system mounted at [" + this.mountPoint + "]: " + e);
            }
        } catch (RuntimeException e) {
            this.fsUsage = null;
            log.error("An error occurred while refreshing the usage data for file system mounted at [" + this.mountPoint
                    + "].", e);
        }
    }

    public String getMountPoint() {
        return this.mountPoint;
    }

    /**
     * Provides with static information about the file system.
     *
     * @return static file system information
     */
    public FileSystem getFileSystem() {
        return this.fs;
    }

    /**
     * This returns the usage information on the file system. This may return <code>null</code> if the file system is
     * not available (e.g when a "device not ready" error for CD-ROM drives occurs) or not accessible due to lack of
     * permission.
     *
     * @return file system usage data
     */
    public FileSystemUsage getFileSystemUsage() {
        return this.fsUsage;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
            "mountPoint='" + mountPoint + '\'' +
            ", fs=" + fs +
            ", fsUsage=" + fsUsage +
            ']';
    }
}