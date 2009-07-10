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

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.ChannelComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.CreateException;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

@Local
public interface ChannelManagerLocal {

    /**
     * @see ChannelManagerRemote#findChannels(Subject, PageControl)
     */
    PageList<Channel> findChannels(Subject subject, PageControl pc);

    /**
     * @see ChannelManagerRemote#findAssociatedContentSources(Subject, int, PageControl)
     */
    PageList<ContentSource> findAssociatedContentSources(Subject subject, int channelId, PageControl pc);

    /**
     * Gets all channels that are subscribed to by the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return the list of subscriptions
     */
    PageList<ChannelComposite> findResourceSubscriptions(Subject subject, int resourceId, PageControl pc);

    /**
     * Gets all channels that aren't subscribed to for the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return the list of available channels for the given resource
     */
    PageList<ChannelComposite> findAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc);

    /**
     * Gets all channels that are subscribed to by the given resource.
     *
     * @param  resourceId
     *
     * @return the list of subscriptions
     */
    List<ChannelComposite> getResourceSubscriptions(int resourceId);

    /**
     * Gets all channels that aren't subscribed to for the given resource.
     *
     * @param  resourceId
     *
     * @return the list of available channels for the given resource
     */
    List<ChannelComposite> findAvailableResourceSubscriptions(int resourceId);

    /**
     * Returns the set of package versions that can currently be accessed via the given channel.
     *
     * @param  subject   user asking to perform this
     * @param  channelId identifies the channel
     * @param  pc        pagination controls
     *
     * @return the package versions that are available in the channel
     */
    PageList<PackageVersion> findPackageVersionsInChannel(Subject subject, int channelId, PageControl pc);

    /**
     * @see ChannelManagerRemote#getPackageVersionsInChannel(Subject, int, String, PageControl)
     */
    PageList<PackageVersion> findPackageVersionsInChannel(Subject subject, int channelId, String filter, PageControl pc);

    /**
     * @see ChannelManagerRemote#addContentSourcesToChannel(Subject, int, int[])
     */
    void addContentSourcesToChannel(Subject subject, int channelId, int[] contentSourceIds) throws Exception;

    /**
     * @see ChannelManagerRemote#removeContentSourcesFromChannel(Subject, int, int[])
     */
    void removeContentSourcesFromChannel(Subject subject, int channelId, int[] contentSourceIds) throws Exception;

    /**
     * @see ChannelManagerRemote#getPackageVersionCountFromChannel(Subject, String, int)
     */
    long getPackageVersionCountFromChannel(Subject subject, int channelId);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * @throws UpdateException TODO
     * @see ChannelManagerRemote#addPackageVersionsToChannel(Subject, int, int[])
     */
    void addPackageVersionsToChannel(Subject subject, int channelId, int[] packageVersionIds) throws UpdateException;

    /**
     * @throws CreateException TODO
     * @see ChannelManagerRemote#createChannel(Subject, Channel)
     */
    Channel createChannel(Subject subject, Channel channel) throws CreateException;

    /**
     * @throws DeleteException TODO
     * @see ChannelManagerRemote#deleteChannel(Subject, int)
     */
    void deleteChannel(Subject subject, int channelId) throws DeleteException;

    /**
     * @throws FetchException TODO
     * @see ChannelManagerRemote#getChannel(Subject, int)
     */
    Channel getChannel(Subject subject, int channelId) throws FetchException;

    /**
     * @see ChannelManagerRemote#findChannels(Subject, Channel, PageControl)
     */
    public PageList<Channel> findChannels(Subject subject, Channel criteria, PageControl pc) throws FetchException;

    /**
     * @see ChannelManagerRemote#findPackageVersionsInChannel(Subject, int, PackageVersion, PageControl)
     */
    public PageList<PackageVersion> findPackageVersionsInChannel(Subject subject, int channelId,
        PackageVersion criteria, PageControl pc) throws FetchException;

    /**
     * @see ChannelManagerRemote#subscribeResourceToChannels(Subject, int, int[])
     */
    void subscribeResourceToChannels(Subject subject, int resourceId, int[] channelIds);

    /**
     * @see ChannelManagerRemote#unsubscribeResourceFromChannels(Subject, int, int[])
     */
    void unsubscribeResourceFromChannels(Subject subject, int resourceId, int[] channelIds);

    /**
     * @see ChannelManagerRemote#findSubscribedResources(Subject, int, PageControl)
     */
    PageList<Resource> findSubscribedResources(Subject subject, int channelId, PageControl pc);

    /**
     * @see ChannelManagerRemote#updateChannel(Subject, Channel)
     */
    Channel updateChannel(Subject subject, Channel channel) throws ChannelException;

}