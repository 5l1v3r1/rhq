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

package org.rhq.gui.webdav;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.stream.StreamUtil;

/**
 * The measurement traits resource that provides information on all traits of a managed resource.
 * 
 * @author John Mazzitelli
 */
public class TraitsResource extends BasicResource implements GetableResource {

    private List<MeasurementDataTrait> traits;
    private String content;

    public TraitsResource(Subject subject, Resource managedResource, List<MeasurementDataTrait> traits) {
        super(subject, managedResource);
        this.traits = traits;
    }

    public String getUniqueId() {
        return "traits_" + getManagedResource().getId();
    }

    public String getName() {
        return "measurement_traits.xml";
    }

    /**
     * The modified date is that of the last time a trait changed.
     */
    public Date getModifiedDate() {
        long latestTimestamp = 0L;
        for (MeasurementDataTrait trait : this.traits) {
            if (latestTimestamp < trait.getTimestamp()) {
                latestTimestamp = trait.getTimestamp();
            }
        }
        return new Date(latestTimestamp);
    }

    /**
     * The created date is the date of the resource itself was created.
     */
    public Date getCreateDate() {
        return new Date(getManagedResource().getCtime());
    }

    public Long getContentLength() {
        return Long.valueOf(loadContent().length());
    }

    public String getContentType(String accepts) {
        return "text/xml";
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> params, String str) throws IOException,
        NotAuthorizedException {

        byte[] bytes = loadContent().getBytes();
        long start = (range != null) ? range.getStart() : 0L;
        long length = (range != null) ? ((range.getFinish() - start) + 1) : bytes.length;

        InputStream in = new ByteArrayInputStream(bytes);
        StreamUtil.copy(in, out, start, length);
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return Long.valueOf(0L);
    }

    private String loadContent() {
        if (this.content == null) {
            StringBuilder str = new StringBuilder();
            str.append("<?xml version=\"1.0\"?>\n");
            str.append("<traits>\n");
            for (MeasurementDataTrait trait : this.traits) {
                str.append("   <trait>\n");
                str.append("      <name>").append(trait.getName()).append("</name>\n");
                str.append("      <value>").append(trait.getValue()).append("</value>\n");
                str.append("      <last-changed>").append(new Date(trait.getTimestamp())).append("</last-changed>\n");
                str.append("      <schedule-id>").append(trait.getScheduleId()).append("</schedule-id>\n");
                str.append("   </trait>\n");
            }
            str.append("</traits>\n");
            this.content = str.toString();
        }
        return this.content;
    }
}
