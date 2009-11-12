/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc;

import org.quartz.StatefulJob;

/**
 * The actual quartz job that the plugin container will submit when it needs to invoke
 * a scheduled job on behalf of a plugin. This is a "stateful job" which tells quartz
 * that only one job should be invoked at any one time on any server.
 * 
 * Note that server plugin developers do not extend this class. Instead, developers
 * need to have their plugin components implement {@link ScheduledJob}.
 *  
 * @author John Mazzitelli
 */
public class StatefulJobWrapper extends AbstractJobWrapper implements StatefulJob {
}
