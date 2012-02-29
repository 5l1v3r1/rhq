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
package org.rhq.enterprise.server.plugins.drift.mongodb;

import org.bson.types.ObjectId;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.testng.annotations.Test;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

public class MapperTest {

    @Test
    public void mapChangeSetToDTO() {
        MongoDBChangeSet changeSet = new MongoDBChangeSet();
        changeSet.setId(new ObjectId());
        changeSet.setCategory(DRIFT);
        changeSet.setDriftDefinitionId(1);
        changeSet.setDriftDefinitionName("testdef");
        changeSet.setResourceId(1);
        changeSet.setVersion(1);
        changeSet.setDriftHandlingMode(normal);

        Mapper mapper = new Mapper();
        DriftChangeSetDTO actual = mapper.toDTO(changeSet);

        DriftChangeSetDTO expected = new DriftChangeSetDTO();
        expected.setId(changeSet.getId());
        expected.setCategory(changeSet.getCategory());
        expected.setCtime(changeSet.getCtime());
        expected.setDriftDefinitionId(changeSet.getDriftDefinitionId());
        expected.setResourceId(changeSet.getResourceId());
        expected.setDriftHandlingMode(changeSet.getDriftHandlingMode());
        expected.setVersion(changeSet.getVersion());

        assertPropertiesMatch(expected, actual, "Failed to map " + MongoDBChangeSet.class.getSimpleName() + " to " +
                DriftChangeSetDTO.class.getSimpleName());
    }

//    @Test
//    public void mapNewEntryToDTO() {
//        MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry("./foo", FILE_ADDED);
//        entry.setNewFileHash("1ab2c34d");
//    }

}
