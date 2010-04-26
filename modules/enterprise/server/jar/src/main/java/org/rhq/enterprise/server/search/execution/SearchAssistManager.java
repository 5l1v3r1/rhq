package org.rhq.enterprise.server.search.execution;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.core.domain.search.SearchSuggestion.Kind;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.search.assist.AbstractSearchAssistant;
import org.rhq.enterprise.server.search.assist.ResourceSearchAssistant;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;
import org.rhq.enterprise.server.util.LookupUtil;

public class SearchAssistManager {

    private static final Log LOG = LogFactory.getLog(SearchAssistManager.class);
    private SavedSearchManagerLocal savedSearchManager = LookupUtil.getSavedSearchManager();

    private static List<String> comparisonOperators = Arrays.asList("!==", "!=", "==", "=");
    private static List<String> booleanOperators = Arrays.asList("and", "or", "|");

    private Subject subject;
    private AbstractSearchAssistant completor;

    /*
     * states:
     * 
     *    empty expression                                --> suggest contexts with empty filter
     *    incomplete context                              --> suggest contexts with passed filter, suffixed with open bracket for parameterization
     *    complete context                                --> suggest comparison operators with empty filter
     *    begin parameterizations                         --> suggest params with empty filter
     *    incomplete parameterization                     --> suggest params with passed filter
     *    complete parameterization                       --> suggest comparison operators with empty filter
     *    incomplete operator                             --> suggest comparison operators with passed filter
     *    complete operator                               --> suggest values with empty filter
     *    incomplete value                                --> suggest values with passed filter
     *    otherwise assume complete previous expression   --> suggest boolean operators at the top
     *                                                    --> suggest contexts with empty filter below that
     */
    static class SearchTermAssistant {

        private static final String CARET = "@@@";

        String expression;

        List<String> terms;
        int currentTermIndex;
        int indexWithinTerm;

        public SearchTermAssistant(String expression, int caretPos) {
            // insert CARET token into expression at caretPos
            String before = expression.substring(0, caretPos);
            String after = expression.substring(caretPos);
            this.expression = before + CARET + after;
            tokenize();
            for (int i = 0; i < terms.size(); i++) {
                String term = terms.get(i);
                int index = term.indexOf("@@@");
                if (index != -1) {
                    String replaced = term.replace("@@@", "");
                    terms.set(i, replaced);
                    currentTermIndex = i;
                    indexWithinTerm = index;
                    break;
                }
            }
        }

        private void tokenize() {
            List<String> fragments = tokenizeIntoFragments(expression);
            this.terms = joinIntoTerms(fragments);
        }

        private List<String> tokenizeIntoFragments(String expression) {
            List<String> fragments = new ArrayList<String>();

            char quoteChar = 0;
            StringBuilder term = new StringBuilder();
            for (char nextChar : expression.toCharArray()) {
                if (quoteChar != 0) { // accept space inside quoted strings
                    term.append(nextChar);
                    if (nextChar == quoteChar) { // look for end quote
                        quoteChar = 0; // i just left a quoted string
                    }
                } else { // ignore spaces outside quoted string
                    if (Character.isWhitespace(nextChar)) { // spaces delimit terms outside quoted strings
                        if (term.length() > 0) {
                            fragments.add(term.toString());
                            term = new StringBuilder();
                        }
                    } else if (nextChar == '(' || nextChar == ')') {
                        // completed ignore parentheses outside quoted strings
                    } else {
                        term.append(nextChar);
                        if (nextChar == '\'' || nextChar == '"') {
                            quoteChar = nextChar; // entering a quoted string
                        }
                    }
                }
            }
            if (term.length() > 0) {
                fragments.add(term.toString());
            }

            return fragments;
        }

        private List<String> joinIntoTerms(List<String> fragments) {
            if (fragments.size() < 3) {
                return fragments;
            }
            List<String> terms = new ArrayList<String>();
            int i = 1;
            while (i < fragments.size() - 1) {
                String before = fragments.get(i - 1);
                String term = fragments.get(i);
                if (comparisonOperators.contains(term)) {
                    String after = fragments.get(i + 1);
                    terms.add(before + term + after);
                    i += 3; // a triple of terms were processed
                } else {
                    terms.add(before);
                    i++; // only one term processed
                }
            }

            // if the last three terms weren't a valid triple, there will be two left over
            if (i < fragments.size()) {
                String nextToLast = fragments.get(fragments.size() - 2);
                String last = fragments.get(fragments.size() - 1);
                if (comparisonOperators.contains(last)) { // last couple was an incomplete term
                    terms.add(nextToLast + last);
                } else {
                    terms.add(nextToLast); // there are unrelated terms, possibly simple text matches
                    terms.add(last);
                }
            } else if (i == fragments.size()) { // found a triple just before last fragment
                String last = fragments.get(fragments.size() - 1);
                terms.add(last);
            }
            return terms;
        }

