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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;

/**
 * @author Greg Hinkle
 */
public class AutodiscoveryPortlet extends ResourceAutodiscoveryView implements Portlet {

    public static final String KEY = "Discovery Queue";

    public AutodiscoveryPortlet() {
        super(true);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        // TODO: Implement this method.
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet offers the ability to import newly discovered resources into the inventory " +
                "for monitoring and management or to ingnore them from further action.");
    }

    public DynamicForm getCustomSettingsForm() {
        return null;  // TODO: Implement this method.
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance() {
            return GWT.create(AutodiscoveryPortlet.class);
        }
    }
}
