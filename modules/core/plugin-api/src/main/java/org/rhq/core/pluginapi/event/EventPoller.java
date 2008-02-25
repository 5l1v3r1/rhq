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
package org.rhq.core.pluginapi.event;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;

/**
 * A class that polls at a designated interval for {@link Event}s of a specific type from a specific source.
 *
 * @author Ian Springer
 */
public interface EventPoller {
    /**
     * Minimum polling interval, in seconds.
     */
    int MINIMUM_POLLING_INTERVAL = 60; // 1 minute

    /**
     * Maximum polling interval, in seconds.
     */
    int MAXIMUM_POLLING_INTERVAL = 600; // 10 minutes

    /**
     * Returns the type of event (i.e. the {@link org.rhq.core.domain.event.EventDefinition} name) that this poller
     * checks for.
     *
     * @return the type of event (i.e. the {@link org.rhq.core.domain.event.EventDefinition} name) that this poller
     *         checks for
     */
    @NotNull
    String getEventType();

    /**
     * Returns the location that should be polled for {@link Event}s.
     *
     * @return the location that should be polled for {@link Event}s
     */
    @NotNull
    String getSourceLocation();

    /**
     * Poll for new Events (i.e. Events that have occurred since the last time poll() was called).
     * 
     * @return any new Events (i.e. Events that have occurred since the last time poll() was called)
     */
    @Nullable
    Set<Event> poll();
    
    /**
     * Returns the number of seconds to wait between polls. If a value less than {@link #MINIMUM_POLLING_INTERVAL} is
     * specified, {@link #MINIMUM_POLLING_INTERVAL} will be used instead.
     *
     * @return the number of seconds to wait between polls
     */
    long getPollingInterval();
}
