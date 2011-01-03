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
package org.rhq.enterprise.gui.coregui.client.search;

import com.google.gwt.user.client.ui.Composite;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.search.favorites.SavedSearchManager;

/**
 * Abstract class for SearchBar, so that a separate implementation can be written alongside the original. 
 * 
 * @author Joseph Marques
 */
public abstract class AbstractSearchBar extends Composite {

    public abstract SearchSubsystem getSearchSubsystem();

    public abstract String getSelectedTab();

    public abstract void activateSavedSearch(String savedSearchName);

    public abstract void onSavedSearchManagerLoaded();

    public abstract SavedSearchManager getSavedSearchManager();
}
