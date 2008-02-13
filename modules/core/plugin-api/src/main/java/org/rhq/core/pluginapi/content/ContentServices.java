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
package org.rhq.core.pluginapi.content;

import java.io.OutputStream;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * This is the interface to the content services provided to a plugin's {@link ContentFacet}. A content facet
 * implementation can use these services to request information from the plugin container.
 *
 * @author John Mazziteli
 */
public interface ContentServices {
    /**
     * Requests that the plugin container download and stream the bits for the specified package. If the package cannot
     * be found, an exception will be thrown.
     *
     * @param  context           identifies the resource requesting the bits; this is passed in through the
     *                           {@link ContentFacet#startContentFacet(ContentContext)} method.
     * @param  packageDetailsKey identifies the package
     * @param  outputStream      an output stream where the plugin container should write the package contents. It is up
     *                           to the plugin, before making this call, to prepare this output stream in order to write
     *                           the package content to an appropriate location. It is also up to the plugin to close
     *                           this stream after this call completes.
     *
     * @return the number of bytes written to the output stream - this is the size of the package version that was
     *         downloaded
     */
    long downloadPackageBits(ContentContext context, PackageDetailsKey packageDetailsKey, OutputStream outputStream);

    /**
     * Requests that the plugin container download and stream the bits for the specified package. If the package cannot
     * be found, an exception will be thrown.
     *
     * @param  context           identifies the resource requesting the bits; this is passed in through the
     *                           {@link ContentFacet#startContentFacet(ContentContext)} method.
     * @param  packageDetailsKey identifies the package
     * @param  outputStream      an output stream where the plugin container should write the package contents. It is up
     *                           to the plugin, before making this call, to prepare this output stream in order to write
     *                           the package content to an appropriate location.
     * @param  startByte         the first byte (inclusive) of the byte range to retrieve and output (bytes start at
     *                           index 0)
     * @param  endByte           the last byte (inclusive) of the byte range to retrieve and output (-1 means up to EOF)
     *                           (bytes start at index 0)
     *
     * @return the number of bytes written to the output stream - this is the size of the chunk downloaded
     */
    long downloadPackageBitsRange(ContentContext context, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte);

    /**
     * Requests the size, in bytes, of the identified package version.
     *
     * @param  context           identifies the resource requesting the info; this is passed in through the
     *                           {@link ContentFacet#startContentFacet(ContentContext)} method.
     * @param  packageDetailsKey identifies the package whose size is to be returned
     *
     * @return the size, in number of bytes, of the package version
     */
    long getPackageBitsLength(ContentContext context, PackageDetailsKey packageDetailsKey);

    /**
     * Requests all {@link PackageVersion#getMetadata() metadata} for all package versions that the calling resource
     * component is {@link Channel#getResources() subscribed to see}. The returned object has the metadata bytes that
     * are meaningful to the calling plugin component.
     *
     * <p>Because the result set is potentially large, callers should consider caching the returned data. You can use
     * {@link #getResourceSubscriptionMD5(ContentContext)} to determine when the cached data is stale.</p>
     *
     * @param  context identifies the resource requesting the data; this is passed in through the
     *                 {@link ContentFacet#startContentFacet(ContentContext)} method.
     * @param  pc      this method can potentially return a large set; this page control object allows the caller to
     *                 page through that large set, as opposed to requesting the entire set in one large chunk
     *
     * @return the list of all package versions' metadata
     */
    PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(ContentContext context, PageControl pc);

    /**
     * Gets the MD5 hash which identifies a resource "content subscription". This MD5 hash will change when any channel
     * the resource is subscribed to has changed its contents (that is, if a package version was added/updated/removed
     * from it).
     *
     * @param  context identifies the resource requesting the data; this is passed in through the
     *                 {@link ContentFacet#startContentFacet(ContentContext)} method.
     *
     * @return the MD5 of all package versions' metadata
     *
     * @see    #getPackageVersionMetadata(ContentContext, PageControl)
     */
    String getResourceSubscriptionMD5(ContentContext context);
}