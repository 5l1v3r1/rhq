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
package org.rhq.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;

/**
 * A unit test for {@link PropertiesFileUpdate}.
 */
@Test
public class PropertiesFileUpdateTest {
    private File existingPropertiesFile;

    @BeforeMethod
    public void beforeMethod() throws IOException {
        existingPropertiesFile = File.createTempFile("properties-file-update-test", ".properties");
        PrintStream ps = new PrintStream(new FileOutputStream(existingPropertiesFile), true, "8859_1");
        ps.println("# first comment");
        ps.println("one=1"); // no spaces around the equals
        ps.println();
        ps.println("# second comment");
        ps.println("two = 12"); // a single space on either side of the equals
        ps.println();
        ps.println("# third comment");
        ps.print("three  =  123"); // multiple spaces around the equals - no newline!
        ps.flush();
        ps.close();
    }

    @AfterMethod
    public void afterMethod() {
        if (existingPropertiesFile != null) {
            existingPropertiesFile.delete();
        }
    }

    public void testSpacesAroundEquals() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        // we want to change the values, but reuse the lines that originally set them
        Properties newProps = new Properties();
        newProps.setProperty("one", "new1");
        newProps.setProperty("two", "new2");
        newProps.setProperty("three", "new3");

        update.update(newProps);
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("new1");
        assert props.getProperty("two").equals("new2");
        assert props.getProperty("three").equals("new3");
        assert props.size() == 3;

        // now make sure we didn't add extra lines, we should have reused the original lines in the file
        // we should only ever have a single instance of one=, two=, three= lines.
        int oneCount = 0;
        int twoCount = 0;
        int threeCount = 0;
        BufferedReader reader = new BufferedReader(new FileReader(existingPropertiesFile));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("one")) {
                    oneCount++;
                }
                if (line.startsWith("two")) {
                    twoCount++;
                }
                if (line.startsWith("three")) {
                    threeCount++;
                }
            }
        } finally {
            reader.close();
        }

        assert oneCount == 1 : "we added extraneous one= properties:\n" + slurpPropertiesFile();
        assert twoCount == 1 : "we added extraneous two= properties:\n" + slurpPropertiesFile();
        assert threeCount == 1 : "we added extraneous three= properties:\n" + slurpPropertiesFile();
    }

    private String slurpPropertiesFile() throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(existingPropertiesFile))); // slurp will close our stream for us
    }

    public void testEmptyValue() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        // we want to change some of the values, but leave others alone
        Properties newProps = new Properties();
        newProps.setProperty("two", "");
        newProps.setProperty("four", "");

        update.update(newProps);
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("");
        assert props.getProperty("three").equals("123");
        assert props.getProperty("four").equals("");
        assert props.size() == 4;

        update.update("one", null); // null is same as ""
        update.update("five", null); // null is same as ""
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("");
        assert props.getProperty("two").equals("");
        assert props.getProperty("three").equals("123");
        assert props.getProperty("four").equals("");
        assert props.getProperty("five").equals("");
        assert props.size() == 5;
    }

    public void testBulkUpdate() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        // we want to change some of the values, but leave others alone
        Properties newProps = new Properties();
        newProps.setProperty("two", "new2");
        newProps.setProperty("three", "123"); // same as the old value - should be ignored
        newProps.setProperty("four", "44444");

        update.update(newProps);
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("new2");
        assert props.getProperty("three").equals("123");
        assert props.getProperty("four").equals("44444");
        assert props.size() == 4;
    }

    public void testUpdateKeyValue() throws Exception {
        Properties props = loadPropertiesFile();

        // sanity check - validate our original test properties file is as we expect
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("12");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        PropertiesFileUpdate update = new PropertiesFileUpdate(existingPropertiesFile.getAbsolutePath());

        update.update("two", "22222");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("1");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        update.update("one", "11111");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("11111");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("123");
        assert props.size() == 3;

        update.update("three", "33333");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("11111");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("33333");
        assert props.size() == 3;

        update.update("four", "1234");
        props = loadPropertiesFile();
        assert props.getProperty("one").equals("11111");
        assert props.getProperty("two").equals("22222");
        assert props.getProperty("three").equals("33333");
        assert props.getProperty("four").equals("1234");
        assert props.size() == 4;
    }

    private Properties loadPropertiesFile() throws IOException {
        Properties props = new Properties();
        FileInputStream is = new FileInputStream(existingPropertiesFile);
        props.load(is);
        is.close();
        return props;
    }
}