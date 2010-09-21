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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions;

import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Joseph Marques
 */
public class SingleGroupDefinitionView extends LocatableVLayout implements BookmarkableView {

    private int groupDefinitionId;
    private GroupDefinition groupDefinition;

    private ViewId viewId;

    // editable form
    private TextItem id;
    private TextItem name;
    private TextAreaItem description;
    private CheckboxItem recursive;
    private SelectItem templateSelector;
    private TextAreaItem expression;
    private SpinnerItem recalculationInterval;

    // read-only form
    private StaticTextItem idStatic;
    private StaticTextItem nameStatic;
    private StaticTextItem descriptionStatic;
    private StaticTextItem recursiveStatic;
    private StaticTextItem expressionStatic;
    private StaticTextItem recalculationIntervalStatic;

    private GroupDefinitionDataSource dataSource;

    public SingleGroupDefinitionView(String locatorId) {
        this(locatorId, null);
    }

    public SingleGroupDefinitionView(String locatorId, GroupDefinition groupDefinition) {
        super(locatorId);

        this.dataSource = GroupDefinitionDataSource.getInstance();

        setPadding(10);
        setOverflow(Overflow.VISIBLE);
        setWidth(5);

        buildForm();

        this.groupDefinition = groupDefinition;
    }

    public void setGroupDefinition(GroupDefinition groupDefinition) {
        this.groupDefinition = groupDefinition;

        // form setup
        id.setValue(groupDefinition.getId());
        idStatic.setValue(groupDefinition.getId());

        name.setValue(groupDefinition.getName());
        nameStatic.setValue(groupDefinition.getName());

        recursive.setValue(groupDefinition.isRecursive());
        recursiveStatic.setValue(groupDefinition.isRecursive());

        description.setValue(groupDefinition.getDescription());
        descriptionStatic.setValue(groupDefinition.getDescription());

        recalculationInterval.setValue(groupDefinition.getRecalculationInterval());
        recalculationIntervalStatic.setValue(groupDefinition.getRecalculationInterval());

        expression.setValue(groupDefinition.getExpression());
        expressionStatic.setValue(groupDefinition.getExpression());

        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("GroupDefinitionForm"));
        form.setFields(id, idStatic, name, nameStatic, description, descriptionStatic, expression, expressionStatic,
            recursive, recursiveStatic, recalculationInterval, recalculationIntervalStatic);
        form.setDataSource(dataSource);
        form.setHiliteRequiredFields(true);
        form.setRequiredTitleSuffix(" <span style=\"color: red;\">* </span>:");
        if (groupDefinition.getId() == 0) {
            form.setSaveOperationType(DSOperationType.ADD);
        } else {
            form.setSaveOperationType(DSOperationType.UPDATE);
        }

