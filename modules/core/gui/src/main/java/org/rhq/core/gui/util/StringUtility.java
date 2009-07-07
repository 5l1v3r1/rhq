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
package org.rhq.core.gui.util;

import java.util.List;

public class StringUtility {
    private static char CHAR_SPACE = ' ';

    public static <T extends Object> String getListAsDelimitedString(List<T> items) {
        return getListAsDelimitedString(items, ',');
    }

    public static <T extends Object> String getListAsDelimitedString(List<T> items, char delimiter) {
        if (items.size() < 1) {
            return "";
        }

        StringBuilder results = new StringBuilder();

        results.append(items.get(0).toString());
        for (int i = 1; i < items.size(); i++) {
            results.append(delimiter).append(CHAR_SPACE).append(items.get(i).toString());
        }

        return results.toString();
    }

    public static Integer[] getIntegerArray(List<String> list) {
        if (list == null) {
            return new Integer[0];
        }

        Integer[] results = new Integer[list.size()];
        int i = 0;
        for (String item : list) {
            results[i++] = Integer.valueOf(item);
        }

        return results;
    }

    public static Integer[] getIntegerArray(String[] list) {
        if (list == null) {
            return new Integer[0];
        }

        Integer[] results = new Integer[list.length];
        int i = 0;
        for (String item : list) {
            results[i++] = Integer.valueOf(item);
        }

        return results;
    }

    public static int[] getIntArray(String[] list) {
        if (list == null) {
            return new int[0];
        }

        int[] results = new int[list.length];
        int i = 0;
        for (String item : list) {
            results[i++] = Integer.valueOf(item);
        }

        return results;
    }

    public static int[] getIntArray(List<String> list) {
        if (list == null) {
            return new int[0];
        }

        int[] results = new int[list.size()];
        int i = 0;
        for (String item : list) {
            results[i++] = Integer.valueOf(item);
        }

        return results;
    }
}