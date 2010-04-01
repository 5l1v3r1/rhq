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
package org.rhq.enterprise.gui.coregui.client.util.preferences;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTServiceAsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UserPreferences {
    private static final String PREF_LIST_DELIM = "|";
    private static final String PREF_LIST_DELIM_REGEX = "\\|";

    private Subject subject;
    private Configuration userConfiguration;
    private SubjectGWTServiceAsync subjectService = GWTServiceLookup.getSubjectService();

    private ArrayList<UserPreferenceChangeListener> changeListeners = new ArrayList<UserPreferenceChangeListener>();


    public UserPreferences(Subject subject) {
        this.subject = subject;        
        this.userConfiguration = subject.getUserConfiguration();
    }

    public Set<Integer> getFavoriteResources() {
        return getPreferenceAsIntegerSet(UserPreferenceNames.RESOURCE_HEALTH_RESOURCES);
    }

    public void setFavoriteResources(Set<Integer> resourceIds, AsyncCallback<Subject> callback) {
        setPreference(UserPreferenceNames.RESOURCE_HEALTH_RESOURCES, resourceIds, callback);
    }

    protected String getPreference(String name) {
        return userConfiguration.getSimpleValue(name, null);
    }

    protected void setPreference(String name, Collection value, AsyncCallback<Subject> callback) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (Object item : value) {
            if (first) {
                first = false;
            } else {
                buffer.append(PREF_LIST_DELIM);
            }
            buffer.append(item);
        }
        setPreference(name, buffer.toString(), callback);
    }

    protected void setPreference(String name, String value, AsyncCallback<Subject> callback) {
        PropertySimple prop = this.userConfiguration.getSimple(name);
        String oldValue = null;
        if (prop == null) {
            this.userConfiguration.put(new PropertySimple(name, value));
        } else {
            oldValue = prop.getStringValue();
            prop.setStringValue(value);
        }
        this.subjectService.updateSubject(this.subject, callback);


        UserPreferenceChangeEvent event = new UserPreferenceChangeEvent(name, value, oldValue);
        for (UserPreferenceChangeListener listener : changeListeners) {
            listener.onPreferrenceChange(event);
        }
    }




    protected List<String> getPreferenceAsList(String key) {
        String pref = null;
        try {
            pref = getPreference(key);
        } catch (IllegalArgumentException e) {
//            log.debug("A user preference named '" + key + "' does not exist.");
        }

        return (pref != null) ? Arrays.asList(pref.split(PREF_LIST_DELIM_REGEX)) : new ArrayList<String>();
    }

    protected Set<Integer> getPreferenceAsIntegerSet(String key) {
        try {
            List<String> value = getPreferenceAsList(key);
            // Use a TreeSet, so the Resource id's are sorted.
            Set<Integer> result = new TreeSet<Integer>();
            for (String aValue : value) {
                String trimmed = aValue.trim();
                if (trimmed.length() > 0) {
                    Integer intValue = Integer.valueOf(trimmed);
                    result.add(intValue);
                }
            }
            return result;
        } catch (Exception e) {
            return new HashSet<Integer>();
        }
    }


    public void addChangeListener(UserPreferenceChangeListener listener) {
        changeListeners.add(listener);
    }
}
