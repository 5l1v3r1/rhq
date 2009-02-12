package org.rhq.enterprise.server.auth.prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class SubjectPreferencesCache {

    protected final Log log = LogFactory.getLog(SubjectPreferencesCache.class);

    private Map<Integer, Configuration> subjectPreferences;

    private static final SubjectPreferencesCache instance = new SubjectPreferencesCache();

    private SubjectManagerLocal subjectManager;
    private EntityManagerFacadeLocal entityManagerFacade;

    private SubjectPreferencesCache() {
        subjectPreferences = new HashMap<Integer, Configuration>();
        subjectManager = LookupUtil.getSubjectManager();
        entityManagerFacade = LookupUtil.getEntityManagerFacade();
    }

    public static SubjectPreferencesCache getInstance() {
        return instance;
    }

    public synchronized Configuration getUserConfiguration(int subjectId) {
        if (!subjectPreferences.containsKey(subjectId)) {
            Subject subject = subjectManager.loadUserConfiguration(subjectId);
            Configuration configuration = subject.getUserConfiguration();
            subjectPreferences.put(subjectId, configuration);
        }
        return subjectPreferences.get(subjectId).deepCopy(true);
    }

    public synchronized void setUserConfiguration(int subjectId, Configuration configuration, Set<String> changed) {
        for (PropertySimple simpleProperty : configuration.getSimpleProperties().values()) {
            if (changed.contains(simpleProperty.getName())) {
                log.debug("Changed: " + simpleProperty);
                entityManagerFacade.merge(simpleProperty); // only merge changes
            }
        }
        subjectPreferences.put(subjectId, configuration);
    }
}
