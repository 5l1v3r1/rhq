/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * @author John Sanda
 */
public class RawNumericMetricMapper implements ResultSetMapper<RawNumericMetric> {

    private ResultSetMapper<RawNumericMetric> mapper;

    public RawNumericMetricMapper() {
        this(false);
    }

    public RawNumericMetricMapper(boolean metaDataIncluded) {
        if (metaDataIncluded) {
            mapper = new ResultSetMapper<RawNumericMetric>() {
                @Override
                public List<RawNumericMetric> mapAll(ResultSet resultSet) {
                    List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
                    for (Row row : resultSet) {
                        metrics.add(map(row));
                    }

                    return metrics;
                }

                @Override
                public RawNumericMetric mapOne(ResultSet resultSet) {
                    return mapper.map(resultSet.fetchOne());
                }

                @Override
                public List<RawNumericMetric> map(Row... row) {
                    List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();

                    for (Row singleRow : row) {
                        RawNumericMetric metric = new RawNumericMetric(singleRow.getInt(0), singleRow.getDate(1)
                            .getTime(), singleRow.getDouble(2));
                        ColumnMetadata metadata = new ColumnMetadata(singleRow.getInt(3), singleRow.getLong(4));
                        metric.setColumnMetadata(metadata);

                        metrics.add(metric);
                    }

                    return metrics;
                }

                @Override
                public RawNumericMetric map(Row row) {
                    RawNumericMetric metric = new RawNumericMetric(row.getInt(0), row.getDate(1).getTime(),
                        row.getDouble(2));
                    ColumnMetadata metadata = new ColumnMetadata(row.getInt(3), row.getLong(4));
                    metric.setColumnMetadata(metadata);

                    return metric;
                }
            };
        } else {
            mapper = new ResultSetMapper<RawNumericMetric>() {
                @Override
                public List<RawNumericMetric> mapAll(ResultSet resultSet) {
                    List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
                    for (Row row : resultSet) {
                        metrics.add(map(row));
                    }

                    return metrics;
                }

                @Override
                public RawNumericMetric mapOne(ResultSet resultSet) {
                    return map(resultSet.fetchOne());
                }

                @Override
                public List<RawNumericMetric> map(Row... row) {
                    List<RawNumericMetric> metrics = new ArrayList<RawNumericMetric>();
                    for (Row singleRow : row) {
                        metrics.add(new RawNumericMetric(singleRow.getInt(0), singleRow.getDate(1).getTime(), singleRow
                            .getDouble(2)));
                    }

                    return metrics;
                }

                @Override
                public RawNumericMetric map(Row row) {
                    return new RawNumericMetric(row.getInt(0), row.getDate(1).getTime(), row.getDouble(2));
                }
            };
        }
    }

    @Override
    public List<RawNumericMetric> mapAll(ResultSet resultSet) {
        return mapper.mapAll(resultSet);
    }

    @Override
    public RawNumericMetric mapOne(ResultSet resultSet) {
        return mapper.mapOne(resultSet);
    }

    @Override
    public List<RawNumericMetric> map(Row... row) {
        return mapper.map(row);
    }

    @Override
    public RawNumericMetric map(Row row) {
        return mapper.map(row);
    }
}
