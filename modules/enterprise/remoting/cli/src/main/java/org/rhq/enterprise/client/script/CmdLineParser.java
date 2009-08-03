/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.client.script;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import static org.rhq.enterprise.client.script.ScriptCmdLine.ArgType.INDEXED;
import static org.rhq.enterprise.client.script.ScriptCmdLine.ArgType.NAMED;
import org.rhq.enterprise.client.ClientMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdLineParser {

    public ScriptCmdLine parse(String[] cmdLine) throws CommandLineParseException {
        String[] args = Arrays.copyOfRange(cmdLine, 1, cmdLine.length);
        
        String shortOpts = "-:f:";
        LongOpt[] longOpts = {
                new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                new LongOpt("args-style", LongOpt.REQUIRED_ARGUMENT, null, -2)
        };
        Getopt getopt = new Getopt("exec", args, shortOpts, longOpts, false);

        List<String> scriptArgs = new ArrayList<String>();
        String argStyle = "indexed";
        String scriptName = null;

        int code = getopt.getopt();
        while (code != -1) {
            switch (code) {
                case ':':
                case '?':
                   throw new IllegalArgumentException("Invalid options");
                case 1:
                    scriptArgs.add(getopt.getOptarg());
                    break;
                case 'f':
                    scriptName = getopt.getOptarg();
                    break;
                case -2://ClientMain.ARGS_STYLE_ID:
                    argStyle = getopt.getOptarg();
                    if (isInvalidArgStyle(argStyle)) {
                        throw new CommandLineParseException(argStyle + " - invalid value for style option");
                    }
                    break;
            }
            code = getopt.getopt();
        }

        return createScriptCmdLine(scriptName, argStyle, scriptArgs);
    }

    private boolean isInvalidArgStyle(String argStyle) {
        return !(INDEXED.value().equals(argStyle) || NAMED.value().equals(argStyle));
    }

    private ScriptCmdLine createScriptCmdLine(String scriptName, String argStyle,
            List<String> args) {
        ScriptCmdLine cmdLine = new ScriptCmdLine();
        cmdLine.setScriptFileName(scriptName);

        if (INDEXED.value().equals(argStyle)) {
            cmdLine.setArgType(INDEXED);
            for (String arg : args) {
                cmdLine.addArg(new ScriptArg(arg));
            }
        }
        else {
            cmdLine.setArgType(NAMED);
            for (String arg : args) {
                cmdLine.addArg(parseNamedArg(arg));
            }
        }

        return cmdLine;
    }

    private NamedScriptArg parseNamedArg(String arg) {
        String[] tokens = arg.split("=");
        String name = tokens[0];
        String value = null;

        if (tokens.length > 1) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 1; i < tokens.length; ++i) {
                buffer.append(tokens[i]);
            }
            value = buffer.toString();
        }

        return new NamedScriptArg(name, value);
    }

}
