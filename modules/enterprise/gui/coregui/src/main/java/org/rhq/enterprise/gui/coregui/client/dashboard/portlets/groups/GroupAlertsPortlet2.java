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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups;

import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.alert.AlertPortletDataSource;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.PortletAlertSelector;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Simeon Pinder
 */
public class GroupAlertsPortlet2 extends AlertHistoryView implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "Group: Alerts2";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_recentAlerts();

    //widget keys also used in form population
    public static final String ALERT_RANGE_DISPLAY_AMOUNT_VALUE = "alert-range-display-amount-value";
    public static final String ALERT_RANGE_PRIORITY_VALUE = "alert-range-priority-value";
    public static final String ALERT_RANGE_TIME_VALUE = "alert-range-time-value";
    public static final String ALERT_RANGE_RESOURCES_VALUE = "alert-range-resource-value";
    public static final String ALERT_RANGE_RESOURCE_IDS = "alert-range-resource-ids";
    //configuration default information
    private static final String defaultAlertCountValue = "5";
    private static final String PRIORITY_ALL = MSG.common_label_all();
    private static final String PRIORITY_HIGH = AlertPriority.HIGH.getDisplayName();
    private static final String PRIORITY_MEDIUM = AlertPriority.MEDIUM.getDisplayName();
    private static final String PRIORITY_LOW = AlertPriority.LOW.getDisplayName();
    private static final String defaultPriorityValue = PRIORITY_ALL;
    private static final String TIME_30_MINS = "30 " + MSG.common_label_minutes();
    private static final String TIME_HOUR = MSG.common_label_hour();
    private static final String TIME_12_HRS = "12 " + MSG.common_label_hours();
    private static final String TIME_DAY = MSG.common_label_day();
    private static final String TIME_WEEK = MSG.common_label_week();
    private static final String TIME_MONTH = MSG.common_label_month();
    private static final String defaultTimeValue = TIME_DAY;
    public static final String RESOURCES_ALL = MSG.common_label_all_resources();
    public static final String RESOURCES_SELECTED = MSG.common_label_selected_resources();
    public static final String defaultResourceValue = RESOURCES_ALL;
    private static final String unlimited = MSG.common_label_unlimited();
    //alert resource labels
    public static final String ALERT_LABEL_SELECTED_RESOURCES = MSG.common_title_selected_resources();
    public static final String ALERT_LABEL_AVAILABLE_RESOURCES = MSG.common_title_available_resources();
    public static final String ALERT_LABEL_RESOURCE_INVENTORY = MSG.common_title_resource_inventory();
    public static final int ALERT_RESOURCE_SELECTION_WIDTH = 800;
    public static final String ID = "id";

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;

    //shared private UI elements
    private AlertResourceSelectorRegion resourceSelector;

    private AlertPortletDataSource dataSource;
    //instance ui widgets
    private Canvas containerCanvas;

    private Timer refreshTimer;

    public GroupAlertsPortlet2(String locatorId) {
        super(locatorId);

        //override the shared datasource
        this.dataSource = new AlertPortletDataSource();
        setDataSource(this.dataSource);

        setShowHeader(false);
        setShowFooter(true);
        setShowFooterRefresh(false); //disable footer refresh
        setShowFilterForm(false); //disable filter form for portlet

        setOverflow(Overflow.VISIBLE);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        //Operation range property - retrieve existing value
        PropertySimple property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            int translatedAlertRangeSelection = translatedAlertRangeSelection(retrieved);
            getDataSource().setAlertRangeCompleted(translatedAlertRangeSelection);
        } else {//create setting
            storedPortlet.getConfiguration().put(
                new PropertySimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE, defaultAlertCountValue));
            getDataSource().setAlertRangeCompleted(Integer.parseInt(defaultAlertCountValue));
        }
        //Operation priority property setting
        property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_PRIORITY_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            int translatedPriorityIndex = translatedPriorityToValidIndex(retrieved);
            getDataSource().setAlertPriorityIndex(translatedPriorityIndex);
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_PRIORITY_VALUE, defaultPriorityValue));
            getDataSource().setAlertPriorityIndex(translatedPriorityToValidIndex(PRIORITY_ALL));
        }

        //Range to time that alerts will be shown for
        property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_TIME_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            long translatedRange = translateTimeToValidRange(retrieved);
            getDataSource().setAlertTimeRange(translatedRange);
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_TIME_VALUE, defaultTimeValue));
            getDataSource().setAlertTimeRange(translateTimeToValidRange(defaultTimeValue));
        }

        //Range of resources to be included in the query
        property = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE);
        if ((property != null) && (property.getStringValue() != null)) {
            //retrieve and translate to int
            String retrieved = property.getStringValue();
            if (retrieved.trim().equalsIgnoreCase(RESOURCES_SELECTED)) {
                getDataSource().setAlertResourcesToUse(RESOURCES_SELECTED);
            } else {
                getDataSource().setAlertResourcesToUse(RESOURCES_ALL);
            }
        } else {//create setting
            storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_RESOURCES_VALUE, defaultResourceValue));
            getDataSource().setAlertResourcesToUse(RESOURCES_ALL);
        }

        //resource ids to be conditionally included in the query
        Integer[] filterResourceIds = null;
        filterResourceIds = getDataSource().extractFilterResourceIds(storedPortlet, filterResourceIds);
        //no defaults

        if (filterResourceIds != null) {
            getDataSource().setAlertFilterResourceId(filterResourceIds);
        }

        //conditionally display the selected resources ui
        if (containerCanvas != null) {
            //empty out earlier canvas
            for (Canvas c : containerCanvas.getChildren()) {
                c.destroy();
            }
            if ((resourceSelector != null) && getDataSource().getAlertResourcesToUse().equals(RESOURCES_SELECTED)) {
                containerCanvas.addChild(resourceSelector.getCanvas());
            } else {
                containerCanvas.addChild(new Canvas());
            }
        }
    }

    private int translatedAlertRangeSelection(String retrieved) {
        int translated = -1;
        if ((retrieved != null) && (!retrieved.trim().isEmpty())) {
            if (retrieved.equalsIgnoreCase(unlimited)) {
                translated = -1;
            } else {
                translated = Integer.parseInt(retrieved);//default to all
            }
        } else {//default to defaultValue
            if (defaultAlertCountValue.equalsIgnoreCase(unlimited)) {
                translated = -1;
            } else {
                translated = Integer.parseInt(defaultAlertCountValue);
            }
        }
        return translated;
    }

    private int translatedPriorityToValidIndex(String retrieved) {
        int translatedPriority = 0;//default to all
        if ((retrieved != null) && (!retrieved.trim().isEmpty())) {
            if (retrieved.equalsIgnoreCase(PRIORITY_HIGH)) {
                translatedPriority = 3;
            } else if (retrieved.equalsIgnoreCase(PRIORITY_MEDIUM)) {
                translatedPriority = 2;
            } else if (retrieved.equalsIgnoreCase(PRIORITY_LOW)) {
                translatedPriority = 1;
            } else {
                translatedPriority = 0;//default to all
            }
        }
        return translatedPriority;
    }

    /**Translates the UI selection options into time values for alert query.
     *
     * @param retrieved
     * @return long value mapping to string passed in.
     */
    private long translateTimeToValidRange(String retrieved) {
        long translated = 0;//default to ALL
        if ((retrieved != null) && (!retrieved.trim().isEmpty())) {
            if (retrieved.equalsIgnoreCase(TIME_30_MINS)) {
                translated = MeasurementUtility.MINUTES * 30;
            } else if (retrieved.equalsIgnoreCase(TIME_HOUR)) {
                translated = MeasurementUtility.HOURS;
            } else if (retrieved.equalsIgnoreCase(TIME_12_HRS)) {
                translated = MeasurementUtility.HOURS * 12;
            } else if (retrieved.equalsIgnoreCase(TIME_DAY)) {
                translated = MeasurementUtility.DAYS;
            } else if (retrieved.equalsIgnoreCase(TIME_WEEK)) {
                translated = MeasurementUtility.WEEKS;
            } else if (retrieved.equalsIgnoreCase(TIME_MONTH)) {
                translated = MeasurementUtility.DAYS * 28;//replicated from old struts def.
            } else {
                translated = MeasurementUtility.DAYS;//default to day otherwise.
            }
        }
        return translated;
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentAlerts());
    }

    public DynamicForm getCustomSettingsForm() {
        //root dynamic form instance
        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("custom-settings"));
        form.setWidth(GroupAlertsPortlet2.ALERT_RESOURCE_SELECTION_WIDTH + 40);//largest widget display + 40 for buttons
        form.setHeight(400);
        form.setMargin(5);

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        //vertical container
        VLayout column = new VLayout();

        //label
        LocatableLabel alertRangeLabel = new LocatableLabel(extendLocatorId("DynamicForm_Label_Alert_Range"), "<b>"
            + MSG.common_title_alert_range() + "</b>");

        //horizontal layout
        LocatableHLayout row = new LocatableHLayout(extendLocatorId("alert-range-settings-row-1"));
        row.setMembersMargin(10);

        //-------------combobox for number of completed scheduled ops to display on the dashboard
        final SelectItem alertRangeLastComboBox = new SelectItem(ALERT_RANGE_DISPLAY_AMOUNT_VALUE);
        alertRangeLastComboBox.setTitle(MSG.view_measureRange_last());
        alertRangeLastComboBox.setType("selection");
        alertRangeLastComboBox.setWrapTitle(false);
        //define acceptable values for display amount
        String[] acceptableDisplayValues = { "5", "10", MSG.common_label_unlimited() };
        alertRangeLastComboBox.setValueMap(acceptableDisplayValues);
        //set width of dropdown display region
        alertRangeLastComboBox.setWidth(100);
        alertRangeLastComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_DISPLAY_AMOUNT_VALUE, selectedItem);
            }
        });

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        String selectedValue = defaultAlertCountValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE)
                    .getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(
                    new PropertySimple(ALERT_RANGE_DISPLAY_AMOUNT_VALUE, defaultAlertCountValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertRangeLastComboBox.setDefaultValue(selectedValue);

        //-------------combobox for number of completed scheduled ops to display on the dashboard
        final SelectItem alertRangePriorityComboBox = new SelectItem(ALERT_RANGE_PRIORITY_VALUE);
        alertRangePriorityComboBox.setTitle("");
        alertRangePriorityComboBox.setHint("<nobr> <b> " + MSG.view_portlet_recentAlerts_config_priority_label()
            + "</b></nobr>");
        alertRangePriorityComboBox.setType("selection");
        //define acceptable values for display amount
        String[] acceptablePriorityDisplayValues = { PRIORITY_ALL, PRIORITY_HIGH, PRIORITY_MEDIUM, PRIORITY_LOW };
        alertRangePriorityComboBox.setValueMap(acceptablePriorityDisplayValues);
        //set width of dropdown display region
        alertRangePriorityComboBox.setWidth(100);
        alertRangePriorityComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_PRIORITY_VALUE, selectedItem);
            }
        });

        //default selected value to 'unlimited'(live lists) and check both combobox settings here.
        selectedValue = defaultPriorityValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_PRIORITY_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_PRIORITY_VALUE).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(
                    new PropertySimple(ALERT_RANGE_PRIORITY_VALUE, defaultPriorityValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertRangePriorityComboBox.setDefaultValue(selectedValue);
        row.addMember(alertRangeLabel);
        DynamicForm wrappedRange = new DynamicForm();
        wrappedRange.setFields(alertRangeLastComboBox);
        row.addMember(wrappedRange);

        DynamicForm wrappedPriority = new DynamicForm();
        wrappedPriority.setFields(alertRangePriorityComboBox);
        row.addMember(wrappedPriority);

        //horizontal layout
        LocatableHLayout row2 = new LocatableHLayout(extendLocatorId("alert-range-settings-row-2"));

        LocatableLabel alertRangeSpanLabel = new LocatableLabel(extendLocatorId("range-span-label"), "<b>"
            + MSG.view_portlet_recentAlerts_config_when() + "<b>");
        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem alertRangeTimeComboBox = new SelectItem(ALERT_RANGE_TIME_VALUE);
        alertRangeTimeComboBox.setTitle("");
        alertRangeTimeComboBox.setHint("");
        alertRangeTimeComboBox.setType("selection");
        String[] acceptableTimeDisplayValues = { TIME_30_MINS, TIME_HOUR, TIME_12_HRS, TIME_DAY, TIME_WEEK, TIME_MONTH };
        alertRangeTimeComboBox.setValueMap(acceptableTimeDisplayValues);
        alertRangeTimeComboBox.setWidth(100);
        alertRangeTimeComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_TIME_VALUE, selectedItem);
            }
        });

        //set to default
        selectedValue = defaultTimeValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_TIME_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_TIME_VALUE).getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(new PropertySimple(ALERT_RANGE_TIME_VALUE, defaultTimeValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertRangeTimeComboBox.setDefaultValue(selectedValue);
        DynamicForm timeSelectionWrapper = new DynamicForm();
        timeSelectionWrapper.setFields(alertRangeTimeComboBox);

        // build resource selection drop down
        //------------- Build second combobox for timeframe for problem resources search.
        final SelectItem alertResourcesComboBox = new SelectItem(ALERT_RANGE_RESOURCES_VALUE);
        alertResourcesComboBox.setTitle(MSG.common_val_for());
        alertResourcesComboBox.setHint("");
        alertResourcesComboBox.setType("selection");
        String[] acceptableResourceDisplayValues = { RESOURCES_ALL, RESOURCES_SELECTED };
        alertResourcesComboBox.setValueMap(acceptableResourceDisplayValues);
        alertResourcesComboBox.setWidth(150);
        alertResourcesComboBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                String selectedItem = "" + event.getValue();
                //stuff into the master form for retrieval
                form.setValue(ALERT_RANGE_RESOURCES_VALUE, selectedItem);
                //empty out earlier canvas
                for (Canvas c : containerCanvas.getChildren()) {
                    c.destroy();
                }
                if (selectedItem.equals(RESOURCES_SELECTED)) {
                    containerCanvas.addChild(resourceSelector.getCanvas());
                } else {
                    containerCanvas.addChild(new Canvas());
                }
            }
        });

        //set to default
        selectedValue = defaultResourceValue;
        if (storedPortlet != null) {
            //if property exists retrieve it
            if (storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE) != null) {
                selectedValue = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE)
                    .getStringValue();
            } else {//insert default value
                storedPortlet.getConfiguration().put(
                    new PropertySimple(ALERT_RANGE_RESOURCES_VALUE, defaultResourceValue));
            }
        }
        //prepopulate the combobox with the previously stored selection
        alertResourcesComboBox.setDefaultValue(selectedValue);
        DynamicForm resourceSelectionWrapper = new DynamicForm();
        resourceSelectionWrapper.setFields(alertResourcesComboBox);

        alertRangeSpanLabel.setWrap(false);
        alertRangeSpanLabel.setWidth(150);
        row2.addMember(alertRangeSpanLabel);
        row2.addMember(timeSelectionWrapper);
        row2.addMember(resourceSelectionWrapper);

        //if portlet config setting exist, then retrieve
        Integer[] alertFilterResourceIds = null;
        alertFilterResourceIds = getDataSource().extractFilterResourceIds(storedPortlet, alertFilterResourceIds);

        LocatableHLayout resourceSelectionRegion = new LocatableHLayout(extendLocatorId("selection-canvas"));
        resourceSelector = new AlertResourceSelectorRegion(extendLocatorId("ResourcesWithAlerts"),
            alertFilterResourceIds);
        resourceSelectionRegion.setWidth100();

        if (alertFilterResourceIds != null) {
            getDataSource().setAlertFilterResourceId(alertFilterResourceIds);
            resourceSelector.setCurrentlyAssignedIds(alertFilterResourceIds);
        }

        //instantiate canvas area to display empty or rich resource selection based on dropdown selection
        containerCanvas = new Canvas();
        String previousAlertFilterChoice = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE)
            .getStringValue();

        //reload the ResourceSelectionRegion if user has chosen to focus on specific resources with alerts
        if (previousAlertFilterChoice.equals(RESOURCES_SELECTED)) {
            containerCanvas.addChild(resourceSelector.getCanvas());
        } else {// define empty canvas
            containerCanvas.addChild(new Canvas());
        }

        //add contain resource selection region.
        resourceSelectionRegion.addMember(containerCanvas);

        //finish construction of the layout
        column.addMember(row);
        column.addMember(row2);
        SpacerItem verticalSpace = new SpacerItem();
        verticalSpace.setHeight(20);
        DynamicForm spacerWrapper = new DynamicForm();
        spacerWrapper.setItems(verticalSpace);
        column.addMember(spacerWrapper);
        column.addMember(resourceSelectionRegion);
        form.addChild(column);

        //submit handler
        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //no need to insert validation here as user not allowed to enter values
                parseFormAndPopulateConfiguration(form, storedPortlet, ALERT_RANGE_DISPLAY_AMOUNT_VALUE,
                    ALERT_RANGE_PRIORITY_VALUE, ALERT_RANGE_RESOURCES_VALUE, ALERT_RANGE_TIME_VALUE);

                //retrieve alert-resource-selection property
                PropertySimple prop = storedPortlet.getConfiguration().getSimple(ALERT_RANGE_RESOURCES_VALUE);

                //check to see if "Selected Resources" or "All Resources"
                if (prop != null && RESOURCES_SELECTED.equals(prop.getStringValue())) {
                    //retrieve currentlyAssignedIds
                    Integer[] valuesToPersist = resourceSelector.getListGridValues();
                    resourceSelector.setCurrentlyAssignedIds(valuesToPersist);

                    //build property list of ids to persist
                    PropertyList list = new PropertyList(ALERT_RANGE_RESOURCE_IDS);
                    for (int rid : resourceSelector.getCurrentlyAssignedIds()) {
                        list.add(new PropertySimple(ALERT_RANGE_RESOURCE_IDS, rid));
                    }
                    storedPortlet.getConfiguration().put(new PropertyList(ALERT_RANGE_RESOURCE_IDS, list));
                    getDataSource().setAlertFilterResourceId(resourceSelector.getCurrentlyAssignedIds());
                }

                configure(portletWindow, storedPortlet);

                refresh();//reload form with new data selections
                markForRedraw();
            }
        });

        return form;
    }

    /**Iterates over DynamicForm instance to check for properties passed in and if they have been set
     * to put that property into the DashboardPortlet configuration.
     *
     * @param form Dynamic form storing user selections
     * @param portlet Container for configuration changes
     * @param properties Variable list of keys used to verify or populate properties.
     */
    private void parseFormAndPopulateConfiguration(final DynamicForm form, DashboardPortlet storedPortlet,
        String... properties) {
        if ((form != null) && (storedPortlet != null)) {
            for (String property : properties) {
                if (form.getValue(property) != null) {//if new value supplied
                    storedPortlet.getConfiguration().put(new PropertySimple(property, form.getValue(property)));
                }
            }
        }
    }

    public AlertPortletDataSource getDataSource() {
        return dataSource;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {

            return new GroupAlertsPortlet2(locatorId);
        }
    }

    @Override
    public void startRefreshCycle() {
        //current setting
        final int refreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();

        //cancel any existing timer
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        if (refreshInterval >= MeasurementUtility.MINUTES) {

            refreshTimer = new Timer() {
                public void run() {

                    redraw();
                }
            };

            refreshTimer.scheduleRepeating(refreshInterval);
        }
    }

    @Override
    protected void onDestroy() {
        if (refreshTimer != null) {

            refreshTimer.cancel();
        }

        super.onDestroy();
    }

    @Override
    protected void setupTableInteractions(boolean hasWriteAccess) {
        // The portlet is a "subsystem" view. Meaning the alerts displayed can be from any accessible group for
        // the user.  This means the user can have varying permissions on the underlying groups and/or resources,
        // which makes button enablement tricky. So, for the portlet don't even show the buttons unless the user
        // is inventory manager.  Other users will just have to navigate to the alert in question in order to
        // manipulate it.

        //determine if the user is inventory manager and if so render the buttons
        Set<Permission> permissions = this.portletWindow.getGlobalPermissions();
        if ((null != permissions) && permissions.contains(Permission.MANAGE_INVENTORY)) {
            super.setupTableInteractions(true);
        }
    }

    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer recordId = getId(record);
                Integer resourceId = record.getAttributeAsInt("resourceId");
                String detailsUrl = LinkManager.getSubsystemAlertHistoryLink(resourceId, recordId);
                return SeleniumUtility.getLocatableHref(detailsUrl, value.toString(), null);
            }
        };
    }

    @Override
    protected void configureTable() {
        // TODO Auto-generated method stub
        super.configureTable();

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelection();
                if (selectedRows != null && selectedRows.length == 1) {
                    Integer recordId = getId(selectedRows[0]);
                    Integer resourceId = selectedRows[0].getAttributeAsInt("resourceId");
                    CoreGUI.goToView(LinkManager.getSubsystemAlertHistoryLink(resourceId, recordId));
                }
            }
        });
    }

}

