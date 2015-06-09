/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.core.util.file.FileUtil.writeFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecution.CaptureMode;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.file.FileUtil;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.util.PropertyReplacer;

/**
 * @author Lukas Krejci
 * @author Thomas Segismont
 *
 * @since 4.12
 */
public final class ServerControl {

    private static final Log LOG = LogFactory.getLog(ServerControl.class);

    private static final long MAX_PROCESS_WAIT_TIME = 3600000L;

    private final ServerPluginConfiguration serverPluginConfig;
    private final Configuration pluginConfiguration;
    private final String startScriptPrefix;
    private final Map<String, String> startScriptEnv;
    private final AS7Mode serverMode;
    private final SystemInfo systemInfo;

    private long waitTime;
    private boolean killOnTimeout;

    private ServerControl(Configuration pluginConfiguration, AS7Mode serverMode, SystemInfo systemInfo) {
        this.pluginConfiguration = pluginConfiguration;
        this.serverMode = serverMode;
        this.systemInfo = systemInfo;
        serverPluginConfig = new ServerPluginConfiguration(pluginConfiguration);

        StartScriptConfiguration startScriptConfiguration = new StartScriptConfiguration(pluginConfiguration);

        startScriptPrefix = startScriptConfiguration.getStartScriptPrefix();

        startScriptEnv = startScriptConfiguration.getStartScriptEnv();
        for (String envVarName : startScriptEnv.keySet()) {
            String envVarValue = startScriptEnv.get(envVarName);
            envVarValue = PropertyReplacer.replacePropertyPatterns(envVarValue, pluginConfiguration);
            startScriptEnv.put(envVarName, envVarValue);
        }
    }

    public static ServerControl onServer(Configuration serverPluginConfig, AS7Mode serverMode, SystemInfo systemInfo) {

        return new ServerControl(serverPluginConfig, serverMode, systemInfo);
    }

    /**
     * 0, the default, means waiting forever. Any positive number means waiting for given number of milliseconds
     * and timing out afterwards. Any negative value means timing out immediately.
     */
    public ServerControl waitingFor(long milliseconds) {
        this.waitTime = milliseconds;
        return this;
    }

    public ServerControl killingOnTimeout(boolean kill) {
        killOnTimeout = kill;
        return this;
    }

    /**
     * @return lifecycle methods on the server.
     */
    public Lifecycle lifecycle() {
        return new Lifecycle();
    }

    /**
     * @return cli interface.
     */
    public Cli cli() {
        return new Cli();
    }

    private ProcessExecutionResults execute(String prefix, File executable, String... args) {
        File homeDir = serverPluginConfig.getHomeDir();
        File startScriptFile = executable.isAbsolute() ? executable : new File(homeDir, executable.getPath());

        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(prefix, startScriptFile);

        processExecution.setEnvironmentVariables(startScriptEnv);

        List<String> arguments = processExecution.getArguments();
        if (arguments == null) {
            arguments = new ArrayList<String>();
            processExecution.setArguments(arguments);
        }

        for (String arg : args) {
            if (arg != null) {
                arguments.add(arg);
            }
        }

        // When running on Windows 9x, standalone.bat and domain.bat need the cwd to be the AS bin dir in order to find
        // standalone.bat.conf and domain.bat.conf respectively.
        processExecution.setWorkingDirectory(startScriptFile.getParent());
        processExecution.setCaptureMode(CaptureMode.memory());
        processExecution.setWaitForCompletion(MAX_PROCESS_WAIT_TIME);
        processExecution.setKillOnTimeout(false);

        if (waitTime > 0) {
            processExecution.setWaitForCompletion(waitTime);
        } else if (waitTime < 0) {
            processExecution.setWaitForCompletion(0);
        }

        processExecution.setKillOnTimeout(killOnTimeout);

        if (LOG.isDebugEnabled()) {
            LOG.debug("About to execute the following process: [" + processExecution + "]");
        }

        return systemInfo.executeProcess(processExecution);
    }

    public final class Lifecycle {
        /**
         * This command ignores the timeout set by the {@link #waitingFor(long)} method. It starts the process and returns
         * immediately. Other means have to be used to determine if the server finished starting up.
         */
        public ProcessExecutionResults startServer() {
            StartScriptConfiguration startScriptConfiguration = new StartScriptConfiguration(pluginConfiguration);
            File startScriptFile = startScriptConfiguration.getStartScript();

            if (startScriptFile == null) {
                startScriptFile = new File("bin", serverMode.getStartScriptFileName());
            }

            List<String> startScriptArgsL = startScriptConfiguration.getStartScriptArgs();
            String[] startScriptArgs = startScriptArgsL.toArray(new String[startScriptArgsL.size()]);

            for (int i = 0; i < startScriptArgs.length; ++i) {
                startScriptArgs[i] = PropertyReplacer.replacePropertyPatterns(startScriptArgs[i], pluginConfiguration);
            }

            long origWaitTime = waitTime;
            try {
                //we really don't want to wait for the server start, because, hopefully, it will keep on running ;)
                waitTime = -1;
                return execute(startScriptPrefix, startScriptFile, startScriptArgs);
            } finally {
                waitTime = origWaitTime;
            }
        }

