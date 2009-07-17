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
package org.rhq.enterprise.client.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.client.TabularWriter;
import org.rhq.enterprise.client.Controller;
import org.rhq.enterprise.client.script.CmdLineParser;
import org.rhq.enterprise.client.script.NamedScriptArg;
import org.rhq.enterprise.client.script.ScriptArg;
import org.rhq.enterprise.client.script.ParseException;
import org.rhq.enterprise.client.script.ScriptCmdLine;
import org.rhq.enterprise.client.utility.PackageFinder;
import org.rhq.enterprise.client.utility.ScriptUtil;

import javax.script.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ScriptCommand implements ClientCommand {

    private ScriptEngineManager sem;
    private ScriptEngine jsEngine;

    private Controller controller;

    private final Log log = LogFactory.getLog(ScriptCommand.class);

    public ScriptCommand() {
        sem = new ScriptEngineManager();
        PageControl pc = new PageControl();
        pc.setPageNumber(-1);
        sem.getBindings().put("unlimitedPC", pc);
        sem.getBindings().put("pageControl", PageControl.getUnlimitedInstance());
        jsEngine = sem.getEngineByName("JavaScript");

        importRecursive(jsEngine);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    private void importRecursive(ScriptEngine jsEngine) {

        try {

            List<String> packages = new PackageFinder().findPackages("org.rhq.core.domain");

            for (String pkg : packages) {
                jsEngine.eval("importPackage(" + pkg + ")");
            }
            

        } catch (ScriptException e) {
            e.printStackTrace();
        }

    }

    public ScriptEngine getScriptEngine() {
        return jsEngine;
    }

    public String getPromptCommandString() {
        return "exec";
    }

    public boolean execute(ClientMain client, String[] args) {
        initBindings(client);

        if (isScriptFileCommandLine(args)) {
            try {
                CmdLineParser cmdLineParser = new CmdLineParser();
                ScriptCmdLine scriptCmdLine = cmdLineParser.parse(args);

                bindScriptArgs(scriptCmdLine);

                return executeScriptFile(new FileReader(scriptCmdLine.getScriptFileName()), client);
            } catch (FileNotFoundException e) {
                client.getPrintWriter().println(e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Unable to locate script file: " + e.getMessage());
                }
            } catch (ParseException e) {
                client.getPrintWriter().println("parse error: " + e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("A parse error occurred.", e);
                }
            }

            return true;
        }

        StringBuilder script = new StringBuilder();
        for (int i = ("exec".equals(args[0]) ? 1 : 0); i < args.length; i++) {
            script.append(args[i]);
            script.append(" ");
        }
        try {

            Object result = jsEngine.eval(script.toString());
            if (result != null) {
                client.getPrintWriter().print("result: ");
                new TabularWriter(client.getPrintWriter()).print(result);
            }
        } catch (ScriptException e) {
            client.getPrintWriter().println(e.getMessage());
            client.getPrintWriter().println(script);
            for (int i = 0; i < e.getColumnNumber(); i++) {
                client.getPrintWriter().print(" ");
            }
            client.getPrintWriter().println("^");
        }
        client.getPrintWriter().println();
        return true;
    }

    public void initBindings(ClientMain client) {
        // These are prepared on every call in case the user logs out and logs into another server
        if (controller.getSubject() != null) {
            sem.getBindings().put("subject", controller.getSubject());
            sem.getBindings().putAll(controller.getManagers());
        }
        TabularWriter tw = new TabularWriter(client.getPrintWriter());
        tw.setWidth(client.getConsoleWidth());
        sem.getBindings().put("pretty", tw);

        bindScriptUtils();

        jsEngine.put("rhq", controller);
    }

    private void bindScriptUtils() {
        ScriptUtil scriptUtil = new ScriptUtil(jsEngine);
        jsEngine.put("scriptUtil", scriptUtil);

        String func = "function isDefined(identifier) { return scriptUtil.isDefined(identifier); }";
        try {
            jsEngine.eval(func);
        } catch (ScriptException e) {
            log.warn("Unable to bind script utility function isDefined()", e);
        }
    }

    private boolean isScriptFileCommandLine(String[] args) {
        if (args == null || args.length < 3) {
            return false;
        }

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-f")) {
                return true;
            }
        }

        return false;

    }

    private void bindScriptArgs(ScriptCmdLine cmdLine) {
        bindArgsArray(cmdLine);

        if (cmdLine.getArgType() == ScriptCmdLine.ArgType.NAMED) {
            bindNamedArgs(cmdLine);    
        }
    }

    private void bindArgsArray(ScriptCmdLine cmdLine) {
        String[] args = new String[cmdLine.getArgs().size()];
        int i = 0;

        for (ScriptArg arg : cmdLine.getArgs()) {
            args[i++] = arg.getValue();
        }

        jsEngine.put("args", args);
    }

    private void bindNamedArgs(ScriptCmdLine cmdLine) {
        for (ScriptArg arg : cmdLine.getArgs()) {
            NamedScriptArg namedArg = (NamedScriptArg) arg;
            jsEngine.put(namedArg.getName(), namedArg.getValue());
        }
    }

    private boolean executeScriptFile(Reader reader, ClientMain client) {
        try {
            Object result = jsEngine.eval(reader);
            if (result != null) {
                client.getPrintWriter().print("result: ");
                new TabularWriter(client.getPrintWriter()).print(result);
            }
        }
        catch (ScriptException e) {
            client.getPrintWriter().println(e.getMessage());
            client.getPrintWriter().println("^");
        }
        return true;
    }

    public String getSyntax() {
        String example = "Ex.  exec 2 + 2";
        example += "\n" + "Ex2. exec -f (path to file)";
        String syntax = "exec <scripting code>";

        return example + "\n" + syntax;

    }

    public String getHelp() {
        return "Excecutes JavaScript commands using the various service interfaces";
    }

    public String getDetailedHelp() {
        StringBuilder help = new StringBuilder();
        help.append("Executes JavaScript commands. You can utilize the following service managers: "
                + RemoteClient.Manager.values());
        return help.toString();
    }


    public ScriptContext getContext() {
        return jsEngine.getContext();
    }
}
