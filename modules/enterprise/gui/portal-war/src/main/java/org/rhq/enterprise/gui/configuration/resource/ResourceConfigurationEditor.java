/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.gui.configuration.resource;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import java.util.HashSet;
import java.util.Set;

@Name("configurationEditor")
@Scope(ScopeType.CONVERSATION)
public class ResourceConfigurationEditor extends ResourceConfigurationViewer {

    private Configuration originalResourceConfiguration;

    private int numberOfFilesModified;

    private Set<String> modifiedFiles = new HashSet<String>();

    @RequestParameter
    private String tab;

    @Override
    protected void doInitialization() {
        if (tab != null) {
            setSelectedTab(tab);
        }

        originalResourceConfiguration = resourceConfiguration.deepCopy(true);
    }

    @Override
    protected void changeToRawTab() {
        resourceConfiguration = translateToRaw();
//        initRawConfigDirectories();
        for (RawConfiguration raw : resourceConfiguration.getRawConfigurations()) {
            RawConfigUIBean uiBean = findRawConfigUIBeanByPath(raw.getPath());
            uiBean.setRawConfiguration(raw);
        }
    }

    private Configuration translateToRaw() {
        ConfigurationManagerLocal configurationMgr = LookupUtil.getConfigurationManager();
        return configurationMgr.translateResourceConfiguration(loggedInUser.getSubject(), resourceId,
            resourceConfiguration, STRUCTURED_MODE);
    }

    @Override
    protected void changeToStructuredTab() {
        resourceConfiguration = translateToStructured();
    }

    private Configuration translateToStructured() {
        ConfigurationManagerLocal configurationMgr = LookupUtil.getConfigurationManager();
        return configurationMgr.translateResourceConfiguration(loggedInUser.getSubject(), resourceId,
            resourceConfiguration, RAW_MODE);
    }

    @Observer("rawConfigUpdate")
    public void rawConfigUpdated(RawConfigUIBean rawConfigUIBean) {
        if (rawConfigUIBean.isModified()) {
            // If the file is modified and not already in the cache, then the file was previously in an unmodified state
            // so we want to increment the number of files modified and put the file in the cache to track its current
            // state.
            if (!modifiedFiles.contains(rawConfigUIBean.getPath())) {
                modifiedFiles.add(rawConfigUIBean.getPath());
                ++numberOfFilesModified;
            }
            // There is kind of an implicit else block to do nothing. If the file is modified and already in the cache,
            // then that means we have already incremented the number of files modified, so we do not need to
            // increment again.
        }
        else if (modifiedFiles.contains(rawConfigUIBean.getPath())) {
            // We fall into this block if the file is not modified and if the cache contains the file, which means it
            // was previously in a modified state; therefore, we remove it from the cache, and decrement the number of
            // files modified.
            modifiedFiles.remove(rawConfigUIBean.getPath());
            --numberOfFilesModified;
        }
    }

    public boolean isDisplayChangedFilesLabel() {
        if (isStructuredMode()) {
            return false;
        }

        if (isRawSupported()) {
            return true;
        }

        return isRawMode();
    }

    public String getModifiedFilesMsg() {
        if (!isDisplayChangedFilesLabel() || numberOfFilesModified == 0) {
            return "";
        }

        if (numberOfFilesModified == 1) {
            return "1 file changed in this configuration";
        }

        return numberOfFilesModified + " files changed in this configuration";
    }
}
