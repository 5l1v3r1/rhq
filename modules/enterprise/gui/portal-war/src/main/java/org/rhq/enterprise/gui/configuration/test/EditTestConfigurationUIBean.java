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
package org.rhq.enterprise.gui.configuration.test;

import javax.faces.application.FacesMessage;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.Outcomes;

/**
 * @author Ian Springer
 */
public class EditTestConfigurationUIBean extends AbstractTestConfigurationUIBean {
    public static final String MANAGED_BEAN_NAME = "EditTestConfigurationUIBean";
    protected static final String SUCCESS_OUTCOME = "success";
    protected static final String FAILURE_OUTCOME = "failure";

    public String updateConfiguration() {
        // Any values changed in the group config (i.e. via the inputs on the main page) need to be
        // applied to all member configs before persisting them.
        getConfigurationSet().applyGroupConfigurationForUpdate();
        // TODO (low priority): Persist the test config somewhere.
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration updated.");
        return Outcomes.SUCCESS;
    }
}