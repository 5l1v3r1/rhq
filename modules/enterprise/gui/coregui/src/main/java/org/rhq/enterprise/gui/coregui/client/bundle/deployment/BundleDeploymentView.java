/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.bundle.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentView extends VLayout implements BookmarkableView {
    private BundleGWTServiceAsync bundleService;

    private BundleDeployment deployment;
    private BundleVersion version;
    private Bundle bundle;

    private VLayout detail;

    public BundleDeploymentView() {
        setWidth100();
        setHeight100();
        setMargin(10);
    }

    private void viewBundleDeployment(BundleDeployment bundleDeployment, ViewId current) {

        this.deployment = bundleDeployment;
        this.version = bundleDeployment.getBundleVersion();
        this.bundle = bundleDeployment.getBundleVersion().getBundle();

        addMember(new BackButton("Back to Destination: " + deployment.getDestination().getName(), "Bundles/Bundle/" + version.getBundle().getId() + "/destinations/" + deployment.getDestination().getId()));


        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleDeployment_24.png"), deployment.getName()));

        DynamicForm form = new DynamicForm();
        form.setNumCols(4);

        LinkItem bundleName = new LinkItem("bundle");
        bundleName.setTitle("Bundle");
        bundleName.setValue("#Bundles/Bundle/" + bundle.getId());
        bundleName.setLinkTitle(bundle.getName());
        bundleName.setTarget("_self");

        CanvasItem tagItem = new CanvasItem("tag");
        tagItem.setShowTitle(false);
        TagEditorView tagEditor = new TagEditorView(version.getTags(), false, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleDeploymentTags(deployment.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to update bundle deployment's tags", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                        new Message("Bundle Deployment Tags updated", Message.Severity.Info));
                            }
                        });
            }
        });
        tagEditor.setVertical(true);
        tagItem.setCanvas(tagEditor);
        tagItem.setRowSpan(4);

        StaticTextItem deployed = new StaticTextItem("deployed", "Deployed");
        deployed.setValue(new Date(deployment.getCtime()));

        LinkItem destinationGroup = new LinkItem("group");
        destinationGroup.setTitle("Group");
        destinationGroup.setValue(LinkManager.getResourceGroupLink(deployment.getDestination().getGroup().getId()));
        destinationGroup.setLinkTitle(deployment.getDestination().getGroup().getName());
        destinationGroup.setTarget("_self");


        StaticTextItem path = new StaticTextItem("path", "Path");
        path.setValue(deployment.getDestination().getDeployDir());

        form.setFields(bundleName, tagItem, deployed, destinationGroup, path);

        addMember(form);

        Table deployments = createDeploymentsTable();
        deployments.setHeight("30%");
        deployments.setShowResizeBar(true);
        deployments.setResizeBarTarget("next");
        addMember(deployments);

        detail = new VLayout();
        detail.setAutoHeight();
        detail.hide();
        addMember(detail);

    }

    private Table createDeploymentsTable() {
        Table table = new Table("Deployment Machines");


        table.setTitleComponent(new HTMLFlow("Select a row to show install detials"));

        ListGridField resourceIcon = new ListGridField("resourceAvailabity", "");
        HashMap<String, String> icons = new HashMap<String, String>();
        icons.put("UP", "types/Platform_up_16.png");
        icons.put("DOWN", "types/Platform_down_16.png");
        resourceIcon.setValueIcons(icons);
        resourceIcon.setValueIconSize(16);
        resourceIcon.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "";
            }
        });
        resourceIcon.setWidth(30);


        ListGridField resource = new ListGridField("resource", "Platform");
        resource.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("resourceId")) + "\">" + o + "</a>";

            }
        });
        ListGridField resourceVersion = new ListGridField("resourceVersion", "Operating System");
        ListGridField status = new ListGridField("status", "Status");
        HashMap<String, String> statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setWidth(80);


        table.getListGrid().setFields(resourceIcon, resource, resourceVersion, status);

        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();
        for (BundleResourceDeployment rd : deployment.getResourceDeployments()) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("resource", rd.getResource().getName());


            record.setAttribute("resourceAvailabity", rd.getResource().getCurrentAvailability().getAvailabilityType().name());
            record.setAttribute("resourceId", rd.getResource().getId());
            record.setAttribute("resourceVersion", rd.getResource().getVersion());
            record.setAttribute("status", rd.getStatus().name());
            record.setAttribute("id", rd.getId());
            record.setAttribute("entity", rd);
            records.add(record);
        }

        table.getListGrid().setData(records.toArray(new ListGridRecord[records.size()]));

        table.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {

                    BundleResourceDeployment bundleResourceDeployment = (BundleResourceDeployment) selectionEvent
                            .getRecord().getAttributeAsObject("entity");
                    BundleResourceDeploymentHistoryListView detailView = new BundleResourceDeploymentHistoryListView(
                            bundleResourceDeployment);

                    detail.removeMembers(detail.getMembers());
                    detail.addMember(detailView);
                    detail.setHeight("50%");
                    detail.animateShow(AnimationEffect.SLIDE);

                    /*
                                        BundleResourceDeploymentCriteria criteria = new BundleResourceDeploymentCriteria();
                                        criteria.addFilterId(selectionEvent.getRecord().getAttributeAsInt("id"));
                                        criteria.fetchHistories(true);
                                        criteria.fetchResource(true);
                                        criteria.fetchBundleDeployment(true);
                                        bundleService.findBundleResourceDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleResourceDeployment>>() {
                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError("Failed to load resource deployment history details",caught);
                                            }

                                            public void onSuccess(PageList<BundleResourceDeployment> result) {

                                            }
                                        });
                    */

                } else {
                    detail.animateHide(AnimationEffect.SLIDE);
                }
            }
        });

        return table;
    }

    public void renderView(final ViewPath viewPath) {
        int bundleDeploymentId = Integer.parseInt(viewPath.getCurrent().getPath());

        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
        criteria.addFilterId(bundleDeploymentId);
        criteria.fetchBundleVersion(true);
        criteria.fetchConfiguration(true);
        criteria.fetchResourceDeployments(true);
        criteria.fetchDestination(true);
        criteria.fetchTags(true);

        bundleService = GWTServiceLookup.getBundleService();
        bundleService.findBundleDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleDeployment>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load budle version", caught);
            }

            public void onSuccess(PageList<BundleDeployment> result) {

                final BundleDeployment deployment = result.get(0);

                BundleCriteria bundleCriteria = new BundleCriteria();
                bundleCriteria.addFilterId(deployment.getBundleVersion().getBundle().getId());
                bundleService.findBundlesByCriteria(bundleCriteria, new AsyncCallback<PageList<Bundle>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to find bundle", caught);
                    }

                    public void onSuccess(PageList<Bundle> result) {

                        deployment.getBundleVersion().setBundle(result.get(0));

                        BundleResourceDeploymentCriteria criteria = new BundleResourceDeploymentCriteria();
                        criteria.addFilterBundleDeploymentId(deployment.getId());
                        criteria.fetchHistories(true);
                        criteria.fetchResource(true);
                        criteria.fetchBundleDeployment(true);
                        bundleService.findBundleResourceDeploymentsByCriteria(criteria,
                                new AsyncCallback<PageList<BundleResourceDeployment>>() {

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError("Failed to load deployment detail", caught);
                                    }

                                    public void onSuccess(PageList<BundleResourceDeployment> result) {

                                        deployment.setResourceDeployments(result);

                                        viewBundleDeployment(deployment, viewPath.getCurrent());

                                    }
                                });

                    }
                });

            }
        });

    }

}
