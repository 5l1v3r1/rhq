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
package org.rhq.enterprise.gui.legacy.portlet.didYouKnow;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the <code>PORTAL_PARAM</code> request
 * parameter (or the default portal, if the parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */
public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String tip = "Tip #0: tips are only available to logged in users";
        try {

            ClassLoader cl = this.getClass().getClassLoader();
            Properties tips = new Properties();

            tips.load(cl.getResourceAsStream("tips.properties"));

            // Load a random tip
            long index = System.currentTimeMillis() % tips.size();
            String strIndex = new Long(index + 1).toString();

            tip = "Tip #" + strIndex + ": " + (String) tips.get(strIndex);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [DidYouKnow] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [DidYouKnow] experienced an error: " + e.getMessage());
            }
        } finally {
            context.putAttribute("tip", tip);
        }

        return null;
    }
}