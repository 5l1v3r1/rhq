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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Individually tests these situations:
 * 
 * (X, Y, Z, ? represent hashcodes; none means file doesn't exist):
 * 
 *    ORIGINAL CURRENT    NEW   What To Do...
 * a.        X       X      X   New file is installed over current*
 * b.        X       X      Y   New file is installed over current
 * c.        X       Y      X   Current file is left as-is
 * d.        X       Y      Y   New file is installed over current*
 * e.        X       Y      Z   New file is installed over current, current is backed up
 * f.     none       ?      ?   New file is installed over current, current is backed up
 * g.        X    none      ?   New file is installed
 * h.        ?       ?   none   Current file deleted, backed up if different than original
 *
 * (*) means the new and current files will actually be the same content
 * 
 * @author John Mazzitelli
 */
@Test
public class SimpleDeployerTest {

    private final String originalContent = "original content";
    private final String originalFileName = "original_file_name.txt";
    private File tmpDir;
    private File deployDir;
    private File originalZipFile;
    private Set<File> originalZipFiles;
    private DeploymentProperties originalDeployProps;
    private FileHashcodeMap originalFileHashcodeMap;
    private String originalHashcode;
    private File currentFile;
    private DeploymentProperties newDeployProps;
    private DeployDifferences diff;
    private DeploymentsMetadata metadata;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        this.tmpDir = FileUtil.createTempDirectory("simpleDeployer_TMP", ".test", null);
        this.deployDir = FileUtil.createTempDirectory("simpleDeployer", ".test", null);
        this.originalHashcode = MessageDigestGenerator.getDigestString(originalContent);
        this.originalZipFile = createZip(originalContent, tmpDir, "original.zip", originalFileName);
        this.originalZipFiles = new HashSet<File>(1);
        this.originalZipFiles.add(originalZipFile);
        this.originalDeployProps = new DeploymentProperties(1, "simple", "1.0", "original test deployment");
        Deployer deployer = new Deployer(originalDeployProps, originalZipFiles, null, deployDir, null, null, null);
        this.originalFileHashcodeMap = deployer.deploy(null);
        this.currentFile = new File(deployDir, originalFileName);

        this.newDeployProps = new DeploymentProperties(2, "simple", "2.0", "new test deployment");
        this.diff = new DeployDifferences();
        this.metadata = new DeploymentsMetadata(this.deployDir);

        // sanity check due to my paranoia
        assert this.currentFile.exists();
        assert this.originalHashcode.equals(MessageDigestGenerator.getDigestString(currentFile));
        assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        FileUtil.purge(this.tmpDir, true);
        FileUtil.purge(this.deployDir, true);
    }

    public void testX_X_X() throws Exception {
        baseX_X_X(false);
    }

    public void testX_X_Y() throws Exception {
        baseX_X_Y(false);
    }

    public void testX_Y_X() throws Exception {
        baseX_Y_X(false);
    }

    public void testX_Y_Y() throws Exception {
        baseX_Y_Y(false);
    }

    public void testX_Y_Z() throws Exception {
        baseX_Y_Z(false);
    }

    public void testNoOriginalNoCurrentWithNew() throws Exception {
        baseNoOriginalNoCurrentWithNew(false);
    }

    public void testNoOriginalWithCurrentWithNew() throws Exception {
        baseNoOriginalWithCurrentWithNew(false);
    }

    public void testNoCurrent() throws Exception {
        baseNoCurrent(false);
    }

    public void testNoNew() throws Exception {
        baseNoNew(false);
    }

    public void testNoNewWithCurrentDifferentThanOriginal() throws Exception {
        baseNoNewWithCurrentDifferentThanOriginal(false);
    }

    public void testX_X_X_DryRun() throws Exception {
        baseX_X_X(true);
    }

    public void testX_X_Y_DryRun() throws Exception {
        baseX_X_Y(true);
    }

    public void testX_Y_X_DryRun() throws Exception {
        baseX_Y_X(true);
    }

    public void testX_Y_Y_DryRun() throws Exception {
        baseX_Y_Y(true);
    }

    public void testX_Y_Z_DryRun() throws Exception {
        baseX_Y_Z(true);
    }

    public void testNoOriginalNoCurrentWithNew_DryRun() throws Exception {
        baseNoOriginalNoCurrentWithNew(true);
    }

    public void testNoOriginalWithCurrentWithNew_DryRun() throws Exception {
        baseNoOriginalWithCurrentWithNew(true);
    }

    public void testNoCurrent_DryRun() throws Exception {
        baseNoCurrent(true);
    }

    public void testNoNew_DryRun() throws Exception {
        baseNoNew(true);
    }

    public void testNoNewWithCurrentDifferentThanOriginal_DryRun() throws Exception {
        baseNoNewWithCurrentDifferentThanOriginal(true);
    }

    private void baseX_X_X(boolean dryRun) throws Exception {
        Deployer deployer = new Deployer(newDeployProps, originalZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // nothing changed!

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(this.originalContent);
        assert contentHash[1].equals(this.originalHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_X_Y(boolean dryRun) throws Exception {
        String newContent = "testX_X_Y";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        File newZipFile = createZip(newContent, tmpDir, "new-content.zip", originalFileName);
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new file changed the original file. The current file was never touched, so this is a simple upgrade

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        if (dryRun) {
            assert contentHash[0].equals(originalContent);
            assert contentHash[1].equals(originalHashcode);
        } else {
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_Y_X(boolean dryRun) throws Exception {
        String newContent = "testX_Y_X";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        writeFile(newContent, this.currentFile);

        Deployer deployer = new Deployer(newDeployProps, originalZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // very important to understand this - even though the current file is changed, the hashcode
        // stored in the map and the metadata directory is the ORIGINAL hashcode. This is to make it
        // known that the new deployment itself is the same as the original deployment. It is just
        // that we allow the user's manual changes to continue to live on in the filesystem. However,
        // if a newer deployment comes along in the future and changes the new file, this current file
        // must be updated/backed up as appropriate and the only way to know when that happens is if
        // the metadata retains the original/new hashcode and not the current one.

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        // note nothing changed - our current file remains as is
        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_Y_Y(boolean dryRun) throws Exception {
        String newContent = "testX_Y_Y";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        writeFile(newContent, this.currentFile);
        File newZipFile = createZip(newContent, tmpDir, "new-content.zip", originalFileName);
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new file changed the original, but our current file has already been manually updated
        // to match the new file. Therefore, the current file doesn't have to change its content.
        // Technically, the file could be overwritten, but the content will still be the same.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_Y_Z(boolean dryRun) throws Exception {
        String newContentY = "testX_Y_Z_YYY";
        writeFile(newContentY, this.currentFile);
        String newHashcodeY = MessageDigestGenerator.getDigestString(newContentY);

        String newContentZ = "testX_Y_Z_ZZZ";
        String newHashcodeZ = MessageDigestGenerator.getDigestString(newContentZ);
        File newZipFile = createZip(newContentZ, tmpDir, "new-content.zip", originalFileName);
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new file changed the original, and our current file has been manually updated
        // but that current file's change does not match to new file. Therefore, the current file
        // it out of date. The safest thing to do is backup the current and copy the new file
        // to become the current file.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcodeZ);
        String[] contentHash = getOriginalFilenameContentHashcode();
        if (dryRun) {
            assert contentHash[0].equals(newContentY);
            assert contentHash[1].equals(newHashcodeY);
        } else {
            assert contentHash[0].equals(newContentZ);
            assert contentHash[1].equals(newHashcodeZ);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(newContentY) : "did not backup the correct file?";
        }
    }

    private void baseNoOriginalNoCurrentWithNew(boolean dryRun) throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(new String[] { originalContent, newContent }, tmpDir, "new.zip", new String[] {
            originalFileName, newFileName });
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // the new deployment introduces a new file. This is simple - its just added to the filesystem

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 2;
        assert newFileHashcodeMap.get(originalFileName).equals(originalHashcode);
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);
        try {
            contentHash = getFilenameContentHashcode(newFileName);
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseNoOriginalWithCurrentWithNew(boolean dryRun) throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(new String[] { originalContent, newContent }, tmpDir, "new.zip", new String[] {
            originalFileName, newFileName });
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        File inTheWayFile = new File(this.deployDir, newFileName);
        String inTheWayContent = "this is in the way";
        String inTheWayHashcode = MessageDigestGenerator.getDigestString(inTheWayContent);
        writeFile(inTheWayContent, inTheWayFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new deployment introduces a new file. However, there is already a current file at the new file location.
        // That current file is unknown and in the way - it must be backed up and overwritten.
        // This is considered a "change" not an "addition" since the file system already had the file, it just got changed.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 2;
        assert newFileHashcodeMap.get(originalFileName).equals(originalHashcode);
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);
        contentHash = getFilenameContentHashcode(newFileName);
        if (dryRun) {
            assert contentHash[0].equals(inTheWayContent);
            assert contentHash[1].equals(inTheWayHashcode);
        } else {
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(newFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(newFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(newFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(inTheWayContent) : "did not backup the correct file?";
        }
    }

    private void baseNoCurrent(boolean dryRun) throws Exception {
        assert this.currentFile.delete() : "Failed to delete the current file, cannot prepare the test";

        Deployer deployer = new Deployer(newDeployProps, originalZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // its the same deployment as before, except someone deleted our current file.
        // This adds the new file back (which is the same as the original).

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        try {
            String[] contentHash = getOriginalFilenameContentHashcode();
            assert contentHash[0].equals(this.originalContent);
            assert contentHash[1].equals(this.originalHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(this.originalFileName) : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseNoNew(boolean dryRun) throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(newContent, tmpDir, "new.zip", newFileName);
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new deployment removes a file that was in the original (it also introduces a new file).
        // There is already a current file at the original file location that is the same as the original, as you would expect. 
        // That current file is to be deleted (since its not in the new deployment) and is not backed up since it is
        // the same as the original.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        if (dryRun) {
            assert this.currentFile.exists() : "this should have been left as-is";
        } else {
            assert !this.currentFile.exists() : "this should have been deleted";
        }
        try {
            String[] contentHash = getFilenameContentHashcode(newFileName);
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseNoNewWithCurrentDifferentThanOriginal(boolean dryRun) throws Exception {
        String currentContent = "modified content";
        writeFile(currentContent, this.currentFile);

        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(newContent, tmpDir, "new.zip", newFileName);
        Set<File> newZipFiles = new HashSet<File>(1);
        newZipFiles.add(newZipFile);

        Deployer deployer = new Deployer(newDeployProps, newZipFiles, null, deployDir, null, null, null);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new deployment removes a file that was in the original (it also introduces a new file).
        // However, there is already a current file at the original file location as you would expect but
        // its different than the original.
        // That current file is to be deleted (since its not in the new deployment) and it must be backed up
        // since it looks modified from the original.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        if (dryRun) {
            assert this.currentFile.exists() : "this should have been left as-is";
        } else {
            assert !this.currentFile.exists() : "this should have been deleted";
        }
        try {
            String[] contentHash = getFilenameContentHashcode(newFileName);
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(currentContent) : "did not backup the correct file?";
        }
    }

    private String[] getOriginalFilenameContentHashcode() throws Exception {
        return getFilenameContentHashcode(this.originalFileName);
    }

    private String[] getFilenameContentHashcode(String filename) throws Exception {
        String content = readFile(new File(this.deployDir, filename));
        String hashcode = MessageDigestGenerator.getDigestString(content);
        String[] contentHash = new String[] { content, hashcode };
        return contentHash;
    }

    private String readFile(File file) throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(file)));
    }

    private File writeFile(String content, File fileToOverwrite) throws Exception {
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(fileToOverwrite);
            fileToOverwrite.getParentFile().mkdirs();
            out.write(content.getBytes());
            return fileToOverwrite;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File writeFile(String content, File destDir, String fileName) throws Exception {
        File destFile = new File(destDir, fileName);
        return writeFile(content, destFile);
    }

    private File createZip(String content, File destDir, String zipName, String entryName) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            ZipEntry zipAdd = new ZipEntry(entryName);
            zipAdd.setTime(System.currentTimeMillis());
            out.putNextEntry(zipAdd);
            out.write(content.getBytes());
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    private File createZip(String[] content, File destDir, String zipName, String[] entryName) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            assert content.length == entryName.length;
            for (int i = 0; i < content.length; i++) {
                ZipEntry zipAdd = new ZipEntry(entryName[i]);
                zipAdd.setTime(System.currentTimeMillis());
                out.putNextEntry(zipAdd);
                out.write(content[i].getBytes());
            }
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }
}
