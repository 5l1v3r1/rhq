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
package org.rhq.enterprise.server.util.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AlertSerializer {
    private final Log log = LogFactory.getLog(AlertSerializer.class);

    private static Map<Integer, ReentrantReadWriteLock> locks = new HashMap<Integer, ReentrantReadWriteLock>();
    private static AlertSerializer singleton = new AlertSerializer();;

    // synchronize so only one thread creates our singleton
    public static synchronized AlertSerializer getSingleton() {
        return singleton;
    }

    public void lock(int alertDefinitionId) {
        ReentrantReadWriteLock lock = null;

        String msg = "tid= " + Thread.currentThread().getId() + ": alertDefinitionId=" + alertDefinitionId;

        /*
         * synchronize on only what is needed
         */
        log.debug(msg + ": about to synchronize");
        synchronized (this) {
            log.debug(msg + ": synchronized");
            lock = locks.get(alertDefinitionId);
            if (lock == null) {
                log.debug(msg + ": creating new lock");
                lock = new ReentrantReadWriteLock();
                locks.put(alertDefinitionId, lock);
            }
        }

        log.debug(msg + ": acquiring write lock");
        lock.writeLock().lock();
        log.debug(msg + ": acquired write lock");
    }

    public void unlock(int alertDefinitionId) {
        ReentrantReadWriteLock lock = locks.get(alertDefinitionId);

        String msg = "tid= " + Thread.currentThread().getId() + ": alertDefinitionId=" + alertDefinitionId;

        if (lock != null) {
            log.debug(msg + ": releasing write lock");
            lock.writeLock().unlock();
            log.debug(msg + ": released write lock");
        } else {
            log.warn(msg + ": cannot release write lock");
        }
    }
}