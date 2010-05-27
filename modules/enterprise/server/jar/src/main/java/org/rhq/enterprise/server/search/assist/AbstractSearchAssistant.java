package org.rhq.enterprise.server.search.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.util.LookupUtil;

public abstract class AbstractSearchAssistant implements SearchAssistant {

    private final Log log = LogFactory.getLog(SearchAssistant.class);

    private int maxResultCount = 20;

    public int getMaxResultCount() {
        return maxResultCount;
    }

    public void setMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
    }

    public String getPrimarySimpleContext() {
        return null;
    }

    public List<String> getSimpleContexts() {
        return Collections.emptyList();
    }

    public List<String> getParameterizedContexts() {
        return Collections.emptyList();
    }

    public List<String> getParameters(String context, String filter) {
        if (getParameterizedContexts().contains(context) == false) {
            throw new IllegalArgumentException("context[" + context
                + "] is not parameterized, no completions available");
        }
        return Collections.emptyList();
    }

    public List<String> getValues(String context, String param, String filter) {
        if (getSimpleContexts().contains(context) && param != null) {
            throw new IllegalArgumentException("context[" + context + "] is simple, param[" + param
                + "] can not be handled");
        }
        if (getParameterizedContexts().contains(context) && param == null) {
            throw new IllegalArgumentException("context[" + context + "] is parameterized, param must not be null");
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    protected final List<String> execute(String jpql) {
        log.debug("Executing JPQL: " + jpql);
        Query query = LookupUtil.getEntityManager().createQuery(jpql);
        query.setMaxResults(maxResultCount);
        List<String> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    protected final Map<String, List<String>> executeMap(String jpql) {
        log.debug("Executing Map JPQL: " + jpql);
        Query query = LookupUtil.getEntityManager().createQuery(jpql);
        List<Object[]> rawResults = query.getResultList();
        Map<String, List<String>> results = new HashMap<String, List<String>>();
        for (Object[] nextTuple : rawResults) {
            String key = (String) nextTuple[0];
            String value = (String) nextTuple[1];
            List<String> valueList = results.get(key);
            if (valueList == null) {
                valueList = new ArrayList<String>();
                results.put(key, valueList);
            }
            valueList.add(value);
        }
        return results;
    }

    protected final String add(String fragment, String parameter) {
        if (!parameter.equals("")) {
            return fragment;
        }
        return "";
    }

    protected final List<String> filter(Class<? extends Enum<?>> enumType, String filter) {
        List<String> results = new ArrayList<String>();
        for (Enum<?> next : enumType.getEnumConstants()) {
            String enumName = next.name().toLowerCase();
            if (filter == null || filter.equals("") || enumName.contains(filter)) {
                results.add(enumName);
            }
        }
        return Collections.unmodifiableList(results);
    }

    protected final List<String> filter(List<String> data, String filter) {
        return filter(data, filter, 10);
    }

    protected final List<String> filter(List<String> data, String filter, int max) {
        List<String> results = new ArrayList<String>();
        int count = 0;
        for (String next : data) {
            if (filter.equals("") //
                || (filter.equals("null") && next == null) //
                || (next != null && next.toLowerCase().indexOf(filter) != -1)) {
                count++;
                results.add(next);
                if (count == max) {
                    break;
                }
            }
        }
        return Collections.unmodifiableList(results);
    }

    protected final String quote(String data) {
        return "'" + data + "'";
    }

    protected final String getFormatterValueFragment(String data) {
        boolean hasWhitespace = false;
        for (char next : data.toCharArray()) {
            if (Character.isWhitespace(next)) {
                hasWhitespace = true;
                break;
            }
        }

        if (hasWhitespace) {
            // interpret everything as a literal
            return " = " + data.toLowerCase();
        }

        return null; // change all ? to _ and all * to %
    }
}
