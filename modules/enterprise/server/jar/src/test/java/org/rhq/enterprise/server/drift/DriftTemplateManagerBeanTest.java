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

import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.enterprise.server.util.LookupUtil.getDriftTemplateManager;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.test.TransactionCallback;

public class DriftTemplateManagerBeanTest extends AbstractEJB3Test {

    private final String RESOURCE_TYPE_NAME = DriftTemplateManagerBeanTest.class.getName();

    private ResourceType resourceType;

    private DriftTemplateManagerLocal templateMgr;

    @BeforeClass(groups = "drift-template")
    public void initClass() {
        templateMgr = getDriftTemplateManager();
    }

    @BeforeMethod(groups = "drift-template")
    public void initDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
                createResourceType();
                getEntityManager().persist(resourceType);
            }
        });
    }

    @AfterClass(groups = "drift-template")
    public void resetDB() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
            }
        });
    }

    private void purgeDB() {
        EntityManager em = getEntityManager();

        List results =  em.createQuery("select t from ResourceType t where t.name = :name")
            .setParameter("name", RESOURCE_TYPE_NAME)
            .getResultList();
        if (results.isEmpty()) {
            return;
        }
        ResourceType type = (ResourceType) results.get(0);
        for (DriftDefinitionTemplate template : type.getDriftDefinitionTemplates()) {
            em.remove(template);
        }
        em.remove(type);
    }

    private void createResourceType() {
        resourceType = new ResourceTypeBuilder().createResourceType()
            .withId(0)
            .withName(DriftTemplateManagerBeanTest.class.getName())
            .withCategory(SERVER)
            .withPlugin(DriftTemplateManagerBeanTest.class.getName().toLowerCase())
            .build();
    }

    @Test(groups = "drift-template")
    public void createNewTemplate() {
        final DriftDefinition definition = new DriftDefinition(new Configuration());
        definition.setName("test::createNewTemplate");
        definition.setEnabled(true);
        definition.setDriftHandlingMode(normal);
        definition.setInterval(2400L);
        definition.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, "/foo/bar/test"));

        templateMgr.createTemplate(getOverlord(), resourceType.getId(), definition);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                ResourceType updatedType = em.find(ResourceType.class, resourceType.getId());

                assertEquals("Failed to add new drift definition to resource type", 1,
                    updatedType.getDriftDefinitionTemplates().size());

                DriftDefinitionTemplate newTemplate = updatedType.getDriftDefinitionTemplates().iterator().next();

                DriftDefinitionTemplate expectedTemplate = new DriftDefinitionTemplate();
                expectedTemplate.setTemplateDefinition(definition);

                assertDriftTemplateEquals("Failed to save template", expectedTemplate, newTemplate);
            }
        });
    }

    private Subject getOverlord() {
        return getSubjectManager().getOverlord();
    }

    private void assertDriftTemplateEquals(String msg, DriftDefinitionTemplate expected,
        DriftDefinitionTemplate actual) {
        assertPropertiesMatch(msg + ": basic drift definition template properties do not match", expected,
                    actual, "id", "resourceType", "ctime", "templateDefinition");
                assertDriftDefEquals(msg + ": template definitions do not match", expected.getTemplateDefinition(),
                    actual.getTemplateDefinition());
    }

    private void assertDriftDefEquals(String msg, DriftDefinition expected, DriftDefinition actual) {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        assertEquals(msg, 0, comparator.compare(expected, actual));
    }

}
