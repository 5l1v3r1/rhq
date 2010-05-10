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
package org.rhq.enterprise.gui.coregui.client.components.tagging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class TagEditorView extends Layout {

    private LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();

    private boolean editing = false;

    private DynamicForm form;
    private boolean readOnly;
    private TagsChangedCallback callback;

    private boolean vertical = false;
    private boolean alwaysEdit = false;

    public TagEditorView(Set<Tag> tags, boolean readOnly, TagsChangedCallback callback) {
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.readOnly = readOnly;
        this.callback = callback;
    }

    public LinkedHashSet<Tag> getTags() {
        return tags;
    }

    public void setTags(LinkedHashSet<Tag> tags) {
        this.tags = tags;
        setup();
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public void setAlwaysEdit(boolean alwaysEdit) {
        this.alwaysEdit = alwaysEdit;
        this.editing = true;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setup();
    }


    private void setup() {
        for (Canvas child : getMembers()) {
            child.destroy();
        }

        Layout layout = vertical ? new VLayout() : new HLayout();
        layout.setMembersMargin(8);

        HTMLFlow title = new HTMLFlow("<b>Tags:</b>");
        title.setAutoWidth();
        layout.addMember(title);

        for (final Tag tag : tags) {
            HLayout tagLayout = new HLayout();
            //tagLayout.set

            HTMLFlow tagString = new HTMLFlow(tag.toString());
            tagString.setAutoWidth();
            tagLayout.addMember(tagString);
            if (editing) {
                Img remove = new Img("[skin]/images/actions/remove.png", 16, 16);
                remove.setTooltip("Click to remove this tag");
                remove.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        tags.remove(tag);
                        save();
//                        TagEditorView.this.setup();
                    }
                });
                tagLayout.addMember(remove);
            }

            tagLayout.setHeight(32);
            layout.addMember(tagLayout);

//
//            Canvas spacer = new Canvas();
//            spacer.setWidth(8);
//            layout.addMember(spacer);
        }

        HLayout editLayout = new HLayout();
        editLayout.setHeight(32);

        if (editing) {
            form = new DynamicForm();
            final ComboBoxItem tagInput = new ComboBoxItem("tag");
            tagInput.setShowTitle(false);
            tagInput.setHideEmptyPickList(true);
//            tagInput.setOptionDataSource(new TaggingDataSource());
            TagCriteria criteria = new TagCriteria();
            criteria.addSortNamespace(PageOrdering.ASC);
            criteria.addSortSemantic(PageOrdering.ASC);
            criteria.addSortName(PageOrdering.ASC);
            GWTServiceLookup.getTagService().findTagsByCriteria(criteria,
                    new AsyncCallback<PageList<Tag>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to load tags", caught);
                        }

                        public void onSuccess(PageList<Tag> result) {
                            String[] values = new String[result.size()];
                            int i = 0;
                            for (Tag tag : result) {
                                values[i++] = tag.toString();
                            }
                            tagInput.setValueMap(values);
                        }
                    });


            tagInput.setValueField("tag");
            tagInput.setDisplayField("tag");
            tagInput.setType("comboBox");
            tagInput.setTextMatchStyle(TextMatchStyle.SUBSTRING);
            tagInput.setTooltip("Enter a tag in the format \"(namespace:)(semantic=)tagname\" (e.g. it:env=QA, or owner=John)");
            /*tagInput.addBlurHandler(new BlurHandler() {
                public void onBlur(BlurEvent blurEvent) {
                    String tag = form.getValueAsString("tag");
                    if (tag != null) {
                        Tag newTag = new Tag(tag);
                        tags.add(newTag);
                        save();
//                        TagEditorView.this.setup();
                    }
                }
            });*/
            tagInput.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                        String tag = form.getValueAsString("tag");
                        if (tag != null) {
                            Tag newTag = new Tag(tag);
                            tags.add(newTag);
                            save();
//                            TagEditorView.this.setup();
                        }
                    }
                }
            });

            form.setFields(tagInput);

            editLayout.addMember(form);
        }


        if (!readOnly) {
            Img modeImg = new Img("[skin]/images/actions/" + (editing ? "approve" : "edit") + ".png", 16, 16);
            modeImg.setTooltip(editing ? "Click to save edits" : "Click to edit tags");
            modeImg.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {

                    if (editing) {
                        save();
                    }
                    editing = !editing;
                    TagEditorView.this.setup();

                }
            });
            editLayout.addMember(modeImg);
        }


        layout.addMember(editLayout);


        layout.setAutoWidth();
        addMember(layout);

        markForRedraw();
    }

    private void save() {
        this.callback.tagsChanged(tags);
        TagEditorView.this.setup();
    }
}
