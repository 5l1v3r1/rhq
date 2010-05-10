/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.bundle.deployment;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentDataSource extends RPCDataSource<BundleDeployment> {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    public BundleDeploymentDataSource() {

        DataSourceIntegerField id = new DataSourceIntegerField("id","ID");
        id.setPrimaryKey(true);
        addField(id);

        DataSourceTextField name = new DataSourceTextField("name", "Deployment Name");
        addField(name);

        DataSourceTextField description = new DataSourceTextField("description", "Description");
        addField(description);

        DataSourceDateTimeField created = new DataSourceDateTimeField("createdTime", "Deployment Time");
        addField(created);

        DataSourceTextField bundleVersion = new DataSourceTextField("bundleVersion", "Bundle Version");
        addField(bundleVersion);


    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();


        if (request.getCriteria().getValues().containsKey("bundleId")) {
            criteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttribute("bundleId")));
        }

        if (request.getCriteria().getValues().containsKey("bundleVersionId")) {
            criteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttribute("bundleVersionId")));
        }

        bundleService.findBundleDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleDeployment>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load bundle deployments",caught);
            }

            public void onSuccess(PageList<BundleDeployment> result) {
                response.setData(buildRecords(result));
                processResponse(request.getRequestId(), response);
            }
        });


    }

    @Override
    public BundleDeployment copyValues(ListGridRecord from) {
        return null;  // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(BundleDeployment from) {

        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("installDir", from.getInstallDir());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("createdTime", new Date(from.getCtime()));
        record.setAttribute("configuration", from.getConfiguration());


        if (from.getBundleVersion() != null) {
            record.setAttribute("bundleVersionVersion", from.getBundleVersion().getVersion());
            record.setAttribute("bundleVersionId", from.getBundleVersion().getId());
        }


        return record;
    }
}
