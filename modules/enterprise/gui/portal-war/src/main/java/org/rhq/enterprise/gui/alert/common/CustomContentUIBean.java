/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert.common;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;

@Scope(ScopeType.PAGE)
@Name("customContentUIBean")
public class CustomContentUIBean {

    @RequestParameter
    private String senderName;

    @RequestParameter("nid")
    private Integer notificationId;

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    @In("#{webUser.subject}")
    private Subject subject;

    private String contentUrl;

    public String getContentUrl() {
        return contentUrl;
    }

    @Create
    public void init()  {
        AlertSenderInfo info = alertNotificationManager.getAlertInfoForSender(this.senderName);

        if (info != null && info.getUiSnippetUrl() != null) {
            this.contentUrl = info.getUiSnippetUrl().toString();
        }

        String backingBeanName = alertNotificationManager.getBackingBeanNameForSender(this.senderName);
        CustomAlertSenderBackingBean backingBean = alertNotificationManager.getBackingBeanForSender(this.senderName, // TODO notificationId may be stale after removal of notification
                notificationId);

        if (backingBeanName != null && backingBean != null) {
            backingBean.setWebUser(subject);
            outjectBean(backingBeanName, backingBean);
        }
    }

    /**
     * We are just getting an Object from the plugin manager which acts as our backing bean.
     * This method is used instead of @Out or @Factory because we need to be able to
     * dynamically assign the component's name so that the plugin author can define the
     * name of bean, but this class is not an "official" seam component.
     */
    private void outjectBean(String name, CustomAlertSenderBackingBean bean) {

        Context context = Contexts.getSessionContext();

        CustomAlertSenderBackingBean csb = (CustomAlertSenderBackingBean) context.get(name);
        if (csb ==null)
            context.set(name,bean);

    }
}
