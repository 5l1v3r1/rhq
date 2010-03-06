/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.alert;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.alert.AbstractAlertDataSource;
import org.rhq.enterprise.gui.coregui.client.alert.AbstractAlertsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

/**
 * @author Ian Springer
 */
public class ResourceAlertHistoryView extends AbstractAlertsView
        implements ResourceSelectListener {
    private int resourceId;

    public ResourceAlertHistoryView(int resourceId) {
        super();
        this.resourceId = resourceId;
    }

    @Override
    protected void onInit() {
        super.onInit();
    }

    @Override
    protected AbstractAlertDataSource createDataSource() {
        return new ResourceAlertDataSource(this.resourceId);
    }

    public void onResourceSelected(Resource resource) {
        this.resourceId = resource.getId();        
        markForRedraw();
    }
}
