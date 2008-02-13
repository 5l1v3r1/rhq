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
package org.rhq.core.domain.configuration.group;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorValue("resource")
@Entity
public class AggregateResourceConfigurationUpdate extends AbstractAggregateConfigurationUpdate {
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "aggregateConfigurationUpdate", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<ResourceConfigurationUpdate> configurationUpdates = new ArrayList<ResourceConfigurationUpdate>();

    protected AggregateResourceConfigurationUpdate() {
    } // JPA

    public AggregateResourceConfigurationUpdate(ResourceGroup group, Configuration configuration, String subjectName) {
        super(group, configuration, subjectName);
    }

    public void setConfigurationUpdates(List<ResourceConfigurationUpdate> configurationUpdates) {
        this.configurationUpdates = configurationUpdates;
    }

    public List<ResourceConfigurationUpdate> getConfigurationUpdates() {
        return this.configurationUpdates;
    }

    public void addConfigurationUpdates(ResourceConfigurationUpdate groupMember) {
        this.configurationUpdates.add(groupMember);
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", resourceConfigurationUpdates=").append(getConfigurationUpdates());
    }
}