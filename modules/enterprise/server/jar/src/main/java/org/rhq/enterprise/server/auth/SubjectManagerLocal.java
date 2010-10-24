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
package org.rhq.enterprise.server.auth;

import java.util.Collection;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.LoginException;

/**
 * The local EJB interface to the Authentication Boss.
 *
 * @author John Mazzitelli
 */
@Local
public interface SubjectManagerLocal {
    /**
     * Loads in the given subject's {@link Subject#getUserConfiguration() preferences} and
     * {@link Subject#getRoles() roles}.
     *
     * @param  subjectId identifies the subject whose preferences and roles are to be loaded
     *
     * @return the subject, with its preferences and roles loaded
     */
    Subject loadUserConfiguration(Integer subjectId);

    /**
     * Given a set of subject Ids, this returns a list of all the subjects.
     *
     * @param  subjectIds
     * @param  pageControl
     *
     * @return all the subjects with the given ID
     */
    PageList<Subject> findSubjectsById(Integer[] subjectIds, PageControl pageControl);

    /**
     * This returns the system super user subject that can be used to authorize the caller for any other system call.
     * This must <b>not</b> be exposed to remote clients.
     *
     * @return a subject that is authorized to do anything
     */
    Subject getOverlord();

    /**
     * Returns a paged list of all Subjects.
     *
     * @param pageControl the page control that specifies which page of the result set to return and how sort it
     */
    PageList<Subject> findAllSubjects(PageControl pageControl);

    /**
     * Logs in a user without performing any authentication. This method should be used with care and not available to
     * remote clients. Because of the unauthenticated nature of this login, the new login session will have a session
     * timeout of only a few seconds. However, if you pass in <code>true</code> for the "reattach", this method will
     * re-attach to an existing session for the user, if one is active already. If one does not exist, this method will
     * login and create a new session just as if that parameter was <code>false</code>.
     *
     * @param  user     The user to authenticate and login
     * @param  reattach If <code>true</code>, will re-attach to an existing login session, if one exists
     *
     * @return the user's {@link Subject}
     *
     * @throws LoginException if failed to create a new session for the given user
     */
    Subject loginUnauthenticated(String user, boolean reattach) throws LoginException;

    /**
     * Creates a new principal (username and password) in the internal database.
     *
     * @param  subject   The subject of the currently logged in user
     * @param  principal The principal to add
     *
     * @throws Exception if the principal could not be added
     */
    void createPrincipal(Subject subject, Principal principal) throws SubjectException;

    /**
     * Checks that the user exists <b>and</b> has a {@link Principal} associated with it. This means that the user both
     * exists and is authenticated via JDBC. An LDAP user will not have a {@link Principal} because it is authenticated
     * via the LDAP server, not from the database.
     *
     * @param  username the user whose existence is to be checked
     *
     * @return <code>true</code> if the user exists and has a {@link Principal}, <code>false</code> otherwise
     */
    boolean isUserWithPrincipal(String username);

    /**
     * Get a collection of all user names, where the collection contains the names of all users that have principals
     * only. You will not get a list of usernames for those users authenticated by LDAP.
     *
     * @return collection of all user names that have principals
     */
    Collection<String> findAllUsersWithPrincipals();

    /**
     * Deletes the given set of users, including both the {@link Subject} and {@link Principal} objects associated with
     * those users.
     *
     * @param  subject    the person requesting the deletion
     * @param  subjectIds identifies the subject IDs for all the users that are to be deleted
     *
     * @throws Exception if failed to delete one or more users
     */
    void deleteUsers(Subject subject, int[] subjectIds);

    /**
     * Determines if the given session ID is valid and it is associated with the given username and user ID.
     *
     * @param  session
     * @param  username
     * @param  userid
     *
     * @return <code>true</code> if the session ID indentifies a valid session; <code>false</code> if it is invalid or
     *         has timed out
     */
    boolean isValidSessionId(int session, String username, int userid);

    /**
     * This returns a list of subjects that are available to be assigned to a given role but not yet assigned to that
     * role. This excludes subjects already assigned to the role. The returned list will not include the subjects
     * identified by <code>pendingSubjectIds</code> since it is assumed the pending subjects will be assigned to the
     * role.
     *
     * @param  whoami            user attempting to make this call
     * @param  roleId            the role whose list of available subjects are to be returned
     * @param  pendingSubjectIds the list of subjects that are planned to be given to the role
     * @param  pc
     *
     * @return the list of subjects that can be assigned to the given role, not including the pending subjects
     */
    PageList<Subject> findAvailableSubjectsForRole(Subject whoami, Integer roleId, Integer[] pendingSubjectIds,
        PageControl pc);

    void logout(int sessionId);

    Subject getSubjectById(int id);

    Subject getSubjectBySessionId(int sessionId) throws Exception;

    boolean isLoggedIn(String username);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * #see {@link SubjectManagerRemote#changePassword(Subject, String, String)
     */
    void changePassword(Subject subject, String username, String password);

    /**
     * #see {@link SubjectManagerRemote#createPrincipal(Subject, String, String)
     */
    void createPrincipal(Subject subject, String username, String password) throws SubjectException;

    /**
     * #see {@link SubjectManagerRemote#createSubject(Subject, Subject)
     */
    Subject createSubject(Subject subject, Subject subjectToCreate) throws SubjectException;

    /**
     * #see {@link SubjectManagerRemote#deleteSubjects(Subject, int[])
     */
    void deleteSubjects(Subject subject, int[] subjectIds);

    /**
     * @see {@link SubjectManagerRemote#getSubjectByName(String)}
     */
    Subject getSubjectByName(String username);

    /**
     * @see {@link SubjectManagerRemote#getSubjectByNameAndSessionId(String, int)}
     */
    Subject getSubjectByNameAndSessionId(String username, int sessionId) throws Exception;

    /**
     * @see SubjectManagerRemote#login(String, String)
     */
    Subject login(String username, String password) throws LoginException;

    /**
     * @see SubjectManagerRemote#logout(Subject)
     */
    void logout(Subject subject);

    /**
     * @see SubjectManagerRemote#updateSubject(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.auth.Subject)
     */
    Subject updateSubject(Subject subject, Subject subjectToModify);

    /**
     * @see SubjectManagerRemote#findSubjectsByCriteria(Subject, SubjectCriteria)
     */
    PageList<Subject> findSubjectsByCriteria(Subject subject, SubjectCriteria criteria);

    Subject processSubjectForLdap(Subject subject, String subjectPassword) throws LoginException;
}