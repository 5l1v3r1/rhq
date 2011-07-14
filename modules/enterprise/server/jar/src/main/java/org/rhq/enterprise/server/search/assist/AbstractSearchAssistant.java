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
package org.rhq.enterprise.server.search.assist;

import static org.rhq.enterprise.server.search.common.SearchQueryGenerationUtility.getJPQLForString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.search.SearchExpressionException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public abstract class AbstractSearchAssistant implements SearchAssistant {

    private final Log log = LogFactory.getLog(SearchAssistant.class);

    private int subjectId;
    private boolean requiresAuthorizationFragment;
    private int maxResultCount = 20;

    public AbstractSearchAssistant(Subject subject) {
        this.subjectId = subject.getId();
        this.requiresAuthorizationFragment = !LookupUtil.getAuthorizationManager().isInventoryManager(subject);
    }

    public int getSubjectId() {
        return subjectId;
    }

    public boolean requiresAuthorizationFragment() {
        return requiresAuthorizationFragment;
    }

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

    public boolean isNumericalContext(String context) {
        return false; // all contexts are assumed string, unless otherwise stated
    }

    public boolean isEnumContext(String context) {
        return false; // all contexts are assumed string, unless otherwise stated
    }

    public List<String> getParameters(String context, String filter) {
        if (getParameterizedContexts().contains(context) == false) {
            throw new SearchExpressionException("context[" + context
                + "] is not parameterized, no completions available");
        }
        return Collections.emptyList();
    }

    public List<String> getValues(String context, String param, String filter) {
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

    protected final String conditionallyAddJPQLString(String fragment, String filter) {
        if (filter == null || filter.equals("")) {
            return "";
        }

        return " AND " + getJPQLForString(fragment, filter);
    }

    protected final String conditionallyAddAuthzFragment(String fragment) {
        if (requiresAuthorizationFragment == false) {
            return "";
        }

        return " AND " + fragment;
    }

    protected final List<String> filter(Class<? extends Enum<?>> enumType, String filter) {
        return filter(enumType, filter, false);
    }

    protected final List<String> filter(Class<? extends Enum<?>> enumType, String filter, boolean includeAnyOption) {
        List<String> results = new ArrayList<String>();
        if (includeAnyOption && "any".contains(filter)) {
            results.add("any");
        }
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

    protected final String stripQuotes(String data) {
        if (data.length() == 0) {
            return "";
        }

        char first = data.charAt(0);
        char last = data.charAt(data.length() - 1);
        if (first == '\'' || first == '"') {
            if (data.length() == 1) {
                return "";
            }
            data = data.substring(1);
        }
        if (last == '\'' || last == '"') {
            if (data.length() == 1) {
                return "";
            }
            data = data.substring(0, data.length() - 1);
        }
        return data;
    }
}