        public List<String> getTerms() {
            return terms;
        }

        public String getPreviousToken() {
            return terms.get(currentTermIndex - 1);
        }

        public String getCurrentToken() {
            return terms.get(currentTermIndex);
        }

        public String getFragmentBeforeCaret() {
            return getCurrentToken().substring(0, indexWithinTerm);
        }

        public void report(PrintStream output) {
            output.println("Expression: " + expression);
            List<String> fragments = tokenizeIntoFragments(expression);
            List<String> terms = joinIntoTerms(fragments);
            int counter = 0;
            for (String result : terms) {
                output.println("Token[" + (++counter) + "]: " + result);
            }
            output.println();
        }

        public String toString() {
            return null;
        }
    }

    static class ParsedContext {
        public enum State {
            CONTEXT, PARAM, OPERATOR, VALUE;
        }

        public final String context;
        public final String param;
        public final String operator;
        public final String value;
        public final State state;

        private ParsedContext(String context, String param, String operator, String value) {
            this.context = context;
            this.param = param;
            this.operator = operator;
            this.value = value;
            this.state = computeState(context, param, operator, value);
        }

        private State computeState(String context, String param, String operator, String value) {
            if (value != null) {
                return State.VALUE;
            }
            if (operator != null) {
                return State.OPERATOR;
            }
            if (param != null) {
                return State.PARAM;
            }
            return State.CONTEXT;
        }

        public static ParsedContext get(String term) {
            int index = 0;
            char[] expr = term.toCharArray();
            StringBuilder buffer = new StringBuilder();

            while (index < expr.length && expr[index] != '[' && expr[index] != '!' && expr[index] != '=') { // read up until beginning of param or operator
                buffer.append(expr[index]);
                index++;
            }
            String context = buffer.toString(); // either beginning of param, beginning of operator, or end of expression
            buffer = new StringBuilder();
            if (index == expr.length || (expr[index] != '[' && expr[index] != '!' && expr[index] != '=')) { // if not beginning of param or operator, return 
                return new ParsedContext(context, null, null, null);
            }

            String param = null;
            if (expr[index] == '[') { // parameterized context
                index++; // skip over '['
                while (index < expr.length && expr[index] != ']') { // read up until end of param
                    buffer.append(expr[index]);
                    index++;
                }
                param = buffer.toString();
                buffer = new StringBuilder();
                if (index == expr.length || expr[index] != ']') { // if not end of param, incomplete param so return 
                    return new ParsedContext(context, param, null, null);
                }
                index++; // skip over ']'
            }

            while (index < expr.length && (expr[index] == '!' || expr[index] == '=')) { // read up until end of operator chars
                buffer.append(expr[index]);
                index++;
            }
            String operator = buffer.toString();
            if (index == expr.length) { // return if end of expression 
                return new ParsedContext(context, param, operator, null);
            }

            String value = term.substring(index); // any remain characters are the value
            if (value.length() > 0) {
                if (value.charAt(0) == '\'' || value.charAt(0) == '"') {
                    value = value.substring(1);
                }
            }
            return new ParsedContext(context, param, operator, value);
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(getClass().getSimpleName()).append("[");
            buffer.append("state=").append(state);
            buffer.append(", context(").append(context);
            buffer.append("), param(").append(param);
            buffer.append("), operator(").append(operator);
            buffer.append("), value(").append(value).append(")]");
            return buffer.toString();
        }
    }

    public SearchAssistManager(Subject subject, SearchSubsystem searchSubsystem) {
        this.subject = subject;
        this.completor = getAutoCompletor(searchSubsystem);
    }

