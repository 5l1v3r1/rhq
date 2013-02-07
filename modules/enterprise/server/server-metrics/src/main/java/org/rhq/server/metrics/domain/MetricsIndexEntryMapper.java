/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 *
 */

package org.rhq.server.metrics.domain;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;


/**
 * @author John Sanda
 */
public class MetricsIndexEntryMapper implements ResultSetMapper<MetricsIndexEntry> {

    private MetricsTable bucket;

    public MetricsIndexEntryMapper(MetricsTable bucket) {
        this.bucket = bucket;
    }

    @Override
    public List<MetricsIndexEntry> mapAll(ResultSet resultSet) {
        List<MetricsIndexEntry> result = new ArrayList<MetricsIndexEntry>();
        for (Row singleRow : resultSet) {
            result.add(map(singleRow));
        }

        return result;
    }

    @Override
    public MetricsIndexEntry mapOne(ResultSet resultSet) {
        return map(resultSet.fetchOne());
    }

    @Override
    public List<MetricsIndexEntry> map(Row... row) {
        List<MetricsIndexEntry> result = new ArrayList<MetricsIndexEntry>();
        for (Row singleRow : row) {
            result.add(new MetricsIndexEntry(bucket, singleRow.getDate(0), singleRow.getInt(1)));
        }

        return result;
    }

    @Override
    public MetricsIndexEntry map(Row row) {
        return new MetricsIndexEntry(bucket, row.getDate(0), row.getInt(1));
    }
}
