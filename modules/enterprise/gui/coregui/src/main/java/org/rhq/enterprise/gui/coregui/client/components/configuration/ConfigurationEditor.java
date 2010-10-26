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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.util.ValueCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FloatItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.form.validator.FloatRangeValidator;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.IsBooleanValidator;
import com.smartgwt.client.widgets.form.validator.IsFloatValidator;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellSavedEvent;
import com.smartgwt.client.widgets.grid.events.CellSavedHandler;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.CanvasUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A SmartGWT widget for editing an RHQ {@link Configuration} that conforms to a {@link ConfigurationDefinition}.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
//
// Note: There was a failed attempt at an editor composed with ListGrid components instead of DynamicForm components,
// but there were problems with having different editors active for different rows in the table at the same time.
// Smart says they're working on enhancing this area, but the DynamicForm might be a better option anyway. (ghinkle)
//
@SuppressWarnings({"UnnecessarySemicolon"})
public class ConfigurationEditor extends LocatableVLayout {
    private static final String RHQ_PROPERTY_ATTRIBUTE_NAME = "rhq:property";
    
    private ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    private TabSet tabSet;
    private LocatableToolStrip toolStrip;

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private Configuration originalConfiguration;

    private ValuesManager valuesManager = new ValuesManager();

    private boolean changed = false;

    private Label loadingLabel = new Label("<b>Loading...</b>");

    private int resourceId;
    private int resourceTypeId;
    private ConfigType configType;
    private IButton saveButton;

    private boolean readOnly = false;
    private Set<String> invalidPropertyNames = new HashSet<String>();
    private Set<PropertyValueChangeListener> propertyValueChangeListeners =
        new HashSet<PropertyValueChangeListener>();

    public static enum ConfigType {
        plugin, resource
    }
    ; // Need this extra semicolon for the qdox parser

    public ConfigurationEditor(String locatorId) {
        super(locatorId);
    }

    public ConfigurationEditor(String locatorId, int resourceId, int resourceTypeId) {
        this(locatorId, resourceId, resourceTypeId, ConfigType.resource);
    }

    public ConfigurationEditor(String locatorId, int resourceId, int resourceTypeId, ConfigType configType) {
        super(locatorId);
        this.resourceId = resourceId;
        this.resourceTypeId = resourceTypeId;
        this.configType = configType;
        setOverflow(Overflow.AUTO);
    }

