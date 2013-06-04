/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.admin.storage;

import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ADDRESS;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_JMX_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite.MeasurementAggregateWithUnits;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

/**
 * Shows details of a storage node.
 * 
 * @author Jirka Kremser
 */
public class StorageNodeDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int storageNodeId;

    private static final int SECTION_COUNT = 2;
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection;
    private SectionStackSection loadSection;
    private ListGrid loadDataGrid;

    private volatile int initSectionCount = 0;

    public StorageNodeDetailView(int storageNodeId) {
        super();
        this.storageNodeId = storageNodeId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();
        StorageNodeCriteria criteria = new StorageNodeCriteria();
        criteria.addFilterId(storageNodeId);
        criteria.fetchResource(true);
        GWTServiceLookup.getStorageService().findStorageNodesByCriteria(criteria,
            new AsyncCallback<PageList<StorageNode>>() {
                public void onSuccess(final PageList<StorageNode> storageNodes) {
                    if (storageNodes == null || storageNodes.isEmpty() || storageNodes.size() != 1) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)));
                        initSectionCount = SECTION_COUNT;
                        return;
                    }
                    final StorageNode node = storageNodes.get(0);
                    prepareDetailsSection(sectionStack, node);
                    prepareLoadSection(sectionStack, node);

                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                            + caught.getMessage(), caught);
                    initSectionCount = SECTION_COUNT;
                }
            });
    }

    public boolean isInitialized() {
        return initSectionCount >= SECTION_COUNT;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != loadSection) {
                        sectionStack.addSection(loadSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = SECTION_COUNT;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareDetailsSection(SectionStack stack, final StorageNode storageNode) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        final StaticTextItem nameItem = new StaticTextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
        nameItem.setValue("<b>" + storageNode.getAddress() + "</b>");

        final TextItem jmxPortItem = new TextItem(FIELD_JMX_PORT.propertyName(), FIELD_JMX_PORT.title());
        jmxPortItem.setValue(storageNode.getJmxPort());

        final StaticTextItem jmxConnectionUrlItem = new StaticTextItem("jmxConnectionUrl",
            MSG.view_adminTopology_storageNode_jmxConnectionUrl());
        jmxConnectionUrlItem.setValue(storageNode.getJMXConnectionURL());

        final TextItem cqlPortItem = new TextItem(FIELD_CQL_PORT.propertyName(), FIELD_CQL_PORT.title());
        
        cqlPortItem.setValue(storageNode.getCqlPort());

        final SelectItem operationModeItem = new SelectItem(FIELD_OPERATION_MODE.propertyName(),
            MSG.view_adminTopology_serverDetail_operationMode());
        operationModeItem.setValueMap("NORMAL", "MAINTENANCE");
        operationModeItem.setValue(storageNode.getOperationMode());

        // make clickable link to associated resource
        StaticTextItem resourceItem = new StaticTextItem("associatedResource", "Associated Resource");
        String storageNodeItemText = "";
        Resource storageNodeResource = storageNode.getResource();
        if (storageNodeResource != null && storageNodeResource.getName() != null) {
            String detailsUrl = LinkManager.getResourceLink(storageNodeResource.getId());
            String formattedValue = StringUtility.escapeHtml(storageNodeResource.getName());
            storageNodeItemText = LinkManager.getHref(detailsUrl, formattedValue);
        } else {
            storageNodeItemText = MSG.common_label_none();
        }
        resourceItem.setValue(storageNodeItemText);

        StaticTextItem installationDateItem = new StaticTextItem(FIELD_CTIME.propertyName(), FIELD_CTIME.title());
        installationDateItem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getCtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        StaticTextItem lastUpdatetem = new StaticTextItem(FIELD_MTIME.propertyName(), FIELD_MTIME.title());
        lastUpdatetem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        IButton saveButton = new IButton();
        saveButton.setOverflow(Overflow.VISIBLE);
        saveButton.setTitle(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (form.validate()) {
                    storageNode.setOperationMode(OperationMode.valueOf(operationModeItem.getValueAsString()));
                    SC.say(storageNode.toString());
                    // TODO: logic
                }
            }
        });

        form.setItems(nameItem, jmxPortItem, cqlPortItem, jmxConnectionUrlItem, operationModeItem, resourceItem,
            installationDateItem, lastUpdatetem);

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addMember(saveButton);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form, footer);

        detailsSection = section;
        initSectionCount++;
    }

    private void prepareLoadSection(SectionStack stack, final StorageNode storageNode) {
        loadDataGrid = new ListGrid();
        ListGridField nameField = new ListGridField("name", MSG.common_title_metric());
        ListGridField minField = new ListGridField("min", MSG.view_resource_monitor_table_min());
        ListGridField avgField = new ListGridField("avg", MSG.view_resource_monitor_table_avg());
        ListGridField maxField = new ListGridField("max", MSG.view_resource_monitor_table_max());
        nameField.setWidth("40%");
        loadDataGrid.setFields(nameField, minField, avgField, maxField);

        IButton refreshButton = new IButton();
        refreshButton.setOverflow(Overflow.VISIBLE);
        refreshButton.setTitle(MSG.common_button_refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                showFreshLoadData(storageNode);
            }
        });

        showFreshLoadData(storageNode);

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addMember(refreshButton);

        SectionStackSection section = new SectionStackSection("Load");
        section.setItems(loadDataGrid, footer);
        section.setExpanded(true);

        loadSection = section;
        initSectionCount++;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        Log.debug("StorageNodeDetainView: " + viewPath);
    }

    private ListGridRecord makeListGridRecord(MeasurementAggregateWithUnits aggregateWithUnits, String name) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("name", name);
        record.setAttribute("min", MeasurementConverterClient.format(aggregateWithUnits.getAggregate().getMin(),
            aggregateWithUnits.getUnits(), true));
        record.setAttribute("avg", MeasurementConverterClient.format(aggregateWithUnits.getAggregate().getAvg(),
            aggregateWithUnits.getUnits(), true));
        record.setAttribute("max", MeasurementConverterClient.format(aggregateWithUnits.getAggregate().getMax(),
            aggregateWithUnits.getUnits(), true));
        return record;
    }

    private void showFreshLoadData(final StorageNode node) {
        GWTServiceLookup.getStorageService().getLoad(node, 8, MeasurementUtils.UNIT_HOURS,
            new AsyncCallback<StorageNodeLoadComposite>() {
                @SuppressWarnings("unchecked")
                public void onSuccess(final StorageNodeLoadComposite loadComposite) {
                    ListGridRecord[] records = new ListGridRecord[6];

                    List<List<Object>> loadFields = Arrays.<List<Object>> asList(
                        Arrays.<Object> asList(loadComposite.getHeapCommitted(), "Heap Commited"),
                        Arrays.<Object> asList(loadComposite.getHeapUsed(), "Heap Used"),
                        Arrays.<Object> asList(loadComposite.getHeapPercentageUsed(), "Heap Used in Percent"),
                        Arrays.<Object> asList(loadComposite.getLoad(), "Load (data stored on the node)"),
                        Arrays.<Object> asList(loadComposite.getActuallyOwns(), "Token Ownership in Percent"));
                    int i = 0;
                    for (List<Object> aggregateWithUnitsList : loadFields) {
                        if (aggregateWithUnitsList.get(0) != null) {
                            records[i++] = makeListGridRecord(
                                (MeasurementAggregateWithUnits) aggregateWithUnitsList.get(0),
                                (String) aggregateWithUnitsList.get(1));
                        }
                    }
                    records[i] = new ListGridRecord();
                    records[i].setAttribute("name", "Number of Tokens");
                    records[i].setAttribute("min", loadComposite.getTokens().getMin());
                    records[i].setAttribute("avg", loadComposite.getTokens().getAvg());
                    records[i].setAttribute("max", loadComposite.getTokens().getMax());

                    loadDataGrid.setData(records);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                            + caught.getMessage(), caught);
                    initSectionCount = SECTION_COUNT;
                }
            });
    }

}
