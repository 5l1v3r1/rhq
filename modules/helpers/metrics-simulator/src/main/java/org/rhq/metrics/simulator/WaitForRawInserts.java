/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.metrics.simulator;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.RawDataInsertedCallback;

/**
* @author John Sanda
*/
class WaitForRawInserts implements RawDataInsertedCallback {

    private final Log log = LogFactory.getLog(WaitForRawInserts.class);

    private CountDownLatch latch;

    private Throwable throwable;

    public WaitForRawInserts(int numInserts) {
        latch = new CountDownLatch(numInserts);
    }

    @Override
    public void onFinish() {
    }

    @Override
    public void onSuccess(MeasurementDataNumeric measurementDataNumeric) {
        latch.countDown();
    }

    @Override
    public void onFailure(Throwable throwable) {
        latch.countDown();
        this.throwable = throwable;
        log.error("An async operation failed", throwable);
    }

    public void await(String errorMsg) throws Throwable {
        latch.await();
        if (throwable != null) {
            throw throwable;
        }
    }
}