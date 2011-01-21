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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.GwtTuple;
import org.rhq.enterprise.gui.coregui.client.util.measurement.GwtMonitorUtils;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The content pane of the Resource Summary>Activity tab.
 *
 * @author Simeon Pinder
 */
public class ActivityView2 extends LocatableHLayout implements RefreshableView {
    private LocatableVLayout leftPane;
    private LocatableVLayout rightPane;
    private LocatableCanvas recentMeasurementsContent = new LocatableCanvas(extendLocatorId("RecentMetricsContent"));
    private LocatableCanvas recentAlertsContent = new LocatableCanvas(extendLocatorId("RecentAlertsContent"));
    private LocatableCanvas recentOobContent = new LocatableCanvas(extendLocatorId("RecentOobsContent"));
    private LocatableCanvas recentConfigurationContent = new LocatableCanvas(extendLocatorId("RecentConfigContent"));
    private LocatableCanvas recentOperationsContent = new LocatableCanvas(extendLocatorId("RecentOperationsContent"));
    private LocatableCanvas recentEventsContent = new LocatableCanvas(extendLocatorId("RecentEventsContent"));
    private LocatableCanvas recentPkgHistoryContent = new LocatableCanvas(extendLocatorId("RecentPkgHistoryContent"));
    private String RECENT_MEASUREMENTS = MSG.common_title_recent_measurements();
    private String RECENT_MEASUREMENTS_NONE = MSG.view_resource_inventory_activity_no_recent_metrics();
    private String RECENT_ALERTS = MSG.common_title_recent_alerts();
    private String RECENT_ALERTS_NONE = MSG.view_resource_inventory_activity_no_recent_alerts();
    private String RECENT_OOB = MSG.common_title_recent_oob_metrics();
    private String RECENT_OOB_NONE = MSG.view_resource_inventory_activity_no_recent_oob();
    private String RECENT_CONFIGURATIONS = MSG.common_title_recent_configuration_updates();
    private String RECENT_CONFIGURATIONS_NONE = MSG.view_resource_inventory_activity_no_recent_config_history();
    private String RECENT_OPERATIONS = MSG.common_title_recent_operations();
    private String RECENT_OPERATIONS_NONE = MSG.view_resource_inventory_activity_no_recent_operations();
    private String RECENT_EVENTS = MSG.common_title_recent_event_counts();
    private String RECENT_EVENTS_NONE = MSG.view_resource_inventory_activity_no_recent_events();
    private String RECENT_PKG_HISTORY = MSG.common_title_recent_pkg_history();
    private String RECENT_PKG_HISTORY_NONE = MSG.view_resource_inventory_activity_no_recent_pkg_history();

    private Timer sparklineReloader = null;
    private ResourceComposite resourceComposite;

    public ActivityView2(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        this.resourceComposite = resourceComposite;
        setID(locatorId);
        initializeUi();
    }

    private void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        //dividers definition
        HTMLFlow divider1 = new HTMLFlow("<hr/>");
        HTMLFlow divider2 = new HTMLFlow("<hr/>");
        HTMLFlow divider3 = new HTMLFlow("<hr/>");
        HTMLFlow divider4 = new HTMLFlow("<hr/>");
        HTMLFlow divider5 = new HTMLFlow("<hr/>");
        divider1.setWidth("50%");
        divider2.setWidth("50%");
        divider3.setWidth("50%");
        divider4.setWidth("50%");
        divider5.setWidth("50%");

