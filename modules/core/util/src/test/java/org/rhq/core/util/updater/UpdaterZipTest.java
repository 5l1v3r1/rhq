/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.util.updater;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;

@Test
public class UpdaterZipTest {

    private TemplateEngine templateEngine;
    private String javaVersion;

    @BeforeClass
    public void beforeClass() {
        javaVersion = System.getProperty("java.version");

        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("rhq.system.hostname", "localhost");
        tokens.put("rhq.system.sysprop.java.version", javaVersion);

        templateEngine = new TemplateEngine(tokens);
    }

    public void testWalkExtract() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testWalkExtract", ".dir", null);
        try {
            File testZipFile = new File("target/test-classes/updater-test2.zip");

            ExtractorZipFileVisitor visitorNoRealize = new ExtractorZipFileVisitor(tmpDir, null, templateEngine);
            ZipUtil.walkZipFile(testZipFile, visitorNoRealize);
            FileHashcodeMap mapRaw = visitorNoRealize.getFileHashcodeMap();
            assert mapRaw.size() == 7 : mapRaw;
            String f = "dir1" + File.separator + "file1";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));
            f = "dir1" + File.separator + "file2";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));
            f = "dir2" + File.separator + "file3";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));
            f = "dir3" + File.separator + "dir4" + File.separator + "file4";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));
            f = "fileA";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));
            f = "dir1" + File.separator + "fileB";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));
            f = "dir2" + File.separator + "fileC";
            assert mapRaw.containsKey(f) : mapRaw;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(mapRaw.get(f));

            // now walk the zip and realize some files

            FileUtil.purge(tmpDir, false);
            Pattern filesToRealizeRegex = Pattern.compile("(fileA)|(dir1.fileB)"); // '.' in place of file separator to support running test on windows & unix
            ExtractorZipFileVisitor visitor = new ExtractorZipFileVisitor(tmpDir, filesToRealizeRegex, templateEngine);
            ZipUtil.walkZipFile(testZipFile, visitor);

            FileHashcodeMap map = visitor.getFileHashcodeMap();
            assert map.size() == 7 : map;
            f = "dir1" + File.separator + "file1";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1" + File.separator + "file2";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2" + File.separator + "file3";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir3" + File.separator + "dir4" + File.separator + "file4";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "fileA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1" + File.separator + "fileB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2" + File.separator + "fileC";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));

            // check that our unrealized files between the two walks are the same
            assert map.get("dir1" + File.separator + "file1").equals(mapRaw.get("dir1" + File.separator + "file1"));
            assert map.get("dir1" + File.separator + "file2").equals(mapRaw.get("dir1" + File.separator + "file2"));
            assert map.get("dir2" + File.separator + "file3").equals(mapRaw.get("dir2" + File.separator + "file3"));
            assert map.get("dir3" + File.separator + "dir4" + File.separator + "file4").equals(
                mapRaw.get("dir3" + File.separator + "dir4" + File.separator + "file4"));

            // check that our realized files have different hashcodes from their unrealized forms
            assert !map.get("fileA").equals(mapRaw.get("fileA"));
            assert !map.get("dir1" + File.separator + "fileB").equals(mapRaw.get("dir1" + File.separator + "fileB"));
        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }

    public void testWalkInMemory() throws Exception {
        File testZipFile = new File("target/test-classes/updater-test2.zip");

        InMemoryZipFileVisitor visitorNoRealize = new InMemoryZipFileVisitor(null, templateEngine);
        ZipUtil.walkZipFile(testZipFile, visitorNoRealize);
        FileHashcodeMap mapNoRealize = visitorNoRealize.getFileHashcodeMap();
        assert mapNoRealize.size() == 7 : mapNoRealize;
        assert mapNoRealize.containsKey("dir1" + File.separator + "file1") : mapNoRealize;
        assert mapNoRealize.containsKey("dir1" + File.separator + "file2") : mapNoRealize;
        assert mapNoRealize.containsKey("dir2" + File.separator + "file3") : mapNoRealize;
        assert mapNoRealize.containsKey("dir3" + File.separator + "dir4" + File.separator + "file4") : mapNoRealize;
        assert mapNoRealize.containsKey("fileA") : mapNoRealize;
        assert mapNoRealize.containsKey("dir1" + File.separator + "fileB") : mapNoRealize;
        assert mapNoRealize.containsKey("dir2" + File.separator + "fileC") : mapNoRealize;

        Pattern filesToRealizeRegex = Pattern.compile("(fileA)|(dir1.fileB)"); // '.' in place of file separator to support running test on windows & unix
        InMemoryZipFileVisitor visitor = new InMemoryZipFileVisitor(filesToRealizeRegex, templateEngine);
        ZipUtil.walkZipFile(testZipFile, visitor);

        FileHashcodeMap map = visitor.getFileHashcodeMap();
        assert map.size() == 7 : map;
        assert map.containsKey("dir1" + File.separator + "file1") : map;
        assert map.containsKey("dir1" + File.separator + "file2") : map;
        assert map.containsKey("dir2" + File.separator + "file3") : map;
        assert map.containsKey("dir3" + File.separator + "dir4" + File.separator + "file4") : map;
        assert map.containsKey("fileA") : map;
        assert map.containsKey("dir1" + File.separator + "fileB") : map;
        assert map.containsKey("dir2" + File.separator + "fileC") : map;

        // check that our unrealized files between the two walks are the same
        assert map.get("dir1" + File.separator + "file1").equals(mapNoRealize.get("dir1" + File.separator + "file1"));
        assert map.get("dir1" + File.separator + "file2").equals(mapNoRealize.get("dir1" + File.separator + "file2"));
        assert map.get("dir2" + File.separator + "file3").equals(mapNoRealize.get("dir2" + File.separator + "file3"));
        assert map.get("dir3" + File.separator + "dir4" + File.separator + "file4").equals(
            mapNoRealize.get("dir3" + File.separator + "dir4" + File.separator + "file4"));

        // check that our realized files have different hashcodes from their unrealized forms
        assert !map.get("fileA").equals(mapNoRealize.get("fileA"));
        assert !map.get("dir1" + File.separator + "fileB").equals(mapNoRealize.get("dir1" + File.separator + "fileB"));
    }

    public void testRealize() throws Exception {
        File testZipFile = new File("target/test-classes/updater-test2.zip");
        InMemoryZipEntryRealizer realizer = new InMemoryZipEntryRealizer(testZipFile, templateEngine);
        String fileA = realizer.realize("fileA");
        String fileB = realizer.realize("dir1" + File.separator + "fileB");
        assert fileA != null;
        assert fileB != null;
        assert fileA.contains("this is fileA") : fileA;
        assert fileB.contains("this is fileB") : fileB;
        assert fileA.contains("rhq.system.hostname = [localhost]") : fileA;
        assert fileB.contains("rhq.system.hostname = [localhost]") : fileB;
        assert fileA.contains("rhq.system.sysprop.java.version = [" + javaVersion + "]") : fileA;
        assert fileB.contains("rhq.system.sysprop.java.version = [" + javaVersion + "]") : fileB;
        assert fileA.contains("custom.prop = [@@custom.prop@@]") : fileA;
        assert fileB.contains("custom.prop = [@@custom.prop@@]") : fileB;
    }

    public void testNoRealize() throws Exception {
        File testZipFile = new File("target/test-classes/updater-test2.zip");
        InMemoryZipEntryRealizer realizer = new InMemoryZipEntryRealizer(testZipFile, null); // notice a null template engine
        String fileA = realizer.realize("fileA");
        String fileB = realizer.realize("dir1" + File.separator + "fileB");
        assert fileA != null;
        assert fileB != null;
        assert fileA.contains("this is fileA") : fileA;
        assert fileB.contains("this is fileB") : fileB;
        assert fileA.contains("rhq.system.hostname = [@@rhq.system.hostname@@]") : fileA;
        assert fileB.contains("rhq.system.hostname = [@@rhq.system.hostname@@]") : fileB;
        assert fileA.contains("rhq.system.sysprop.java.version = [@@rhq.system.sysprop.java.version@@]") : fileA;
        assert fileB.contains("rhq.system.sysprop.java.version = [@@rhq.system.sysprop.java.version@@]") : fileB;
        assert fileA.contains("custom.prop = [@@custom.prop@@]") : fileA;
        assert fileB.contains("custom.prop = [@@custom.prop@@]") : fileB;
    }
}
