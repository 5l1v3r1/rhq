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
package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class Table extends LocatableVLayout {

    private static final SelectionEnablement DEFAULT_SELECTION_ENABLEMENT = SelectionEnablement.ALWAYS;

    private HTMLFlow title;

    private Canvas titleComponent;

    private ListGrid listGrid;
    private ToolStrip footer;
    private Label tableInfo;
    private String[] excludedFieldNames;

    private String headerIcon;

    private boolean showHeader = true;
    private boolean showFooter = true;

    /**
     * Specifies how many rows must be selected in order for a {@link TableAction} button to be enabled.
     */
    public enum SelectionEnablement {
        /**
         * Enabled no matter how many rows are selected (zero or more)
         */
        ALWAYS,
        /**
         * One or more rows are selected.
         */
        ANY,
        /**
         * Exactly one row is selected.
         */
        SINGLE,
        /**
         * Two or more rows are selected.
         */
        MULTIPLE,
        /**
         * Never enabled - usually due to the user having a lack of permissions
         */
        NEVER
    }

    ;

    private List<TableActionInfo> tableActions = new ArrayList<TableActionInfo>();
    private List<Canvas> extraWidgets = new ArrayList<Canvas>();

    public Table(String locatorId) {
        this(locatorId, null, null, null, null, true);
    }

    public Table(String locatorId, String tableTitle) {
        this(locatorId, tableTitle, null, null, null, true);
    }

    public Table(String locatorId, String tableTitle, Criteria criteria) {
        this(locatorId, tableTitle, criteria, null, null, true);
    }

    public Table(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers) {
        this(locatorId, tableTitle, null, sortSpecifiers, null, true);
    }

    public Table(String locatorId, String tableTitle, boolean autoFetchData) {
        this(locatorId, tableTitle, null, null, null, autoFetchData);
    }

    public Table(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        this(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames, true);
    }

    public Table(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(locatorId);

        setWidth100();
        setHeight100();

        // Title
        title = new HTMLFlow();
        setTableTitle(tableTitle);

        // Grid
        listGrid = new LocatableListGrid(locatorId);
        if (criteria != null) {
            listGrid.setInitialCriteria(criteria);
        }
        if (sortSpecifiers != null) {
            listGrid.setInitialSort(sortSpecifiers);
        }
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setAutoFetchData(autoFetchData);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);
        listGrid.setResizeFieldsInRealTime(false);
        // By default, SmartGWT will disable any rows that have a record named "enabled" with a value of false - setting
        // these fields to a bogus field name will disable this behavior. Note, setting them to null does *not* disable
        // the behavior.
        listGrid.setRecordEnabledProperty("foobar");
        //listGrid.setRecordCanSelectProperty("foobar");
        listGrid.setRecordEditProperty("foobar");

        // Footer
        footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        tableInfo = new Label("Total: " + listGrid.getTotalRows());

        this.excludedFieldNames = excludedFieldNames;
    }

    @Override
    protected void onInit() {
        super.onInit();

        // NOTE: It is essential that we wait to hide any excluded fields until after super.onDraw() is called, since
        //       super.onDraw() is what actually adds the fields to the ListGrid (based on what fields are defined in
        //       the underlying datasource).
        if (this.excludedFieldNames != null) {
            for (String excludedFieldName : excludedFieldNames) {
                this.listGrid.hideField(excludedFieldName);
            }
        }

        tableInfo.setWrap(false);

    }

    @Override
    protected void onDraw() {
        super.onDraw();

        removeMembers(getMembers());

        if (showHeader) {

            HLayout titleLayout = new HLayout();
            titleLayout.setAutoHeight();
            titleLayout.setAlign(VerticalAlignment.BOTTOM);

            if (headerIcon != null) {
                Img img = new Img(headerIcon, 24, 24);
                img.setPadding(4);
                titleLayout.addMember(img);
            }

            titleLayout.addMember(title);

            if (titleComponent != null) {
                titleLayout.addMember(new LayoutSpacer());
                titleLayout.addMember(titleComponent);
            }

            addMember(titleLayout);
        }

        addMember(listGrid);
        if (showFooter) {

            footer.removeMembers(footer.getMembers());

            for (final TableActionInfo tableAction : tableActions) {
                IButton button = new LocatableIButton(tableAction.getLocatorId(), tableAction.getTitle());
                button.setDisabled(true);
                button.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        if (tableAction.confirmMessage != null) {

                            String message = tableAction.confirmMessage.replaceAll("\\#", String.valueOf(listGrid
                                .getSelection().length));

                            SC.ask(message, new BooleanCallback() {
                                public void execute(Boolean confirmed) {
                                    if (confirmed) {
                                        tableAction.action.executeAction(listGrid.getSelection());
                                    }
                                }
                            });
                        } else {
                            tableAction.action.executeAction(listGrid.getSelection());
                        }
                    }
                });
                tableAction.actionButton = button;
                footer.addMember(button);
            }

            for (Canvas extraWidgetCanvas : extraWidgets) {
                footer.addMember(extraWidgetCanvas);
            }

            footer.addMember(new LayoutSpacer());

            IButton refreshButton = new LocatableIButton("Refresh", "Refresh");
            refreshButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    listGrid.invalidateCache();
                }
            });
            footer.addMember(refreshButton);

            footer.addMember(tableInfo);

            // Manages enable/disable buttons for the grid
            listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    refreshTableInfo();
                }
            });

            listGrid.addDataArrivedHandler(new DataArrivedHandler() {
                public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                    refreshTableInfo();
                    fieldSizes.clear();
                }
            });

            addMember(footer);
        }
    }

    protected void setListGrid(ListGrid listGrid) {
        this.listGrid = listGrid;
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    public boolean isShowFooter() {
        return showFooter;
    }

    public void setShowFooter(boolean showFooter) {
        this.showFooter = showFooter;
    }

    private ArrayList<Integer> fieldSizes = new ArrayList<Integer>();

    public void refresh(Criteria criteria) {
        this.listGrid.invalidateCache();
        this.listGrid.setCriteria(criteria);
        this.listGrid.markForRedraw();
    }

    public void refresh() {
        this.listGrid.invalidateCache();
        this.listGrid.markForRedraw();
    }

    public void setTableTitle(String titleString) {
        if (titleString == null) {
            titleString = "";
        }
        if (titleString.length() > 0) {
            title.setWidth100();
            title.setHeight(35);
            title.setContents(titleString);
            title.setPadding(4);
            title.setStyleName("HeaderLabel");
        } else {
            title.setWidth100();
            title.setHeight(0);
            title.setContents(null);
            title.setPadding(0);
            title.setStyleName("normal");
        }

        title.markForRedraw();
    }

    @SuppressWarnings("unchecked")
    public void setDataSource(RPCDataSource dataSource) {
        listGrid.setDataSource(dataSource);
    }

    @SuppressWarnings("unchecked")
    public RPCDataSource getDataSource() {
        return (RPCDataSource) listGrid.getDataSource();
    }

    public ListGrid getListGrid() {
        return listGrid;
    }

    public void setTitleComponent(Canvas canvas) {
        this.titleComponent = canvas;
    }

    public void addTableAction(String locatorId, String title, TableAction tableAction) {
        this.addTableAction(locatorId, title, null, null, tableAction);
    }

    public void addTableAction(String locatorId, String title, SelectionEnablement enablement, String confirmation,
        TableAction tableAction) {
        if (enablement == null) {
            enablement = DEFAULT_SELECTION_ENABLEMENT;
        }
        TableActionInfo info = new TableActionInfo(locatorId, title, enablement, tableAction);
        info.confirmMessage = confirmation;
        tableActions.add(info);
    }

    public void addExtraWidget(Canvas canvas) {
        this.extraWidgets.add(canvas);
    }

    public String getHeaderIcon() {
        return headerIcon;
    }

    public void setHeaderIcon(String headerIcon) {
        this.headerIcon = headerIcon;
    }

    private void refreshTableInfo() {
        if (showFooter) {
            int count = this.listGrid.getSelection().length;
            for (TableActionInfo tableAction : tableActions) {
                boolean enabled;
                switch (tableAction.enablement) {
                case ALWAYS:
                    enabled = true;
                    break;
                case NEVER:
                    enabled = false;
                    break;
                case ANY:
                    enabled = (count >= 1);
                    break;
                case SINGLE:
                    enabled = (count == 1);
                    break;
                case MULTIPLE:
                    enabled = (count > 1);
                    break;
                default:
                    throw new IllegalStateException("Unhandled SelectionEnablement: " + tableAction.enablement.name());
                }
                tableAction.actionButton.setDisabled(!enabled);
            }
            for (Canvas extraWidget : extraWidgets) {
                if (extraWidget instanceof TableWidget) {
                    ((TableWidget) extraWidget).refresh(this.listGrid);
                }
            }
            this.tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + count + " selected)");
        }
    }

    // -------------- Inner utility class -------------

    private static class TableActionInfo {

        private String locatorId;
        private String title;
        private SelectionEnablement enablement;
        private TableAction action;
        private String confirmMessage;
        private IButton actionButton;

        protected TableActionInfo(String locatorId, String title, SelectionEnablement enablement, TableAction action) {
            this.locatorId = locatorId;
            this.title = title;
            this.enablement = enablement;
            this.action = action;
        }

        public String getLocatorId() {
            return locatorId;
        }

        public String getTitle() {
            return title;
        }

        public SelectionEnablement getEnablement() {
            return enablement;
        }

        public IButton getActionButton() {
            return actionButton;
        }

        String getConfirmMessage() {
            return confirmMessage;
        }

        void setConfirmMessage(String confirmMessage) {
            this.confirmMessage = confirmMessage;
        }

        void setActionButton(IButton actionButton) {
            this.actionButton = actionButton;
        }
    }
}
