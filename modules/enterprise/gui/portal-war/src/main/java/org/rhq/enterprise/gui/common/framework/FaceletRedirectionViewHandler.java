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
package org.rhq.enterprise.gui.common.framework;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import com.sun.facelets.FaceletViewHandler;

import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;

/**
 * @author Joseph Marques
 */
public class FaceletRedirectionViewHandler extends FaceletViewHandler {

    public FaceletRedirectionViewHandler(ViewHandler handler) {
        super(handler);
    }

    @Override
    public String getActionURL(FacesContext facesContext, String viewId) {
        // Evaluate any EL variables that are contained in the view id.
        // (e.g. "/rhq/resource/artifact/view.xhtml?id=#{param.id}")
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(viewId, String.class);
        String actionURL = FacesExpressionUtility.getValue(valueExpression, String.class);
        return actionURL;
    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException {
        long monitorId = HibernatePerformanceMonitor.get().start();
        super.renderView(context, viewToRender);
        HibernatePerformanceMonitor.get().stop(monitorId, "URL " + viewToRender.getViewId());
    }
}