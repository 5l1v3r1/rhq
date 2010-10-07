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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.composite.ResourceConfigurationComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.GroupConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.GroupMemberConfiguration;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for editing a group's configuration.
 *
 * @author Ian Springer
 */
public class GroupResourceConfigurationEditView extends LocatableVLayout implements PropertyValueChangeListener {
    private final ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    private ResourceGroup group;
    private ResourcePermission resourcePermission;
    private ConfigurationDefinition configurationDefinition;
    private Configuration aggregateConfiguration;
    private List<GroupMemberConfiguration> memberConfigurations;

    private ConfigurationEditor editor;
    private IButton saveButton;

    public GroupResourceConfigurationEditView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);

        this.group = groupComposite.getResourceGroup();
        this.resourcePermission = groupComposite.getResourcePermission();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        build();
    }

    public void build() {
        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        toolStrip.addMember(saveButton);
        
        addMember(toolStrip);
        reloadConfiguration();

        if (!this.resourcePermission.isConfigureWrite()) {
            Message message = new Message("You do not have permission to edit this group's configuration.",
                Message.Severity.Info, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            CoreGUI.getMessageCenter().notify(message);
        }
    }

    private void reloadConfiguration() {
        this.saveButton.disable();
        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }

        loadConfigurationDefinition();
        loadConfigurations();
    }

    private void initEditor() {
        if (this.configurationDefinition != null && this.aggregateConfiguration != null) {
            this.editor = new GroupConfigurationEditor(this.extendLocatorId("Editor"), this.configurationDefinition,
                this.memberConfigurations);
            this.editor.setOverflow(Overflow.AUTO);
            this.editor.addPropertyValueChangeListener(this);
            this.editor.setReadOnly(!this.resourcePermission.isConfigureWrite());
            addMember(this.editor);
        }
    }

    private void loadConfigurationDefinition() {
        if (this.configurationDefinition == null) {
            final ResourceType type = this.group.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { type.getId() },
                EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                new ResourceTypeRepository.TypesLoadedCallback() {
                    public void onTypesLoaded(Map<Integer, ResourceType> types) {
                        configurationDefinition = types.get(type.getId()).getResourceConfigurationDefinition();
                        if (configurationDefinition == null) {
                            throw new IllegalStateException("Configuration is not supported by this group.");
                        }
                        initEditor();
                    }
                });
        }
    }

    private void loadConfigurations() {
        this.aggregateConfiguration = null;
        this.memberConfigurations = null;
        this.configurationService.findResourceConfigurationsForGroup(group.getId(), new AsyncCallback<List<DisambiguationReport<ResourceConfigurationComposite>>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to retrieve member Resource configurations for " + group + ".", caught);
                    }

                    public void onSuccess(List<DisambiguationReport<ResourceConfigurationComposite>> results) {
                        memberConfigurations = new ArrayList<GroupMemberConfiguration>(results.size());
                        for (DisambiguationReport<ResourceConfigurationComposite> result : results) {
                            String parentsHtml = ReportDecorator.decorateResourceLineage(result.getParents());
                            int resourceId = result.getOriginal().getResourceId();
                            String resourceHtml = ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL,
                                result.getResourceType(), result.getName(), resourceId);
                            String label = parentsHtml + ReportDecorator.DEFAULT_SEPARATOR + resourceHtml;
                            GroupMemberConfiguration memberConfiguration = new GroupMemberConfiguration(resourceId, label,
                                result.getOriginal().getConfiguration());
                            memberConfigurations.add(memberConfiguration);
                        }
                        initEditor();
                    }
                });
    }

    private void save() {        
        // TODO

        SC.say("Boo!");
/*
        GWTServiceLookup.getConfigurationService().updateResourceConfiguration(resource.getId(), updatedConfiguration,
            new AsyncCallback<ResourceConfigurationUpdate>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to update configuration.", caught);
                }

                public void onSuccess(ResourceConfigurationUpdate result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Configuration updated.",
                            "Configuration updated for Resource [" + resource.getName() + "].",
                            Message.Severity.Info));
                    reloadConfiguration();
                }
            });
*/
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isValidationStateChanged()) {
            Set<String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();                
                message = new Message("All configuration properties have valid values, so the configuration can now be saved.",
                    Message.Severity.Info, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message("The following configuration properties have invalid values: "
                    + invalidPropertyNames + ". The values must be corrected before the configuration can be saved.",
                    Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else {
            this.saveButton.enable();
        }
    }
}
