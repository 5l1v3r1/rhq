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
package org.rhq.core.pluginapi.event.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * An Event poller that polls a log file for new entries. 
 *
 * @author Ian Springer
 */
public class LogFileEventPoller implements EventPoller {
    private static final long DEFAULT_POLLING_INTERVAL = 60;

    private final Log log = LogFactory.getLog(this.getClass());

    private String eventType;
    private File logFile;
    private FileInfo logFileInfo;
    private LogEntryProcessor entryProcessor;
    private long pollingInterval;

    public LogFileEventPoller(EventContext eventContext, String eventType, File logFile, LogEntryProcessor entryProcessor) {
        this.eventType = eventType;
        this.logFile = logFile;
        Sigar sigar = eventContext.getSigar();
        try {
            this.logFileInfo = sigar.getFileInfo(logFile.getPath());
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
        this.entryProcessor = entryProcessor;
        this.pollingInterval = DEFAULT_POLLING_INTERVAL;
    }

    @NotNull
    public String getEventType() {
        return this.eventType;
    }

    @NotNull
    public String getSourceLocation() {
        return this.logFile.getPath();
    }

    public Set<Event> poll() {
        if (!this.logFile.exists()) {
            log.warn("Log file [" + this.logFile + "' being polled does not exist.");
            return null;
        }
        try {
            if (!this.logFileInfo.changed()) {
                return null;
            }
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
        return processNewLines();
    }

    private Set<Event> processNewLines() {
        Set<Event> events = new HashSet<Event>();
        Reader reader = null;
        try {
            reader = new FileReader(this.logFile);

            long offset = getOffset();

            if (offset > 0) {
                reader.skip(offset);
            }
            String line;
            BufferedReader buffer = new BufferedReader(reader);
            while ((line = buffer.readLine()) != null) {
                Event event = this.entryProcessor.processLine(line);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read log file being tailed: " + this.logFile, e);
        } finally {
            if (reader != null) {
                //noinspection EmptyCatchBlock
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return events;
    }

    public long getPollingInterval() {
        return this.pollingInterval;
    }

    public void setPollingInterval(long pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    private long getOffset() {
        FileInfo previousFileInfo = this.logFileInfo.getPreviousInfo();

        if (previousFileInfo == null) {
            if (log.isDebugEnabled()) {
                log.debug(this.logFile + ": first stat");
            }
            return this.logFileInfo.getSize();
        }

        if (this.logFileInfo.getInode() != previousFileInfo.getInode()) {
            if (log.isDebugEnabled()) {
                log.debug(this.logFile + ": file inode changed");
            }
            return -1;
        }

        if (this.logFileInfo.getSize() < previousFileInfo.getSize()) {
            if (log.isDebugEnabled()) {
                log.debug(this.logFile + ": file truncated");
            }
            return -1;
        }

        if (log.isDebugEnabled()) {
            long diff = this.logFileInfo.getSize() - previousFileInfo.getSize();
            log.debug(this.logFile + ": " + diff + " new bytes");
        }

        return previousFileInfo.getSize();
    }

}
