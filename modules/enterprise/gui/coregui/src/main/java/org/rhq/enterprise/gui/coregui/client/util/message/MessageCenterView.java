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
package org.rhq.enterprise.gui.coregui.client.util.message;

import java.util.LinkedList;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;

/**
 * @author Greg Hinkle
 */
public class MessageCenterView extends HLayout implements MessageCenter.MessageListener {


    @Override
    protected void onDraw() {
        super.onDraw();
        setAlign(Alignment.LEFT);
        setAlign(VerticalAlignment.CENTER);
        CoreGUI.getMessageCenter().addMessageListener(this);

        final Menu recentEventsMenu = new Menu();
        IMenuButton recentEventsButton = new IMenuButton("Messages",recentEventsMenu);
        recentEventsButton.setShowMenuBelow(false);
        recentEventsButton.setAutoFit(true);
        recentEventsButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                LinkedList<Message> messages = CoreGUI.getMessageCenter().getMessages();
                if (messages.isEmpty()) {
                    recentEventsMenu.setItems(new MenuItem("No recent messages"));
                } else {
                    MenuItem[] items = new MenuItem[messages.size()];
                    int i = 0;
                    for (final Message message : messages) {
                        MenuItem messageItem = new MenuItem(message.title, getSeverityIcon(message.severity));

                        items[i++] = messageItem;

                        messageItem.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                            public void onClick(MenuItemClickEvent event) {
                                showDetails(message);
                            }
                        });
                    }
                    recentEventsMenu.setItems(items);
                }
            }
        });

        addMember(recentEventsButton);

        addMember(new LayoutSpacer());
    }

    private void showDetails(Message message) {
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);

        StaticTextItem title = new StaticTextItem("title","Title");
        title.setValue(message.title);

        StaticTextItem severity = new StaticTextItem("severity","Severity");
        FormItemIcon severityIcon = new FormItemIcon();
        severityIcon.setSrc(getSeverityIcon(message.severity));
        severity.setIcons(severityIcon);
        severity.setValue(message.severity.name());

        StaticTextItem date = new StaticTextItem("time","Time");
        date.setValue(message.fired);

        StaticTextItem detail = new StaticTextItem("detail","Detail");
        detail.setTitleOrientation(TitleOrientation.TOP);
        detail.setValue(message.detail);

        ButtonItem okButton = new ButtonItem("Ok","Ok");
        okButton.setColSpan(2);
        okButton.setAlign(Alignment.CENTER);

        form.setItems(title,severity,date,detail, okButton );

        Window window = new Window();
        window.setTitle(message.title);
        window.setWidth(600);
        window.setHeight(400);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(form);
        window.show();
        okButton.focusInItem();
    }

    public void onMessage(final Message message) {
        final Label label = new Label(message.title);
        label.setMargin(5);
        label.setAutoFit(true);
        label.setHeight(25);
        label.setWrap(false);

        String iconSrc = getSeverityIcon(message.severity);

        label.setIcon(iconSrc);

        label.setTooltip(message.detail);

        label.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                showDetails(message);
            }
        });

        addMember(label,1);
        redraw();

        Timer hideTimer = new Timer() {
            @Override
            public void run() {
                label.animateHide(AnimationEffect.FADE, new AnimationCallback() {
                    public void execute(boolean b) {
                        label.destroy();
                    }
                });
            }
        };
        hideTimer.schedule(10000);
    }

    private String getSeverityIcon(Message.Severity severity) {
        String iconSrc = null;
        switch (severity) {
            case Info:
                iconSrc = "info/icn_info_blue.png";
                break;
            case Warning:
                iconSrc = "info/icn_info_orange.png";
                break;
            case Error:
                iconSrc = "info/icn_info_red.png";
                break;
        }
        return iconSrc;
    }
}
