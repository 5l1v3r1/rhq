/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public abstract class ControlCommand {

    public static final String SERVER_OPTION = "server";

    public static final String STORAGE_OPTION = "storage";

    public static final String AGENT_OPTION = "agent";

    public static final String RHQ_STORAGE_BASEDIR_PROP = "rhq.storage.basedir";

    public static final String RHQ_AGENT_BASEDIR_PROP = "rhq.agent.basedir";

    protected final String STORAGE_BASEDIR_NAME = "rhq-storage";

    protected final String AGENT_BASEDIR_NAME = "rhq-agent";

    protected final Log log = LogFactory.getLog(getClass().getName());

    protected File basedir;

    protected File binDir;

    private PropertiesConfiguration rhqctlConfig;

    public ControlCommand() {
        basedir = new File(System.getProperty("rhq.server.basedir"));
        binDir = new File(basedir, "bin");

        File rhqctlPropertiesFile = getRhqCtlProperties();
        try {
            rhqctlConfig = new PropertiesConfiguration(rhqctlPropertiesFile);
        } catch (ConfigurationException e) {
            throw new RHQControlException("Failed to load configuration", e);
        }
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract Options getOptions();

    protected abstract void exec(CommandLine commandLine);

    public void exec(String[] args) {
        Options options = getOptions();
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(options, args);
            exec(cmdLine);
            rhqctlConfig.save();
        } catch (ParseException e) {
            printUsage();
        } catch (ConfigurationException e) {
            throw new RHQControlException("Failed to update " + getRhqCtlProperties(), e);
        }
    }

    public void printUsage() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        String header = "\n" + getDescription() + "\n\n";
        String syntax;

        if (options.getOptions().isEmpty()) {
            syntax = "rhqctl.sh " + getName();
        } else {
            syntax = "rhqctl.sh " + getName() + " [options]";
        }

        helpFormatter.setNewLine("\n");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    protected List<Integer> toIntList(String s) {
        String[] args = s.split(",");
        List<Integer> list = new ArrayList<Integer>(args.length);
        for (String arg : args) {
            list.add(Integer.parseInt(arg));
        }

        return list;
    }

    protected File getStorageBasedir() {
        return new File(getProperty(RHQ_STORAGE_BASEDIR_PROP, new File(basedir,
            STORAGE_BASEDIR_NAME).getAbsolutePath()));
    }

    protected File getAgentBasedir() {
        return new File(getProperty(RHQ_AGENT_BASEDIR_PROP, new File(basedir,
            AGENT_BASEDIR_NAME).getAbsolutePath()));
    }

    protected boolean isServerInstalled() {
        File modulesDir = new File(basedir, "modules");
        File metaInfDir = new File(modulesDir,
            "org/rhq/rhq-enterprise-server-startup-subsystem/main/deployments/rhq.ear/META-INF/");
        File markerFile = new File(metaInfDir, ".installed");

        return markerFile.exists();
    }

    protected boolean isAgentInstalled() {
        return getAgentBasedir().exists();
    }

    protected  boolean isStorageInstalled() {
        return getStorageBasedir().exists();
    }

    protected String getStoragePid() throws IOException {
        File storageBasedir = getStorageBasedir();
        File storageBinDir = new File(storageBasedir, "bin");
        File pidFile = new File(storageBinDir, "cassandra.pid");

        if (pidFile.exists()) {
            return StreamUtil.slurp(new FileReader(pidFile));
        }
        return null;
    }

    protected String getServerPid() throws IOException {
        File pidFile = new File(binDir, "rhq-server.pid");
        if (pidFile.exists()) {
            return StreamUtil.slurp(new FileReader(pidFile));
        }
        return null;
    }

    protected String getAgentPid() throws IOException {
        File agentBinDir = new File(getAgentBasedir(), "bin");
        File pidFile = new File(agentBinDir, "rhq-agent.pid");

        if (pidFile.exists()) {
            return StreamUtil.slurp(new FileReader(pidFile));
        }
        return null;
    }

    protected String getProperty(String key) {
        return rhqctlConfig.getString(key);
    }

    private String getProperty(String key, String defaultValue)  {
        return rhqctlConfig.getString(key, defaultValue);
    }

    protected void putProperty(String key, String value) {
        rhqctlConfig.setProperty(key, value);
    }

    protected File getRhqCtlProperties() {
        String sysprop = System.getProperty("rhqctl.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhqctl.properties-file] is not defined.");
        }

        File file = new File(sysprop);
        if (!file.isFile()) {
            throw new RHQControlException("rhqctl.properties-file has as its values [" + file + "] which is not " +
                "a file.");
        }
        return file;
    }

    protected String getExtension() {
        if (isWindows()) {
            return "bat";
        }
        return "sh";
    }

    protected boolean isWindows() {
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.US);
        return operatingSystem.contains("windows");
    }
}
