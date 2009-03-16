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
package org.rhq.enterprise.gui.configuration.group;

import java.util.Map;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract base class for the Seam components for viewing and editing group Configurations. Requires the 'groupId'
 * request parameter to be specified.
 *
 * @author Ian Springer
 */
public abstract class AbstractGroupResourceConfigurationUIBean
{
    private final Log log = LogFactory.getLog(AbstractGroupResourceConfigurationUIBean.class);

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private ResourceGroup group;
    private Map<Integer, Configuration> resourceConfigurations;
    private ConfigurationSet configurationSet;

    /**
     * Load the ConfigurationDefinition and member Configurations for the current compatible group.
     */
    protected void loadConfigurations()
    {
        try
        {
            this.group = loadGroup();
            this.resourceConfigurations = this.configurationManager.getResourceConfigurationsForCompatibleGroup(
                    EnterpriseFacesContextUtility.getSubject(), this.group.getId());
        }
        catch (RuntimeException e)
        {
            // NOTE: In order for this message to be displayed, an EL expression referencing the managed bean must
            //       be on the page somewhere above the h:messages tag.
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_FATAL, "Failed to load group Resource configuration.",
                    e);
            log.error("Failed to load group Resource configuration.", e);
            return;
        }
        catch (Exception e)
        {
            // NOTE: In order for this message to be displayed, an EL expression referencing the managed bean must
            //       be on the page somewhere above the h:messages tag.
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Cannot load group Resource configuration.", e);
            return;
        }
        this.configurationSet = GroupResourceConfigurationUtility.buildConfigurationSet(
                EnterpriseFacesContextUtility.getSubject(), this.group, this.resourceConfigurations);           
        return;
    }

    private ResourceGroup loadGroup() throws Exception
    {
        ResourceGroup group;
        try
        {
            group = EnterpriseFacesContextUtility.getResourceGroup();
        }
        catch (Exception e)
        {
            throw new Exception("No group is associated with this request ('groupId' request parameter is not set).");
        }
        if (group.getGroupCategory() != GroupCategory.COMPATIBLE)
        {
            throw new Exception("Group with id " + group.getId() + " is not a compatible group.");
        }
        return group;
    }

    public ConfigurationManagerLocal getConfigurationManager()
    {
        return configurationManager;
    }

    public ResourceManagerLocal getResourceManager()
    {
        return resourceManager;
    }

    public ResourceGroup getGroup()
    {
        return group;
    }

    public Map<Integer, Configuration> getResourceConfigurations()
    {
        return resourceConfigurations;
    }

    public ConfigurationSet getConfigurationSet()
    {
        return configurationSet;
    }
}
