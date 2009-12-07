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
package org.rhq.enterprise.server.plugin.pc.content;

import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.CVE;

/**
 * @author Pradeep Kilambi
 */
public class AdvisoryCVEDetails {

    private Advisory advisory;
    private CVE cve;

    public AdvisoryCVEDetails(Advisory advisoryIn, CVE cveIn) {
        advisory = advisoryIn;
        cve = cveIn;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public void setAdvisory(Advisory advisory) {
        this.advisory = advisory;
    }

    public CVE getCVE() {
        return cve;
    }

    public void setCVE(CVE cveIn) {
        this.cve = cveIn;
    }

    public String toString() {
        return "Advisory = " + advisory + ", cve =" + getCVE();
    }
}
