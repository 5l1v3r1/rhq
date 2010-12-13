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
package org.rhq.enterprise.gui.configuration.resource;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.configuration.AbstractAddNewOpenMapMemberPropertyUIBean;

/**
 * @author Ian Springer
 */
@Name("AddNewOpenMapMemberPropertyToResourceConfigurationUIBean")
@Scope(ScopeType.PAGE)
public class ExistingResourceConfigurationAddNewOpenMapMemberPropertyUIBean extends
    AbstractAddNewOpenMapMemberPropertyUIBean {
    
    @In("configurationEditor")
    private ResourceConfigurationEditor currentEditor;
    
    protected Configuration getConfiguration() {
        return currentEditor.getResourceConfiguration();
    }
}