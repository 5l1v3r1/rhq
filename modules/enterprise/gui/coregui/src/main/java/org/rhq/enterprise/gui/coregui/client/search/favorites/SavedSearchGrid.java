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
package org.rhq.enterprise.gui.coregui.client.search.favorites;

import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Grid;

import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

/**
 * @author Joseph Marques
 */
public class SavedSearchGrid extends Grid {

    private PatternSelectionHandler patternSelectionHandler;
    private SearchBar searchBar;

    public interface PatternSelectionHandler {
        public void handleSelection(int rowIndex, int columnIndex, String patternName);
    }

    public void setPatternSelectionHandler(PatternSelectionHandler handler) {
        this.patternSelectionHandler = handler;
    }

    class SavedSearchRowFormatter extends RowFormatter {
        @Override
        public String getStyleName(int row) {
            if (row < count()) {
                return " savedSearchesPanel-row";
            }
            return "";
        }

        @Override
        public String getStylePrimaryName(int row) {
            return getStyleName(row);
        }
    }

    public SavedSearchGrid(SearchBar searchBar) {
        super(0, 2); // assume no rows to start, but we'll always have 2 columns

        setRowFormatter(new SavedSearchRowFormatter());
        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK);
        setCellSpacing(0);
        setCellPadding(5);
        setStyleName("savedSearchesGrid");

        this.searchBar = searchBar;
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element td = getEventTargetCell(event);
        if (td == null) {
            return;
        }
        Element tr = DOM.getParent(td);
        Element table = DOM.getParent(tr);
        switch (DOM.eventGetType(event)) {
        case Event.ONCLICK: {
            int columnIndex = DOM.getChildIndex(tr, td);
            int rowIndex = DOM.getChildIndex(table, tr);
            String text = getHTML(rowIndex, 0);
            int startIndex = text.indexOf('>') + 1;
            int endIndex = text.indexOf("</span>", startIndex);
            String patternName = text.substring(startIndex, endIndex);
            patternSelectionHandler.handleSelection(rowIndex, columnIndex, patternName);
            if (columnIndex == 0) {
                onRowOut(tr);
            }
            break;
        }
        case Event.ONMOUSEOVER: {
            onRowOver(tr);
            break;
        }
        case Event.ONMOUSEOUT: {
            onRowOut(tr);
            break;
        }
        }
    }

    protected void onRowOut(Element row) {
        Element actionCell = DOM.getChild(row, 1);
        actionCell.setAttribute("style", "");
        row.setAttribute("style", "background-color: white;");
    }

    protected void onRowOver(Element row) {
        Element actionCell = DOM.getChild(row, 1);
        actionCell.setAttribute("style", "width: 24px; height: 24px; background: url(" + SearchBar.TRASH
            + ") no-repeat center;");
        row.setAttribute("style", "background-color: #eeeeee;");
    }

    public void updateModel() {
        List<String> names = searchBar.getSavedSearchManager().getPatternNamesMRU();

        clear(true);
        resizeRows(names.size());

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String pattern = searchBar.getSavedSearchManager().getPatternByName(name);
            setHTML(i, 0, stylize(name, pattern));
            setHTML(i, 1, trashify());
        }
        setRowFormatter(new SavedSearchRowFormatter());
    }

    private static String stylize(String name, String pattern) {
        return "<span class=\"savedSearchesPanel-top\">" + name + "</span>" + "<br/>"
            + "<span class=\"savedSearchesPanel-bottom\">" + pattern + "</span>";
    }

    private static String trashify() {
        return "<div name=\"action\">&nbsp;</div>";
    }

    private int count() {
        return searchBar.getSavedSearchManager().getSavedSearchCount();
    }

    public String getSelectedItem() {
        return "";
    }

    public static void main(String[] args) {
        Grid grid = new Grid();
        grid.clear(true);

    }
}
