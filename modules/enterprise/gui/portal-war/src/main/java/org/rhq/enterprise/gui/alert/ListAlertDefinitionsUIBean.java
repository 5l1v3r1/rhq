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
package org.rhq.enterprise.gui.alert;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class ListAlertDefinitionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListAlertDefinitionsUIBean";

    private AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();

    public ListAlertDefinitionsUIBean() {
    }

    public String createNewAlertDefinition() {
        return "createNewAlertDefinition";
    }

    public String deleteSelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        int[] alertDefinitionIds = StringUtility.getIntArray(selectedAlertDefinitions);

        try {
            int deleted = alertDefinitionManager.removeAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + deleted + " alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete alert definitions.", e);
        }

        return "success";
    }

    public String enableSelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        int[] alertDefinitionIds = StringUtility.getIntArray(selectedAlertDefinitions);

        try {
            int enabled = alertDefinitionManager.enableAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled " + enabled + " alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enable alert definitions.", e);
        }

        return "success";
    }

    public String disableSelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        int[] alertDefinitionIds = StringUtility.getIntArray(selectedAlertDefinitions);

        try {
            int disabled = alertDefinitionManager.disableAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled " + disabled + " alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to disable alert definitions.", e);
        }

        return "success";
    }

    public String copySelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        Integer[] alertDefinitionIds = StringUtility.getIntegerArray(selectedAlertDefinitions);

        try {
            alertDefinitionManager.copyAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Copied " + alertDefinitionIds.length
                + " alert definitions into the disabled state.  Edit each as appropriate, and then renable.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to copy alert definitions.", e);
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListAlertDefinitionsDataModel(PageControlView.AlertDefinitionsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListAlertDefinitionsDataModel extends PagedListDataModel<AlertDefinition> {
        public ListAlertDefinitionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AlertDefinition> fetchPage(PageControl pc) {
            PageList<AlertDefinition> results = alertDefinitionManager.findAlertDefinitions(getSubject(), getResource()
                .getId(), pc);
            return results;
        }
    }

    private String[] getSelectedAlertDefinitions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAlertDefinitions");
    }
}