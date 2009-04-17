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
package org.rhq.enterprise.gui.common.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.jetbrains.annotations.Nullable;

import org.jboss.seam.core.Manager;

/**
 * A phase listener that propogates global JSF messages across redirects.
 *
 * @author Joseph Marques
 * @author Ian Springer
 */
public class FacesMessagePropogationPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 5393413660742308456L;

    private static final String SAVED_GLOBAL_FACES_MESSAGES = "SAVED_GLOBAL_FACES_MESSAGES";

    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public void beforePhase(PhaseEvent event) {
        PhaseId phaseId = event.getPhaseId();
        if (phaseId == PhaseId.RESTORE_VIEW) {
            // We want to add the saved messages back to the context immediately after the view is restored.
            // Remove them from the session once we've added them back, otherwise the messages will just keep
            // building up, since they are stored in session scope.
            List<FacesMessage> savedMessages = removeGlobalFacesMessagesFromSession();
            putGlobalFacesMessagesInFacesContext(savedMessages);
        }
    }

    public void afterPhase(PhaseEvent event) {
        PhaseId phaseId = event.getPhaseId();
        if (phaseId == PhaseId.INVOKE_APPLICATION) {
            // We want to store the messages in the context after the application has done its processing.
            if (!Manager.instance().isReallyLongRunningConversation()) {
                putGlobalFacesMessagesInSession();
            }
        } else if (phaseId == PhaseId.RENDER_RESPONSE) {
            // If we've just rendered a response, this isn't a redirect, so we don't want to propogate messages.
            // (fix for http://jira.jboss.com/jira/browse/JBNADM-1548, ips, 08/15/07)
            removeGlobalFacesMessagesFromSession();
        }
    }

    private void putGlobalFacesMessagesInSession() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        Iterator<FacesMessage> messages = facesContext.getMessages();
        List<FacesMessage> savedMessages = new ArrayList<FacesMessage>();
        while (messages.hasNext()) {
            FacesMessage message = messages.next();
            savedMessages.add(message);
        }

        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> sessionMap = externalContext.getSessionMap();

        sessionMap.put(SAVED_GLOBAL_FACES_MESSAGES, savedMessages);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private List<FacesMessage> removeGlobalFacesMessagesFromSession() {
        Map<String, Object> sessionMap = getSessionMap();
        return (List<FacesMessage>) sessionMap.remove(SAVED_GLOBAL_FACES_MESSAGES);
    }

    private void putGlobalFacesMessagesInFacesContext(@Nullable List<FacesMessage> messages) {
        // no work to do, if there weren't any messages
        if (messages == null) {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        // Only add the messages if there aren't already any messages in the context. If there's already messages,
        // it's probably because Seam FacesMessages already took care of propogating them.
        if (!facesContext.getMessages().hasNext()) {
            for (FacesMessage message : messages) {
                facesContext.addMessage(null, message);
            }
        }
    }

    private Map<String, Object> getSessionMap() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        return externalContext.getSessionMap();
    }
}