    protected AbstractSearchAssistant getAutoCompletor(SearchSubsystem searchSubsystem) {
        if (searchSubsystem == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant();
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchSubsystem + "]");
        }
    }

    public List<String> getAllContexts() {
        List<String> results = new ArrayList<String>(completor.getSimpleContexts());
        for (String parameterized : completor.getParameterizedContexts()) {
            results.add(parameterized + "[");
        }
        return results;
    }

    public List<SearchSuggestion> getSuggestions(String expression, int caretPos) {
        //List<SearchSuggestion> simple = getSimpleSuggestions(expression, caretPos);
        List<SearchSuggestion> advanced = getAdvancedSuggestions(expression, caretPos, SearchSuggestion.Kind.Advanced);
        List<SearchSuggestion> userSavedSearches = getUserSavedSearchSuggestions(expression);
        List<SearchSuggestion> globalSavedSearches = getGlobalSavedSearchSuggestions(expression);

        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();
        //results.addAll(simple);
        results.addAll(advanced);
        results.addAll(userSavedSearches);
        results.addAll(globalSavedSearches);
        Collections.sort(results);
        return results;
    }

    public List<SearchSuggestion> getSimpleSuggestions(String expression, int caretPos) {
        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();
        for (String nextContext : completor.getSimpleContexts()) {
            String intermediateExpression = nextContext + "=" + expression;
            List<SearchSuggestion> suggestions = getAdvancedSuggestions(intermediateExpression, caretPos,
                SearchSuggestion.Kind.Simple);
            results.addAll(suggestions);
        }
        Collections.sort(results);
        if (results.size() > completor.getMaxResultCount()) {
            return results.subList(0, completor.getMaxResultCount());
        } else {
            return results;
        }
    }

    public List<SearchSuggestion> getAdvancedSuggestions(String expression, int caretPos,
        SearchSuggestion.Kind targetKind) {
        debug("getAdvancedSuggestions: START");
        long id = HibernatePerformanceMonitor.get().start();
        SearchTermAssistant assistant = new SearchTermAssistant(expression, caretPos);
        HibernatePerformanceMonitor.get().stop(id, "SearchAssistant");
        String[] tokens = assistant.getTerms().toArray(new String[0]);
        debug("" + tokens.length + " tokens are " + Arrays.asList(tokens));

        if (tokens.length == 0) {
            debug("getAdvancedSuggestions: no terms");
            return convert(getAllContexts(), targetKind); // no terms yet defined
        }

        String beforeCaret = assistant.getFragmentBeforeCaret();
        debug("getAdvancedSuggestions: beforeCaret is '" + beforeCaret + "'");

        ParsedContext parsed = ParsedContext.get(beforeCaret);
        debug("getAdvancedSuggestions: parsed is " + parsed);
        switch (parsed.state) {
        case CONTEXT:
            if (parsed.context.equals("")) {
                if (tokens.length == 1) {
                    debug("getAdvancedSuggestions: no terms yet, suggesting contexts");
                    return convert(getAllContexts(), targetKind);
                } else if (isBooleanTerm(assistant.getPreviousToken())) {
                    debug("getAdvancedSuggestions: previous term was boolean, suggesting contexts");
                    return convert(getAllContexts(), targetKind);
                } else {
                    debug("getAdvancedSuggestions: previous term was not boolean, suggesting boolean");
                    return convert(booleanOperators, targetKind);
                }
            } else if (isBooleanTerm(parsed.context)) {
                debug("getAdvancedSuggestions: beforeCaret is whole boolean operator");
                return convert(getAllContexts(), targetKind); // TODO: should we tell user to type a space first?
            } else {
                // check if this context is complete or not
                if (completor.getSimpleContexts().contains(parsed.context)) {
                    debug("getAdvancedSuggestions: search term is simple context, wants operator");
                    return convert(pad(parsed.context, comparisonOperators, ""), targetKind);
                }
                if (completor.getParameterizedContexts().contains(parsed.context)) {
                    debug("getAdvancedSuggestions: search term is parameterized context, wants open bracket");
                    return convert(Arrays.asList(parsed.context + "["), targetKind);
                }

                debug("getAdvancedSuggestions: search term wants context completion");
                List<String> startsWithContexts = new ArrayList<String>();
                for (String context : completor.getSimpleContexts()) {
                    if (context.indexOf(parsed.context) != -1) {
                        startsWithContexts.add(context);
                    }
                }
                for (String context : completor.getParameterizedContexts()) {
                    if (context.indexOf(parsed.context) != -1) {
                        startsWithContexts.add(context + "[");
                    }
                }
                return convert(startsWithContexts, targetKind);
            }
        case PARAM:
            debug("getAdvancedSuggestions: param state");
            return convert(pad(parsed.context + "[", completor.getParameters(parsed.context, parsed.param), "]"),
                targetKind);
        case OPERATOR:
            debug("getAdvancedSuggestions: operator state");
            if (comparisonOperators.contains(parsed.operator)) {
                debug("search term is complete operator, suggesting values instead");
                List<String> valueSuggestions = pad("\"", completor.getValues(parsed.context, parsed.param, ""), "\"");
                if (completor.getSimpleContexts().contains(parsed.context)) {
                    debug("getAdvancedSuggestions: suggesting value completions for a simple context");
                    return convert(pad(parsed.context + parsed.operator, valueSuggestions, ""), targetKind);
                } else {
                    debug("getAdvancedSuggestions: suggesting value completions for a parameterized context");
                    return convert(pad(parsed.context + "[" + parsed.param + "]" + parsed.operator, valueSuggestions,
                        ""), targetKind);
                }
            }

            List<String> operatorSuggestions = new ArrayList<String>();
            for (String op : comparisonOperators) {
                if (op.startsWith(parsed.operator)) {
                    operatorSuggestions.add(op);
                }
            }

            debug("getAdvancedSuggestions: providing suggestions for comparison operators");
            if (completor.getSimpleContexts().contains(parsed.context)) {
                return convert(pad(parsed.context, operatorSuggestions, ""), targetKind);
            } else {
                return convert(pad(parsed.context + "[" + parsed.param + "]", operatorSuggestions, ""), targetKind);
            }
        case VALUE:
            debug("getAdvancedSuggestions: value state");
            List<String> valueSuggestions = pad("\"", completor.getValues(parsed.context, parsed.param, parsed.value),
                "\"");
            if (completor.getSimpleContexts().contains(parsed.context)) {
                debug("getAdvancedSuggestions: suggesting value completions for a simple context");
                return convert(pad(parsed.context + parsed.operator, valueSuggestions, ""), targetKind);
            } else {
                debug("getAdvancedSuggestions: suggesting value completions for a parameterized context");
                return convert(pad(parsed.context + "[" + parsed.param + "]" + parsed.operator, valueSuggestions, ""),
                    targetKind);
            }
        default:
            return Collections.emptyList();
        }
    }

    public List<SearchSuggestion> getUserSavedSearchSuggestions(String expression) {
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(subject.getId());
        if (expression != null && expression.trim().equals("")) {
            criteria.addFilterName(expression);
        }

        criteria.setCaseSensitive(false);
        criteria.addSortName(PageOrdering.ASC);

        List<SavedSearch> savedSearchResults = savedSearchManager.findSavedSearchesByCriteria(subject, criteria);

        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();
        for (SavedSearch next : savedSearchResults) {
            String label = next.getName();
            String value = next.getPattern();
            int index = next.getName().toLowerCase().indexOf(expression);
            SearchSuggestion suggestion = new SearchSuggestion(Kind.UserSavedSearch, label, value, index, expression
                .length());
            results.add(suggestion);
        }
        return results;
    }

    public List<SearchSuggestion> getGlobalSavedSearchSuggestions(String expression) {
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterGlobal(true);
        if (expression != null && expression.trim().equals("")) {
            criteria.addFilterName(expression);
        }

        criteria.setCaseSensitive(false);
        criteria.addSortName(PageOrdering.ASC);

        List<SavedSearch> savedSearchResults = savedSearchManager.findSavedSearchesByCriteria(subject, criteria);

        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();
        for (SavedSearch next : savedSearchResults) {
            String label = next.getName();
            String value = next.getPattern();
            int index = next.getName().toLowerCase().indexOf(expression);
            SearchSuggestion suggestion = new SearchSuggestion(Kind.GlobalSavedSearch, label, value, index, expression
                .length());
            results.add(suggestion);
        }
        return results;
    }

    private boolean isBooleanTerm(String term) {
        for (String op : booleanOperators) {
            if (op.equals(term)) {
                return true;
            }
        }
        return false;
    }

    private List<SearchSuggestion> convert(List<String> suggestions, SearchSuggestion.Kind targetKind) {
        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>(suggestions.size());
        for (String suggestion : suggestions) {
            results.add(new SearchSuggestion(targetKind, suggestion));
        }
        return results;
    }

    private List<String> pad(String leftPad, List<String> data, String rightPad) {
        List<String> results = new ArrayList<String>();
        for (String next : data) {
            results.add(leftPad + next + rightPad);
        }
        return results;
    }

    private void debug(String message) {
        LOG.info(message);
    }
}
