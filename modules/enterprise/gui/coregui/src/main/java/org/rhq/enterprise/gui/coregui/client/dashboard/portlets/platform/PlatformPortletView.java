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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform;

import java.util.HashMap;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletView;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;

/**
 * @author Greg Hinkle
 */
public class PlatformPortletView extends VLayout implements PortletView {


    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();
    private ResourceTypeGWTServiceAsync typeService = GWTServiceLookup.getResourceTypeGWTService();


    private PlatformListGrid listGrid;

    private HashMap<Integer, ResourceType> types = new HashMap<Integer, ResourceType>();
    private HashMap<Integer, PlatformMetricDefinitions> platformMetricDefinitionsHashMap = new HashMap<Integer, PlatformMetricDefinitions>();
    public static final String KEY = "Platforms Summary";


    public PlatformPortletView() {

        prefetch();
    }

    private void prefetch() {

        ResourceTypeCriteria typeCriteria = new ResourceTypeCriteria();
        typeCriteria.addFilterCategory(ResourceCategory.PLATFORM);
        typeCriteria.fetchMetricDefinitions(true);
        typeCriteria.fetchOperationDefinitions(true);

        // TODO GH: Find a way to pass resource type criteria lookups through the type cache
        typeService.findResourceTypesByCriteria(typeCriteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Couldn't load type data", caught);
            }

            public void onSuccess(PageList<ResourceType> result) {
                setTypes(result);
                buildUI();
            }
        });
    }


    private void buildUI() {

        addMember(new HeaderLabel("Platforms"));

        listGrid = new PlatformListGrid();
        listGrid.setDataSource(new PlatformMetricDataSource(this));
        listGrid.setInitialCriteria(new Criteria("category", ResourceCategory.PLATFORM.name()));
        listGrid.setAutoFetchData(true);
        listGrid.setUseAllDataSourceFields(true);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);

        ListGridField nameField = new ListGridField("name", "Name", 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Resource/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });
        listGrid.setFields(nameField);

        addMember(listGrid);

        listGrid.getField("icon").setWidth(25);

        listGrid.hideField("id");
        listGrid.hideField("description");
        listGrid.hideField("pluginName");
        listGrid.hideField("category");
        listGrid.hideField("currentAvailability");


    }


    protected void loadMetricsForResource(Resource resource, final ListGridRecord record) {
        final PlatformMetricDefinitions pmd = platformMetricDefinitionsHashMap.get(resource.getResourceType().getId());
        measurementService.findLiveData(
                resource.getId(),
                pmd.getDefinitionIds(),
                new AsyncCallback<Set<MeasurementData>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load platform metrics", caught);
                    }

                    public void onSuccess(Set<MeasurementData> result) {

                        for (MeasurementData data : result) {
                            if (data instanceof MeasurementDataNumeric) {
                                record.setAttribute(data.getName(), ((MeasurementDataNumeric) data).getValue());
                            }
                        }

                        /*double idle = record.getAttributeAsDouble(CPUMetric.Idle.property);
                        record.setAttribute("cpu", 1 - idle);

                        double totalMem = record.getAttributeAsDouble(MemoryMetric.Total.property);
                        double usedMem = record.getAttributeAsDouble(MemoryMetric.Used.property);
                        double percent = usedMem / totalMem;
                        record.setAttribute("memory", percent);
                        */

                        listGrid.setSortField(1);
                        listGrid.refreshFields();
                        markForRedraw();
                    }
                });
    }


    private void setTypes(PageList<ResourceType> types) {

        for (ResourceType platformType : types) {

            Set<MeasurementDefinition> defs = platformType.getMetricDefinitions();

            PlatformMetricDefinitions pmd = new PlatformMetricDefinitions();
            pmd.freeMemory = findDef(defs, MemoryMetric.Free.property);
            pmd.usedMemory = findDef(defs, MemoryMetric.Used.property);
            pmd.totalMemory = findDef(defs, MemoryMetric.Total.property);

            pmd.freeSwap = findDef(defs, SwapMetric.Free.property);
            pmd.usedSwap = findDef(defs, SwapMetric.Used.property);
            pmd.totalSwap = findDef(defs, SwapMetric.Total.property);

            pmd.idleCpu = findDef(defs, CPUMetric.Idle.property);
            pmd.systemCpu = findDef(defs, CPUMetric.System.property);
            pmd.userCpu = findDef(defs, CPUMetric.User.property);
            pmd.waitCpu = findDef(defs, CPUMetric.Wait.property);

            platformMetricDefinitionsHashMap.put(platformType.getId(), pmd);
        }
    }

    private MeasurementDefinition findDef(Set<MeasurementDefinition> defs, String property) {
        for (MeasurementDefinition def : defs) {
            if (def.getName().equals(property)) {
                return def;
            }
        }
        return null;
    }

    public void configure(StoredPortlet storedPortlet) {
        // TODO: Implement this method.
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet displays information about platforms in inventory.");
    }

    public Canvas getSettingsCanvas() {
        return null;  // TODO: Implement this method.
    }


    private static class PlatformListGrid extends ListGrid {

        private PlatformListGrid() {
            setShowRecordComponents(true);
            setShowRecordComponentsByCell(true);
        }

        @Override
        protected Canvas createRecordComponent(ListGridRecord listGridRecord, Integer colNum) {

            String fieldName = this.getFieldName(colNum);

            try {
                if (fieldName.equals("cpu")) {
                    if (listGridRecord.getAttribute(CPUMetric.Idle.property) != null) {
                        HLayout bar = new HLayout();
                        bar.setHeight(18);
                        bar.setWidth100();

                        double value = listGridRecord.getAttributeAsDouble(CPUMetric.Idle.property);
                        value = 1 - value;

                        HTMLFlow text = new HTMLFlow(MeasurementConverterClient.format(value, MeasurementUnits.PERCENTAGE, true));
                        text.setAutoWidth();
                        bar.addMember(text);

                        Img first = new Img("availBar/up.png");
                        first.setHeight(18);
                        first.setWidth((value * 100) + "%");
                        bar.addMember(first);

                        Img second = new Img("availBar/unknown.png");
                        second.setHeight(18);
                        second.setWidth((100 - (value * 100)) + "%");
                        bar.addMember(second);


                        return bar;
                    }


                } else if (fieldName.equals("memory")) {
                    if (listGridRecord.getAttribute(MemoryMetric.Total.property) != null) {
                        HLayout bar = new HLayout();
                        bar.setHeight(18);
                        bar.setWidth100();

                        double total = listGridRecord.getAttributeAsDouble(MemoryMetric.Total.property);
                        double value = listGridRecord.getAttributeAsDouble(MemoryMetric.Used.property);
                        double percent = value / total;

                        HTMLFlow text = new HTMLFlow(MeasurementConverterClient.format(percent, MeasurementUnits.PERCENTAGE, true));
                        text.setAutoWidth();
                        bar.addMember(text);

                        Img first = new Img("availBar/up.png");
                        first.setHeight(18);
                        first.setWidth((percent * 100) + "%");
                        bar.addMember(first);

                        Img second = new Img("availBar/unknown.png");
                        second.setHeight(18);
                        second.setWidth((100 - (percent * 100)) + "%");
                        bar.addMember(second);


                        return bar;
                    }

                }
                return null;

            } catch (Exception e) {
                // expected until first data loaded
                return null;
            }

        }
    }


    private enum MemoryMetric {
        Used("Native.MemoryInfo.used"), Free("Native.MemoryInfo.free"), Total("Native.MemoryInfo.total");

        private final String property;

        MemoryMetric(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }


    private enum CPUMetric {
        Idle("CpuPerc.idle"), System("CpuPerc.sys"), User("CpuPerc.user"), Wait("CpuPerc.wait");

        private final String property;

        CPUMetric(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }

    private enum SwapMetric {
        Used("Native.SwapInfo.used"), Free("Native.SwapInfo.free"), Total("Native.SwapInfo.total");

        private final String property;

        SwapMetric(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }


    private static class PlatformMetricDefinitions {

        MeasurementDefinition freeMemory, usedMemory, totalMemory;
        MeasurementDefinition freeSwap, usedSwap, totalSwap;
        MeasurementDefinition idleCpu, systemCpu, userCpu, waitCpu;

        MeasurementDefinition[] definitions = new MeasurementDefinition[]{freeMemory, usedMemory, totalMemory, freeSwap, usedSwap, totalSwap, idleCpu, systemCpu, userCpu, waitCpu};


        int[] getDefinitionIds() {
            return new int[]{freeMemory.getId(), usedMemory.getId(), totalMemory.getId(),
                    freeSwap.getId(), usedSwap.getId(), totalSwap.getId(),
                    idleCpu.getId(), systemCpu.getId(), userCpu.getId(), waitCpu.getId()};
        }


    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final PortletView getInstance() {
            return GWT.create(PlatformPortletView.class);
        }
    }
}
