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

package org.rhq.server.control.command;

import java.io.File;
import java.io.FileReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Status extends ControlCommand {

    private Options options;

    public Status() {
        options = new Options().addOption(null, STORAGE_OPTION, false, "Check status of RHQ storage node")
            .addOption(null, SERVER_OPTION, false, "Check status of RHQ server")
            .addOption(null, AGENT_OPTION, false, "Check status of RHQ agent");
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getDescription() {
        return "Check status of RHQ services";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        try {
            // if no options specified, then check the status of whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isStorageInstalled()) {
                    checkStorageStatus();
                }
                if (isServerInstalled()) {
                    checkServerStatus();
                }
                if (isAgentInstalled()) {
                    checkAgentStatus();
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        checkStorageStatus();
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        checkServerStatus();
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        checkAgentStatus();
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to check statuses", e);
        }
    }

    private void checkStorageStatus() throws Exception {
        log.debug("Checking RHQ storage node status");

        if (isWindows()) {
            Executor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler());
            org.apache.commons.exec.CommandLine commandLine;
            executor.setWorkingDirectory(getBinDir());
            commandLine = getCommandLine("rhq-storage", "status");
            try {
                executor.execute(commandLine);

            } catch (Exception e) {
                log.debug("Failed to check storage service status", e);
            }
        } else {

            File storageBinDir = new File(getStorageBasedir(), "bin");
            File pidFile = new File(storageBinDir, "cassandra.pid");

            if (pidFile.exists()) {
                String pid = StreamUtil.slurp(new FileReader(pidFile));
                System.out.println("RHQ storage node (pid " + pid + ") is running");
            } else {
                System.out.println("RHQ storage node (no pid file) is NOT running");
            }
        }
    }

    private void checkServerStatus() throws Exception {
        log.debug("Checking RHQ server status");

        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "status");
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
        executor.setStreamHandler(new PumpStreamHandler());
        executor.execute(commandLine);
    }

    private void checkAgentStatus() throws Exception {
        log.debug("Checking RHQ agent status");

        File agentBinDir = new File(getAgentBasedir(), "bin");

        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "status");
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(agentBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        try {
            executor.execute(commandLine);
        } catch (ExecuteException e) {
            // For windows the JSW exit code for a status check is expected to be a mask value and the agent wrapper
            // .bat will return it explicitly.  We can ignore it and assume that the logged output is sufficient. 
            // See http://wrapper.tanukisoftware.com/doc/english/launch-win.html#standalone-status
            if (!isWindows()) {
                throw e;
            }
        }
    }
}
