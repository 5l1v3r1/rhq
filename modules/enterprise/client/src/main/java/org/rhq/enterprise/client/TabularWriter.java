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
package org.rhq.enterprise.client;

import java.io.PrintWriter;
import java.util.*;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Greg Hinkle
 */
public class TabularWriter {

    String[] headers;
    int[] maxColumnLength;
    int width = 160;
    PrintWriter out;

    static Set<String> IGNORED_PROPS = new HashSet<String>();

    static {
        IGNORED_PROPS.add("mtime");
        IGNORED_PROPS.add("ctime");
        IGNORED_PROPS.add("itime");
        IGNORED_PROPS.add("uuid");
        IGNORED_PROPS.add("description");
        IGNORED_PROPS.add("parentResource");
    }

    static Set<Class> SIMPLE_TYPES = new HashSet<Class>();

    static {
        SIMPLE_TYPES.add(Byte.class);
        SIMPLE_TYPES.add(Byte.TYPE);
        SIMPLE_TYPES.add(Short.class);
        SIMPLE_TYPES.add(Short.TYPE);
        SIMPLE_TYPES.add(Integer.class);
        SIMPLE_TYPES.add(Integer.TYPE);
        SIMPLE_TYPES.add(Long.class);
        SIMPLE_TYPES.add(Long.TYPE);
        SIMPLE_TYPES.add(Float.class);
        SIMPLE_TYPES.add(Float.TYPE);
        SIMPLE_TYPES.add(Double.class);
        SIMPLE_TYPES.add(Double.TYPE);
        SIMPLE_TYPES.add(Boolean.class);
        SIMPLE_TYPES.add(Boolean.TYPE);
        SIMPLE_TYPES.add(String.class);
    }

    public TabularWriter(PrintWriter out, String... headers) {
        this.headers = headers;
        this.out = out;
    }


    public TabularWriter(PrintWriter out) {
        this.out = out;
    }

    public void print(Object object) {

        if (object instanceof Collection) {
            print((Collection)object);
            return;
        }

        try {

            if (SIMPLE_TYPES.contains(object.getClass())) {
                this.out.println(String.valueOf(object));
                return;
            }


            BeanInfo info = Introspector.getBeanInfo(object.getClass(), object.getClass().getSuperclass());
            headers = new String[info.getPropertyDescriptors().length];
            int i = 0;


            out.println(object.getClass().getSimpleName() + ":");
            Map<String, String> properties = new TreeMap<String, String>();
            int maxLength = 0;
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method m = pd.getReadMethod();
                Object val = null;
                if (m != null) {
                    val = m.invoke(object);
                }

                if (val != null) {
                    try {
                        String str = String.valueOf(val);
                        maxLength = Math.max(maxLength, pd.getName().length());
                        properties.put(pd.getName(), String.valueOf(val));
                    } catch (Exception e) {
                    }
                }
            }

            for (String key : properties.keySet()) {
                this.out.print("\t");
                printPreSpaced(out, key, maxLength);
                this.out.print(": ");
                this.out.println(properties.get(key));
            }

        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IntrospectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public void print(Collection list) {
        // List of arbitrary objects
        if (list == null || list.size() == 0)
            out.println("no data");
        else if (list.size() == 1) {
            out.println("one row");

            print(list.iterator().next());
        } else {

            String[][] data = null;
            Object firstObject = list.iterator().next();
            try {


                if (firstObject instanceof String) {
                    headers = new String[]{"Value"};
                    data = new String[list.size()][1];
                    int i = 0;
                    for (Object object : list) {
                        data[i++][0] = (String) object;
                    }


                } else {

                    BeanInfo info = Introspector.getBeanInfo(firstObject.getClass(), firstObject.getClass().getSuperclass());
                    int i = 0;


                    List<PropertyDescriptor> pdList = new ArrayList<PropertyDescriptor>(); //Arrays.asList(info.getPropertyDescriptors()));
                    for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                        try {
                            boolean allNull = true;
                            for (Object row : list) {
                                Method m = pd.getReadMethod();
                                Object val = null;
                                if (m != null) {
                                    val = pd.getReadMethod().invoke(row);
                                }
                                if ((val != null && !(val instanceof Collection)) ||
                                        ((val != null && (val instanceof Collection) && !((Collection) val).isEmpty())))
                                    allNull = false;
                            }
                            if (!allNull && !IGNORED_PROPS.contains(pd.getName())) {
                                pdList.add(pd);
                            }
                        } catch (Exception e) {
                        }

                    }


                    headers = new String[pdList.size()];
                    data = new String[list.size()][pdList.size()];

                    for (PropertyDescriptor pd : pdList) {
                        headers[i++] = pd.getName();
                    }
                    i = 0;
                    for (Object row : list) {
                        int j = 0;
                        for (PropertyDescriptor pd : pdList) {

                            Object val = "?";
                            try {
                                val = pd.getReadMethod().invoke(row);
                            } catch (Throwable e) {
                                System.out.println("crap");
                            }
                            data[i][j++] = String.valueOf(val);
                        }
                        i++;
                    }
                }

                this.print(data);

            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                headers = null;
            }

        }

    }

    public void print(Object[] data) {
        if (data == null || data.length == 0) {
            out.println("0 rows");
            return;
        }
        out.println("Array of " + (data.getClass().getComponentType().getName()));
        for (Object row : data) {
            out.println("\t" + row);
        }
    }

    public void print(String[][] data) {

        if (data == null || data.length == 0) {
            out.println("0 rows");
            return;
        }
        maxColumnLength = new int[data[0].length];

        for (String[] row : data) {

            for (int i = 0; i < row.length; i++) {
                if (row[i] == null) {
                    row[i] = "";
                }
                maxColumnLength[i] = Math.max(maxColumnLength[i], row[i].length());
            }
        }

        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                maxColumnLength[i] = Math.max(maxColumnLength[i], headers[i].length());
            }
        }

        int totalColumnLength = 0;
        for (int len : maxColumnLength) {
            totalColumnLength += len;
        }
        // add space for spaces
        totalColumnLength += maxColumnLength.length;

        double shrink = 1;
        if (totalColumnLength > width) {
            shrink = ((double) width) / totalColumnLength;
        }

        for (int i = 0; i < maxColumnLength.length; i++) {
            maxColumnLength[i] = (int) Math.floor(shrink * maxColumnLength[i]);
        }

        if (headers != null) {

            for (int i = 0; i < maxColumnLength.length; i++) {
                int colSize = maxColumnLength[i];
                printSpaced(out, headers[i], colSize);
                out.print(" ");
            }

            out.print("\n");

            for (int i = 0; i < width; i++) {
                out.print("-");
            }
            out.print("\n");

        }


        for (String[] row : data) {

            for (int i = 0; i < maxColumnLength.length; i++) {
                int colSize = maxColumnLength[i];

                printSpaced(out, row[i], colSize);
                out.print(" ");
            }
            out.print("\n");
        }

        out.print(data.length + " rows\n");
    }


    private void printSpaced(PrintWriter out, String data, int length) {
        int dataLength = data.length();
        if (dataLength > length) {
            out.print(data.substring(0, length));
        } else {
            out.print(data);

            for (int i = dataLength; i < length; i++) {
                out.print(" ");
            }
        }

    }

    private void printPreSpaced(PrintWriter out, String data, int length) {
        int dataLength = data.length();
        if (dataLength > length) {
            out.print(data.substring(0, length));
        } else {
            for (int i = dataLength; i < length; i++) {
                out.print(" ");
            }
            out.print(data);
        }

    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