        //leftPane
        leftPane = new LocatableVLayout(extendLocatorId("Left"));
        leftPane.setWidth("50%");
        leftPane.setPadding(5);
        leftPane.setMembersMargin(5);
        leftPane.setAutoHeight();
        //recentMetrics.xhtml
        LocatableHLayout recentMetricsTitle = new TitleWithIcon(leftPane, "RecentMetrics",
            "subsystems/monitor/Monitor_24.png", RECENT_MEASUREMENTS);
        leftPane.addMember(recentMetricsTitle);
        leftPane.addMember(recentMeasurementsContent);
        recentMeasurementsContent.setHeight(20);
        recentMeasurementsContent.setContents(RECENT_MEASUREMENTS_NONE);
        leftPane.addMember(divider1);
        //recentAlerts.xhtml
        LocatableHLayout recentAlertsTitle = new TitleWithIcon(leftPane, "RecentAlerts",
            "subsystems/alert/Flag_blue_24.png", RECENT_ALERTS);
        leftPane.addMember(recentAlertsTitle);
        leftPane.addMember(recentAlertsContent);
        recentAlertsContent.setHeight(20);
        leftPane.addMember(divider2);
        //recentOOBs.xhtml
        LocatableHLayout recentOobsTitle = new TitleWithIcon(leftPane, "RecentOobs",
            "subsystems/monitor/Monitor_failed_24.png", RECENT_OOB);
        leftPane.addMember(recentOobsTitle);
        leftPane.addMember(recentOobContent);
        recentOobContent.setHeight(20);

