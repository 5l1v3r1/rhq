/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant.type;

import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * A file to be copied during the bundle deployment.
 *
 * @author Ian Springer
 */
public class FileType extends AbstractFileType {
    private File destinationDir;
    private File destinationFile;

    public File getDestinationDir() {
        return this.destinationDir;
    }

    // Pass in a String, rather than a File, since we don't want Ant to resolve the path relative to basedir if it's relative.
    public void setDestinationDir(String destinationDir) {
        if (getDestinationFile() != null) {
            throw new BuildException("Both 'destinationDir' and 'destinationFile' attributes are defined - only one or the other may be specified.");
        }
        this.destinationDir = new File(destinationDir);
    }

    public File getDestinationFile() {
        return this.destinationFile;
    }

    public void setDestinationFile(String destinationFile) {
        if (getDestinationDir() != null) {
            throw new BuildException("Both 'destinationDir' and 'destinationFile' attributes are defined - only one or the other may be specified.");
        }
        this.destinationFile = new File(destinationFile);
    }
}