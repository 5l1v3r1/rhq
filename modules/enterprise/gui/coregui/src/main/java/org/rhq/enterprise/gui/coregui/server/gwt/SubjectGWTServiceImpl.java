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
package org.rhq.enterprise.gui.coregui.server.gwt;

import com.allen_sauer.gwt.log.client.Log;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class SubjectGWTServiceImpl extends AbstractGWTServiceImpl implements SubjectGWTService {

    private static final long serialVersionUID = 1L;

    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

    public void changePassword(String username, String password) {
        try {
            subjectManager.changePassword(getSessionSubject(), username, password);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void createPrincipal(String username, String password) {
        try {
            subjectManager.createPrincipal(getSessionSubject(), username, password);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Subject createSubject(Subject subjectToCreate) {
        try {
            return SerialUtility.prepare(subjectManager.createSubject(getSessionSubject(), subjectToCreate),
                "SubjectManager.createSubject");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /**Same as create subject, but uses Overlord and returns new/non-temporary session.
     * 
     * @param subjectToCreate
     * @param password
     * @return
     */
    public Subject createSubjectUsingOverlord(Subject subjectToCreate, String password) {
        try {
            //Officially create the new subject
            subjectToCreate = subjectManager.createSubject(subjectManager.getOverlord(), subjectToCreate);
            // nuke the temporary session and establish a new
            // one for this subject.. must be done before pulling the
            // new subject in order to do it with his own credentials
            subjectManager.logout(getSessionSubject().getSessionId());
            subjectToCreate = subjectManager.login(subjectToCreate.getName(), password);
            Log.trace("Created new user with overlord and logged back in with that user.");
            return SerialUtility.prepare(subjectToCreate, "SubjectManager.createSubjectUsingOverlord");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void deleteSubjects(int[] subjectIds) {
        try {
            subjectManager.deleteSubjects(getSessionSubject(), subjectIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Subject login(String username, String password) {
        try {
            return SerialUtility.prepare(subjectManager.login(username, password), "SubjectManager.login");
        } catch (LoginException e) {
            throw new RuntimeException("LoginException: " + e.getMessage());
        }
    }

    public void logout(Subject subject) {
        try {
            subjectManager.logout(subject.getSessionId());
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Subject updateSubject(Subject subjectToModify) {
        try {
            return SerialUtility.prepare(subjectManager.updateSubject(getSessionSubject(), subjectToModify),
                "SubjectManager.updateSubject");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<Subject> findSubjectsByCriteria(SubjectCriteria criteria) {
        try {
            return SerialUtility.prepare(subjectManager.findSubjectsByCriteria(getSessionSubject(), criteria),
                "SubjectManager.findSubjectsByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}
