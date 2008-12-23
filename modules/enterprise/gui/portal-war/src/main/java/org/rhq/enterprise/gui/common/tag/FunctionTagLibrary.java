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
package org.rhq.enterprise.gui.common.tag;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Set;

import com.sun.facelets.tag.AbstractTagLibrary;

import org.rhq.core.clientapi.util.units.DateFormatter;
import org.rhq.core.clientapi.util.units.FormattedNumber;
import org.rhq.core.clientapi.util.units.ScaleConstants;
import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.StringConstants;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A Facelets tag library containing custom EL functions for use in RHQ GUI pages.
 *
 * @author     Ian Springer
 * @deprecated there are weird issues with Facelets function taglibs - managed beans should be used instead
 */
@Deprecated
public class FunctionTagLibrary extends AbstractTagLibrary {
    /**
     * Namespace used to import this library in Facelets pages
     */
    public static final String NAMESPACE = "http://jboss.org/on/function";

    /**
     * Current instance of library.
     */
    public static final FunctionTagLibrary INSTANCE = new FunctionTagLibrary();

    public FunctionTagLibrary() {
        super(NAMESPACE);
        try {
            Method[] methods = FunctionTagLibrary.class.getMethods();
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    this.addFunction(method.getName(), method);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Resource with the id specified via the 'id' query string parameter in the current request.
     *
     * @return the Resource with the id specified via the 'id' query string parameter in the current request
     */
    public static void loadResource() {
        EnterpriseFacesContextUtility.getResource();
    }

    /**
     * Returns a <code>ResourceFacets</code> object that represents the facets (measurement, configuration, etc.) that
     * are supported by the resource type with the specified id. Individual facets can be checked easily via EL, e.g.:
     * <code>&lt;c:set var="resourceFacets" value="${onf:getResourceFacets(Resource.resourceType.id)}"/&gt; &lt;c:if
     * test="${resourceFacets.measurement}"&gt; ...</code>
     *
     * @param  resourceTypeId a {@link org.rhq.core.domain.resource.ResourceType} id
     *
     * @return a <code>ResourceFacets</code> object for the resource type with the specified id
     */
    public static ResourceFacets getResourceFacets(int resourceTypeId) {
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        try {
            return resourceTypeManager.getResourceFacets(EnterpriseFacesContextUtility.getSubject(), resourceTypeId);
        } catch (ResourceTypeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ResourceFacets getFacets() {
        Resource resource = EnterpriseFacesContextUtility.getResource();
        ResourceType resourceType = resource.getResourceType();
        int resourceTypeId = resourceType.getId();
        return getResourceFacets(resourceTypeId);
    }

    /**
     * Returns a <code>ResourcePermission</code> object for the resource associated with the current request. Individual
     * permissions can be checked easily via EL, e.g.: <code>&lt;c:set var="resourcePerm"
     * value="${onf:getResourcePermission()}"/&gt; &lt;c:if test="${resourcePerm.measure}"&gt; ...</code>
     *
     * @return a <code>ResourcePermission</code> object for the resource with the specified id
     */
    public static ResourcePermission getResourcePermission() {
        AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();
        Set<Permission> resourcePerms = authorizationManager.getImplicitResourcePermissions(subject, resource.getId());
        return new ResourcePermission(resourcePerms);
    }

    /**
     * Returns a <code>ResourcePermission</code> object for the group with the specified id. Individual permissions can
     * be checked easily via EL, e.g.: <code>&lt;c:set var="groupPerm" value="${onf:getGroupPermission(groupId)}"/&gt;
     * &lt;c:if test="${groupPerm.measure}"&gt; ...</code>
     *
     * @param  groupId a {@link ResourceGroup} id
     *
     * @return a <code>ResourcePermission</code> object for the group with the specified id
     */
    public static ResourcePermission getGroupPermission(int groupId) {
        AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Set<Permission> groupPerms = authorizationManager.getImplicitGroupPermissions(subject, groupId);
        return new ResourcePermission(groupPerms);
    }

    /**
     * Formats the specified milliseconds-since-epoch timestamp as a String.
     *
     * @param      timestamp a milliseconds-since-epoch timestamp
     *
     * @return     a String representation of the timestamp
     *
     * @deprecated use <code>f:convertDateTime</code> tag instead
     */
    @Deprecated
    public static String formatTimestamp(long timestamp) {
        // NOTE: The below code was snarfed from org.rhq.enterprise.gui.legacy.taglib.DateFormatter.

        UnitsConstants unit = UnitsConstants.UNIT_DATE;
        String key = StringConstants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis";
        String formatString = RequestUtils.message(FacesContextUtility.getRequest(), key);
        DateFormatter.DateSpecifics specs = new DateFormatter.DateSpecifics();
        specs.setDateFormat(new SimpleDateFormat(formatString));
        FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(timestamp, unit, ScaleConstants.SCALE_MILLI),
            FacesContextUtility.getRequest().getLocale(), specs);
        return fmtd.toString();
    }

    public static int sizeOf(Collection<?> collection) {
        if (collection == null) {
            return 0;
        }
        return collection.size();
    }

    public static WebUserPreferences getWebUserPreferences() {
        return new WebUserPreferences(EnterpriseFacesContextUtility.getSubject());
    }

    public static String contextFragmentURL() {
        EntityContext context = WebUtility.getEntityContext();
        switch (context.category) {
        case Resource:
            return ParamConstants.RESOURCE_ID_PARAM + "=" + String.valueOf(context.resourceId);
        case ResourceGroup:
            return ParamConstants.GROUP_ID_PARAM + "=" + String.valueOf(context.groupId);
        case AutoGroup:
            return ParamConstants.RESOURCE_TYPE_ID_PARAM + "=" + String.valueOf(context.parentResourceId) + "&"
                + ParamConstants.RESOURCE_TYPE_ID_PARAM + "=" + String.valueOf(context.resourceTypeId);
        default:
            throw new IllegalArgumentException(context.getUnknownContextMessage());
        }
    }

    public static Resource getResource(int resourceId) {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        return LookupUtil.getResourceManager().getResourceById(user, resourceId);
    }

    /**
     * Returns the alert recovery information
     * 
     * @param    alertId
     * @return   Alert recovery information
     * @author   Fady Matar     
     */
    public static String getAlertRecoveryInfo(int alertId) {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        Alert alert = alertManager.getById(alertId);

        if (alert.getRecoveryId() != 0) {
            Integer recoveryAlertId = alert.getRecoveryId();
            AlertDefinitionManagerLocal alertDefinitionManagerLocal = LookupUtil.getAlertDefinitionManager();
            AlertDefinition recoveryAlertDefinition = alertDefinitionManagerLocal.getAlertDefinitionById(subject,
                recoveryAlertId);
            return recoveryAlertDefinition.getName();
        }

        if (alert.getWillRecover()) {
            return "2";
        }

        return "1";
    }

    public static String trimString(String str, int numChars) {
        if (str == null)
            throw new IllegalArgumentException("cannot retrieve characters of a null string");
        if (numChars > str.length())
            return str;
        return str.substring(0, numChars) + "...";
    }

    private final static String AMP = "&amp;";

    public static String getChartURLParams(MetricDisplaySummary summary) {
        StringBuilder buffer = new StringBuilder();

        buffer.append(contextFragmentURL()).append(AMP);
        buffer.append("imageWidth=647").append(AMP);
        buffer.append("imageHeight=100").append(AMP);
        buffer.append("schedId=").append(summary.getScheduleId()).append(AMP);
        buffer.append("definitionId=").append(summary.getDefinitionId()).append(AMP);
        buffer.append("measurementUnits=").append(summary.getUnits()).append(AMP);
        buffer.append("now=").append(System.currentTimeMillis());

        return buffer.toString();
    }
}