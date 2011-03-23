/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tab.SubTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTab;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedEvent;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSelectedHandler;
import org.rhq.enterprise.gui.coregui.client.components.tab.TwoLevelTabSet;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class AbstractTwoLevelTabSetView<T, U extends Layout> extends LocatableVLayout implements
    BookmarkableView, TwoLevelTabSelectedHandler {

    private String baseViewPath;
    private TwoLevelTabSet tabSet;
    private String tabName;
    private String subTabName;
    private U titleBar;
    protected Set<Permission> globalPermissions;

    public AbstractTwoLevelTabSetView(String locatorId, String baseViewPath) {
        super(locatorId);
        this.baseViewPath = baseViewPath;

        setWidth100();
        setHeight100();

        this.titleBar = createTitleBar();
        addMember(this.titleBar);

        this.tabSet = new TwoLevelTabSet(extendLocatorId("TabSet"));
        this.tabSet.setTabBarPosition(Side.TOP);
        this.tabSet.setWidth100();
        this.tabSet.setHeight100();
        this.tabSet.setEdgeMarginSize(0);
        this.tabSet.setEdgeSize(0);

        List<TwoLevelTab> tabsList = createTabs();
        this.tabSet.setTabs(tabsList.toArray(new TwoLevelTab[tabsList.size()]));

        this.tabSet.addTwoLevelTabSelectedHandler(this);

        addMember(this.tabSet);
    }

    // ------------------ Abstract Methods --------------------
    public abstract Integer getSelectedItemId();

    protected abstract U createTitleBar();

    protected abstract List<TwoLevelTab> createTabs();

    /**
     * TODO
     *
     * @param itemId
     * @param viewPath
     */
    protected abstract void loadSelectedItem(int itemId, ViewPath viewPath);

    protected abstract void updateTabContent(T selectedItem);

    // ---------------------------------------------------------

    protected U getTitleBar() {
        return this.titleBar;
    }

    protected boolean updateTab(TwoLevelTab tab, boolean visible, boolean enabled) {
        if (visible) {
            getTabSet().setTabHidden(tab, false);
            getTabSet().setTabEnabled(tab, enabled);
        } else {
            getTabSet().setTabHidden(tab, true);
        }

        return (visible && enabled);
    }

    protected void updateSubTab(TwoLevelTab tab, SubTab subTab, boolean visible, boolean enabled,
        ViewFactory viewFactory) {
        updateSubTab(tab, subTab, null, visible, enabled, viewFactory);
    }

    protected void updateSubTab(TwoLevelTab tab, SubTab subTab, Canvas canvas, boolean visible, boolean enabled,
        ViewFactory viewFactory) {
        tab.setVisible(subTab, visible);
        if (visible) {
            tab.setSubTabEnabled(subTab, enabled);
            if (enabled) {
                subTab.setCanvas(canvas);
                subTab.setViewFactory(viewFactory);
            }
        }
    }

    // This is invoked by events fired in TwoLevelTabSet whenever a tab/subtab combo has been selected. 
    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {

        // Establishing the proper tabbed view may involve tab add/remove and async loading of content. While doing this 
        // we want to prevent user initiation of another tab change. To block users from clicking tabs we 
        // disable the tab set.  We re-enable the tabset when safe. (see this method and also selectTab()). 

        if (getSelectedItemId() == null) {
            this.tabSet.disable();
            CoreGUI.goToView(History.getToken());

        } else {
            String tabId = tabSelectedEvent.getId();
            String subTabId = tabSelectedEvent.getSubTabId();
            String tabPath = "/" + tabId + "/" + subTabId;
            String path = this.baseViewPath + "/" + getSelectedItemId() + tabPath;

            // If the selected tab or subtab is not already the current history item, the user clicked on the tab, rather
            // than going directly to the tab's URL. In this case, fire a history event to go to the tab and make it the
            // current history item.
            if (!(History.getToken().equals(path) || History.getToken().startsWith(path + "/"))) {
                this.tabSet.disable();
                CoreGUI.goToView(path);

            } else {
                // ensure the tabset is enabled if we're not going to be doing any further tab selection
                this.tabSet.enable();
            }
        }
    }

    public void renderView(final ViewPath viewPath) {

        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = (permissions != null) ? permissions : new HashSet<Permission>();
                renderTabs(viewPath);
            }
        });
    }

    private void renderTabs(final ViewPath viewPath) {

        // e.g. #Resource/10010/Summary/Overview
        //                ^ current path
        final int id = Integer.parseInt(viewPath.getCurrent().getPath());
        viewPath.next();

        if (!viewPath.isEnd()) {
            // e.g. #Resource/10010/Summary/Overview
            //                      ^ current path
            this.tabName = viewPath.getCurrent().getPath();
            viewPath.next();
            if (!viewPath.isEnd()) {
                // e.g. #Resource/10010/Summary/Overview
                //                              ^ current path
                this.subTabName = viewPath.getCurrent().getPath();
                viewPath.next();
            } else {
                this.subTabName = null;
            }
        } else {
            this.tabName = null;
        }

        if (getSelectedItemId() == null || getSelectedItemId() != id) {
            // A different entity (resource or group), load it and try to navigate to the same tabs if possible.
            // Changing entities may change the available tabs as the same tab set may not be supported by the
            // new entity's type. To maintain a valid tab selection for the TabSet, smartgwt will generate an
            // events if the current tab is removed (which can happen say, when navigating from a resource of type A
            // to a resource of type B). We need to ignore tab selection events generated by smartgwt because we
            // handle this at a higher level. To do this we explicitly set events to be be ignored. We re-enable
            // the event handling when safe. (see selectTab()). 
            this.tabSet.setIgnoreSelectEvents(true);
            this.loadSelectedItem(id, viewPath);

        } else {
            // Same Resource - just switch tabs.
            this.selectTab(this.tabName, this.subTabName, viewPath);
        }
    }

    /**
     * Select the tab/subtab with the specified titles (e.g. "Monitoring", "Graphs").
     *
     * @param tabName the title of the tab to select - if null, the default tab (the leftmost non-disabled one) will be selected
     * @param subtabName the title of the subtab to select - if null, the default subtab (the leftmost non-disabled one) will be selected
     * @param viewPath the view path, which may have additional view items to be rendered
     */
    public void selectTab(String tabName, String subtabName, ViewPath viewPath) {
        try {
            TwoLevelTab tab = (tabName != null) ? this.tabSet.getTabByName(tabName) : null;
            SubTab subtab = null;

            // if the requested tab is not available for the tabset then select the defaults (firing a new event) 
            if (tab == null || tab.getDisabled()) {
                this.tabSet.setIgnoreSelectEvents(false);
                subtab = selectDefaultTabAndSubTab();
                return;
            }

            // the tab is available, now get the subtab
            subtab = (subtabName != null) ? tab.getSubTabByName(subtabName) : tab.getDefaultSubTab();

            // due to our attempt to perform sticky tabbing we may request an invalid subtab when
            // switching resources. If the requested subtab is not available the select the default subtab for the tab
            if (subtab == null || tab.getLayout().isSubTabDisabled(subtab)) {
                subtab = tab.getLayout().getDefaultSubTab();
            }

            // select the subTab (this does not fire an event, just sets the subtab)
            tab.getLayout().selectSubTab(subtab);

            // now that the tab/subtab is determined, if currently selected tab is not the desired tab, then
            // select it now (firing a new event).
            if (!tab.equals(this.tabSet.getSelectedTab())) {
                this.tabSet.setIgnoreSelectEvents(false);
                this.tabSet.selectTab(tab);
                return;
            }

            // if the desired tab/subtab are already selected, now we can render the subtab canvas
            Canvas subView = subtab.getCanvas();

            if (subView instanceof BookmarkableView) {
                // Handle any remaining view items (e.g. id of a selected item in a subtab that contains a Master-Details view).
                ((BookmarkableView) subView).renderView(viewPath);
            } else if (subView instanceof RefreshableView && subView.isDrawn()) {
                // Refresh the data on the subtab, so it's not stale.
                Log.debug("Refreshing data for [" + subView.getClass().getName() + "]...");
                ((RefreshableView) subView).refresh();
            }

            // ensure the tabset is enabled (disabled in onTabSelected), and redraw
            this.tabSet.setIgnoreSelectEvents(false);
            this.tabSet.enable();
            this.tabSet.markForRedraw();

        } catch (Exception e) {
            this.tabSet.enable();
            Log.info("Failed to select tab " + tabName + "/" + subtabName + ": " + e);
        }
    }

    private SubTab selectDefaultTabAndSubTab() {
        TwoLevelTab tab = this.tabSet.getDefaultTab();
        if (tab == null) {
            throw new IllegalStateException("No default tab is defined.");
        }

        SubTab subTab = tab.getDefaultSubTab();
        if (subTab == null || tab.getLayout().isSubTabDisabled(subTab)) {
            CoreGUI.getErrorHandler().handleError(MSG.view_tabs_invalidSubTab(subTab.getName()));
            subTab = tab.getLayout().getDefaultSubTab();
        }

        tab.getLayout().selectSubTab(subTab);

        // Now that the subtab has been selected, select the tab (this will cause a tab selected event to fire).
        this.tabSet.selectTab(tab);

        return subTab;
    }

    protected abstract T getSelectedItem();

    public TwoLevelTabSet getTabSet() {
        return tabSet;
    }

    public String getTabName() {
        return tabName;
    }

    public String getSubTabName() {
        return subTabName;
    }

    public String getBaseViewPath() {
        return baseViewPath;
    }

    @Override
    public void destroy() {
        tabSet.destroy();
        super.destroy();
    }
}
