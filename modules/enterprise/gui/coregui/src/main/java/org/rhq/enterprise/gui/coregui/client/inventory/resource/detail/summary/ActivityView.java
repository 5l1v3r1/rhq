/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The content pane of the Resource Summary>Activity tab.
 *
 * @author Ian Springer
 */
// TODO: Implement this.
public class ActivityView extends LocatableVLayout implements RefreshableView {

    private ResourceComposite resourceComposite;
    private FullHTMLPane iFrame;

    public ActivityView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        this.iFrame = new FullHTMLPane(extendLocatorId("IFrame"), null);
        addMember(this.iFrame);

        refresh();
    }

    @Override
    public void refresh() {
        int resourceId = this.resourceComposite.getResource().getId();
        this.iFrame.setContentsURL("/rhq/resource/summary/overview-plain.xhtml?id=" + resourceId);
    }
    
}
