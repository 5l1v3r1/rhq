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
package org.rhq.enterprise.gui.coregui.client.gwt;


import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * This lookup service retreives each RPC service and sets a
 * custom RpcRequestBuilder that adds the login session id to
 * be security checked on the server.
 *
 * @author Greg Hinkle
 */
public class GWTServiceLookup {

    public static final String SESSION_NAME = "RHQ_Sesssion";


    public static ConfigurationGWTServiceAsync getConfigurationService() {
        return secure(ConfigurationGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGWTServiceAsync getResourceService() {
        return secure(ResourceGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGroupGWTServiceAsync getResourceGroupService() {
        return secure(ResourceGroupGWTServiceAsync.Util.getInstance());
    }

    public static ResourceTypeGWTServiceAsync getResourceTypeGWTService() {
        return secure(ResourceTypeGWTServiceAsync.Util.getInstance());
    }

    public static RoleGWTServiceAsync getRoleService() {
        return secure(RoleGWTServiceAsync.Util.getInstance());
    }

    public static SubjectGWTServiceAsync getSubjectService() {
        return secure(SubjectGWTServiceAsync.Util.getInstance());
    }

    public static MeasurementDataGWTServiceAsync getMeasurementDataService() {
        return secure(MeasurementDataGWTServiceAsync.Util.getInstance());
    }


    private static <T> T secure(Object sdt) {
        if (!(sdt instanceof ServiceDefTarget)) return null;

        ((ServiceDefTarget) sdt).setRpcRequestBuilder(new SessionRpcRequestBuilder());

        return (T) sdt;
    }


    public static void registerSession(String sessionId) {
        Cookies.setCookie(SESSION_NAME, sessionId);
    }

    public static class SessionRpcRequestBuilder extends RpcRequestBuilder {

        @Override
        protected void doFinish(RequestBuilder rb) {
            super.doFinish(rb);

            String sid = Cookies.getCookie(SESSION_NAME);
            if (sid != null) {
                rb.setHeader(SESSION_NAME, sid);
            }
        }
    }


}
