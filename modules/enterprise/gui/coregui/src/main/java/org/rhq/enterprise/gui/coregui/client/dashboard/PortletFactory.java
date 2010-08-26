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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.queue.AutodiscoveryPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.FavoriteResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformPortletView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported.RecentlyAddedView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.InventorySummaryView;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary.TagCloudPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MashupPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util.MessagePortlet;

/**
 * @author Greg Hinkle
 */
public class PortletFactory {

    private static Map<String, PortletViewFactory> registeredPortlets;

    static {
        registeredPortlets = new HashMap<String, PortletViewFactory>();

        registeredPortlets.put(InventorySummaryView.KEY, InventorySummaryView.Factory.INSTANCE);
        registeredPortlets.put(RecentlyAddedView.KEY, RecentlyAddedView.Factory.INSTANCE);
        registeredPortlets.put(PlatformPortletView.KEY, PlatformPortletView.Factory.INSTANCE);

        registeredPortlets.put(AutodiscoveryPortlet.KEY, AutodiscoveryPortlet.Factory.INSTANCE);

        registeredPortlets.put(RecentAlertsPortlet.KEY, RecentAlertsPortlet.Factory.INSTANCE);

        registeredPortlets.put(GraphPortlet.KEY, GraphPortlet.Factory.INSTANCE);

        registeredPortlets.put(TagCloudPortlet.KEY, TagCloudPortlet.Factory.INSTANCE);

        registeredPortlets.put(FavoriteResourcesPortlet.KEY, FavoriteResourcesPortlet.Factory.INSTANCE);

        registeredPortlets.put(MashupPortlet.KEY, MashupPortlet.Factory.INSTANCE);
        registeredPortlets.put(MessagePortlet.KEY, MessagePortlet.Factory.INSTANCE);
        registeredPortlets.put(ProblemResourcesPortlet.KEY, ProblemResourcesPortlet.Factory.INSTANCE);
        registeredPortlets.put(OperationsPortlet.KEY, OperationsPortlet.Factory.INSTANCE);
    }

    public static Portlet buildPortlet(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        PortletViewFactory viewFactory = registeredPortlets.get(storedPortlet.getPortletKey());

        Canvas canvas = null;
        Portlet view = viewFactory.getInstance();
        view.configure(portletWindow, storedPortlet);

        return view;

    }

    public static List<String> getRegisteredPortlets() {

        ArrayList portlets = new ArrayList(registeredPortlets.keySet());
        Collections.sort(portlets);
        return portlets;
    }
}
