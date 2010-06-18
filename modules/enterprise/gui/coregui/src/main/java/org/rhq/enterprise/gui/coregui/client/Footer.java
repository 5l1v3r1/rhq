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
package org.rhq.enterprise.gui.coregui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripSeparator;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.footer.FavoritesButton;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenterView;

/**
 * @author Greg Hinkle
 */
public class Footer extends ToolStrip {

    MessageCenterView recentMessage;

    public Footer() {
        super();
        setHeight(30);
        setAlign(VerticalAlignment.CENTER);
//        setPadding(5);
        setWidth100();
        setMembersMargin(15);
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(new Label("Welcome to RHQ"));
        addMember(new ToolStripSeparator());

        recentMessage = new MessageCenterView();
        recentMessage.setWidth("*");

        addMember(recentMessage);

        addMember(new ToolStripSeparator());

        addMember(new FavoritesButton());

        addMember(new AlertsMessage());


    }


    public static class AlertsMessage extends Label {
        public AlertsMessage() {
            setHeight(30);
            setPadding(5);

            setIcon("subsystems/alert/Alert_LOW_16.png");
            setIconSize(16);
            setWrap(false);
        }

        @Override
        protected void onInit() {
            super.onInit();

            refresh();

            Timer t = new Timer() {
                public void run() {
                    refresh();
                }
            };

            // refresh every minute
            t.scheduleRepeating(60000);

            addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    History.newItem("Subsystems/Alerts");
                }
            });
        }

        public void refresh() {

            AlertCriteria alertCriteria = new AlertCriteria();
            alertCriteria.setPaging(1,1);
            // last eight hours
            alertCriteria.addFilterStartTime(System.currentTimeMillis() - (1000L * 60 * 60 * 8));

            GWTServiceLookup.getAlertService().findAlertsByCriteria(alertCriteria, new AsyncCallback<PageList<Alert>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Latest alerts lookup failed", caught);
                }

                public void onSuccess(PageList<Alert> result) {
                    drawAlerts(result);
                }
            });
        }

        public void drawAlerts(PageList<Alert> alerts) {
            if (alerts.isEmpty()) {
                setContents("no recent alerts");
                setIcon("subsystems/alert/Alert_LOW_16.png");

            } else {

                setContents(alerts.getTotalSize() + " recent alerts");
                setIcon("subsystems/alert/Alert_HIGH_16.png");
            }
        }
    }


}
