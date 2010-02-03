/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This generic exception thrown by the plugin container is part of its client API; thus this exception is available to
 * both the plugin container as well as its remote clients.
 *
 * <p>Make sure that when constructing these exceptions, that any {@link #getCause() cause} is also available to remote
 * clients as well! If you are unsure, wrap the cause in a {@link org.rhq.core.util.exception.WrappedRemotingException}
 * before passing it to this class' constructor.</p>
 *
 * @author John Mazzitelli
 */
public class PluginContainerException extends Exception {
    private static final long serialVersionUID = 1L;

    private final List<String> messages;

    public PluginContainerException(List<String> messages) {
        super();
        List<String> msgs = new ArrayList<String>();
        Collections.copy(msgs, messages);
        this.messages = msgs;
    }

    /**
     * Because this exception is part of the plugin container's client API and thus is to be available on remote clients
     * as well, make sure the <code>cause</code> throwable you pass to this constructor is also available to remote
     * clients. If it is not or you are unsure, wrap the cause in a
     * {@link org.rhq.core.util.exception.WrappedRemotingException} before passing it in to this constructor.
     *
     * @see Throwable#Throwable(Throwable)
     */
    public PluginContainerException(Throwable cause) {
        super(cause);
        messages = Collections.emptyList();
    }

    /**
     * Because this exception is part of the plugin container's client API and thus is to be available on remote clients
     * as well, make sure the <code>cause</code> throwable you pass to this constructor is also available to remote
     * clients. If it is not or you are unsure, wrap the cause in a
     * {@link org.rhq.core.util.exception.WrappedRemotingException} before passing it in to this constructor.
     *
     * @see Throwable#Throwable(String, Throwable)
     */
    public PluginContainerException(String message, Throwable cause) {
        super(message, cause);
        messages = Collections.emptyList();
    }

    /**
     * @see Throwable#Throwable()
     */
    public PluginContainerException() {
        messages = Collections.emptyList();
    }

    /**
     * @see Throwable#Throwable(String)
     */
    public PluginContainerException(String message) {
        super(message);
        messages = Collections.emptyList();
    }

    public Iterator<String> messageIterator() {
        return messages.iterator();
    }
}