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
package org.rhq.core.domain.event.alert;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A log record for a triggered JON alert condition.
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = "AlertConditinLog.findAll", query = "SELECT acl " + "FROM AlertConditionLog AS acl"),
    @NamedQuery(name = AlertConditionLog.QUERY_FIND_UNMATCHED_LOG_BY_ALERT_CONDITION_ID, query = "SELECT acl "
        + "FROM AlertConditionLog AS acl " + "WHERE acl.condition.id = :alertConditionId " + "AND acl.alert IS NULL"),
    @NamedQuery(name = AlertConditionLog.QUERY_FIND_UNMATCHED_LOGS_BY_ALERT_DEFINITION_ID, query = "SELECT acl "
        + "FROM AlertConditionLog AS acl " + "WHERE acl.condition.alertDefinition.id = :alertDefinitionId "
        + "AND acl.alert IS NULL"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_RESOURCE, query = "DELETE AlertConditionLog acl "
        + "WHERE acl.id IN " + "( SELECT iacl.id " + "FROM AlertConditionLog iacl "
        + "WHERE iacl.condition.alertDefinition.resource.id = :resourceId " + ")"),
    @NamedQuery(name = AlertConditionLog.QUERY_DELETE_BY_ALERT_CTIME, query = "DELETE AlertConditionLog acl "
        + "WHERE acl.id IN " + "( SELECT iacl.id " + "FROM AlertConditionLog iacl "
        + "WHERE iacl.alert.ctime BETWEEN :begin AND :end " + ")") })
@SequenceGenerator(name = "RHQ_ALERT_CONDITION_LOG_ID_SEQ", sequenceName = "RHQ_ALERT_CONDITION_LOG_ID_SEQ")
@Table(name = "RHQ_ALERT_CONDITION_LOG")
public class AlertConditionLog implements Serializable {
    public static final String QUERY_FIND_UNMATCHED_LOG_BY_ALERT_CONDITION_ID = "AlertConditinLog.findUnmatchedLogByAlertConditionId";
    public static final String QUERY_FIND_UNMATCHED_LOGS_BY_ALERT_DEFINITION_ID = "AlertConditinLog.findUnmatchedLogsByAlertDefinitionId";
    public static final String QUERY_DELETE_BY_RESOURCE = "AlertConditionLog.deleteByResource";
    public static final String QUERY_DELETE_BY_ALERT_CTIME = "AlertConditionLog.deleteByAlertCTime";

    public static final int MAX_LOG_LENGTH = 250;

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_ALERT_CONDITION_LOG_ID_SEQ")
    @Id
    @SuppressWarnings( { "unused" })
    private int id;

    /**
     * Since alert conditions can occur at potentially grossly different times in the system, and since the process for
     * calculating whether an alert should fire based on the states of these independently derived conditions is
     * out-of-band, THIS ctime is now actually more meaningful than the ctime on the alert.
     */
    @Column(name = "CTIME", nullable = false)
    private long ctime;

    /*
     * TODO: jmarques - should this be non-null?
     */
    @Column(name = "VALUE")
    private String value;

    @JoinColumn(name = "ALERT_ID", referencedColumnName = "ID")
    @ManyToOne
    private Alert alert;

    @JoinColumn(name = "CONDITION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private AlertCondition condition;

    /**
     * Creates a new alert condition log record. (required by EJB3 spec, but not used)
     */
    protected AlertConditionLog() {
    }

    /**
     * Creates a new log record for the specified alert condition. The alert that triggered the condition will be filled
     * in later by a separate out-of-band process that ensures all requisite conditions have been satisfied on the
     * corresponding alert.
     *
     * @param cond  condition that is being logged
     * @param ctime the time in millis when this condition was known to be true
     */
    public AlertConditionLog(AlertCondition cond, long ctime) {
        this.condition = cond;
        this.ctime = ctime;
    }

    public int getId() {
        return this.id;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        if ((value != null) && (value.length() >= MAX_LOG_LENGTH)) {
            value = value.substring(0, MAX_LOG_LENGTH);
        }

        this.value = value;
    }

    public Alert getAlert() {
        return this.alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public AlertCondition getCondition() {
        return this.condition;
    }

    public void setCondition(AlertCondition condition) {
        this.condition = condition;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof AlertConditionLog)) {
            return false;
        }

        AlertConditionLog that = (AlertConditionLog) obj;
        if (id != that.id) {
            return false;
        }

        if ((value != null) ? (!value.equals(that.value)) : (that.value != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = (31 * result) + ((value != null) ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.event.alert.AlertConditionLog" + "[ " + "id=" + id + ", " + "value=" + value + ", "
            + condition + " ]";
    }
}