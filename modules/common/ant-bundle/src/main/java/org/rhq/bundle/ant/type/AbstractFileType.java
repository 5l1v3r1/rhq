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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

/**
 * A base class for the functionality shared by {@link FileType} and {@link ArchiveType}.
 *
 * @author Ian Springer
 */
public abstract class AbstractFileType extends DataType {
    private File source;

    public File getSource() {
        return this.source;
    }

    public void setName(String name) {
        File file = new File(name);
        if (file.isAbsolute()) {
            throw new BuildException("Path specified by 'file' attribute (" + name
                + ") is not relative - it must be a relative path, relative to the Ant basedir.");
        }
        this.source = getProject().resolveFile(name);

        /* TODO: we can't validate this here - we might not be parsing on the same machine where it will be executing
        if (!this.source.exists()) {
            throw new BuildException("Path specified by 'file' attribute (" + name + ") does not exist.");
        }
        if (this.source.isDirectory()) {
            throw new BuildException("Path specified by 'file' attribute (" + name + ") is a directory - it must be a regular file.");
        }
        */
    }

}