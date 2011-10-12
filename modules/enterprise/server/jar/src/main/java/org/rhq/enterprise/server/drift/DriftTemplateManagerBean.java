/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.drift;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

@Stateless
public class DriftTemplateManagerBean implements DriftTemplateManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    ResourceTypeManagerLocal resourceTypeMgr;

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @Override
    public void createTemplate(Subject subject, int resourceTypeId, DriftDefinition definition) {
        try {
            ResourceType resourceType = resourceTypeMgr.getResourceTypeById(subject, resourceTypeId);
            DriftDefinitionTemplate template = new DriftDefinitionTemplate();
            template.setName(definition.getName());
            //template.setDescription(definition.);
            template.setConfiguration(definition.getConfiguration().deepCopyWithoutProxies());

            resourceType.addDriftDefinitionTemplate(template);
        } catch (ResourceTypeNotFoundException e) {
        }
    }

}
