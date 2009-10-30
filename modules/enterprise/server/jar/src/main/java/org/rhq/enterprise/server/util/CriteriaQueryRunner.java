/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.server.util;

import java.lang.reflect.Field;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageList;

public class CriteriaQueryRunner<T> {

    private Criteria criteria;

    private CriteriaQueryGenerator queryGenerator;

    private EntityManager entityManager;

    public CriteriaQueryRunner(Criteria criteria, CriteriaQueryGenerator queryGenerator, EntityManager entityManager) {
        this.criteria = criteria;
        this.queryGenerator = queryGenerator;
        this.entityManager = entityManager;
    }

    public PageList<T> execute() {
        Query query = queryGenerator.getQuery(entityManager);
        Query countQuery = queryGenerator.getCountQuery(entityManager);

        long count = (Long) countQuery.getSingleResult();
        List<T> results = query.getResultList();

        initAllPersistentBags(results);

        return new PageList<T>(results, (int) count, criteria.getPageControl());
    }

    private void initAllPersistentBags(List<T> entities) {
        for (T entity : entities) {
            initPersistentBags(entity);
        }
    }

    private void initPersistentBags(T entity) {
        for (Field persistentBagField : queryGenerator.getPersistentBagFields()) {
            List persistentBag = getList(entity, persistentBagField);
            persistentBag.size();
        }
    }

    private List getList(T entity, Field field) {
        try {
            field.setAccessible(true);
            return (List) field.get(entity);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

}