        // button setup
        IButton saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                if (form.validate()) {
                    //createOrUpdate();
                    form.saveData(new DSCallback() {
                        @Override
                        public void execute(DSResponse response, Object rawData, DSRequest request) {
                            History.back();
                        }
                    });
                }
            }
        });

        IButton resetButton = new LocatableIButton(this.extendLocatorId("Reset"), "Reset");
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                form.reset();
            }
        });

        HLayout buttonLayout = new HLayout(10); // margin between members
        buttonLayout.setMargin(10); // margin around layout widget
        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(resetButton);

        // canvas setup
        addMember(form);
        addMember(buttonLayout);

        markForRedraw();
    }

    public void switchToEditMode() {
        name.show();
        description.show();
        recursive.show();
        expression.show();
        recalculationInterval.show();

        nameStatic.hide();
        descriptionStatic.hide();
        recursiveStatic.hide();
        expressionStatic.hide();
        recalculationIntervalStatic.hide();

        if (groupDefinitionId == 0) {
            viewId.getBreadcrumbs().get(0).setDisplayName("New Group Definition");
        } else {
            viewId.getBreadcrumbs().get(0).setDisplayName("Editing '" + nameStatic.getValue().toString() + "'");
        }
        CoreGUI.refreshBreadCrumbTrail();

        markForRedraw();
    }

    public void switchToViewMode() {
        name.hide();
        description.hide();
        recursive.hide();
        expression.hide();
        recalculationInterval.hide();

        nameStatic.show();
        descriptionStatic.show();
        recursiveStatic.show();
        expressionStatic.show();
        recalculationIntervalStatic.show();

        viewId.getBreadcrumbs().get(0).setDisplayName("Viewing '" + nameStatic.getValue().toString() + "'");

        markForRedraw();
    }

    private void buildForm() {
        id = new TextItem("id", "ID");
        id.setVisible(false);
        idStatic = new StaticTextItem("idStatic", "ID");
        idStatic.setVisible(false);

        name = new TextItem("name", "Name");
        name.setWidth(400);
        name.setDefaultValue("");
        nameStatic = new StaticTextItem("nameStatic", "Name");

        description = new TextAreaItem("description", "Description");
        description.setWidth(400);
        description.setHeight(50);
        description.setDefaultValue("");
        descriptionStatic = new StaticTextItem("descriptionStatic", "Description");

        recursive = new CheckboxItem("recursive", "Recursive");
        recursiveStatic = new StaticTextItem("recursiveStatic", "Recursive");

        expression = new TextAreaItem("expression", "Expression");
        expression.setWidth(400);
        expression.setHeight(150);
        expression.setDefaultValue("");
        expressionStatic = new StaticTextItem("expressionStatic", "Expression");

        recalculationInterval = new SpinnerItem("recalculationInterval", "Recalculation Interval");
        recalculationInterval.setWrapTitle(false);
        recalculationInterval.setMin(0);
        recalculationInterval.setDefaultValue(0);
        recalculationIntervalStatic = new StaticTextItem("recalculationIntervalStatic", "Recalculation Interval");

        templateSelector = new SelectItem();
        templateSelector.setValueMap(getTemplates());
    }

    public static LinkedHashMap<String, String> getTemplates() {
        LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();

        // grouped items
        items.put("JBossAS clusters in the system", //
            get("groupby resource.trait[partitionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put("Clustered enterprise application archive (EAR)", //
            get("groupby resource.parent.trait[partitionName]", //
                "groupby resource.name", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = Enterprise Application (EAR)"));
        items.put("Unique JBossAS versions in inventory", //
            get("groupby resource.trait[jboss.system:type=Server:VersionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put("Platform resource in inventory", //
            get("resource.type.category = PLATFORM", // 
                "groupby resource.name"));
        items.put("Unique resource type in inventory", //
            get("groupby resource.type.plugin", //
                "groupby resource.type.name"));

        // simple items
        items.put("All JBossAS hosting any version of 'my' app", //
            get("resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server", //
                "resource.child.name.contains = my"));
        items.put("All Non-secured JBossAS servers", //
            get("empty resource.pluginConfiguration[principal]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put("All resources currently down", //
            get("resource.availability = DOWN"));

        return items;
    }

    private static String get(String... pieces) {
        StringBuilder results = new StringBuilder();
        boolean first = true;
        for (String next : pieces) {
            if (first) {
                first = false;
            } else {
                results.append('\n');
            }
            results.append(next);
        }
        return results.toString();
    }

    private void lookupDetails(final int groupDefinitionId, final boolean hasEditPermission) {
        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        criteria.addFilterId(groupDefinitionId);

        if (groupDefinitionId == 0) {
            GroupDefinition newGroupDefinition = new GroupDefinition();
            setGroupDefinition(newGroupDefinition);
            if (hasEditPermission) {
                switchToEditMode();
            } else {
                switchToViewMode();
            }
        } else {
            GWTServiceLookup.getResourceGroupService().findGroupDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<GroupDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            "Failure loading group definition[id=" + groupDefinitionId + "]", caught);
                        History.back();
                    }

                    public void onSuccess(PageList<GroupDefinition> result) {
                        if (result.size() == 0) {
                            CoreGUI.getErrorHandler().handleError(
                                "No group definition exists with id=" + groupDefinitionId);
                            History.back();
                        } else {
                            GroupDefinition existingGroupDefinition = result.get(0);
                            setGroupDefinition(existingGroupDefinition);
                            if (hasEditPermission) {
                                switchToEditMode();
                            } else {
                                switchToViewMode();
                            }
                        }
                    }
                });
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        groupDefinitionId = viewPath.getCurrentAsInt();
        viewId = viewPath.getCurrent();
        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Could not determine whether user had MANAGE_INVENTORY permission, defaulting to view-only mode",
                    caught);
                lookupDetails(groupDefinitionId, false);
            }

            @Override
            public void onSuccess(Set<Permission> result) {
                lookupDetails(groupDefinitionId, result.contains(Permission.MANAGE_INVENTORY));
            }
        });

    }

}
