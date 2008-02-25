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
package org.rhq.core.domain.content;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;

/**
 * Domain representation of the steps used to install a package.
 *
 * @author Jason Dobies
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PACKAGE_INST_STEP_ID_SEQ")
@Table(name = "RHQ_PACKAGE_INST_STEP")
public class PackageInstallationStep implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    /**
     * Database assigned ID.
     */
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "STEP_KEY", nullable = false)
    private String key;

    /**
     * Relative order of the step in the overall list of steps.
     */
    @Column(name = "STEP_ORDER", nullable = false)
    private int order;

    /**
     * Description of what the step will do.
     */
    @Column(name = "DESCRIPTION")
    private String description;

    /**
     * If this step failed during execution, this will be populated with the plugin provided error message describing
     * the failure.
     */
    @Column(name = "ERROR_MSG")
    private String errorMessage;

    /**
     * Plugin provided indicator of whether or not the step was executed successfully.
     */
    @Column(name = "RESULT", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentResponseResult result;

    /**
     * Package version against which this step applies.
     */
    @JoinColumn(name = "INSTALLED_PKG_HIST_ID", referencedColumnName = "ID")
    @ManyToOne
    private InstalledPackageHistory installedPackageHistory;

    // Constructor ----------------------------------------

    public PackageInstallationStep() {
        // for JPA use
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ContentResponseResult getResult() {
        return result;
    }

    public void setResult(ContentResponseResult result) {
        this.result = result;
    }

    public InstalledPackageHistory getInstalledPackageHistory() {
        return installedPackageHistory;
    }

    public void setInstalledPackageHistory(InstalledPackageHistory installedPackageHistory) {
        this.installedPackageHistory = installedPackageHistory;
    }
}