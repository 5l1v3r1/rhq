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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.lookup.ResourceLookupComboBoxItem;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricStackedBarGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceMetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceScheduledMetricDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public class ResourceGraphPortlet extends ResourceMetricD3Graph implements CustomSettingsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceMetric";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resourceMetric();
    public static final String CFG_RESOURCE_ID = "resourceId";
    public static final String CFG_DEFINITION_ID = "definitionId";
    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;

    public ResourceGraphPortlet() {
        super();
        isPortalGraph = true;
        //setChartHeight("100%");
        setOverflow(Overflow.HIDDEN);
        setGraph(new MetricStackedBarGraph(new MetricGraphData()));
    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        Log.debug("\n\n\nPortlet Configure !!");

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            PropertySimple resourceIdProperty = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID);
            PropertySimple measurementDefIdProperty = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
            if (resourceIdProperty != null && measurementDefIdProperty != null) {
                final Integer entityId = resourceIdProperty.getIntegerValue();
                final Integer measurementDefId = measurementDefIdProperty.getIntegerValue();
                graph.setDefinitionId(measurementDefId);
                graph.setEntityId(entityId);
                if (Log.isDebugEnabled()) {
                    Log.debug("Metric Graph Data: " + graph.getMetricGraphData());
                }

                if (entityId != null && measurementDefId != null) {

                    queryResource(entityId, measurementDefId);
                }
            }
        }
    }

    private void queryResource(Integer entityId, final Integer measurementDefId) {
        //final long startTime = System.currentTimeMillis();

        ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(entityId);
        resourceService.findResourcesByCriteria(resourceCriteria, new AsyncCallback<PageList<Resource>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
            }

            @Override
            public void onSuccess(PageList<Resource> result) {
                if (result.isEmpty()) {
                    return;
                }
                // only concerned with first resource since this is a query by id
                final Resource resource = result.get(0);
                HashSet<Integer> typesSet = new HashSet<Integer>();
                typesSet.add(resource.getResourceType().getId());
                HashSet<String> ancestries = new HashSet<String>();
                ancestries.add(resource.getAncestry());
                // In addition to the types of the result resources, get the types of their ancestry
                typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                    typesSet.toArray(new Integer[typesSet.size()]),
                    EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                    new ResourceTypeRepository.TypesLoadedCallback() {

                        @Override
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            ResourceType type = types.get(resource.getResourceType().getId());
                            for (final MeasurementDefinition def : type.getMetricDefinitions()) {
                                if (def.getId() == measurementDefId) {
                                    Log.debug("Found portlet measurement definition !" + def);

                                    getJsniChart().setEntityId(resource.getId());
                                    getJsniChart().setDefinition(def);
                                    queryMetricsDataForDashboardGraphs(resource.getId(), def);
                                    break;
                                }
                            }
                        }
                    });
            }
        });
    }

    private void queryMetricsDataForDashboardGraphs(final Integer entityId, final MeasurementDefinition def) {
        final long startTime = System.currentTimeMillis();

        // setting up a deferred Command to execute after all resource queries have completed (successfully or unsuccessfully)
        // we know there are exactly 2 resources
        final CountDownLatch countDownLatch = CountDownLatch.create(2, new Command() {
            @Override
            /**
             * Satisfied only after ALL of the metric queries AND availability have completed
             */
            public void execute() {
                Log.debug("Dashboard chart query total time for entity: " + entityId + ", MeasurementDef: "
                    + def.getId() + " in " + (System.currentTimeMillis() - startTime) + " ms");
                drawGraph();
            }
        });

        queryMeasurementsAndMetricData(entityId, def.getId(), countDownLatch);
        queryAvailability(entityId, countDownLatch);
        // now the countDown latch will run sometime asynchronously after BOTH the previous 2 queries have executed
    }

    private void queryAvailability(final Integer entityId, final CountDownLatch countDownLatch) {

        final long startTime = System.currentTimeMillis();

        // now return the availability
        AvailabilityCriteria c = new AvailabilityCriteria();
        c.addFilterResourceId(entityId);
        c.addFilterInitialAvailability(false);
        c.addSortStartTime(PageOrdering.ASC);
        GWTServiceLookup.getAvailabilityService().findAvailabilityByCriteria(c,
            new AsyncCallback<PageList<Availability>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
                    countDownLatch.countDown();
                }

                @Override
                public void onSuccess(PageList<Availability> availList) {
                    Log.debug("Dashboard chart availability query for: " + entityId + ", in: "
                        + (System.currentTimeMillis() - startTime) + " ms.");
                    PageList<Availability> availabilityList = new PageList<Availability>();
                    for (Availability availability : availList) {
                        if (!availability.getAvailabilityType().equals(AvailabilityType.UP)) {
                            availabilityList.add(availability);
                        }
                    }
                    graph.getMetricGraphData().setAvailabilityList(availabilityList);
                    countDownLatch.countDown();
                }
            });
    }

    private void queryMeasurementsAndMetricData(final Integer entityId, final Integer definitionId,
        final CountDownLatch countDownLatch) {
        final long startTime = System.currentTimeMillis();
        //
        GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(entityId, new int[] { definitionId },
            8, MeasurementUtils.UNIT_HOURS, 60,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(), caught);
                    countDownLatch.countDown();
                }

                @Override
                public void onSuccess(final List<List<MeasurementDataNumericHighLowComposite>> measurementData) {
                    Log.debug("Dashboard Metric data in: " + (System.currentTimeMillis() - startTime) + " ms.");
                    graph.getMetricGraphData().setMetricData(measurementData.get(0));
                    countDownLatch.countDown();
                }
            });
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_graph());
    }

    @Override
    protected void onDraw() {
        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        PropertySimple simple = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID);
        if (simple == null || simple.getIntegerValue() == null) {
            removeMembers(getMembers());
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        } else {
            super.onDraw();
        }
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        final DynamicForm form = new DynamicForm();

        final ResourceLookupComboBoxItem resourceLookupComboBoxItem = new ResourceLookupComboBoxItem(CFG_RESOURCE_ID,
            MSG.common_title_resource());
        resourceLookupComboBoxItem.setWidth(300);

        final SelectItem metric = new SelectItem(CFG_DEFINITION_ID, MSG.common_title_metric()) {
            @Override
            protected Criteria getPickListFilterCriteria() {
                Criteria criteria = new Criteria();

                if (resourceLookupComboBoxItem.getValue() != null) {
                    int resourceId = (Integer) resourceLookupComboBoxItem.getValue();
                    criteria.addCriteria(CFG_RESOURCE_ID, resourceId);
                }
                return criteria;
            }
        };

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        metric.setWidth(300);
        metric.setValueField("id");
        metric.setDisplayField("displayName");
        metric.setOptionDataSource(new ResourceScheduledMetricDatasource());

        resourceLookupComboBoxItem
            .addChangedHandler(new com.smartgwt.client.widgets.form.fields.events.ChangedHandler() {
                public void onChanged(ChangedEvent event) {

                    if (form.getValue(CFG_RESOURCE_ID) instanceof Integer) {
                        metric.fetchData();
                        form.clearValue(CFG_DEFINITION_ID);
                    }
                }
            });

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            Integer integerValue = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID).getIntegerValue();
            if (integerValue != null) {
                form.setValue(CFG_RESOURCE_ID, integerValue);
            }

            PropertySimple propertySimple = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
            if (propertySimple != null && propertySimple.getIntegerValue() != null) {
                form.setValue(CFG_DEFINITION_ID, propertySimple.getIntegerValue());
            }
        }

        form.setFields(resourceLookupComboBoxItem, metric);

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_RESOURCE_ID, form.getValue(CFG_RESOURCE_ID)));
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_DEFINITION_ID, form.getValue(CFG_DEFINITION_ID)));

                configure(portletWindow, storedPortlet);

                redraw();
            }
        });

        return form;
    }

    @Override
    public void redraw() {
        Log.debug(" *** Redraw Portlet and set data");
        super.redraw();

        removeMembers(getMembers());

        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();
        PropertySimple simple = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID);

        if (simple == null || simple.getIntegerValue() == null) {
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        } else {
            graph.getMetricGraphData().setEntityId(simple.getIntegerValue());
            PropertySimple simpleDefId = storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID);
            Log.debug("EntityId from portal config: " + simple.getIntegerValue());
            Log.debug("DefintionId from portal config: " + simpleDefId.getIntegerValue());
            graph.getMetricGraphData().setDefinitionId(simpleDefId.getIntegerValue());
            drawGraph();
        }
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        @Override
        public final Portlet getInstance(EntityContext context) {

            return new ResourceGraphPortlet();
        }
    }
}
