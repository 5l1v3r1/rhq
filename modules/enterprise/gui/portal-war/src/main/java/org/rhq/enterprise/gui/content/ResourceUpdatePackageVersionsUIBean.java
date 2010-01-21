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
package org.rhq.enterprise.gui.content;

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceUpdatePackageVersionsUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ResourceUpdatePackageVersionsUIBean";

    public ResourceUpdatePackageVersionsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResourceUpdatePackageVersionsDataModel(PageControlView.ResourceUpdatePackageVersionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResourceUpdatePackageVersionsDataModel extends PagedListDataModel<PackageVersionComposite> {
        public ResourceUpdatePackageVersionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<PackageVersionComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer resourceId = Integer.parseInt(FacesContextUtility.getRequiredRequestParameter("id"));
            //String filter = FacesContextUtility.getRequiredRequestParameter("contentForm:filter");
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();

            PageList<PackageVersionComposite> results = manager.getUpdatePackageVersionCompositesByFilter(subject,
                resourceId, null, pc);
            return results;
        }
    }

}
