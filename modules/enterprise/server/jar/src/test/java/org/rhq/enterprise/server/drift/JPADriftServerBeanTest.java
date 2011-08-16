/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.drift;

import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.drift.JPADriftFileBits;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.TransactionCallback;

import static org.rhq.core.domain.drift.DriftFileStatus.LOADED;
import static org.rhq.test.JPAUtils.executeInTransaction;

public class JPADriftServerBeanTest extends AbstractEJB3Test {

    @BeforeMethod
    public void initDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.createQuery("delete from JPADriftFileBits").executeUpdate();
            }
        });
    }

    @Test
    public void saveAndLoadFileContent() throws Exception {
        String string = "Testing saving and loading content";

        final JPADriftFileBits content = new JPADriftFileBits("a1b2c3");
        content.setStatus(LOADED);
        content.setDataSize((long) string.length());
        content.setData(Hibernate.createBlob(string.getBytes()));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(content);
            }
        });
        JPADriftServerLocal driftServer = LookupUtil.getJPADriftServer();

        assertEquals("Failed to load content", string, driftServer.getDriftFileBits(content.getHashId()));
    }

}
