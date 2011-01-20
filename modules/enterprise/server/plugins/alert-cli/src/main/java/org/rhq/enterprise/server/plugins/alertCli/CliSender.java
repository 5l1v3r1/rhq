/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.alertCli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Uses CLI to perform the alert notification.
 *
 * @author Lukas Krejci
 */
public class CliSender extends AlertSender<ServerPluginComponent> {
    private static final Log LOG = LogFactory.getLog(CliSender.class);

    public SenderResult send(Alert alert) {
        SenderResult result = new SenderResult();
        BufferedReader reader = null;
        try {
            ScriptEngine engine = getScriptEngine(alert);

            PropertySimple packageIdProp = alertParameters.getSimple("packageId");

            if (packageIdProp == null) {
                return SenderResult
                    .getSimpleFailure("The configuration doesn't contain the mandatory 'packageId' property. This should not happen.");
            }

            Integer packageId = packageIdProp.getIntegerValue();

            if (packageId == null) {
                return SenderResult.getSimpleFailure("No script defined.");
            }

            InputStream packageBits = getPackageBits(packageId);

            reader = new BufferedReader(new InputStreamReader(packageBits));

            Object ret = engine.eval(reader);

            //TODO what to do with the return value, if any

            return result;
        } catch (Exception e) {
            //TODO I don't like this
            return SenderResult.getSimpleFailure(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error("Failed to close the script reader.", e);
                }
            }
        }
    }

    private ScriptEngine getScriptEngine(Alert alert) throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        LocalClient client = new LocalClient(overlord);

        //TODO define some meaningful printwriter
        StandardBindings bindings = new StandardBindings(null, client);
        bindings.put("alert", alert);

        return ScriptEngineFactory.getScriptEngine("JavaScript", new PackageFinder(Collections.<File> emptyList()),
            bindings);
    }

    private InputStream getPackageBits(int packageId) throws IOException {
        final ContentSourceManagerLocal csm = LookupUtil.getContentSourceManager();
        final PackageVersion versionToUse = csm.getLatestPackageVersion(packageId, null);

        PipedInputStream ret = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(ret);

        Thread reader = new Thread(new Runnable() {
            public void run() {
                try {
                    csm.outputPackageVersionBits(versionToUse, out);
                } catch (RuntimeException e) {
                    LOG.warn("The thread for reading the bits of package version [" + versionToUse
                        + "] failed with exception.", e);
                    throw e;
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        //doesn't happen in piped output stream
                        LOG.error(
                            "Failed to close the piped output stream receiving the package bits of package version "
                                + versionToUse + ". This should never happen.", e);
                    }
                }
            }
        });
        reader.setName("CLI Alert download thread for package version " + versionToUse);
        reader.setDaemon(true);
        reader.start();

        return ret;
    }
}
