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

package org.rhq.enterprise.server.sync.exporters;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.sync.entity.SystemSettings;
import org.rhq.enterprise.server.sync.ExportException;
import org.rhq.enterprise.server.sync.NoSingleEntity;
import org.rhq.enterprise.server.sync.importers.Importer;
import org.rhq.enterprise.server.sync.importers.SystemSettingsImporter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class SystemSettingsExporter implements Exporter<NoSingleEntity, SystemSettings> {

    private Subject subject;
    private SystemManagerLocal systemManager;
    
    private static class SystemSettingsExportingIterator extends JAXBExportingIterator<SystemSettings, SystemSettings> {
        public SystemSettingsExportingIterator(SystemSettings settings) {
            super(Collections.singleton(settings).iterator(), SystemSettings.class);
        }
        
        @Override
        protected SystemSettings convert(SystemSettings object) {
            return object;
        }
        
        @Override
        public String getNotes() {
            return null;
        }
    }
    
    public SystemSettingsExporter() {
        this(LookupUtil.getSystemManager());
    }
    
    public SystemSettingsExporter(SystemManagerLocal systemManager) {
        this.systemManager = systemManager;        
    }
    
    @Override
    public Set<ConsistencyValidator> getRequiredValidators() {
        return Collections.emptySet();
    }
    
    @Override
    public Class<? extends Importer<NoSingleEntity, SystemSettings>> getImporterType() {
        return SystemSettingsImporter.class;
    }
    
    @Override
    public void init(Subject subject) throws ExportException {
        this.subject = subject;
    }

    @Override
    public ExportingIterator<SystemSettings> getExportingIterator() {
        Properties systemProps = systemManager.getSystemConfiguration(subject);
                
        SystemSettings settings = new SystemSettings(systemProps);
        
        return new SystemSettingsExportingIterator(settings);
    }

    @Override
    public String getNotes() {
        return null;
    }

}
