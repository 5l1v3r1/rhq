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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.components.SimpleCollapsiblePanel;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class PermissionEditorView extends CanvasItem {


    private Set<Permission> selectedPermissions = EnumSet.noneOf(Permission.class);

    private DynamicForm parentForm;

    private DynamicForm form;


    public PermissionEditorView(String name, String title) {
        super(name, title);

        setCanvas(new SimpleCollapsiblePanel("Permissions", buildForm()));
    }


    public Canvas buildForm() {
        System.out.println("Building permissions canvas");

//        Object o = getAttributeAsObject(getFieldName());

        this.form = new DynamicForm();
        ArrayList<FormItem> items = new ArrayList<FormItem>();


        HeaderItem h1 = new HeaderItem("globalPermissions","Global Permissions");
        h1.setValue("Global Permissions");
        items.add(h1);
        for (Permission p : Permission.values()) {
            if (p.getTarget() == Permission.Target.GLOBAL) {
                items.add(new CheckboxItem(p.name(),p.name()));
            }
        }

        HeaderItem h2 = new HeaderItem("resourcePermissions","Resource Permissions");
        h2.setValue("Resource Permissions");
        items.add(h2);
        for (Permission p : Permission.values()) {
            if (p.getTarget() == Permission.Target.RESOURCE) {
                items.add(new CheckboxItem(p.name(),p.name()));
            }
        }

        form.setItems(items.toArray(new FormItem[items.size()]));


        return form;
    }


    public void setPermissions(Set<Permission> permissions) {
        this.selectedPermissions = permissions;
        for (Permission p : Permission.values()) {
            form.setValue(p.name(), permissions.contains(p));
        }

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                for (Permission p : Permission.values()) {
                    if ((Boolean)form.getValue(p.name())) {
                        selectedPermissions.add(p);
                    } else {
                        selectedPermissions.remove(p);
                    }
                }
            }
        });

        form.markForRedraw();
    }

    public Set<Permission> getPermissions() {
        return selectedPermissions;
    }

    @Override
    public Object getValue() {
        System.out.println("Finding Value");
        return super.getValue();
    }

    public void setParentForm(DynamicForm parentForm) {
        this.parentForm = parentForm;
    }
}