        public ProcessExecutionResults shutdownServer() {
            String command = "shutdown";

            if (serverMode == AS7Mode.DOMAIN) {
                ASConnection connection = new ASConnection(ASConnectionParams.createFrom(serverPluginConfig));
                String host = BaseServerComponent.findASDomainHostName(connection);
                command += " --host=" + host;
                connection.shutdown();
            }

            return cli().disconnected(false).executeCliCommand(command);
        }
    }

    public final class Cli {
        private boolean disconnected;

        Cli() {
            // When running the CLI on Windows, make sure no "pause" message is shown after script execution
            // Otherwise the CLI process will just keep running so we'll never get the exit code
            if (systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
                startScriptEnv.put("NOPAUSE", "1");
            }
        }

        public Cli disconnected(boolean disconnected) {
            this.disconnected = disconnected;
            return this;
        }

        /**
         * Runs (a series of) CLI commands against the server.
         * The commands are separated by either a newline or a comma (or a mix thereof).
         *
         * @param commands the commands to execute in order
         * @return the execution results
         */
        public ProcessExecutionResults executeCliCommand(String commands) {
            File executable = new File("bin", serverMode.getCliScriptFileName());
            String connect = disconnected ? null : "--connect";
            boolean local = serverPluginConfig.isNativeLocalAuth();
            commands = commands.replace('\n', ',');
            String user = disconnected || local ? null : "--user=" + serverPluginConfig.getUser();
            String password = disconnected || local ? null : "--password=" + serverPluginConfig.getPassword();
            String controller = disconnected ? null : "--controller=" + serverPluginConfig.getNativeHost() + ":"
                + serverPluginConfig.getNativePort();

            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) {
                return execute(null, executable, connect, commands, user, password, controller);
            }
            WinCliHelper cliHelper = new WinCliHelper(executable, connect, commands, user, password, controller);
            return cliHelper.execute();
        }

        /**
         * Runs the provided script against the server.
         *
         * @param scriptFile the script file to run
         * @return the execution results
         */
        public ProcessExecutionResults executeCliScript(File scriptFile) {
            File homeDir = serverPluginConfig.getHomeDir();
            File script = scriptFile;
            if (!script.isAbsolute()) {
                script = new File(homeDir, scriptFile.getPath());
            }
            File executable = new File("bin", serverMode.getCliScriptFileName());
            String connect = disconnected ? null : "--connect";
            boolean local = serverPluginConfig.isNativeLocalAuth();
            String file = "--file=" + script.getAbsolutePath();
            String user = disconnected || local ? null : "--user=" + serverPluginConfig.getUser();
            String password = disconnected || local ? null : "--password=" + serverPluginConfig.getPassword();
            String controller = disconnected ? null : "--controller=" + serverPluginConfig.getNativeHost() + ":"
                + serverPluginConfig.getNativePort();

            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) {
                return execute(null, executable, connect, file, user, password, controller);
            }
            WinCliHelper cliHelper = new WinCliHelper(executable, connect, file, user, password, controller);
            return cliHelper.execute();
        }
    }

    private class WinCliHelper {
        static final String JBOSS_CLI_WRAPPER_BAT = "jboss-cli-wrapper.bat";
        static final String CLI_WRAPPER = "cli-wrapper";
        static final String BAT = ".bat";

        final File executable;
        final String[] args;

        protected WinCliHelper(File executable, String... args) {
            this.executable = executable;
            this.args = args;
        }

        ProcessExecutionResults execute() {
            String prefix = null;
            File windowsCliWrapper = null;
            // Calling "cmd /c jboss-cli.bat" will always return 0 even if an error occurs in the CLI
            // Calling it through the wrapper solves the issue
            // See WFLY-3578 https://issues.jboss.org/browse/WFLY-3578
            try {
                windowsCliWrapper = File.createTempFile(CLI_WRAPPER, BAT);
                writeFile(Cli.class.getClassLoader().getResourceAsStream(JBOSS_CLI_WRAPPER_BAT), windowsCliWrapper);
                prefix = windowsCliWrapper.getAbsolutePath();
            } catch (IOException e) {
                LOG.warn("Could not create EAP CLI Wrapper. The CLI exit code may not be reported correctly", e);
            }
            try {
                return ServerControl.this.execute(prefix, executable, args);
            } finally {
                FileUtil.purge(windowsCliWrapper, true);
            }
        }

    }
}