    public ConfigurationEditor(String locatorId, ConfigurationDefinition configurationDefinition,
                               Configuration configuration) {
        super(locatorId);
        this.configuration = configuration;
        this.configurationDefinition = configurationDefinition;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void showError(Throwable failure) {
        addMember(new Label(failure.getMessage()));
    }

    public void showError(String message) {
        addMember(new Label(message));
    }

    public boolean validate() {
        return this.valuesManager.validate();
    }

    public boolean isValid() {
        return this.valuesManager.hasErrors();
    }

    public void addPropertyValueChangeListener(PropertyValueChangeListener propertyValueChangeListener) {
        this.propertyValueChangeListeners.add(propertyValueChangeListener);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(loadingLabel);
        this.redraw();

        final long start = System.currentTimeMillis();

        if (configurationDefinition == null || configuration == null) {

            if (configType == ConfigType.resource) {
                configurationService.getResourceConfiguration(resourceId, new AsyncCallback<Configuration>() {
                    public void onFailure(Throwable caught) {
                        showError(caught);
                    }

                    public void onSuccess(Configuration result) {
                        configuration = result;
                        Log.info("Config retreived in: " + (System.currentTimeMillis() - start));
                        reload();
                    }
                });

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { resourceTypeId },
                    EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                    new ResourceTypeRepository.TypesLoadedCallback() {
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            com.allen_sauer.gwt.log.client.Log.debug("ConfigDef retreived in: " + (System.currentTimeMillis() - start));
                            configurationDefinition = types.get(resourceTypeId).getResourceConfigurationDefinition();
                            if (configurationDefinition == null) {
                                loadingLabel.hide();
                                showError("Configuration is not supported by this Resource.");
                            }
                            reload();
                        }
                    });

            } else if (configType == ConfigType.plugin) {
                configurationService.getPluginConfiguration(resourceId, new AsyncCallback<Configuration>() {
                    public void onFailure(Throwable caught) {
                        showError(caught);
                    }

                    public void onSuccess(Configuration result) {
                        configuration = result;
                        reload();
                    }
                });

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { resourceTypeId },
                    EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
                    new ResourceTypeRepository.TypesLoadedCallback() {
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            Log.debug("ConfigDef retreived in: " + (System.currentTimeMillis() - start));
                            configurationDefinition = types.get(resourceTypeId).getPluginConfigurationDefinition();
                            if (configurationDefinition == null) {
                                showError("Connection settings are not supported by this Resource.");
                            }
                            reload();
                        }
                    });
            }
        } else {
            reload();
        }
    }

    public void reload() {
        if (configurationDefinition == null || configuration == null) {
            // Wait for both to load
            return;
        }

        originalConfiguration = configuration.deepCopy();

        tabSet = new LocatableTabSet(getLocatorId());

        if (configurationDefinition.getConfigurationFormat() == ConfigurationFormat.RAW
            || configurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            com.allen_sauer.gwt.log.client.Log.info("Loading files view...");
            Tab tab = new LocatableTab("Files", "Files");
            tab.setPane(buildRawPane());
            tabSet.addTab(tab);
        }

        if (configurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED
            || configurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            com.allen_sauer.gwt.log.client.Log.info("Loading properties view...");
            Tab tab = new LocatableTab("Properties", "Properties");
            tab.setPane(buildStructuredPane());
            tabSet.addTab(tab);
        }

        for (Canvas c : getChildren()) {
            c.destroy();
        }

        addMember(tabSet);

        this.markForRedraw();
    }

    public void reset() {
        this.configuration = this.originalConfiguration;
        reload();
    }

    protected HLayout buildRawPane() {

        LocatableHLayout layout = new LocatableHLayout(extendLocatorId("Raw"));
        final HashMap<String, RawConfiguration> filesMap = new HashMap<String, RawConfiguration>();

        TreeGrid fileTree = new LocatableTreeGrid(layout.extendLocatorId("Files"));
        fileTree.setShowResizeBar(true);

        Tree files = new Tree();
        files.setModelType(TreeModelType.CHILDREN);
        TreeNode root = new TreeNode("root");
        TreeNode[] children = new TreeNode[configuration.getRawConfigurations().size()];
        int i = 0;
        for (RawConfiguration rawConfiguration : configuration.getRawConfigurations()) {
            children[i++] = new TreeNode(rawConfiguration.getPath());
            filesMap.put(rawConfiguration.getPath(), rawConfiguration);
        }
        root.setChildren(children);
        files.setRoot(root);
        fileTree.setData(files);
        fileTree.setWidth(250);

        DynamicForm form = new LocatableDynamicForm(layout.extendLocatorId("Editor"));
        final TextAreaItem rawEditor = new TextAreaItem();
        //        rawEditor.setValue("This is a test");
        rawEditor.setShowTitle(false);
        form.setItems(rawEditor);
        form.setHeight100();
        form.setWidth100();

        fileTree.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                String path = selectionEvent.getRecord().getAttribute("name");
                com.allen_sauer.gwt.log.client.Log.info("Getting Path: " + path);
                rawEditor.setValue(filesMap.get(path).getContents());
                rawEditor.redraw();
                com.allen_sauer.gwt.log.client.Log.info("Data: " + filesMap.get(path).getContents());
            }
        });

        if (children.length > 0) {
            fileTree.selectRecord(children[0]);
            rawEditor.setValue(filesMap.get(children[0].getName()).getContents());
        }

        layout.setMembers(fileTree, form);
        return layout;
    }

    protected VLayout buildStructuredPane() {

        LocatableVLayout layout = new LocatableVLayout(extendLocatorId("Structured"));
        List<PropertyGroupDefinition> groupDefinitions = configurationDefinition.getGroupDefinitions();

        final SectionStack sectionStack = new LocatableSectionStack(layout.extendLocatorId("Sections"));
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setScrollSectionIntoView(true);
        sectionStack.setOverflow(Overflow.AUTO);

        if (!configurationDefinition.getNonGroupedProperties().isEmpty()) {
            sectionStack.addSection(buildGroupSection(layout.extendLocatorId("NoGroup"), null));
        }

        for (PropertyGroupDefinition definition : groupDefinitions) {
            //            com.allen_sauer.gwt.log.client.Log.info("building: " + definition.getDisplayName());
            sectionStack.addSection(buildGroupSection(layout.extendLocatorId(definition.getName()), definition));
        }

        // TODO GH: Save button as saveListener() or remove the buttons from this form and have
        // the container provide them?

        toolStrip = new LocatableToolStrip(layout.extendLocatorId("Tools"));
        toolStrip.setBackgroundImage(null);

        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());
        saveButton = new LocatableIButton(toolStrip.extendLocatorId("Save"), "Save");
        saveButton.setAlign(Alignment.CENTER);
        saveButton.setDisabled(true);
        //        toolStrip.addMember(saveButton);

        IButton resetButton = new LocatableIButton(toolStrip.extendLocatorId("Reset"), "Reset");
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                reload();
            }
        });

        Menu menu = new LocatableMenu(toolStrip.extendLocatorId("JumpMenu"));
        for (SectionStackSection section : sectionStack.getSections()) {
            MenuItem item = new MenuItem(section.getTitle());
            item.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    int x = event.getMenu().getItemNum(event.getItem());
                    sectionStack.expandSection(x);
                    sectionStack.showSection(x);
                }
            });
            menu.addItem(item);
        }
        menu.addItem(new MenuItemSeparator());

        MenuItem hideAllItem = new MenuItem("Hide All");
        hideAllItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                for (int i = 0; i < sectionStack.getSections().length; i++) {
                    sectionStack.collapseSection(i);
                }
            }
        });
        menu.addItem(hideAllItem);

        //        toolStrip.addMember(resetButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(new LocatableIMenuButton(toolStrip.extendLocatorId("Jump"), "Jump to Section", menu));

        layout.addMember(toolStrip);

        layout.addMember(sectionStack);

        return layout;
    }

    public SectionStackSection buildGroupSection(String locatorId, PropertyGroupDefinition group) {
        SectionStackSection section;
        if (group == null) {
            section = new SectionStackSection("General Properties");

        } else {
            section = new SectionStackSection(
                "<div style=\"float:left; font-weight: bold;\">"
                    + group.getDisplayName()
                    + "</div>"
                    + (group.getDescription() != null ? ("<div style='padding-left: 10px; font-weight: normal; font-size: smaller; float: left;'>"
                        + " -" + group.getDescription() + "</div>")
                        : ""));
            section.setExpanded(!group.isDefaultHidden());
        }

        List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>(((group == null) ? configurationDefinition
            .getNonGroupedProperties() : configurationDefinition.getPropertiesInGroup(group.getName())));

        DynamicForm form = buildPropertiesForm(locatorId + "_Props", propertyDefinitions, configuration, true);

        section.addItem(form);
        return section;
    }

    protected DynamicForm buildPropertiesForm(String locatorId, Collection<PropertyDefinition> propertyDefinitions,
        AbstractPropertyMap propertyMap, boolean firePropertyChangedEvents) {

        LocatableDynamicForm form = new LocatableDynamicForm(locatorId);
        form.setValuesManager(valuesManager);
        form.setValidateOnChange(true);

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                if (!changed) {
                    changed = true;
                    CanvasUtility.blink(saveButton);
                    saveButton.setDisabled(false);
                }
            }
        });

        form.setNumCols(4);
        form.setCellPadding(5);
        form.setColWidths(190, 28, 210);

        List<FormItem> fields = new ArrayList<FormItem>();
        addItemsForPropertiesRecursively(locatorId, propertyDefinitions, propertyMap, fields);
        form.setFields(fields.toArray(new FormItem[fields.size()]));

        return form;
    }

    private void addItemsForPropertiesRecursively(String locatorId, Collection<PropertyDefinition> propertyDefinitions,
                                                  AbstractPropertyMap propertyMap, List<FormItem> fields) {        
        boolean odd = true;
        List<PropertyDefinition> sortedPropertyDefinitions = new ArrayList<PropertyDefinition>(propertyDefinitions);
        Collections.sort(sortedPropertyDefinitions, new PropertyDefinitionComparator());
        for (PropertyDefinition propertyDefinition : sortedPropertyDefinitions) {
            Property property = propertyMap.get(propertyDefinition.getName());
            if (property == null) {
                if (propertyDefinition instanceof PropertyDefinitionSimple) {
                    property = new PropertySimple(propertyDefinition.getName(), null);
                    propertyMap.put(property);
                }
            }
            addItemsForPropertyRecursively(locatorId + "_" + propertyDefinition.getName(), propertyDefinition, property,
                odd, fields);
            odd = !odd;
        }
    }

    public void addItemsForPropertyRecursively(String locatorId, PropertyDefinition propertyDefinition,
                                               Property property,
                                               boolean oddRow,
                                               List<FormItem> fields) {
        List<FormItem> fieldsForThisProperty;

        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            final PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple)propertyDefinition;
            PropertySimple propertySimple = (PropertySimple)property;

            if (propertySimple == null) {
                propertySimple = new PropertySimple(propertyDefinitionSimple.getName(), null);
            }

            fieldsForThisProperty = buildFieldsForPropertySimple(propertyDefinition, propertyDefinitionSimple,
                propertySimple);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList)propertyDefinition;
            PropertyDefinition memberDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList propertyList = (PropertyList)property;
            if (propertyList == null) {
                propertyList = new PropertyList(propertyDefinitionList.getName());
            }
            fieldsForThisProperty = buildFieldsForPropertyList(locatorId, propertyDefinition, oddRow,
                propertyDefinitionList,
                memberDefinition, propertyList);
        } else if (propertyDefinition instanceof PropertyDefinitionMap) {
            PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap)propertyDefinition;
            PropertyMap propertyMap = (PropertyMap)property;
            if (propertyMap == null) {
                propertyMap = new PropertyMap(propertyDefinitionMap.getName());
            }

            fieldsForThisProperty = buildFieldsForPropertyMap(locatorId, propertyDefinitionMap, propertyMap);
        } else {
            throw new IllegalStateException("Property definition null or of unknown type: " + propertyDefinition);
        }

        // Add the fields for this property to the master fields list and set the row background color.
        for (FormItem field : fieldsForThisProperty) {
            fields.add(field);
            field.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        }
    }

    protected List<FormItem> buildFieldsForPropertySimple(PropertyDefinition propertyDefinition,
                                              PropertyDefinitionSimple propertyDefinitionSimple,
                                              PropertySimple propertySimple) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinition);
        fields.add(nameItem);

        FormItem valueItem;
        valueItem = buildSimpleField(propertyDefinitionSimple, propertySimple);

        FormItem unsetItem = buildUnsetItem(propertyDefinitionSimple, propertySimple, valueItem);
        fields.add(unsetItem);

        fields.add(valueItem);

        StaticTextItem descriptionItem = buildDescriptionField(propertyDefinition);
        fields.add(descriptionItem);

        return fields;
    }

    protected List<FormItem> buildFieldsForPropertyList(String locatorId, PropertyDefinition propertyDefinition, boolean oddRow,
                                            PropertyDefinitionList propertyDefinitionList,
                                            PropertyDefinition memberDefinition, PropertyList propertyList) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinition);
        fields.add(nameItem);

        if (memberDefinition instanceof PropertyDefinitionMap) {
            // List of Maps is a specially supported case with summary fields as columns in a table
            // Note: This field spans 3 columns.
            PropertyDefinitionMap memberDefinitionMap = (PropertyDefinitionMap)memberDefinition;
            CanvasItem listOfMapsItem = buildListOfMapsField(locatorId,
                memberDefinitionMap,
                propertyList, oddRow);
            fields.add(listOfMapsItem);
        } else if (memberDefinition instanceof PropertyDefinitionSimple) {
            SpacerItem unsetItem = new SpacerItem();
            fields.add(unsetItem);

            CanvasItem listOfSimplesItem = buildListOfSimplesField(locatorId, propertyDefinitionList, propertyList
            );
            fields.add(listOfSimplesItem);

            StaticTextItem descriptionItem = buildDescriptionField(propertyDefinition);
            fields.add(descriptionItem);
        } else {
            Log.error("List " + propertyList + " has unsupported member type: " + memberDefinition);
            Canvas canvas = new Canvas();
            // TODO: Add label with error message to canvas.
            CanvasItem canvasItem = buildComplexPropertyField(canvas);
            canvasItem.setColSpan(3);
            canvasItem.setEndRow(true);
            fields.add(canvasItem);
        }

        return fields;
    }

    protected List<FormItem> buildFieldsForPropertyMap(String locatorId,
                                           PropertyDefinitionMap propertyDefinitionMap, PropertyMap propertyMap) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinitionMap);
        fields.add(nameItem);

        // Note: This field spans 3 columns.
        FormItem mapField =
            buildMapField(locatorId, propertyDefinitionMap, propertyMap);
        fields.add(mapField);

        return fields;
    }

    protected StaticTextItem buildNameItem(PropertyDefinition propertyDefinition) {
        StaticTextItem nameItem = new StaticTextItem();
        nameItem.setStartRow(true);
        String title = "<b>"
            + (propertyDefinition.getDisplayName() != null ? propertyDefinition.getDisplayName() : propertyDefinition
                .getName()) + "</b>";
        nameItem.setValue(title);
        nameItem.setShowTitle(false);
        return nameItem;
    }

    private StaticTextItem buildDescriptionField(PropertyDefinition propertyDefinition) {
        StaticTextItem descriptionItem = new StaticTextItem();
        descriptionItem.setValue(propertyDefinition.getDescription());
        descriptionItem.setShowTitle(false);
        descriptionItem.setEndRow(true);
        return descriptionItem;
    }

    protected void firePropertyChangedEvent(Property property,
                                            PropertyDefinition propertyDefinition,
                                            boolean isValid) {
        boolean wasValidBefore = this.invalidPropertyNames.isEmpty();
        Property topLevelProperty = getTopLevelProperty(property);        
        if (isValid) {
            this.invalidPropertyNames.remove(topLevelProperty.getName());
        } else {
            this.invalidPropertyNames.add(topLevelProperty.getName());
        }
        boolean isValidNow = this.invalidPropertyNames.isEmpty();

        boolean validationStateChanged = (isValidNow != wasValidBefore);
        for (PropertyValueChangeListener propertyValueChangeListener : this.propertyValueChangeListeners) {
            PropertyValueChangeEvent event = new PropertyValueChangeEvent(property, propertyDefinition,
                validationStateChanged, this.invalidPropertyNames);
            propertyValueChangeListener.propertyValueChanged(event);
        }
    }

    public Set<String> getInvalidPropertyNames() {
        return this.invalidPropertyNames;
    }

    private FormItem buildMapField(String parentLocatorId, PropertyDefinitionMap propertyDefinitionMap,
                               final PropertyMap propertyMap) {
        String locatorId = parentLocatorId + "_" + propertyDefinitionMap.getName();
        boolean isDynamic = isDynamic(propertyDefinitionMap);
        if (isDynamic) {
            PropertyDefinitionMap propertyDefinitionMapClone = new PropertyDefinitionMap(propertyDefinitionMap.getName(),
                propertyDefinitionMap.getDescription(), propertyDefinitionMap.isRequired());
            propertyDefinitionMapClone.setConfigurationDefinition(propertyDefinitionMap.getConfigurationDefinition());
            addMemberPropertyDefinitionsToDynamicPropertyMap(propertyDefinitionMapClone, propertyMap);
            propertyDefinitionMap = propertyDefinitionMapClone;
        }
        VLayout layout = new VLayout();

        final PropertyDefinitionMap propertyDefinitionMapFinal = propertyDefinitionMap;
        Canvas valuesCanvas = buildPropertiesForm(parentLocatorId, propertyDefinitionMapFinal.getPropertyDefinitions().values(),
            propertyMap, true);
        layout.addMember(valuesCanvas);

        if (isDynamic && !isReadOnly(propertyDefinitionMap, propertyMap)) {
            // Map is not read-only - add footer with New and Delete buttons to allow user to add or remove members.
            ToolStrip buttonBar = new ToolStrip();
            buttonBar.setPadding(5);
            buttonBar.setWidth100();
            buttonBar.setMembersMargin(15);
            layout.addMember(buttonBar);

            /*final IButton deleteButton = new LocatableIButton(extendLocatorId(propertyMap.getName()), "Delete");
            deleteButton.setDisabled(true);
            deleteButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    final ListGridRecord[] selectedRecords = propertyGrid.getSelection();
                    String noun = (selectedRecords.length == 1) ? "property" : "properties";
                    String message = "Are you sure you want to delete the selected" + noun + "?";
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                for (ListGridRecord selectedRecord : selectedRecords) {
                                    propertyGrid.removeData(selectedRecord);
                                    String propertyName = selectedRecord.getAttribute("Name");
                                    propertyMap.getMap().remove(propertyName);
                                }
                            }
                        }
                    });
                }
            });
            footer.addMember(deleteButton);

            propertyGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
                        public void onSelectionChanged(SelectionEvent selectionEvent) {
                            int count = propertyGrid.getSelection().length;
                            deleteButton.setDisabled(count < 1);
                        }
                    });*/

            final IButton newButton = new LocatableIButton(extendLocatorId(propertyMap.getName()), "New");
            newButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    SC.askforValue("Enter the name of the property to be added.", new ValueCallback() {
                        @Override
                        public void execute(String propertyName) {
                             if (propertyMap.get(propertyName) != null) {
                                 CoreGUI.getMessageCenter().notify(
                                     new Message("Cannot add property named '" + propertyName
                                         + "', because the set already contains a property with that name.",
                                         Message.Severity.Error, EnumSet.of(Message.Option.Transient)));
                             } else {
                                 PropertySimple memberPropertySimple = new PropertySimple(propertyName, null);
                                 addPropertyToDynamicMap(memberPropertySimple, propertyMap);
                                 firePropertyChangedEvent(propertyMap, propertyDefinitionMapFinal, true);

                                 reload();

                                 CoreGUI.getMessageCenter().notify(new Message("Added property to the set.", EnumSet.of(
                                     Message.Option.Transient)));
                             }
                        }
                    });
                }
            });
            buttonBar.addMember(newButton);

            DynamicForm deleteForm = new DynamicForm();
            deleteForm.setNumCols(3);
            buttonBar.addMember(deleteForm);

            final SelectItem selectItem = new SelectItem();
            selectItem.setValueMap(propertyDefinitionMap.getPropertyDefinitions().keySet().toArray(
                new String[propertyDefinitionMap.getPropertyDefinitions().size()]));
            selectItem.setMultiple(true);
            selectItem.setMultipleAppearance(MultipleAppearance.GRID);
            selectItem.setTitle("Delete");

            final ButtonItem okButtonItem = new ButtonItem();
            okButtonItem.setTitle("OK");
            okButtonItem.setDisabled(true);
            okButtonItem.setEndRow(true);
            okButtonItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                    SC.confirm("Are you sure you want to delete the selected properties from the set?", new BooleanCallback() {
                        @Override
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                Object value = selectItem.getValue();
                                if (value != null) {
                                    String stringValue = value.toString();
                                    String[] memberPropertyNames = stringValue.split(",");
                                    for (final String memberPropertyName : memberPropertyNames) {
                                        PropertySimple memberPropertySimple = propertyMap.getSimple(memberPropertyName);
                                        removePropertyFromDynamicMap(memberPropertySimple);
                                        firePropertyChangedEvent(propertyMap, propertyDefinitionMapFinal, true);
                                    }
                                }

                                reload();
                                CoreGUI.getMessageCenter().notify(new Message("Removed properties from the set.", EnumSet.of(
                                             Message.Option.Transient)));
                            }
                        }
                    });
                }
            });

            selectItem.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent changedEvent) {
                    Object value = changedEvent.getValue();
                    if (value != null) {
                        String stringValue = value.toString();
                        String[] memberPropertyNames = stringValue.split(",");
                        okButtonItem.setDisabled(memberPropertyNames.length == 0);
                    }
                }
            });

            deleteForm.setFields(selectItem, okButtonItem);
        }

        CanvasItem canvasItem = buildComplexPropertyField(layout);
        canvasItem.setColSpan(3);
        canvasItem.setEndRow(true);

        return canvasItem;
    }

    protected void addPropertyToDynamicMap(PropertySimple memberPropertySimple, PropertyMap propertyMap) {
        memberPropertySimple.setOverride(true);
        propertyMap.put(memberPropertySimple);
    }

    protected void removePropertyFromDynamicMap(PropertySimple propertySimple) {
        PropertyMap parentMap = propertySimple.getParentMap();
        parentMap.getMap().remove(propertySimple.getName());
    }

    private boolean isDynamic(PropertyDefinitionMap propertyDefinitionMap) {
        Map<String, PropertyDefinition> memberPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        return memberPropertyDefinitions == null || memberPropertyDefinitions.isEmpty();
    }

    private void addMemberPropertyDefinitionsToDynamicPropertyMap(PropertyDefinitionMap propertyDefinitionMap,
                                                                     PropertyMap propertyMap) {
        for (String propertyName : propertyMap.getMap().keySet()) {
            PropertySimple memberPropertySimple = propertyMap.getSimple(propertyName);
            if (memberPropertySimple != null) {
                PropertyDefinitionSimple memberPropertyDefinitionSimple = new PropertyDefinitionSimple(propertyName,
                    null, false, PropertySimpleType.STRING);
                propertyDefinitionMap.put(memberPropertyDefinitionSimple);
            }
        }
    }

    private CanvasItem buildComplexPropertyField(Canvas canvas) {
        CanvasItem canvasItem = new CanvasItem();
        canvasItem.setCanvas(canvas);
        canvasItem.setShowTitle(false);
        return canvasItem;
    }

    private CanvasItem buildListOfMapsField(final String locatorId,
                                            final PropertyDefinitionMap memberPropertyDefinitionMap,
                                            final PropertyList propertyList, boolean oddRow
    ) {
        Log.debug("Building list-of-maps field for " + propertyList + "...");

        final ListGrid summaryTable = new ListGrid();
        //        summaryTable.setID("config_summaryTable_" + propertyDefinition.getName());
        summaryTable.setAlternateRecordStyles(true);
        summaryTable.setShowAllRecords(true);
        summaryTable.setBodyOverflow(Overflow.VISIBLE);
        summaryTable.setOverflow(Overflow.VISIBLE);
        summaryTable.setAutoFitData(Autofit.HORIZONTAL);

        summaryTable.addCellSavedHandler(new CellSavedHandler() {
            public void onCellSaved(CellSavedEvent cellSavedEvent) {
                Record record = cellSavedEvent.getRecord();
                PropertyMap propertyMap = (PropertyMap)record.getAttributeAsObject(RHQ_PROPERTY_ATTRIBUTE_NAME);
                for (String memberPropertyName : memberPropertyDefinitionMap.getPropertyDefinitions().keySet()) {
                    PropertySimple memberProperty = propertyMap.getSimple(memberPropertyName);
                    if (memberProperty == null) {
                        memberProperty = new PropertySimple(memberPropertyName, null);
                        propertyMap.put(memberProperty);
                    }
                    String newValue = record.getAttribute(memberPropertyName);
                    memberProperty.setStringValue(newValue);
                }                                                                
            }
        });

        List<ListGridField> fieldsList = new ArrayList<ListGridField>();
        List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>(memberPropertyDefinitionMap
            .getPropertyDefinitions().values());
        Collections.sort(propertyDefinitions, new PropertyDefinitionComparator());

        for (PropertyDefinition subDef : propertyDefinitions) {
            if (subDef.isSummary()) {
                ListGridField field = new ListGridField(subDef.getName(), subDef.getDisplayName(), 90);

                PropertyDefinitionSimple defSimple = (PropertyDefinitionSimple) subDef;
                if (defSimple.getType() == PropertySimpleType.INTEGER) {
                    field.setType(ListGridFieldType.INTEGER);
                } else if (defSimple.getType() == PropertySimpleType.FLOAT) {
                    field.setType(ListGridFieldType.FLOAT);
                }

                fieldsList.add(field);
            }
        }

        if (fieldsList.isEmpty()) {
            // An extra "feature of the config system". If no fields are labeled summary, all are considered summary.
            for (PropertyDefinition subDef : propertyDefinitions) {
                ListGridField field = new ListGridField(subDef.getName(), subDef.getDisplayName());
                fieldsList.add(field);
                PropertyDefinitionSimple defSimple = (PropertyDefinitionSimple) subDef;
                if (defSimple.getType() == PropertySimpleType.INTEGER) {
                    field.setType(ListGridFieldType.FLOAT);
                } else if (defSimple.getType() == PropertySimpleType.FLOAT) {
                    field.setType(ListGridFieldType.FLOAT);
                }
            }
        }

        ListGridField editField = new ListGridField("edit", 20);
        editField.setType(ListGridFieldType.ICON);
        //        editField.setIcon(Window.getImgURL("[SKIN]/actions/edit.png"));
        editField.setCellIcon(Window.getImgURL("[SKIN]/actions/edit.png"));
        editField.setCanEdit(false);
        editField.setCanGroupBy(false);
        editField.setCanSort(false);
        editField.setCanHide(false);
        editField.addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(RecordClickEvent recordClickEvent) {
                Log.info("You want to edit: " + recordClickEvent.getRecord());
                PropertyMap memberPropertyMap = (PropertyMap)recordClickEvent.getRecord().getAttributeAsObject(
                    RHQ_PROPERTY_ATTRIBUTE_NAME);
                displayMapEditor(extendLocatorId("MapEdit"), summaryTable, recordClickEvent.getRecord(),
                    memberPropertyDefinitionMap, propertyList, memberPropertyMap);
            }
        });
        fieldsList.add(editField);

        if (!readOnly) {
            ListGridField removeField = new ListGridField("remove", 20);
            removeField.setType(ListGridFieldType.ICON);
            //        removeField.setIcon(Window.getImgURL("[SKIN]/actions/remove.png")); //"/images/tbb_delete.gif");
            removeField.setCellIcon(Window.getImgURL("[SKIN]/actions/remove.png")); //"/images/tbb_delete.gif");
            removeField.setCanEdit(false);
            removeField.setCanFilter(true);
            removeField.setFilterEditorType(new SpacerItem());
            removeField.setCanGroupBy(false);
            removeField.setCanSort(false);
            removeField.setCanHide(false);

            removeField.addRecordClickHandler(new RecordClickHandler() {
                public void onRecordClick(final RecordClickEvent recordClickEvent) {
                    Log.info("You want to delete: " + recordClickEvent.getRecordNum());
                    SC.confirm("Are you sure you want to delete this row?", new BooleanCallback() {
                        public void execute(Boolean aBoolean) {
                            if (aBoolean) {
                                summaryTable.removeData(recordClickEvent.getRecord());
                            }
                        }
                    });
                }
            });

            editField.setEditorType(new ButtonItem("delete", "Delete"));
            fieldsList.add(removeField);
        }

        summaryTable.setFields(fieldsList.toArray(new ListGridField[fieldsList.size()]));

        // Now add rows containing the actual data (i.e. member property values).
        ListGridRecord[] rows = buildSummaryRecords(propertyList, propertyDefinitions);
        summaryTable.setData(rows);

        VLayout summaryTableHolder = new LocatableVLayout(locatorId);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        IButton addRowButton = new IButton();
        addRowButton.setIcon(Window.getImgURL("[SKIN]/actions/add.png"));
        addRowButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                displayMapEditor(extendLocatorId("MapEdit"), summaryTable, null, memberPropertyDefinitionMap, propertyList, null);
            }
        });

        toolStrip.addMember(addRowButton);

        summaryTableHolder.setMembers(summaryTable, toolStrip);

        CanvasItem canvasItem = buildComplexPropertyField(summaryTableHolder);
        canvasItem.setColSpan(3);
        canvasItem.setEndRow(true);

        return canvasItem;
    }

    private ListGridRecord[] buildSummaryRecords(PropertyList propertyList, List<PropertyDefinition> definitions) {
        ListGridRecord[] rows = new ListGridRecord[propertyList == null ? 0 : propertyList.getList().size()];
        int i = 0;
        for (Property row : propertyList.getList()) {
            PropertyMap rowMap = (PropertyMap) row;
            ListGridRecord record = buildSummaryRecord(definitions, rowMap);
            rows[i++] = record;
        }
        return rows;
    }

    private ListGridRecord buildSummaryRecord(List<PropertyDefinition> memberPropertyDefinitions, PropertyMap memberPropertyMap) {
        ListGridRecord record = new ListGridRecord();
        for (PropertyDefinition subDef : memberPropertyDefinitions) {
            PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
            PropertySimple propertySimple = ((PropertySimple) memberPropertyMap.get(subDefSimple.getName()));

            if (propertySimple.getStringValue() != null) {
                record.setAttribute(subDefSimple.getName(), propertySimple.getStringValue());
                /*
                switch (((PropertyDefinitionSimple) subDef).getType()) {
                    case BOOLEAN:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getBooleanValue());
                        break;
                    case INTEGER:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getLongValue());
                        break;
                    case LONG:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getLongValue());
                        break;
                    case FLOAT:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getDoubleValue());
                        break;
                    case DOUBLE:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getDoubleValue());
                        break;
                    default:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getStringValue());
                        break;
                }*/
            }
        }
        record.setAttribute(RHQ_PROPERTY_ATTRIBUTE_NAME, memberPropertyMap);
        return record;
    }

    private CanvasItem buildListOfSimplesField(String locatorId, final PropertyDefinitionList propertyDefinitionList,
                                               final PropertyList propertyList) {
        Log.debug("Building list-of-simples field for " + propertyList + "...");

        LocatableVLayout vLayout = new LocatableVLayout(locatorId);

        final DynamicForm listGrid = new DynamicForm();
        vLayout.addMember(listGrid);

        final SelectItem membersItem = new SelectItem(propertyList.getName());
        membersItem.setShowTitle(false);
        membersItem.setMultiple(true);
        membersItem.setMultipleAppearance(MultipleAppearance.GRID);
        membersItem.setWidth(220);
        membersItem.setHeight(60);
        LinkedHashMap<String, String> memberValueToIndexMap = buildValueMap(propertyList);
        membersItem.setValueMap(memberValueToIndexMap);
        listGrid.setItems(membersItem);

        if (!isReadOnly(propertyDefinitionList, propertyList)) {
            // List is not read-only - add footer with New and Delete buttons to allow user to add or remove members.
            ToolStrip footer = new ToolStrip();
            footer.setPadding(5);
            footer.setWidth100();
            footer.setMembersMargin(15);
            vLayout.addMember(footer);
            
            final IButton deleteButton = new LocatableIButton(extendLocatorId("Delete"));
            deleteButton.setIcon(Window.getImgURL("[SKIN]/actions/remove.png"));
            deleteButton.setTooltip("Delete the selected items from the list.");
            deleteButton.setDisabled(true);
            deleteButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    final String[] selectedValues = membersItem.getValues();
                    final String noun = (selectedValues.length == 1) ? "item" : "items";
                    String message = "Are you sure you want to delete the " + selectedValues.length + " selected "
                        + noun + "?";
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                for (int i = selectedValues.length - 1; i >= 0; i--) {
                                    String selectedValue = selectedValues[i];
                                    int index = Integer.valueOf(selectedValue);
                                    propertyList.getList().remove(index);

                                    // Rebuild the select item options.
                                    LinkedHashMap<String, String> memberValueToIndexMap = buildValueMap(propertyList);
                                    membersItem.setValueMap(memberValueToIndexMap);

                                    deleteButton.disable();

                                    firePropertyChangedEvent(propertyList, propertyDefinitionList, true);
                                    CoreGUI.getMessageCenter().notify(new Message(selectedValues.length + " " + noun
                                        + " deleted from list.",
                                        EnumSet.of(
                                            Message.Option.Transient)));
                                }
                            }
                        }
                    });
                }
            });
            footer.addMember(deleteButton);

            membersItem.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent changedEvent) {
                    String[] selectedValues = membersItem.getValues();                    
                    int count = selectedValues.length;
                    deleteButton.setDisabled(count < 1);
                }
            });

            final IButton newButton = new LocatableIButton(extendLocatorId("New"));
            newButton.setIcon(Window.getImgURL("[SKIN]/actions/add.png"));
            newButton.setTooltip("Add an item to the list.");
            newButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    final Window popup = new Window();
                    popup.setTitle("Add Item to List");
                    popup.setWidth(300);
                    popup.setHeight(110);
                    popup.setIsModal(true);
                    popup.setShowModalMask(true);
                    popup.setShowCloseButton(false);
                    popup.centerInPage();

                    VLayout vLayout = new VLayout();
                    vLayout.setMargin(10);

                    final DynamicForm form = new DynamicForm();

                    PropertyDefinitionSimple memberPropertyDefinitionSimple =
                                (PropertyDefinitionSimple)propertyDefinitionList.getMemberDefinition();
                    final String propertyName = memberPropertyDefinitionSimple.getName();
                    final PropertySimple newMemberPropertySimple = new PropertySimple(propertyName, null);

                    FormItem simpleField =
                        buildSimpleField(memberPropertyDefinitionSimple, newMemberPropertySimple);
                    simpleField.setAlign(Alignment.CENTER);
                    simpleField.setDisabled(false);
                    simpleField.setRequired(true);
                    simpleField.setEndRow(true);

                    SpacerItem spacer = new SpacerItem();
                    spacer.setHeight(9);

                    form.setItems(simpleField, spacer);
                    vLayout.addMember(form);

                    final IButton okButton = new IButton("OK");
                    okButton.disable();
                    //        saveButton.setID("config_structured_button_save");
                    okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                        public void onClick(ClickEvent clickEvent) {
                            propertyList.add(newMemberPropertySimple);

                            // Rebuild the select item options.
                            LinkedHashMap<String, String> memberValueToIndexMap = buildValueMap(propertyList);
                            membersItem.setValueMap(memberValueToIndexMap);

                            firePropertyChangedEvent(propertyList, propertyDefinitionList, true);
                            CoreGUI.getMessageCenter().notify(new Message("Item added to list.", EnumSet.of(
                                     Message.Option.Transient)));

                            popup.destroy();
                        }
                    });

                    form.addItemChangedHandler(new ItemChangedHandler() {
                        public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                            newMemberPropertySimple.setStringValue((String)itemChangedEvent.getNewValue());

                            // Only enable the OK button, allowing the user to add the property to the map, if the
                            // property is valid.
                            boolean isValid = form.validate();
                            okButton.setDisabled(!isValid);
                        }
                    });

                    final IButton cancelButton = new IButton("Cancel");
                    cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                        public void onClick(ClickEvent clickEvent) {
                            popup.destroy();
                        }
                    });

                    HLayout buttons = new HLayout();
                    buttons.setAlign(Alignment.CENTER);
                    buttons.setTop(10);
                    buttons.setMembersMargin(10);
                    buttons.setMembers(okButton, cancelButton);
                    vLayout.addMember(buttons);

                    popup.addItem(vLayout);
                    popup.show();

                    simpleField.focusInItem();
                }
            });
            footer.addMember(newButton);
        }

        return buildComplexPropertyField(vLayout);
    }

    private LinkedHashMap<String, String> buildValueMap(PropertyList propertyList) {
        LinkedHashMap<String, String> memberValueToIndexMap = new LinkedHashMap<String, String>();
        List<Property> memberProperties = propertyList.getList();
        int index = 0;
        for (Iterator<Property> iterator = memberProperties.iterator(); iterator.hasNext();) {
            Property memberProperty = iterator.next();
            PropertySimple memberPropertySimple = (PropertySimple)memberProperty;
            String memberValue = memberPropertySimple.getStringValue();
            if (memberValue == null) {
                Log.error("List " + propertyList + " contains property with null value - removing and skipping...");
                iterator.remove();
                continue;
            }
            memberValueToIndexMap.put(String.valueOf(index++), memberValue);
        }
        return memberValueToIndexMap;
    }

    protected FormItem buildSimpleField(final PropertyDefinitionSimple propertyDefinitionSimple,
                                        final PropertySimple propertySimple) {
        Log.debug("Building simple field for " + propertySimple + "...");

        FormItem valueItem = null;

        List<PropertyDefinitionEnumeration> enumeratedValues = propertyDefinitionSimple.getEnumeratedValues();
        if (enumeratedValues != null && !enumeratedValues.isEmpty()) {
            LinkedHashMap<String, String> valueOptions = new LinkedHashMap<String, String>();
            for (PropertyDefinitionEnumeration option : propertyDefinitionSimple.getEnumeratedValues()) {
                valueOptions.put(option.getValue(), option.getName());
            }

            if (valueOptions.size() > 5) {
                valueItem = new SelectItem();
            } else {
                valueItem = new RadioGroupItem();
            }
            valueItem.setValueMap(valueOptions);
            if (propertySimple != null) {
                valueItem.setValue(propertySimple.getStringValue());
            }
        } else {
            switch (propertyDefinitionSimple.getType()) {
                case STRING:
                case FILE:
                case DIRECTORY:
                    valueItem = new TextItem();
                    break;
                case LONG_STRING:
                    valueItem = new TextAreaItem();
                    break;
                case PASSWORD:
                    valueItem = new PasswordItem();
                    break;
                case BOOLEAN:
                    RadioGroupItem radioGroupItem = new RadioGroupItem();
                    radioGroupItem.setVertical(false);
                    valueItem = radioGroupItem;
                    LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
                    valueMap.put("true", "Yes");
                    valueMap.put("false", "No");
                    valueItem.setValueMap(valueMap);
                    break;
                case INTEGER:
                case LONG:
                    valueItem = new IntegerItem();
                    break;
                case FLOAT:
                case DOUBLE:
                    valueItem = new FloatItem();
                    break;
            }
        }

        valueItem.setDefaultValue(propertySimple.getStringValue());
        valueItem.setRequired(propertyDefinitionSimple.isRequired());

        List<Validator> validators = buildValidators(propertyDefinitionSimple, propertySimple);
        valueItem.setValidators(validators.toArray(new Validator[validators.size()]));

        valueItem.setDisabled(isReadOnly(propertyDefinitionSimple, propertySimple)
            || isUnset(propertyDefinitionSimple, propertySimple));
        /*
                Click handlers seem to be turned off for disabled fields... need an alternative
                valueItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        com.allen_sauer.gwt.log.client.Log.info("Click in value field");
                        clickEvent.getItem().setDisabled(false);
                        unsetItem.setValue(false);

                    }
                });
        */

        valueItem.setShowTitle(false);
        valueItem.setWidth(220);

        // Only update the underlying property when the input is changed if it's a top-level simple or a simple within a
        // top-level map.
        if (updatePropertyValueOnChange(propertyDefinitionSimple, propertySimple)) {
            valueItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent changedEvent) {                        
                        updatePropertySimpleValue(changedEvent.getValue(), propertySimple, propertyDefinitionSimple);
                        boolean isValid = changedEvent.getItem().validate();
                        firePropertyChangedEvent(propertySimple, propertyDefinitionSimple, isValid);
                    }
                });
        }

        return valueItem;
    }

    protected boolean updatePropertyValueOnChange(PropertyDefinitionSimple propertyDefinitionSimple,
                                                  PropertySimple propertySimple) {
        PropertyDefinitionMap parentPropertyMapDefinition =
            propertyDefinitionSimple.getParentPropertyMapDefinition();
        return propertyDefinitionSimple.getConfigurationDefinition() != null ||
            (parentPropertyMapDefinition != null && parentPropertyMapDefinition.getConfigurationDefinition() != null);
    }

    protected void updatePropertySimpleValue(Object value, PropertySimple propertySimple,
                                             PropertyDefinitionSimple propertyDefinitionSimple) {
        propertySimple.setErrorMessage(null);
        propertySimple.setValue(value);        
    }

    protected static Property getTopLevelProperty(Property property) {
        Property currentProperty = property;
        while (currentProperty.getConfiguration() == null) {
            if (currentProperty.getParentList() != null) {
                currentProperty = currentProperty.getParentList();
            } else if (currentProperty.getParentMap() != null) {
                currentProperty = currentProperty.getParentMap();
            } else {
                Log.error("Property " + currentProperty + " has no parent.");
                break;
            }
        }
        return currentProperty;
    }
    
    protected FormItem buildUnsetItem(final PropertyDefinitionSimple propertyDefinitionSimple, final PropertySimple propertySimple,
                                    final FormItem valueItem) {
        FormItem item;
        if (!propertyDefinitionSimple.isRequired()) {
            final CheckboxItem unsetItem = new CheckboxItem();
            boolean unset = isUnset(propertyDefinitionSimple, propertySimple);
            unsetItem.setValue(unset);
            unsetItem.setDisabled(isReadOnly(propertyDefinitionSimple, propertySimple));
            unsetItem.setShowLabel(false);
            unsetItem.setShowTitle(false);
            unsetItem.setLabelAsTitle(false);
            unsetItem.setColSpan(1);

            unsetItem.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent changeEvent) {
                    Boolean isUnset = (Boolean) changeEvent.getValue();
                    valueItem.setDisabled(isUnset);
                    if (isUnset) {
                        updatePropertySimpleValue(null, propertySimple, propertyDefinitionSimple);
                        setValue(valueItem, null);
                    } else {
                        valueItem.focusInItem();
                    }
                    valueItem.redraw();
                    propertySimple.setValue(valueItem.getValue());
                }
            });

            item = unsetItem;
        } else {
            item = new SpacerItem();
            item.setShowTitle(false);
        }
        return item;
    }

    private boolean isUnset(PropertyDefinitionSimple propertyDefinition, PropertySimple propertySimple) {
        return (!propertyDefinition.isRequired() &&
                (propertySimple == null || propertySimple.getStringValue() == null));
    }

    private boolean isReadOnly(PropertyDefinition propertyDefinition, Property property) {
        boolean isInvalidRequiredProperty = false;
        if (property instanceof PropertySimple) {
            PropertySimple propertySimple = (PropertySimple)property;
            String errorMessage = propertySimple.getErrorMessage();
            if ((null == propertySimple.getStringValue()) || "".equals(propertySimple.getStringValue())
                || ((null != errorMessage) && (!"".equals(errorMessage.trim())))) {
                // Required properties with no value, or an invalid value (assumed if we see an error message) should
                // never be set to read-only, otherwise the user will have no way to give the property a new value and
                // thereby get things to a valid state.
                isInvalidRequiredProperty = true;
            }
        }
        return !isInvalidRequiredProperty && (propertyDefinition.isReadOnly() || this.readOnly);
    }

    protected List<Validator> buildValidators(PropertyDefinitionSimple propertyDefinition, Property property) {
        List<Validator> validators = new ArrayList<Validator>();

        Validator typeValidator = null;
        switch (propertyDefinition.getType()) {
            case STRING:
            case LONG_STRING:
            case FILE:
            case DIRECTORY:
                LengthRangeValidator lengthRangeValidator = new LengthRangeValidator();
                lengthRangeValidator.setMax(PropertySimple.MAX_VALUE_LENGTH);
                typeValidator = lengthRangeValidator;
                break;
            case BOOLEAN:
                typeValidator = new IsBooleanValidator();
                break;
            case INTEGER:
            case LONG:
                typeValidator = new IsIntegerValidator();
                break;
            case FLOAT:
            case DOUBLE:
                typeValidator = new IsFloatValidator();
                break;
        }
        if (typeValidator != null) {
            validators.add(typeValidator);
        }

        Set<Constraint> constraints = propertyDefinition.getConstraints();
        if (constraints != null) {
            for (Constraint constraint : constraints) {
                if (constraint instanceof IntegerRangeConstraint) {
                    IntegerRangeConstraint integerConstraint = ((IntegerRangeConstraint) constraint);
                    IntegerRangeValidator validator = new IntegerRangeValidator();
                    if (integerConstraint.getMinimum() != null) {
                        validator.setMin(integerConstraint.getMinimum().intValue());
                    }
                    if (integerConstraint.getMaximum() != null) {
                        validator.setMax(integerConstraint.getMaximum().intValue());
                    }
                    validators.add(validator);
                } else if (constraint instanceof FloatRangeConstraint) {
                    FloatRangeConstraint floatConstraint = ((FloatRangeConstraint) constraint);
                    FloatRangeValidator validator = new FloatRangeValidator();
                    if (floatConstraint.getMinimum() != null) {
                        validator.setMin(floatConstraint.getMinimum().floatValue());
                    }
                    if (floatConstraint.getMaximum() != null) {
                        validator.setMax(floatConstraint.getMaximum().floatValue());
                    }
                    validators.add(validator);
                } else if (constraint instanceof RegexConstraint) {
                    RegExpValidator validator =
                        new RegExpValidator("^" + constraint.getDetails() + "$");
                    validators.add(validator);
                }
            }
        }

        if (property.getErrorMessage() != null) {
            this.invalidPropertyNames.add(property.getName());
            PluginReportedErrorValidator validator = new PluginReportedErrorValidator(property);
            validators.add(validator);
        }
        
        return validators;
    }

    private void displayMapEditor(String locatorId, final ListGrid summaryTable, final Record existingRecord,
        PropertyDefinitionMap definition, final PropertyList list, final PropertyMap map) {

        final List<PropertyDefinition> memberDefinitions = new ArrayList<PropertyDefinition>(definition
            .getPropertyDefinitions().values());
        Collections.sort(memberDefinitions, new PropertyDefinitionComparator());

        final boolean newRow = (map == null);
        final PropertyMap workingMap = newRow ? new PropertyMap(definition.getName()) : map.deepCopy(true);

        LocatableVLayout layout = new LocatableVLayout(locatorId);
        layout.setHeight100();

        final DynamicForm childForm = buildPropertiesForm(extendLocatorId("Editor"), memberDefinitions, workingMap,
            false);
        childForm.setHeight100();
        layout.addMember(childForm);

        final Window popup = new Window();
        popup.setTitle("Edit Configuration Row");
        popup.setWidth(800);
        popup.setHeight(600);
        popup.setIsModal(true);
        popup.setShowModalMask(true);
        popup.setShowCloseButton(false);
        popup.centerInPage();

        final IButton okButton = new IButton("OK");
        okButton.disable();
        //        saveButton.setID("config_structured_button_save");
        okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (newRow) {
                    list.add(workingMap);
                    ListGridRecord record = buildSummaryRecord(memberDefinitions, workingMap);
                    try {
                        summaryTable.addData(record);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // TODO: Pass in the map's index, rather than using indexOf(), which is not 100% reliable,
                    //       since a list could contain two or more identical maps.
                    //list.getList().set(list.getList().indexOf(map), workingMap);
                    for (PropertyDefinition subDef : memberDefinitions) {
                        PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
                        PropertySimple propertySimple = ((PropertySimple) workingMap.get(subDefSimple.getName()));
                        existingRecord.setAttribute(subDefSimple.getName(), propertySimple != null ? propertySimple
                            .getStringValue() : null);
                    }
                    summaryTable.updateData(existingRecord);
                }
                firePropertyChangedEvent(list, null, true);
                summaryTable.redraw();

                //                ListGridRecord[] rows = buildSummaryRecords(list, definitions);
                //                summaryTable.setData(rows);
                //                summaryTable.redraw();
                //                summaryTable.addData();
                popup.destroy();
            }
        });

        // Only enable the OK button if all properties are valid.
        childForm.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {                
                okButton.setDisabled(!childForm.validate());
            }
        });

        final IButton cancelButton = new IButton("Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                popup.destroy();
            }
        });

        HLayout buttons = new HLayout();
        buttons.setAlign(Alignment.CENTER);
        buttons.setMembersMargin(10);
        buttons.setMembers(okButton, cancelButton);
        layout.addMember(buttons);

        popup.addItem(layout);

        popup.show();
    }

    protected static void setValue(FormItem item, Object value) {
        if (value instanceof String) {
            item.setValue((String)value);
        } else if (value instanceof Boolean) {
            item.setValue((Boolean)value);
        }  else if (value instanceof Integer) {
            item.setValue((Integer)value);
        } else if (value instanceof Float) {
            item.setValue((Float)value);
        } else if (value instanceof Double) {
            item.setValue((Double)value);
        } else if (value instanceof Date) {
            item.setValue((Date)value);
        } else {
            String stringValue = (value != null) ? value.toString() : null;
            item.setValue(stringValue);
        }
        item.setDefaultValue((String)null);
    }

    private static class PropertyDefinitionComparator implements Comparator<PropertyDefinition> {
        public int compare(PropertyDefinition o1, PropertyDefinition o2) {
            return new Integer(o1.getOrder()).compareTo(o2.getOrder());
        }
    }
    
    private class PluginReportedErrorValidator extends CustomValidator {
        private Property property;

        public PluginReportedErrorValidator(Property property) {
            this.property = property;
        }

        @Override
        protected boolean condition(Object value) {
            String errorMessage = this.property.getErrorMessage();
            boolean valid = (errorMessage != null);
            if (!valid) {
                setErrorMessage(errorMessage);
            }
            return valid;
        }
    }
}
