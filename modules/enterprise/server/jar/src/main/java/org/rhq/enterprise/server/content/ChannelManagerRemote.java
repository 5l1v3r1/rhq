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
package org.rhq.enterprise.server.content;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.ChannelCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ChannelManagerRemote {

    /**
     * Associates the package versions (identified by their IDs) to the given channel (also identified by its ID).
     *
     * @param subject           The logged in user's subject.
     * @param channelId         the ID of the channel
     * @param packageVersionIds the list of package version IDs to add to the channel
     */
    @WebMethod
    void addPackageVersionsToChannel( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channelId") int channelId, //
        @WebParam(name = "packageVersionIds") int[] packageVersionIds);

    /**
     * Creates a new {@link Repo}. Note that the created channel will not have any content sources assigned and no
     * resources will be subscribed. It is a virgin channel.
     *
     * @param subject The logged in user's subject.
     * @param repo a new channel object.
     *
     * @return the newly created channel
     */
    @WebMethod
    Repo createChannel( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channel") Repo repo) //
        throws ChannelException;

    /**
     * Deletes the identified channel. If this deletion orphans package versions (that is, its originating resource or
     * content source has been deleted), this will also purge those orphaned package versions.
     *
     * @param subject The logged in user's subject.
     * @param channelId
     */
    @WebMethod
    void deleteChannel( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channelId") int channelId);

    @WebMethod
    Repo getChannel( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channelId") int channelId);

    @WebMethod
    PageList<Repo> findChannelsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ChannelCriteria criteria);

    /**
     * @param subject
     * @param criteria Caller must add a valid channelId via {@link PackageVersionCriteria.addFilterChannelId}
     * @return PackageVersions for the channel
     * @throws IllegalArgumentException for invalid channelId filter
     */
    @WebMethod
    PageList<PackageVersion> findPackageVersionsInChannelByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") PackageVersionCriteria criteria);

    // change exception
    /**
     * Update an existing {@link Repo} object's basic fields, like name, description, etc. Note that the given <code>
     * channel</code>'s relationships will be ignored and not merged with the existing channel (e.g. is subscribed
     * resources will not be changed, regardless of what the given channel's subscribed resources set it). See methods
     * like {@link #addContentSourcesToChannel(Subject, int, int[])} to alter its relationships.
     *
     * @param subject The logged in user's subject.
     * @param repo to be updated
     *
     * @return Repo that was updated
     */
    @WebMethod
    Repo updateChannel( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channel") Repo repo) //
        throws ChannelException;

    /**
     * Returns the set of package versions that can currently be accessed via the given channel.
     *
     * @param subject   The logged in user's subject.
     * @param channelId identifies the channel
     * @param filter    A channel filter.
     * @param pc        pagination controls
     *
     * @return the package versions that are available in the channel
     */
    @WebMethod
    PageList<PackageVersion> findPackageVersionsInChannel( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channelId") int channelId, //
        @WebParam(name = "filter") String filter, //
        @WebParam(name = "pageControl") PageControl pc);

    @WebMethod
    PageList<Repo> findChannels( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "pageControl") PageControl pc);

    /**
     * Gets all resources that are subscribed to the given channel.
     *
     * @param subject The logged in user's subject.
     * @param channelId
     * @param pc
     *
     * @return the list of subscribers
     */
    @WebMethod
    PageList<Resource> findSubscribedResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "channelId") int channelId, //
        @WebParam(name = "pageControl") PageControl pc);

    /**
     * Subscribes the identified resource to the set of identified channels. Once complete, the resource will be able to
     * access all package content from all content sources that are assigned to the given channels.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource to be subscribed.
     * @param channelIds A list of channels to which the resource is subscribed.
     */
    @WebMethod
    void subscribeResourceToChannels( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "channelIds") int[] channelIds);

    /**
     * Unsubscribes the identified resource from the set of identified channels.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource to be subscribed.
     * @param channelIds A list of channels to which the resource is subscribed.
     */
    @WebMethod
    void unsubscribeResourceFromChannels( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "channelIds") int[] channelIds);

}