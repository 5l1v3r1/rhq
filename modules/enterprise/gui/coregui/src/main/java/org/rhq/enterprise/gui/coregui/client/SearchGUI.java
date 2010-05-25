/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

/**
 * @author Joseph Marques
 */
public class SearchGUI implements EntryPoint {

    public static SearchGUI singleton = new SearchGUI();
    private static Subject sessionSubject;
    private SearchBar searchBar;

    private SearchGUI() {
    }

    public static SearchGUI get() {
        return singleton;
    }

    public void onModuleLoad() {
        if (SearchBar.existsOnPage() == false) {
            System.out.println("Suppressing load of SearchGUI module");
            return;
        }

        portalWarLogin();
    }

    public void buildSearchGUI() {
        searchBar = new SearchBar();
    }

    private static void portalWarLogin() {
        SubjectGWTServiceAsync subjectService = SubjectGWTServiceAsync.Util.getInstance();

        subjectService.login("rhqadmin", "rhqadmin", new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                System.out.println("Failed to login - cause: " + caught);
                Label loginFailed = new Label("Failed to login - cause: " + caught);
                loginFailed.draw();
            }

            public void onSuccess(Subject result) {
                System.out.println("Logged in: " + result.getSessionId());
                GWTServiceLookup.registerSession(String.valueOf(result.getSessionId()));
                SearchGUI.sessionSubject = result;
                singleton.buildSearchGUI();
            }
        });
    }

    public static Subject getSessionSubject() {
        return sessionSubject;
    }

    public SearchBar getSearchBar() {
        return searchBar;
    }
}
