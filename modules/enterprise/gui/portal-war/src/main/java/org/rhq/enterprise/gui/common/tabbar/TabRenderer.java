/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.common.tabbar;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.UrlUtility;

/**
 * A renderer that renders a {@link TabComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class TabRenderer extends Renderer {
    static final String IMAGES_PATH = "/images";
    static final int TAB_IMAGE_WIDTH = 102;
    static final int TAB_IMAGE_HEIGHT = 21;

    /**
     * Encode this component.
     *
     * @param facesContext <code>FacesContext</code> for the current request
     * @param component    <code>TabComponent</code> to be encoded
     */
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        TabComponent tab = (TabComponent) component;
        processAttributes(tab);

        // Process f:param child tags...
        tab.setParameters(FacesComponentUtility.getParameters(tab));

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement("td", tab);
        if (!tab.isSelected()) {
            writer.startElement("a", tab);
            writer.writeAttribute("href", buildURL(tab), "url");
        }

        writer.startElement("img", tab);
        String imageBasePath = IMAGES_PATH + "/tab_" + tab.getName();
        String imageQualifier = (tab.isSelected()) ? "on" : "off";
        String imageURL = imageBasePath + "_" + imageQualifier + ".gif";
        writer.writeAttribute("src", imageURL, null);
        if (!tab.isSelected()) {
            writer.writeAttribute("onmouseover", "imageSwap(this, '" + imageBasePath + "', '_over')", null);
            writer.writeAttribute("onmouseout", "imageSwap(this, '" + imageBasePath + "', '_off')", null);
        }

        writer.writeAttribute("alt", buildAlt(tab), "alt");
        writer.writeAttribute("width", TAB_IMAGE_WIDTH, null);
        writer.writeAttribute("height", TAB_IMAGE_HEIGHT, null);
        writer.writeAttribute("border", 0, null);
        writer.endElement("img");
        if (!tab.isSelected()) {
            writer.endElement("a");
        }

        writer.endElement("td");
    }

    private void processAttributes(TabComponent tab) {
        if (tab.getName() == null) {
            throw new IllegalStateException("The 'tab' element requires a 'name' attribute.");
        }

        if (tab.getUrl() == null) {
            if (tab.getChildCount() == 0) {
                throw new IllegalStateException(
                    "The 'tab' element requires a 'url' attribute when it has no child 'subtab' elements.");
            }
        }
    }

    private String buildURL(TabComponent tab) {
        String url;
        SubtabComponent defaultSubtab = null;
        if (tab.getSubtabs().isEmpty()) {
            url = tab.getUrl();
        } else {
            // NOTE: If we are not the selected tab, we inherit the URL of our first (i.e. leftmost-displayed) subtab.
            defaultSubtab = tab.getDefaultSubtab();
            assert defaultSubtab != null;
            url = defaultSubtab.getUrl();
        }

        // Create a master list of params from our default subtab's params (if we have subtabs), our params, and the
        // params of our enclosing tabBar, in that order of precedence.
        TabBarComponent tabBar = (TabBarComponent) tab.getParent();
        Map<String, String> parameters = new LinkedHashMap<String, String>(tabBar.getParameters());
        parameters.putAll(tab.getParameters());
        if (defaultSubtab != null) {
            // We need to process the subtab's f:param child tags first, since the subtab's renderer hasn't had a chance to
            // do it yet!
            defaultSubtab.setParameters(FacesComponentUtility.getParameters(defaultSubtab));
            parameters.putAll(defaultSubtab.getParameters());
        }

        url = UrlUtility.addParametersToQueryString(url, parameters);

        // Session-encode the URL in case the client doesn't have cookies enabled.
        url = FacesContext.getCurrentInstance().getExternalContext().encodeResourceURL(url);

        return url;
    }

    private String buildAlt(TabComponent tab) {
        String alt;
        SubtabComponent defaultSubtab = tab.getDefaultSubtab();
        if (defaultSubtab != null) {
            alt = (defaultSubtab.getAlt() != null) ? defaultSubtab.getAlt() : "";
        } else {
            alt = (tab.getAlt() != null) ? tab.getAlt() : "";
        }

        return alt;
    }
}