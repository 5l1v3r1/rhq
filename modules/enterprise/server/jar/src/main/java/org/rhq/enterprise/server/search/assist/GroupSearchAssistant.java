package org.rhq.enterprise.server.search.assist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;

public class GroupSearchAssistant extends TabAwareSearchAssistant {

    private static final List<String> parameterizedContexts;
    private static final List<String> simpleContexts;

    static {
        parameterizedContexts = Collections.emptyList();
        simpleContexts = Collections.unmodifiableList(Arrays.asList("availability", "category", "type", "plugin",
            "name"));
    }

    public GroupSearchAssistant(String tab) {
        super(tab);
    }

    public SearchSubsystem getSearchSubsystem() {
        return SearchSubsystem.GROUP;
    }

    @Override
    public String getPrimarySimpleContext() {
        return "name";
    }

    @Override
    public List<String> getSimpleContexts() {
        return simpleContexts;
    }

    @Override
    public List<String> getParameterizedContexts() {
        return parameterizedContexts;
    }

    @Override
    public List<String> getParameters(String context, String filter) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getValues(String context, String param, String filter) {
        filter = stripQuotes(filter);
        if (context.equals("availability")) {
            return filter(AvailabilityType.class, filter);

        } else if (context.equals("category")) {
            return filter(ResourceCategory.class, filter);

        } else if (context.equals("type")) {
            return execute("" //
                + "SELECT DISTINCT type.name " //
                + "  FROM ResourceType type, ResourceGroup rg " //
                + " WHERE rg.resourceType = type " // only suggest names that exist for visible groups in inventory
                + "   AND rg.visible = true " //
                + add("   AND LOWER(rg.groupCategory) = '" + tab + "'", tab) //
                + add("   AND LOWER(type.name) LIKE '%" + escape(filter.toLowerCase()) + "%'", filter) //
                + " ORDER BY type.name ");

        } else if (context.equals("plugin")) {
            return execute("" //
                + "SELECT DISTINCT type.plugin " //
                + "  FROM ResourceType type, ResourceGroup rg " //
                + " WHERE rg.resourceType = type " // only suggest names that exist for visible groups in inventory
                + "   AND rg.visible = true " //
                + add("   AND LOWER(rg.groupCategory) = '" + tab + "'", tab) //
                + add("   AND LOWER(type.plugin) LIKE '%" + escape(filter.toLowerCase()) + "%'", filter) //
                + " ORDER BY type.plugin ");

        } else if (context.equals("name")) {
            return execute("" //
                + "SELECT DISTINCT rg.name " //
                + "  FROM ResourceGroup rg " //
                + " WHERE rg.visible = true " // only suggest names that exist for visible groups in inventory
                + add("   AND LOWER(rg.groupCategory) = '" + tab + "'", tab) //
                + add("   AND LOWER(rg.name) LIKE '%" + escape(filter.toLowerCase()) + "%'", filter) //
                + " ORDER BY rg.name ");

        } else {
            return Collections.emptyList();

        }
    }

}
