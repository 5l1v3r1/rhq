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
package org.rhq.enterprise.agent.promptcmd;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.PrintWriter;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import mazz.i18n.Msg;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.AgentUpdateDownload;
import org.rhq.enterprise.agent.AgentUpdateInformation;
import org.rhq.enterprise.agent.AgentUpdateThread;
import org.rhq.enterprise.agent.AgentUpdateVersion;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Provides functionality around agent updating.
 * 
 * @author John Mazzitelli
 */
public class UpdatePromptCommand implements AgentPromptCommand {

    private static final Msg MSG = AgentI18NFactory.getMsg();

    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.UPDATE);
    }

    public boolean execute(AgentMain agent, String[] args) {
        processArguments(agent, args);
        return true;
    }

    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.UPDATE_SYNTAX);
    }

    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.UPDATE_HELP);
    }

    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.UPDATE_DETAILED_HELP);
    }

    private void processArguments(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (args.length <= 1) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        String sopts = "vudeos";
        LongOpt[] lopts = { new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'), // check the version info
            new LongOpt("update", LongOpt.NO_ARGUMENT, null, 'u'), // updates the agent right now
            new LongOpt("disable", LongOpt.NO_ARGUMENT, null, 'd'), // disable agent updates
            new LongOpt("enable", LongOpt.NO_ARGUMENT, null, 'e'), // enable agent updates
            new LongOpt("download", LongOpt.NO_ARGUMENT, null, 'o'), // downloads the agent update binary
            new LongOpt("status", LongOpt.NO_ARGUMENT, null, 's') // status as to whether its enabled/disabled
        };

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                break;
            }

            case 'u': {
                // this method outputs messages to the user; it also will not return if successful
                AgentUpdateThread.updateAgentNow(agent, true);
                break;
            }

            case 'v': {
                URL url = null;
                try {
                    AgentUpdateVersion check = new AgentUpdateVersion(agent);
                    url = check.getVersionUrl();
                    AgentUpdateInformation info = check.getAgentUpdateInformation();
                    out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_CHECK_INFO, url, info.getUpdateVersion(), info
                        .getUpdateBuild(), info.getAgentVersion(), info.getAgentBuild()));

                    if (info.isAgentOutOfDate()) {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_CHECK_OOD));
                    } else {
                        if (info.isAgentOutOfDateStrict()) {
                            out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_CHECK_OOD_STRICT));
                        } else {
                            out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_CHECK_NOT_OOD));
                        }
                    }
                } catch (Exception e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_CHECK_FAILED, url, e));
                }
                break;
            }

            case 'd': {
                Preferences prefs = agent.getConfiguration().getPreferences();
                String prefName = AgentConfigurationConstants.AGENT_UPDATE_ENABLED;
                boolean prefValue = false;
                prefs.putBoolean(prefName, prefValue);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.CANNOT_STORE_PREFERENCES, prefName, prefValue));
                }
                out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_DISABLED));
                break;
            }

            case 'e': {
                Preferences prefs = agent.getConfiguration().getPreferences();
                String prefName = AgentConfigurationConstants.AGENT_UPDATE_ENABLED;
                boolean prefValue = true;
                prefs.putBoolean(prefName, prefValue);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.CANNOT_STORE_PREFERENCES, prefName, prefValue));
                }
                out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_ENABLED));
                break;
            }

            case 'o': {
                try {
                    AgentUpdateDownload aud = new AgentUpdateDownload(agent);
                    aud.download();
                    aud.validate();
                    out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_DOWNLOADED, aud.getAgentUpdateBinaryFile()));
                } catch (Exception e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_DOWNLOAD_FAILED, ThrowableUtil
                        .getAllMessages(e)));
                }
                break;
            }

            case 's': {
                if (agent.getConfiguration().isAgentUpdateEnabled()) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_ENABLED));
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATE_DISABLED));
                }
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
        }

        return;
    }
}
