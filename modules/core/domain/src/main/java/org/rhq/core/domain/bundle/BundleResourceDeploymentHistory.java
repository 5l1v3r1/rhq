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
package org.rhq.core.domain.bundle;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * This is a many-to-one entity that provides audit capability for a bundle deployment (a bundle-platform pairing).
 * 
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries( { @NamedQuery(name = BundleResourceDeploymentHistory.QUERY_FIND_ALL, query = "SELECT brdh FROM BundleResourceDeploymentHistory brdh") //
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_RES_DEP_HIST_ID_SEQ")
@Table(name = "RHQ_BUNDLE_RES_DEP_HIST")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleResourceDeploymentHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "BundleResourceDeploymentHistory.findAll";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "BUNDLE_RES_DEPLOY_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private BundleResourceDeployment resourceDeployment;

    @Column(name = "SUBJECT_NAME", nullable = true)
    protected String subjectName;

    @Column(name = "AUDIT_TIME", nullable = false)
    private Long auditTime = System.currentTimeMillis();

    @Column(name = "AUDIT_ACTION", nullable = false)
    private String auditAction;

    @Column(name = "AUDIT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private BundleDeploymentStatus auditStatus;

    @Column(name = "AUDIT_MESSAGE", nullable = true)
    private String auditMessage;

    // required for JPA
    protected BundleResourceDeploymentHistory() {
    }

    public BundleResourceDeploymentHistory(String subjectName, String auditAction, BundleDeploymentStatus auditStatus,
        String auditMessage) {

        this.subjectName = subjectName;
        this.auditAction = auditAction;
        this.auditStatus = auditStatus;
        this.auditMessage = auditMessage;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Long getAuditTime() {
        return this.auditTime;
    }

    public void setAuditTime(Long auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditAction() {
        return auditAction;
    }

    public void setAuditAction(String auditAction) {
        this.auditAction = auditAction;
    }

    public BundleDeploymentStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(BundleDeploymentStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getAuditMessage() {
        return this.auditMessage;
    }

    public void setAuditMessage(String auditMessage) {
        this.auditMessage = auditMessage;
    }

    public BundleResourceDeployment getResourceDeployment() {
        return resourceDeployment;
    }

    public void setResourceDeployment(BundleResourceDeployment resourceDeployment) {
        this.resourceDeployment = resourceDeployment;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("BundleDeploymentAudit: ");
        str.append(", time=[").append(this.auditTime).append("]");
        str.append(", rd=[").append(this.resourceDeployment).append("]");
        str.append(", action=[").append(this.auditAction).append("]");
        str.append(", status=[").append(this.auditStatus).append("]");
        str.append(", message=[").append(this.auditMessage).append("]");
        return str.toString();
    }
}