        //rightPane
        rightPane = new LocatableVLayout(extendLocatorId("Right"));
        rightPane.setWidth("50%");
        rightPane.setPadding(5);
        rightPane.setMembersMargin(5);
        rightPane.setAutoHeight();
        //recentConfigUpdates.xhtml
        LocatableHLayout recentConfigUpdatesTitle = new TitleWithIcon(leftPane, "RecentConfigUpdates",
            "subsystems/configure/Configure_24.png", RECENT_CONFIGURATIONS);
        rightPane.addMember(recentConfigUpdatesTitle);
        rightPane.addMember(recentConfigurationContent);
        recentConfigurationContent.setHeight(20);
        rightPane.addMember(divider3);
        //recentOperations.xhtml
        LocatableHLayout recentOperationsTitle = new TitleWithIcon(leftPane, "RecentOperations",
            "subsystems/control/Operation_24.png", RECENT_OPERATIONS);
        rightPane.addMember(recentOperationsTitle);
        rightPane.addMember(recentOperationsContent);
        recentOperationsContent.setHeight(20);
        rightPane.addMember(divider4);
        //recentEventCounts.xhtml
        LocatableHLayout recentEventsTitle = new TitleWithIcon(leftPane, "RecentEvent",
            "subsystems/event/Events_24.png", RECENT_EVENTS);
        rightPane.addMember(recentEventsTitle);
        rightPane.addMember(recentEventsContent);
        recentEventsContent.setHeight(20);
        //        recentEventsContent.setContents(RECENT_EVENTS_NONE);
        rightPane.addMember(divider5);
        //recentPackageHistory.xhtml
        LocatableHLayout recentPkgHistoryTitle = new TitleWithIcon(leftPane, "RecentPkgHistory",
            "subsystems/content/Content_24.png", RECENT_PKG_HISTORY);
        rightPane.addMember(recentPkgHistoryTitle);
        rightPane.addMember(recentPkgHistoryContent);
        recentPkgHistoryContent.setHeight(20);
        loadData();
    }

    private void loadData() {
        getRecentAlerts();
        getRecentOperations();
        getRecentConfigurationUpdates();
        getRecentEventUpdates();
        getRecentOobs();
        getRecentPkgHistory();
        getRecentMetrics();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(leftPane);
        addMember(rightPane);
        refresh();
    }

    @Override
    public void refresh() {
        //        int resourceId = this.resourceComposite.getResource().getId();
        //        this.iFrame.setContentsURL("/rhq/resource/summary/overview-plain.xhtml?id=" + resourceId);
        markForRedraw();
    }

    class TitleWithIcon extends LocatableHLayout {
        public TitleWithIcon(String locatorId) {
            super(locatorId);
        }

        public TitleWithIcon(Locatable parentContainer, String locatorIdentifier, String imageUrl, String title) {
            super(parentContainer.extendLocatorId(locatorIdentifier));
            Img titleImage = new Img(imageUrl, 24, 24);
            HTMLFlow titleElement = new HTMLFlow();
            titleElement.setWidth("*");
            titleElement.setContents(title);
            titleElement.setStyleName("HeaderLabel");
            addMember(titleImage);
            addMember(titleElement);
            setMembersMargin(10);
        }
    }

    /** Fetches alerts and updates the DynamicForm instance with the latest
     *  alert information.
     */
    private void getRecentAlerts() {
        final int resourceId = this.resourceComposite.getResource().getId();
        //fetches last five alerts for this resource
        AlertCriteria criteria = new AlertCriteria();
        PageControl pageControl = new PageControl(0, 5);
        pageControl.initDefaultOrderingField("ctime", PageOrdering.DESC);
        criteria.setPageControl(pageControl);
        criteria.addFilterResourceIds(resourceId);
        GWTServiceLookup.getAlertService().findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            @Override
            public void onSuccess(PageList<Alert> result) {
                LocatableVLayout column = new LocatableVLayout(recentAlertsContent.extendLocatorId("Content"));
                column.setHeight(10);
                if (!result.isEmpty()) {
                    for (Alert alert : result) {
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);

                        StaticTextItem iconItem = new StaticTextItem();
                        FormItemIcon img = new FormItemIcon();
                        img.setSrc(ImageManager.getAlertIcon(alert.getAlertDefinition().getPriority()));
                        img.setWidth(16);
                        img.setHeight(16);
                        img.setPrompt(alert.getAlertDefinition().getPriority().getDisplayName());
                        iconItem.setIcons(img);
                        iconItem.setShowTitle(false);

                        LinkItem link = new LinkItem();
                        link.setLinkTitle(alert.getAlertDefinition().getName() + ": ");
                        link.setTitle(alert.getAlertDefinition().getName());
                        link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Alerts/History/"
                            + alert.getId());
                        link.setTarget("_self");
                        link.setShowTitle(false);

                        StaticTextItem time = new StaticTextItem();
                        time.setDefaultValue(GwtRelativeDurationConverter.format(alert.getCtime()));
                        time.setShowTitle(false);
                        time.setShowPickerIcon(false);
                        time.setWrap(false);
                        row.setItems(iconItem, link, time);

                        column.addMember(row);
                    }
                } else {
                    column.setContents(RECENT_ALERTS_NONE);
                }
                recentAlertsContent.setContents("");
                for (Canvas child : recentAlertsContent.getChildren()) {
                    child.destroy();
                }
                recentAlertsContent.addChild(column);
                recentAlertsContent.markForRedraw();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.debug("Error retrieving recent alerts for resource [" + resourceId + "]:" + caught.getMessage());
            }
        });
    }

    /** Fetches operations and updates the DynamicForm instance with the latest
     *  operation information.
     */
    private void getRecentOperations() {
        final int resourceId = this.resourceComposite.getResource().getId();
        //fetches five most recent operations.
        PageControl pageControl = new PageControl(0, 5);
        pageControl.initDefaultOrderingField("ro.createdTime", PageOrdering.DESC);
        GWTServiceLookup.getOperationService().findRecentCompletedOperations(resourceId, pageControl,
            new AsyncCallback<List<DisambiguationReport<ResourceOperationLastCompletedComposite>>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent operations for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(List<DisambiguationReport<ResourceOperationLastCompletedComposite>> result) {
                    LocatableVLayout column = new LocatableVLayout(recentOperationsContent.extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (DisambiguationReport<ResourceOperationLastCompletedComposite> report : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                            row.setNumCols(3);

                            StaticTextItem iconItem = new StaticTextItem();
                            FormItemIcon img = new FormItemIcon();
                            img.setSrc(ImageManager.getOperationResultsIcon(report.getOriginal().getOperationStatus()));
                            img.setWidth(16);
                            img.setHeight(16);
                            img.setPrompt(report.getOriginal().getOperationStatus().getDisplayName());
                            iconItem.setIcons(img);
                            iconItem.setShowTitle(false);

                            LinkItem link = new LinkItem();
                            link.setLinkTitle(report.getOriginal().getOperationName() + ": ");
                            link.setTitle(report.getOriginal().getOperationName());
                            link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Operations/History/"
                                + report.getOriginal().getOperationHistoryId());
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            StaticTextItem time = new StaticTextItem();
                            time.setDefaultValue(GwtRelativeDurationConverter.format(report.getOriginal()
                                .getOperationStartTime()));
                            time.setShowTitle(false);
                            time.setShowPickerIcon(false);
                            time.setWrap(false);
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        StaticTextItem none = new StaticTextItem();
                        none.setShowTitle(false);
                        none.setDefaultValue(RECENT_OPERATIONS_NONE);
                        none.setWrap(false);
                        row.setItems(none);
                        column.addMember(row);
                    }
                    for (Canvas child : recentOperationsContent.getChildren()) {
                        child.destroy();
                    }
                    recentOperationsContent.addChild(column);
                    recentOperationsContent.markForRedraw();
                }
            });
    }

    /** Fetches configuration updates and updates the DynamicForm instance with the latest
     *  config change information.
     */
    private void getRecentConfigurationUpdates() {
        final int resourceId = this.resourceComposite.getResource().getId();

        //construct criteria( last five config updates for resourceId sorted by cTime)
        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.addFilterResourceIds(resourceId);
        PageControl pageControl = new PageControl(0, 5);
        criteria.addSortCtime(PageOrdering.DESC);
        criteria.setPageControl(pageControl);

        GWTServiceLookup.getConfigurationService().findResourceConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent configuration updates for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<ResourceConfigurationUpdate> result) {
                    LocatableVLayout column = new LocatableVLayout(recentConfigurationContent
                        .extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (ResourceConfigurationUpdate update : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                            row.setNumCols(3);

                            StaticTextItem iconItem = new StaticTextItem();
                            FormItemIcon img = new FormItemIcon();
                            img.setSrc(ImageManager.getResourceConfigurationIcon(update.getStatus()));
                            img.setWidth(16);
                            img.setHeight(16);
                            iconItem.setIcons(img);
                            iconItem.setShowTitle(false);

                            LinkItem link = new LinkItem();
                            String linkTitle = MSG.view_resource_inventory_activity_changed_by() + " "
                                + update.getSubjectName() + ":";
                            if ((update.getSubjectName() == null) || (update.getSubjectName().trim().isEmpty())) {
                                linkTitle = MSG.common_msg_changeAutoDetected();
                                ;
                            }
                            link.setLinkTitle(linkTitle);
                            link.setTitle(linkTitle);
                            link.setValue(ReportDecorator.GWT_RESOURCE_URL + resourceId + "/Configuration/History/"
                                + update.getId());
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            StaticTextItem time = new StaticTextItem();
                            time.setDefaultValue(GwtRelativeDurationConverter.format(update.getCreatedTime()));
                            time.setShowTitle(false);
                            time.setShowPickerIcon(false);
                            time.setWrap(false);
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        StaticTextItem none = new StaticTextItem();
                        none.setShowTitle(false);
                        none.setDefaultValue(RECENT_CONFIGURATIONS_NONE);
                        none.setWrap(false);
                        row.setItems(none);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentConfigurationContent.getChildren()) {
                        child.destroy();
                    }
                    recentConfigurationContent.addChild(column);
                    recentConfigurationContent.markForRedraw();
                }
            });
    }

    /** Fetches recent events and updates the DynamicForm instance with the latest
     *  event information over last 24hrs.
     */
    private void getRecentEventUpdates() {

        final int resourceId = this.resourceComposite.getResource().getId();
        long now = System.currentTimeMillis();
        long nowMinus24Hours = now - (24 * 60 * 60 * 1000);

        GWTServiceLookup.getEventService().getEventCountsBySeverity(resourceId, nowMinus24Hours, now,
            new AsyncCallback<Map<EventSeverity, Integer>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent event counts for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(Map<EventSeverity, Integer> eventCounts) {
                    //Now populated Tuples
                    List<GwtTuple<EventSeverity, Integer>> results = new ArrayList<GwtTuple<EventSeverity, Integer>>();
                    for (EventSeverity severity : eventCounts.keySet()) {
                        int count = eventCounts.get(severity);
                        if (count > 0) {
                            results.add(new GwtTuple<EventSeverity, Integer>(severity, count));
                        }
                    }
                    //build display
                    LocatableVLayout column = new LocatableVLayout(recentEventsContent.extendLocatorId("Content"));
                    column.setHeight(10);

                    if (!results.isEmpty()) {
                        for (GwtTuple<EventSeverity, Integer> tuple : results) {
                            LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                            row.setNumCols(2);
                            row.setWidth(10);//pack.

                            //icon
                            StaticTextItem iconItem = new StaticTextItem();
                            FormItemIcon img = new FormItemIcon();
                            img.setSrc(ImageManager.getEventSeverityIcon(tuple.getLefty()));
                            img.setWidth(16);
                            img.setHeight(16);
                            img.setPrompt(tuple.getLefty().name());
                            iconItem.setIcons(img);
                            iconItem.setShowTitle(false);

                            //count
                            StaticTextItem count = new StaticTextItem();
                            count.setDefaultValue(tuple.righty);
                            count.setShowTitle(false);
                            count.setShowPickerIcon(false);
                            count.setWrap(false);
                            row.setItems(iconItem, count);

                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        StaticTextItem none = new StaticTextItem();
                        none.setShowTitle(false);
                        none.setDefaultValue(RECENT_EVENTS_NONE);
                        none.setWrap(false);
                        row.setItems(none);
                        column.addMember(row);
                    }
                    recentEventsContent.setContents("");
                    //cleanup
                    for (Canvas child : recentEventsContent.getChildren()) {
                        child.destroy();
                    }
                    recentEventsContent.addChild(column);
                    recentEventsContent.markForRedraw();
                }
            });
    }

    /** Fetches OOB measurements and updates the DynamicForm instance with the latest 5
     *  oob change details.
     */
    private void getRecentOobs() {
        final int resourceId = this.resourceComposite.getResource().getId();
        GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resourceId, 5,
            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent out of bound metrics for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<MeasurementOOBComposite> result) {
                    LocatableVLayout column = new LocatableVLayout(recentAlertsContent.extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (MeasurementOOBComposite oob : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                            row.setNumCols(2);

                            LinkItem link = new LinkItem();
                            link.setLinkTitle(oob.getScheduleName() + ":");
                            link.setTitle(oob.getScheduleName() + ":");
                            //TODO: change link after charting updated for gwt.
                            link.setValue("/resource/common/monitor/Visibility.do?m=" + oob.getDefinitionId() + "&id="
                                + resourceId + "&mode=chartSingleMetricSingleResource");
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            StaticTextItem time = new StaticTextItem();
                            time.setDefaultValue(GwtRelativeDurationConverter.format(oob.getTimestamp()));
                            time.setShowTitle(false);
                            time.setShowPickerIcon(false);
                            time.setWrap(false);
                            row.setItems(link, time);

                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        StaticTextItem none = new StaticTextItem();
                        none.setShowTitle(false);
                        none.setDefaultValue(RECENT_OOB_NONE);
                        none.setWrap(false);
                        row.setItems(none);
                        column.addMember(row);
                    }
                    recentOobContent.setContents("");
                    for (Canvas child : recentOobContent.getChildren()) {
                        child.destroy();
                    }
                    recentOobContent.addChild(column);
                    recentOobContent.markForRedraw();
                }
            });
    }

    /** Fetches recent package history information and updates the DynamicForm instance with details.
     */
    private void getRecentPkgHistory() {
        final int resourceId = this.resourceComposite.getResource().getId();
        InstalledPackageCriteria criteria = new InstalledPackageCriteria();
        criteria.addFilterResourceId(resourceId);
        PageControl pageControl = new PageControl(0, 5);
        criteria.setPageControl(pageControl);

        GWTServiceLookup.getContentService().getInstalledPackageHistoryForResource(resourceId, 5,
            new AsyncCallback<PageList<InstalledPackageHistory>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving installed package history for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<InstalledPackageHistory> result) {
                    LocatableVLayout column = new LocatableVLayout(recentPkgHistoryContent.extendLocatorId("Content"));
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (InstalledPackageHistory history : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                            row.setNumCols(3);

                            StaticTextItem iconItem = new StaticTextItem();
                            FormItemIcon img = new FormItemIcon();
                            img.setSrc("subsystems/content/Content_16.png");
                            img.setWidth(16);
                            img.setHeight(16);
                            iconItem.setIcons(img);
                            iconItem.setShowTitle(false);

                            LinkItem link = new LinkItem();
                            link.setLinkTitle(history.getPackageVersion().getFileName() + ":");
                            link.setTitle(history.getPackageVersion().getFileName() + ":");

                            //TODO: spinder. Upgrade link when content available in coreGui. Not yet iframed correctly.
                            link.setValue("/rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId
                                + "&selectedHistoryId=" + history.getId());
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            StaticTextItem time = new StaticTextItem();
                            time.setDefaultValue(GwtRelativeDurationConverter.format(history.getTimestamp()));
                            time.setShowTitle(false);
                            time.setShowPickerIcon(false);
                            time.setWrap(false);
                            row.setItems(iconItem, link, time);

                            column.addMember(row);
                        }
                    } else {
                        LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                        row.setNumCols(3);
                        StaticTextItem none = new StaticTextItem();
                        none.setShowTitle(false);
                        none.setDefaultValue(RECENT_PKG_HISTORY_NONE);
                        none.setWrap(false);
                        row.setItems(none);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentPkgHistoryContent.getChildren()) {
                        child.destroy();
                    }
                    recentPkgHistoryContent.addChild(column);
                    recentPkgHistoryContent.markForRedraw();
                }
            });
    }

    /** Fetches recent metric information and updates the DynamicForm instance with i)sparkline information,
     * ii) link to recent metric graph for more details and iii) last metric value formatted to show significant
     * digits.
     */
    private void getRecentMetrics() {
        //display container
        final LocatableVLayout column = new LocatableVLayout(recentMeasurementsContent.extendLocatorId("Content"));
        final int resourceId = this.resourceComposite.getResource().getId();

        //retrieve all relevant measurement definition ids.
        Set<MeasurementDefinition> definitions = this.resourceComposite.getResource().getResourceType()
            .getMetricDefinitions();

        //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
        final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
        for (MeasurementDefinition definition : definitions) {
            measurementDefMap.put(definition.getDisplayName(), definition);
        }

        //bundle definition ids for asynch call.
        int[] definitionArrayIds = new int[definitions.size()];
        final String[] displayOrder = new String[definitions.size()];
        measurementDefMap.keySet().toArray(displayOrder);
        //sort the charting data ex. Free Memory, Free Swap Space,..System Load
        Arrays.sort(displayOrder);

        //organize definitionArrayIds for ordered request on server.
        int index = 0;
        for (String definitionToDisplay : displayOrder) {
            definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay).getId();
        }

        //make the asynchronous call for all the measurement data
        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
            System.currentTimeMillis() - (1000L * 60 * 60 * 8), System.currentTimeMillis(), 60,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent metrics charting data for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                    if (!results.isEmpty()) {
                        //iterate over the retrieved charting data
                        for (int index = 0; index < displayOrder.length; index++) {

                            //retrieve the correct measurement definition
                            MeasurementDefinition md = measurementDefMap.get(displayOrder[index]);

                            //load the data results for the given metric definition
                            List<MeasurementDataNumericHighLowComposite> data = results.get(index);

                            //locate last and minimum values.
                            double lastValue = -1;
                            double minValue = Double.MAX_VALUE;//
                            for (MeasurementDataNumericHighLowComposite d : data) {
                                if ((!Double.isNaN(d.getValue()))
                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
                                    if (d.getValue() < minValue) {
                                        minValue = d.getValue();
                                    }
                                    lastValue = d.getValue();
                                }
                            }

                            //collapse the data into comma delimited list for consumption by third party javascript library(jquery.sparkline)
                            String commaDelimitedList = "";

                            for (MeasurementDataNumericHighLowComposite d : data) {
                                if ((!Double.isNaN(d.getValue()))
                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
                                    commaDelimitedList += d.getValue() + ",";
                                }
                            }
                            LocatableDynamicForm row = new LocatableDynamicForm(column.extendLocatorId("ContentForm"));
                            row.setNumCols(3);
                            HTMLFlow graph = new HTMLFlow();
                            //                        String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0'>"
                            //                            + commaDelimitedList + "</span>";
                            String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0' "
                                + "values='" + commaDelimitedList + "'>...</span>";
                            graph.setContents(contents);
                            graph.setContentsType(ContentsType.PAGE);
                            //diable scrollbars on span
                            graph.setScrollbarSize(0);

                            CanvasItem graphContainer = new CanvasItem();
                            graphContainer.setShowTitle(false);
                            graphContainer.setHeight(16);
                            graphContainer.setWidth(60);
                            graphContainer.setCanvas(graph);

                            //Link/title element
                            LinkItem link = new LinkItem();
                            link.setLinkTitle(md.getDisplayName() + ":");
                            //TODO: spinder, change link whenever portal.war/graphing is removed.
                            link
                                .setValue("/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&id="
                                    + resourceId + "&m=" + md.getId());
                            link.setTarget("_self");
                            link.setShowTitle(false);

                            //Value
                            StaticTextItem value = new StaticTextItem();
                            String convertedValue = lastValue + " " + md.getUnits();
                            convertedValue = convertLastValueForDisplay(lastValue, md);

                            value.setDefaultValue(convertedValue);
                            value.setShowTitle(false);
                            value.setShowPickerIcon(false);
                            value.setWrap(false);
                            row.setItems(graphContainer, link, value);
                            //if graph content returned
                            if ((md.getName().trim().indexOf("Trait.") == -1) && (lastValue != -1)) {
                                column.addMember(row);
                            }
                        }
                        //call out to 3rd party javascript lib
                        graphSparkLines();
                    } else {
                        column.setContents(RECENT_MEASUREMENTS_NONE);
                    }
                }
            });

        //cleanup
        for (Canvas child : recentMeasurementsContent.getChildren()) {
            child.destroy();
        }
        recentMeasurementsContent.setContents("");
        recentMeasurementsContent.addChild(column);
        //        graphSparkLines();
        recentMeasurementsContent.markForRedraw();
        if (sparklineReloader == null) {
            sparklineReloader = new Timer() {
                public void run() {
                    refresh();
                }
            };
            sparklineReloader.schedule(750);
        }
    }

    /** Takes last double value returned and the relevant MeasurementDefinition and formats
     *  the results for display in the UI.  'Formatting' refers to relevant rounding,
     *  number format for significant digits depending upon the measurement definition
     *  details.
     *
     * @param lastValue
     * @param md MeasurementDefinition
     * @return formatted String representation of the last value retrieved.
     */
    protected String convertLastValueForDisplay(double lastValue, MeasurementDefinition md) {
        String convertedValue = "";
        String[] convertedValues = GwtMonitorUtils.formatSimpleMetrics(new double[] { lastValue }, md);
        convertedValue = convertedValues[0];

        return convertedValue;
    }

    //This is a JSNI call out to the third party javascript lib to execute on the data inserted into the DOM.
    public static native void graphSparkLines()
    /*-{
     //find all elements where attribute class contains 'dynamicsparkline' and graph their contents
     $wnd.jQuery('.dynamicsparkline').sparkline();
    }-*/;
}
