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
package org.rhq.enterprise.gui.coregui.client;

import java.util.HashMap;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.docs.Image;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;

/**
 * @author Greg Hinkle
 */
public class LoginView {


    private Window window;
    private DynamicForm form;

    public void showLoginDialog() {

        form = new DynamicForm();
        form.setMargin(10);
        form.setShowInlineErrors(false);

        CanvasItem logo = new CanvasItem();
        logo.setCanvas(new Img("header/rhq_logo_28px.png",80,28));
        logo.setShowTitle(false);

        HeaderItem header = new HeaderItem();
        header.setValue("RHQ Login");


        TextItem user = new TextItem("user", "User");
        user.setRequired(true);
        PasswordItem password = new PasswordItem("password", "Password");
        password.setRequired(true);

        final SubmitItem login = new SubmitItem("login", "Login");
        login.setAlign(Alignment.CENTER);
        login.setColSpan(2);


        form.setFields(logo, header, user, password, login);


        window = new Window();
        window.setTitle("RHQ Login");
        window.setWidth(400);
        window.setHeight(400);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(form);
        window.show();

        form.focusInItem(user);

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                if (form.validate()) {
                    login(form.getValueAsString("user"), form.getValueAsString("password"));
                }
            }
        });

    }

    private void login(String user, String password) {

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET,
                "/j_security_check.do?j_username=" + user + "&j_password=" + password);
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        System.out.println("Portal-War logged in");
                        window.destroy();
                        CoreGUI.checkLoginStatus();
                    } else {
                        form.setFieldErrors("user", "The username or password provided does not match our records.", true);
                    }
                }

                public void onError(Request request, Throwable exception) {
                    System.out.println("Portal-War login failed");
                }
            });
            b.send();
        } catch (RequestException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }


        /*
        SubjectGWTServiceAsync subjectService = SubjectGWTServiceAsync.Util.getInstance();

        subjectService.login(user, password, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                System.out.println("Failed to login - cause: " + caught);
                Label loginFailed = new Label("Failed to login - cause: " + caught);
                loginFailed.draw();
            }

            public void onSuccess(Subject result) {
                System.out.println("Logged in: " + result.getSessionId());
                CoreGUI.setSessionSubject(result);

                *//* We can cache all metadata right here
                ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                        (Integer[]) null, EnumSet.allOf(ResourceTypeRepository.MetadataType.class), new ResourceTypeRepository.TypesLoadedCallback() {
                    public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                        System.out.println("Preloaded [" + types.size() + "] resource types");
                        buildCoreUI();
                    }
                });
                *//*
            }
        });  */

    }
}
