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
package org.rhq.bindings.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URL;

/**
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class PackageFinder {

    private List<File> jarLocations;

    public PackageFinder(List<File> jarLocations) {
        this.jarLocations = new ArrayList<File>(jarLocations);
    }

    public List<String> findPackages(String packageRoot) throws IOException {
        ArrayList<String> found = new ArrayList<String>();

//TODO this is where the original package finder used to look for the jars.
//update the code somewhere in the CLI client to supply this dir to this
//new style impl.
//        String cwd = System.getProperty("user.dir");
//        File libDir = new File(cwd, "lib");
        
        
        List<File> jars = new ArrayList<File>();

        for (File loc : jarLocations) {
            if (loc.exists()) {
                jars.addAll(Arrays.asList(loc.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.isFile() && pathname.getName().endsWith(".jar");
                    }
                })));
            }
        }

        jars.addAll(loadJARsFromClassPath(packageRoot));

        for (File jar : jars) {
            findPackages(packageRoot, found, jar);
        }

        return found;
    }

    private List<File> loadJARsFromClassPath(String pkgRoot) throws IOException {
        List<File> jarFiles = new ArrayList<File>();
        String pkgPath = pkgRoot.replaceAll("\\.", "/");
        Enumeration<URL> resources = getClass().getClassLoader().getResources(pkgPath);
        URL resource = null;

        while (resources.hasMoreElements()) {
            resource = resources.nextElement();
            if (resource.toString().startsWith("jar:file")) {
                String jarFilePath = getJARFilePath(resource);
                jarFiles.add(new File(jarFilePath));
            }
        }

        return jarFiles;
    }

    private String getJARFilePath(URL resource) {
        int startIndex = "jar:file:".length();
        String string = resource.toString().substring(startIndex);
        int endIndex = string.indexOf("!");

        return string.substring(0, endIndex);
    }

    private void findPackages(String packageRoot, List<String> list, File jar) throws IOException {

        Set<String> paths = new HashSet<String>();
        JarFile jf = null;
        try {
            jf = new JarFile(jar);

            String packagePath = packageRoot.replaceAll("\\.", "/");

            Enumeration<JarEntry> entries = jf.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().startsWith(packagePath)) {
                    String match = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
                    paths.add(match.replaceAll("/", "\\."));
                }
            }
            list.addAll(paths);
        } finally {
            try {
                if (jf != null) {
                    jf.close();
                }
            } catch (IOException e) {
                
            }
        }
    }
}