/** Bundles a ResourceSelector instance with labelling in Canvas for display.
 *  Also modifies the AssignedGrid to listen for AvailbleGrid completion and act accordingly.
 */
class AlertResourceSelectorRegion extends LocatableVLayout {
    public AlertResourceSelectorRegion(String locatorId, Integer[] assigned) {
        super(locatorId);
        this.currentlyAssignedIds = assigned;
    }

    private static final Messages MSG = CoreGUI.getMessages();
    private PortletAlertSelector selector = null;

    private Integer[] currentlyAssignedIds;

    public Integer[] getCurrentlyAssignedIds() {
        return currentlyAssignedIds;
    }

    public Integer[] getListGridValues() {
        Integer[] listGridValues = new Integer[0];
        if (null != selector) {
            listGridValues = selector.getAssignedListGridValues();
        }
        return listGridValues;
    }

    public Canvas getCanvas() {
        if (selector == null) {
            selector = new PortletAlertSelector(extendLocatorId("AlertSelector"), this.currentlyAssignedIds,
                ResourceType.ANY_PLATFORM_TYPE, false);
        }
        return selector;
    }

    public void setCurrentlyAssignedIds(Integer[] currentlyAssignedIds) {
        this.currentlyAssignedIds = currentlyAssignedIds;
    }
}