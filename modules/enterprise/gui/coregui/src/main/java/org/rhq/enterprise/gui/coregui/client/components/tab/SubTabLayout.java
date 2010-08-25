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
package org.rhq.enterprise.gui.coregui.client.components.tab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class SubTabLayout extends LocatableVLayout {

    private ToolStrip buttonBar;

    private LinkedHashMap<String, SubTab> subtabs = new LinkedHashMap<String, SubTab>();
    private HashMap<String, Button> subTabButtons = new HashMap<String, Button>();
    private Set<String> disabledSubTabs = new HashSet<String>();

    SubTab currentlyDisplayed;
    String currentlySelected;
    int currentIndex = 0;

    public SubTabLayout(String locatorId) {
        super(locatorId);
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();
        setMargin(0);
        setPadding(0);

        buttonBar = new ToolStrip();
        buttonBar.setBackgroundColor("grey");
        buttonBar.setWidth100();
        buttonBar.setBorder(null);
        buttonBar.setMembersMargin(30);

        addMember(buttonBar);

        int i = 0;

        for (final String locatorId : subtabs.keySet()) {

            SubTab subTab = subtabs.get(locatorId);

            if (currentlySelected == null) {
                currentlyDisplayed = subTab;
                currentlySelected = locatorId;
            }

            Button button = new LocatableButton(locatorId, subTab.getTitle());
            button.setShowRollOver(false);
            button.setActionType(SelectionType.RADIO);
            button.setRadioGroup("subtabs");
            button.setBorder(null);
            button.setAutoFit(true);
            if (disabledSubTabs.contains(locatorId)) {
                button.disable();
            } else {
                button.enable();
            }

            button.setBaseStyle("SubTabButton");

            //            button.setStyleName("SubTabButton");
            //            button.setStylePrimaryName("SubTabButton");

            final Integer index = i++;

            button.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    currentlySelected = locatorId;
                    currentIndex = index;
                    fireSubTabSelection();
                    draw(subtabs.get(locatorId));
                }
            });

            subTabButtons.put(locatorId, button);

            buttonBar.addMember(button);
        }

        // Initial settings
        selectTabByLocatorId(currentlySelected);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        selectTabByLocatorId(currentlySelected);
    }

    public void enableSubTab(String title) {
        disabledSubTabs.remove(title);
        if (subTabButtons.containsKey(title)) {
            subTabButtons.get(title).enable();
            markForRedraw();
        }
    }

    public void disableSubTab(String locatorId) {
        disabledSubTabs.add(locatorId);
        if (subTabButtons.containsKey(locatorId)) {
            subTabButtons.get(locatorId).disable();
            markForRedraw();
        }
    }

    public void updateSubTab(SubTab subTab) {
        // Destroy old views so they don't leak
        Canvas oldCanvas = subTab.getCanvas();
        if (oldCanvas != null) {
            oldCanvas.destroy();
        }

        String locatorId = subTab.getLocatorId();
        subtabs.put(locatorId, subTab);
        if (isDrawn() && locatorId.equals(currentlySelected)) {
            draw(subTab);
        }
    }

    private void draw(SubTab subTab) {
        //        if (currentlyDisplayed != null) {
        //            currentlyDisplayed.getCanvas().hide();
        //            //            removeMember(currentlyDisplayed);
        //        }

        Canvas canvas = subTab.getCanvas();
        if (canvas != null) {
            if (hasMember(canvas)) {
                canvas.show();
            } else {
                if (!canvas.isCreated()) {
                    canvas.setOverflow(Overflow.SCROLL);
                }
                addMember(canvas);
                markForRedraw();
            }
            currentlyDisplayed = subTab;
        }
    }

    public void unregisterAllSubTabs() {
        subtabs.clear();
    }

    public void registerSubTab(SubTab subTab) {
        String locatorId = subTab.getLocatorId();

        if (currentlySelected == null) {
            currentlySelected = locatorId;
        }
        subtabs.put(locatorId, subTab);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean selectTabByLocatorId(String locatorId) {
        boolean foundTab = false;
        currentlySelected = locatorId;
        int i = 0;
        for (String sub : subtabs.keySet()) {
            if (sub.equals(locatorId)) {
                currentIndex = i;
                foundTab = true;
                break;
            }
            i++;
        }

        if (isDrawn()) {
            ((Button) buttonBar.getMember(currentIndex)).select();
            draw(subtabs.get(locatorId));
        }

        return foundTab;
    }

    public boolean selectTab(String title) {
        boolean foundTab = false;
        int i = 0;
        for (String sub : subtabs.keySet()) {
            SubTab subtab = subtabs.get(sub);
            if (subtab.getTitle().equals(title)) {
                this.currentlySelected = subtab.getLocatorId();
                currentIndex = i;
                foundTab = true;
                break;
            }
            i++;
        }

        if (isDrawn()) {
            ((Button) buttonBar.getMember(currentIndex)).select();
            draw(subtabs.get(currentlySelected));
        }

        return foundTab;
    }

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager hm = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return hm.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    public void fireSubTabSelection() {
        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent("?", currentlySelected, -1, currentIndex,
            currentlyDisplayed.getCanvas());
        hm.fireEvent(event);
    }

    public Canvas getCurrentCanvas() {
        return currentlyDisplayed != null ? currentlyDisplayed.getCanvas() : subtabs.get(currentlySelected).getCanvas();
    }

    /**
     * Destroy all the currently held views so that they can be replaced with new versions
     */
    public void destroyViews() {
        for (SubTab subtab : subtabs.values()) {
            if (subtab.getCanvas() != null) {
                subtab.getCanvas().destroy();
            }
        }
    }
}