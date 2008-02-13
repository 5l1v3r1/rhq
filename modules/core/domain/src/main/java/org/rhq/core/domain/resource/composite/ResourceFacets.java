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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;

/**
 * The set of facets a Resource supports - used to determine which quicknav icons and tabs to display in the UI.
 *
 * @author Ian Springer
 */
public class ResourceFacets implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean measurement;
    private boolean configuration;
    private boolean operation;
    private boolean content;
    private boolean callTime;

    public ResourceFacets(boolean measurement, boolean configuration, boolean operation, boolean content,
        boolean callTime) {
        this.measurement = measurement;
        this.configuration = configuration;
        this.operation = operation;
        this.content = content;
        this.callTime = callTime;
    }

    /**
     * Does this resource expose any metrics? (currently not used for anything in the GUI, since the Monitor and Alert
     * tabs are always displayed).
     *
     * @return true if the resource exposes any metrics, false otherwise
     */
    public boolean isMeasurement() {
        return measurement;
    }

    /**
     * Does this resource expose its configuration? If so, the Configure tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its configuration, false otherwise
     */
    public boolean isConfiguration() {
        return configuration;
    }

    /**
     * Does this resource expose any operations? If so, the Operations tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its operations, false otherwise
     */
    public boolean isOperation() {
        return operation;
    }

    /**
     * Does this resource expose any content? If so, the Content tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its content, false otherwise
     */
    public boolean isContent() {
        return content;
    }

    /**
     * Does this resource expose any call-time metrics? If so, the Call Time sub-tab will be displayed in the GUI.
     *
     * @return true if the resource exposes any call-time metrics, false otherwise
     */
    public boolean isCallTime() {
        return callTime;
    }
}