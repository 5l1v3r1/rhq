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
package org.rhq.enterprise.communications.command.impl.identify;

import java.io.Serializable;

/**
 * Identification information that is common for either a JON Agent or JON Server.
 *
 * @author John Mazzitelli
 */
public abstract class Identification implements Serializable {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * The type string that identifies the remote endpoint as a JON Agent.
     */
    public static final String TYPE_AGENT = "agent";

    /**
     * The type string that identifies the remote endpoint as a JON Server.
     */
    public static final String TYPE_SERVER = "server";

    private final String m_type;
    private final String m_locator;

    /**
     * Constructor for {@link Identification}.
     *
     * @param  type    indicates if this is an {@link #TYPE_AGENT JON Agent} or {@link #TYPE_SERVER JON Server}
     * @param  locator the locator endpoint of remoting server
     *
     * @throws IllegalArgumentException if either parameter is <code>null</code>
     */
    public Identification(String type, String locator) throws IllegalArgumentException {
        if (type == null) {
            throw new IllegalArgumentException("type=null");
        }

        if (locator == null) {
            throw new IllegalArgumentException("locator=null");
        }

        m_type = type;
        m_locator = locator;
    }

    /**
     * Returns the locator endpoint of the remoting server that is being identified.
     *
     * @return locator URL as a String
     */
    public String getInvokerLocator() {
        return m_locator;
    }

    /**
     * Returns the type of the remoting server that is being identified. This is one of {@link #TYPE_AGENT} or
     * {@link #TYPE_SERVER}.
     *
     * @return type
     */
    public String getType() {
        return m_type;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof Identification)) {
            return false;
        }

        Identification compareThis = (Identification) obj;

        return this.m_type.equals(compareThis.m_type) && this.m_locator.equals(compareThis.m_locator);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int result = 17;

        result = (37 * result) + this.getType().hashCode();
        result = (37 * result) + this.getInvokerLocator().hashCode();

        return result;
    }
}