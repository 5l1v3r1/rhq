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
package org.rhq.enterprise.server.common;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.enterprise.server.RHQConstants;

/**
 * @author Joseph Marques
 */
@Stateless
public class EntityManagerFacade implements EntityManagerFacadeLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public Query createQuery(String queryStr) {
        return entityManager.createQuery(queryStr);
    }

    public void clear() {
        entityManager.clear();
    }

    public void flush() {
        entityManager.flush();
    }

    public void persist(Object o) {
        entityManager.persist(o);
    }

    public <T> T merge(T o) {
        return entityManager.merge(o);
    }

    public void remove(Object o) {
        entityManager.remove(o);
    }

}
