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
package org.rhq.core.pc.inventory;

import java.util.Set;
import org.rhq.core.domain.resource.Resource;

/**
 * Implementations of this class are notified of changes to the inventory model. Registration of these listeners is done
 * through the {@link InventoryManager#addInventoryEventListener(InventoryEventListener)}.
 *
 * @author Jason Dobies
 */
public interface InventoryEventListener {
    /**
     * Indicates the specified resources were added to the inventory.
     *
     * @param resources resources added to trigger this event; cannot be <code>null</code>
     */
    void resourcesAdded(Set<Resource> resources);

    /**
     * Indicates the specified resources were removed from the inventory.
     *
     * @param resources resources removed to trigger this event; cannot be <code>null</code>
     */
    void resourcesRemoved(Set<Resource> resources);

    /**
     * Indicates a resource has passed all of the necessary approvals and synchronizations to be activated in the plugin
     * container. This may be called for both agent and non-agent mode.
     *
     * @param resource
     *
     * @see   org.rhq.core.pc.inventory.InventoryManager#activateResource(org.rhq.core.domain.resource.Resource,ResourceContainer)
     */
    void resourceActivated(Resource resource);
}