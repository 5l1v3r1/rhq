/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.IconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.IconClickHandler;
import com.smartgwt.client.widgets.form.fields.events.ItemHoverEvent;
import com.smartgwt.client.widgets.form.fields.events.ItemHoverHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * A subclass of SmartGWT's DynamicForm widget that provides the following additional feature:
 *
 * If any {@link TogglableTextItem}s are added to the form, they will initially be rendered as static text items, except
 * when the user hovers over one of them, an edit icon will be displayed immediately to the right of it for five seconds.
 * If the user clicks this icon, the form item will become editable and the user can update its value. Once the user
 * hits Enter or switches focus somewhere outside the form item, the form item will become static again.
 *
 * @author Ian Springer 
 */
public class EnhancedDynamicForm extends LocatableDynamicForm {
    public EnhancedDynamicForm(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        setNumCols(4);
        setColWidths("25%", "25%", "25%", "25%");
        setWrapItemTitles(false);
        setWidth("90%");
    }

    @Override
    public void setItems(FormItem... items) {
        List<FormItem> itemsList = new ArrayList<FormItem>();
        List<String> togglableTextItemNames = new ArrayList<String>();
        for (FormItem item : items) {
            if (item instanceof TogglableTextItem) {
                final TogglableTextItem togglableTextItem = (TogglableTextItem) item;
                togglableTextItemNames.add(togglableTextItem.getName());

                final StaticTextItem staticTextItem = new StaticTextItem(getStaticTextItemName(togglableTextItem
                    .getName()), togglableTextItem.getTitle());
                staticTextItem.setAttribute("editing", false);
                staticTextItem.setTextBoxStyle("editableText");

                FormItemIcon editIcon = new FormItemIcon();
                editIcon.setName("Edit");
                editIcon.setSrc("[SKIN]/actions/edit.png");
                staticTextItem.setIcons(editIcon);
                staticTextItem.setShowIcons(false);

                staticTextItem.setShowIfCondition(new FormItemIfFunction() {
                    public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                        boolean editing = staticTextItem.getAttributeAsBoolean("editing");
                        return !editing;
                    }
                });
                staticTextItem.addIconClickHandler(new IconClickHandler() {
                    public void onIconClick(IconClickEvent iconClickEvent) {
                        if ("Edit".equals(iconClickEvent.getIcon().getName())) {
                            staticTextItem.setAttribute("editing", true);
                            staticTextItem.setShowIcons(false);
                            markForRedraw();
                        }
                    }
                });
                staticTextItem.addItemHoverHandler(new ItemHoverHandler() {
                    public void onItemHover(ItemHoverEvent itemHoverEvent) {
                        staticTextItem.setShowIcons(true);
                        markForRedraw();
                        new Timer() {
                            public void run() {
                                staticTextItem.setShowIcons(false);
                                markForRedraw();
                            }
                        }.schedule(5000);
                    }
                });
                staticTextItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
                    public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                        staticTextItem.setAttribute("editing", true);
                        markForRedraw();
                    }
                });
                staticTextItem.setRedrawOnChange(true);
                itemsList.add(staticTextItem);

                togglableTextItem.addKeyPressHandler(new KeyPressHandler() {
                    public void onKeyPress(KeyPressEvent keyPressEvent) {
                        if (keyPressEvent.getKeyName().equals("Enter")) {
                            updateValue(staticTextItem, togglableTextItem);
                        }
                    }
                });
                togglableTextItem.addBlurHandler(new BlurHandler() {
                    public void onBlur(BlurEvent blurEvent) {
                        updateValue(staticTextItem, togglableTextItem);
                    }
                });
                togglableTextItem.setShowIfCondition(new FormItemIfFunction() {
                    public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                        boolean editing = staticTextItem.getAttributeAsBoolean("editing");
                        return editing;
                    }
                });
                itemsList.add(togglableTextItem);
            } else {
                itemsList.add(item);
            }
        }

        super.setItems((FormItem[]) itemsList.toArray(new FormItem[itemsList.size()]));

        // SmartGWT annoyingly barfs if getValue() is called on a form item before it's been added to a form, so
        // we wait until after we've added all of the items to the form to set the values of the static items we
        // added, because only at that point can we grab the values of the corresponding togglable items.
        for (String name : togglableTextItemNames) {
            String value = getValueAsString(name);
            setValue(getStaticTextItemName(name), value);
        }
    }

    private String getStaticTextItemName(String togglableTextItemName) {
        return "static" + togglableTextItemName;
    }

    private void updateValue(StaticTextItem staticTextItem, TogglableTextItem textItem) {
        String value = (String) textItem.getValue();
        staticTextItem.setValue(value);
        staticTextItem.setAttribute("editing", false);
        for (ValueUpdatedHandler handler : textItem.getValueUpdatedHandlers()) {
            handler.onValueUpdated(value);
        }
        markForRedraw();
    }
}
