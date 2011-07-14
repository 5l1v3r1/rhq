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
package org.rhq.enterprise.server.search.translation;

import static org.rhq.enterprise.server.search.common.SearchQueryGenerationUtility.getJPQLForString;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.assist.AlertSearchAssistParam;
import org.rhq.enterprise.server.search.SearchExpressionException;
import org.rhq.enterprise.server.search.translation.antlr.RHQLAdvancedTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragment;

/**
 * @author Joseph Marques
 */
public class ResourceSearchTranslator extends AbstractSearchTranslator {

    public ResourceSearchTranslator(Subject subject) {
        super(subject);
    }

    public SearchFragment getSearchFragment(String alias, RHQLAdvancedTerm term) {
        String path = term.getPath();
        RHQLComparisonOperator op = term.getOperator();
        String param = term.getParam();

        String filter = term.getValue();

        if (path.equals("availability")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForEnum(alias + ".currentAvailability.availabilityType", op, filter, AvailabilityType.class,
                    true));

        } else if (path.equals("category")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForEnum(alias + ".resourceType.category", op, filter, ResourceCategory.class, false));

        } else if (path.equals("type")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.name", op, filter));

        } else if (path.equals("plugin")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.plugin", op, filter));

        } else if (path.equals("name")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".name", op, filter));

        } else if (path.equals("version")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".version", op, filter));

        } else if (path.equals("alerts")) {
            return new SearchFragment( //
                SearchFragment.Type.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res " //
                    + "  JOIN res.alertDefinitions alertDef " //
                    + "  JOIN alertDef.alerts alert " //
                    + " WHERE alert.ctime > "
                    + AlertSearchAssistParam.getLastTime(param) //
                    + (filter.equalsIgnoreCase("any") ? "" : "   and "
                        + getJPQLForEnum("alertDef.priority", op, filter, AlertPriority.class, false)) //
                    + " GROUP BY res.id " //
                    + "HAVING COUNT(alert) > 0 ");

        } else if (path.equals("trait")) {
            return new SearchFragment( //
                SearchFragment.Type.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res, MeasurementDataTrait trait " //
                    + "  JOIN res.schedules schedule " //
                    + " WHERE trait.schedule = schedule " //
                    + "   AND schedule.definition.dataType = " + DataType.TRAIT.ordinal() //
                    + "   AND " + getJPQLForString("schedule.definition.name", RHQLComparisonOperator.EQUALS, param) //
                    + "   AND " + getJPQLForString("trait.value", op, filter));

        } else if (path.equals("connection")) {
            return new SearchFragment( //
                SearchFragment.Type.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res, PropertySimple simple, PropertyDefinitionSimple simpleDefinition " //
                    + "  JOIN res.resourceType.pluginConfigurationDefinition.propertyDefinitions definition " //
                    + "  JOIN res.pluginConfiguration.properties property " //
                    + " WHERE simpleDefinition = definition " // only provide translations for simple properties
                    + "   AND simpleDefinition.type <> 'PASSWORD' " // do not allow searching by hidden/password fields
                    + "   AND property = simple " // join to simple for filter by 'stringValue' attribute
                    + "   AND " + getJPQLForString("definition.name", RHQLComparisonOperator.EQUALS, param) //
                    + "   AND " + getJPQLForString("simple.stringValue", op, filter));

        } else if (path.equals("configuration")) {
            return new SearchFragment( //
                SearchFragment.Type.PRIMARY_KEY_SUBQUERY, "SELECT res.id" //
                    + "  FROM Resource res, PropertySimple simple, PropertyDefinitionSimple simpleDefinition " //
                    + "  JOIN res.resourceType.resourceConfigurationDefinition.propertyDefinitions definition " //
                    + "  JOIN res.resourceConfiguration.properties property " //
                    + " WHERE simpleDefinition = definition " // only provide translations for simple properties
                    + "   AND simpleDefinition.type <> 'PASSWORD' " // do not allow searching by hidden/password fields
                    + "   AND property = simple " // join to simple for filter by 'stringValue' attribute
                    + conditionallyAddAuthzFragment(getConfigAuthzFragment()) //
                    + "   AND " + getJPQLForString("definition.name", RHQLComparisonOperator.EQUALS, param) //
                    + "   AND " + getJPQLForString("simple.stringValue", op, filter));

        } else {
            if (param == null) {
                throw new SearchExpressionException("No search fragment available for " + path);
            } else {
                throw new SearchExpressionException("No search fragment available for " + path + "[" + param + "]");
            }
        }
    }

    private String getConfigAuthzFragment() {
        return "res.id IN " //
            + "(SELECT ires.id " //
            + "   FROM Resource ires " //
            + "   JOIN ires.implicitGroups igroup " //
            + "   JOIN igroup.roles irole " //
            + "   JOIN irole.subjects isubject " //
            + "   JOIN irole.permissions iperm " //
            + "  WHERE isubject.id = " + getSubjectId() //
            + "    AND iperm = " + Permission.CONFIGURE_READ.ordinal() + ")";
    }

}
