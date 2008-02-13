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
package org.rhq.enterprise.gui.operation.definition.group;

import java.util.List;
import javax.faces.model.SelectItem;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.operation.definition.OperationDefinitionUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

public class ResourceGroupOperationDefinitionUIBean extends OperationDefinitionUIBean {
    private List<SelectItem> resourceExecutionOptions;
    private String resourceExecutionOption;
    private List<IntegerOptionItem> resourceNameItems;
    private boolean haltOnFailure = false;

    public ResourceGroupOperationDefinitionUIBean() {
        resourceExecutionOptions = ResourceGroupOperationDefinitionUtils.getResourceExecutionOptions();
        resourceExecutionOption = ResourceGroupOperationDefinitionUtils.getDefaultExecutionOption();
    }

    @Override
    protected String getBeanName() {
        return "ResourceGroupOperationDefinitionUIBean";
    }

    @Override
    public PageList<OperationDefinition> getOperationDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

        List<OperationDefinition> definitions = operationManager.getSupportedGroupOperations(subject, resourceGroup
            .getId());

        return new PageList<OperationDefinition>(definitions, new PageControl(0, definitions.size()));
    }

    public List<IntegerOptionItem> getResourceNameItems() {
        if (resourceNameItems == null) {
            ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

            this.resourceNameItems = operationManager.getResourceNameOptionItems(resourceGroup.getId());
        }

        return resourceNameItems;
    }

    public void setResourceNameItems(List<IntegerOptionItem> names) {
        this.resourceNameItems = names;
    }

    public List<SelectItem> getResourceExecutionOptions() {
        return resourceExecutionOptions;
    }

    public String getResourceExecutionOption() {
        return resourceExecutionOption;
    }

    public void setResourceExecutionOption(String resourceExecutionOption) {
        this.resourceExecutionOption = resourceExecutionOption;
    }

    public boolean isConcurrent() {
        return ResourceGroupExecutionType.concurrent.name().equals(resourceExecutionOption);
    }

    public boolean getHaltOnFailure() {
        return isHaltOnFailure();
    }

    public boolean isHaltOnFailure() {
        return haltOnFailure;
    }

    public void setHaltOnFailure(boolean haltOnFailure) {
        this.haltOnFailure = haltOnFailure;
    }
}