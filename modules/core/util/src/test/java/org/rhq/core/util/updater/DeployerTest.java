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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;

@Test
public class DeployerTest {

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

    public void testInitialDeployOneZip() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        try {
            File testZipFile1 = new File("target/test-classes/updater-test2.zip");
            Pattern filesToRealizeRegex = Pattern.compile("(fileA)|(dir1/fileB)");

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Set<File> zipFiles = new HashSet<File>(1);
            zipFiles.add(testZipFile1);
            Map<File, String> rawFiles = null;
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Deployer deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex,
                templateEngine, ignoreRegex);
            deployer.deploy();

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null);
            assert map.size() == 7 : map;
            String f = "dir1/file1";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1/file2";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2/file3";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir3/dir4/file4";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "fileA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1/fileB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2/fileC";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));

        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }
}