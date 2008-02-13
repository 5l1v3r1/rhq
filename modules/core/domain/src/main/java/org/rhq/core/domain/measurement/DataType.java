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
package org.rhq.core.domain.measurement;

/**
 * Describe the type of data that is to be collected. The typical type is "measurement" (i.e. numeric) data that
 * normally changes over time (see {@link NumericType} for further classification of this kind of data). A "trait" is
 * typically a value that changes rarely (e.g. number of CPUs on a platform, or an install path). "Complex" data is
 * tabular in nature with N columns of N rows of values. Call-time data is the min/max/avg call durations for each
 * destination (e.g. URL or session EJB method) in a set of destinations.
 *
 * @author John Mazzitelli
 * @see    NumericType
 */
public enum DataType {
    MEASUREMENT, TRAIT, COMPLEX, CALLTIME
}