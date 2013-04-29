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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Remove extends ControlCommand {

    private Options options;

    public Remove() {
        options = new Options().addOption(null, "storage", false, "Remove RHQ storage node service")
            .addOption(null, "server", false, "Remove RHQ server service")
            .addOption(null, "agent", false, "Remove RHQ agent service");
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes RHQ services on Windows. Otherwise, same as Stop command";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        try {
            // if no options specified, then stop whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isAgentInstalled()) {
                    removeAgentService();
                }
                if (isServerInstalled()) {
                    removeServerService();
                }
                if (isStorageInstalled()) {
                    removeStorageService();
                }
            } else {
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        removeAgentService();
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        removeServerService();
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        removeStorageService();
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
    }

    private void removeStorageService() throws Exception {
        log.debug("Stopping RHQ storage node");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        if (isWindows()) {
            commandLine = getCommandLine("rhq-storage", "remove");
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                log.debug("Failed to remove storage service", e);
            }
        } else {
            String pid = getStoragePid();
            if (pid != null) {
                System.out.println("Stopping RHQ storage node...");
                System.out.println("RHQ storage node (pid=" + pid + ") is stopping...");

                commandLine = new org.apache.commons.exec.CommandLine("kill").addArgument(pid);
                executor.execute(commandLine);

                System.out.println("RHQ storage node has stopped");
            }

        }
    }

    private void removeServerService() throws Exception {
        log.debug("Stopping RHQ server");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        if (isWindows()) {
            try {
                commandLine = getCommandLine("rhq-server", "remove");
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to remove server service", e);
            }
        } else {
            String pid = getServerPid();

            if (pid != null) {
                commandLine = getCommandLine("rhq-server", "stop");
                executor.execute(commandLine);
            }
        }
    }

    private void removeAgentService() throws Exception {
        log.debug("Stopping RHQ agent");

        File agentBinDir = new File(getAgentBasedir(), "bin");
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(agentBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        if (isWindows()) {
            try {
                commandLine = getCommandLine("rhq-agent-wrapper", "remove");
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist, script returns 1
                log.debug("Failed to remove agent service", e);
            }
        } else {
            String pid = getAgentPid();

            if (pid != null) {
                commandLine = getCommandLine("rhq-agent-wrapper", "stop");
                executor.execute(commandLine);
            }
        }
    }
}
