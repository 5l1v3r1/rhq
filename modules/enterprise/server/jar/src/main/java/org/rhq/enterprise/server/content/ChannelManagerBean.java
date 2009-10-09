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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoContentSource;
import org.rhq.core.domain.content.ChannelPackageVersion;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.ResourceChannel;
import org.rhq.core.domain.content.composite.ChannelComposite;
import org.rhq.core.domain.criteria.ChannelCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.CriteriaQueryGenerator;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.domain.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginContainer;

@Stateless
public class ChannelManagerBean implements ChannelManagerLocal, ChannelManagerRemote {
    private final Log log = LogFactory.getLog(ChannelManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authzManager;
    @EJB
    private ContentSourceManagerLocal contentSourceManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteChannel(Subject subject, int channelId) {
        log.debug("User [" + subject + "] is deleting channel [" + channelId + "]");

        // bulk delete m-2-m mappings to the doomed channel
        // get ready for bulk delete by clearing entity manager
        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(ResourceChannel.DELETE_BY_CHANNEL_ID).setParameter("channelId", channelId)
            .executeUpdate();

        entityManager.createNamedQuery(RepoContentSource.DELETE_BY_CHANNEL_ID).setParameter("channelId", channelId)
            .executeUpdate();

        entityManager.createNamedQuery(ChannelPackageVersion.DELETE_BY_CHANNEL_ID).setParameter("channelId", channelId)
            .executeUpdate();

        Repo repo = entityManager.find(Repo.class, channelId);
        if (repo != null) {
            entityManager.remove(repo);
            log.debug("User [" + subject + "] deleted channel [" + repo + "]");
        } else {
            log.debug("Repo ID [" + channelId + "] doesn't exist - nothing to delete");
        }

        // remove any unused, orphaned package versions
        contentSourceManager.purgeOrphanedPackageVersions(subject);

        return;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Repo> findChannels(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("c.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_ALL, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_ALL);

        List<Repo> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Repo>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo getChannel(Subject subject, int channelId) {
        Repo repo = entityManager.find(Repo.class, channelId);

        if ((repo != null) && (repo.getChannelContentSources() != null)) {
            // load content sources separately. we can't do this all at once via fetch join because
            // on Oracle we use a LOB column on a content source field and you can't DISTINCT on LOBs
            repo.getChannelContentSources().size();
        }

        return repo;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ContentSource> findAssociatedContentSources(Subject subject, int channelId, PageControl pc) {
        pc.initDefaultOrderingField("cs.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, ContentSource.QUERY_FIND_BY_CHANNEL_ID,
            pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, ContentSource.QUERY_FIND_BY_CHANNEL_ID);

        query.setParameter("id", channelId);
        countQuery.setParameter("id", channelId);

        List<ContentSource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ContentSource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Resource> findSubscribedResources(Subject subject, int channelId, PageControl pc) {
        pc.initDefaultOrderingField("rc.resource.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Repo.QUERY_FIND_SUBSCRIBER_RESOURCES,
            pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Repo.QUERY_FIND_SUBSCRIBER_RESOURCES);

        query.setParameter("id", channelId);
        countQuery.setParameter("id", channelId);

        List<Resource> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Resource>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    // current resource subscriptions should be viewing, but perhaps available ones shouldn't
    public PageList<ChannelComposite> findResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_CHANNEL_COMPOSITES_BY_RESOURCE_ID, pc);
        Query countQuery = entityManager.createNamedQuery(Repo.QUERY_FIND_CHANNEL_COMPOSITES_BY_RESOURCE_ID_COUNT);

        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<ChannelComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ChannelComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ChannelComposite> findAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("c.id");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Repo.QUERY_FIND_AVAILABLE_CHANNEL_COMPOSITES_BY_RESOURCE_ID, pc);
        Query countQuery = entityManager
            .createNamedQuery(Repo.QUERY_FIND_AVAILABLE_CHANNEL_COMPOSITES_BY_RESOURCE_ID_COUNT);

        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<ChannelComposite> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<ChannelComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public List<ChannelComposite> findResourceSubscriptions(int resourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_CHANNEL_COMPOSITES_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);

        List<ChannelComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ChannelComposite> findAvailableResourceSubscriptions(int resourceId) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_AVAILABLE_CHANNEL_COMPOSITES_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);

        List<ChannelComposite> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInChannel(Subject subject, int channelId, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_CHANNEL_ID_WITH_PACKAGE, pc);

        query.setParameter("channelId", channelId);

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromChannel(subject, null, channelId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInChannel(Subject subject, int channelId, String filter,
        PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name, pv.version");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_BY_CHANNEL_ID_WITH_PACKAGE_FILTERED, pc);

        query.setParameter("channelId", channelId);
        query.setParameter("filter", PersistenceUtility.formatSearchParameter(filter));

        List<PackageVersion> results = query.getResultList();
        long count = getPackageVersionCountFromChannel(subject, filter, channelId);

        return new PageList<PackageVersion>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo updateChannel(Subject subject, Repo repo) throws ChannelException {
        if (repo.getName() == null || repo.getName().trim().equals("")) {
            throw new ChannelException("Repo name is required");
        }

        // should we check non-null channel relationships and warn that we aren't changing them?
        log.debug("User [" + subject + "] is updating channel [" + repo + "]");
        repo = entityManager.merge(repo);
        log.debug("User [" + subject + "] updated channel [" + repo + "]");

        return repo;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Repo createChannel(Subject subject, Repo repo) throws ChannelException {
        validateChannel(repo);

        log.debug("User [" + subject + "] is creating channel [" + repo + "]");
        entityManager.persist(repo);
        log.debug("User [" + subject + "] created channel [" + repo + "]");

        return repo; // now has the ID set
    }

    private void validateChannel(Repo c) throws ChannelException {
        if (c.getName() == null || c.getName().trim().equals("")) {
            throw new ChannelException("Repo name is required");
        }

        List<Repo> repos = getChannelByName(c.getName());
        if (repos.size() != 0) {
            throw new ChannelException("There is already a channel with the name of [" + c.getName() + "]");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Repo> getChannelByName(String name) {
        Query query = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<Repo> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void addContentSourcesToChannel(Subject subject, int channelId, int[] contentSourceIds) throws Exception {
        Repo repo = entityManager.find(Repo.class, channelId);
        if (repo == null) {
            throw new Exception("There is no channel with an ID [" + channelId + "]");
        }

        repo.setLastModifiedDate(System.currentTimeMillis());

        log.debug("User [" + subject + "] is adding content sources to channel [" + repo + "]");

        ContentSourcePluginContainer pc = ContentManagerHelper.getPluginContainer();
        Query q = entityManager.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID_NO_FETCH);

        for (int id : contentSourceIds) {
            ContentSource cs = entityManager.find(ContentSource.class, id);
            if (cs == null) {
                throw new Exception("There is no content source with id [" + id + "]");
            }

            RepoContentSource ccsmapping = repo.addContentSource(cs);
            entityManager.persist(ccsmapping);

            Set<PackageVersion> alreadyAssociatedPVs = new HashSet<PackageVersion>(repo.getPackageVersions());

            // automatically associate all of the content source's package versions with this channel
            // but, *skip* over the ones that are already linked to this channel from a previous association
            q.setParameter("id", cs.getId());
            List<PackageVersionContentSource> pvcss = q.getResultList();
            for (PackageVersionContentSource pvcs : pvcss) {
                PackageVersion pv = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
                if (alreadyAssociatedPVs.contains(pv)) {
                    continue; // skip if already associated with this channel
                }
                ChannelPackageVersion mapping = new ChannelPackageVersion(repo, pv);
                entityManager.persist(mapping);
            }

            entityManager.flush();
            entityManager.clear();

            // ask to synchronize the content source immediately (is this the right thing to do?)
            pc.syncNow(cs);
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void addPackageVersionsToChannel(Subject subject, int channelId, int[] packageVersionIds) {
        Repo repo = entityManager.find(Repo.class, channelId);

        for (int packageVersionId : packageVersionIds) {
            PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);

            ChannelPackageVersion mapping = new ChannelPackageVersion(repo, packageVersion);
            entityManager.persist(mapping);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void removeContentSourcesFromChannel(Subject subject, int channelId, int[] contentSourceIds)
        throws ChannelException {
        Repo repo = getChannel(subject, channelId);

        log.debug("User [" + subject + "] is removing content sources from channel [" + repo + "]");

        Set<RepoContentSource> currentSet = repo.getChannelContentSources();

        if ((currentSet != null) && (currentSet.size() > 0)) {
            Set<RepoContentSource> toBeRemoved = new HashSet<RepoContentSource>();
            for (RepoContentSource current : currentSet) {
                for (int id : contentSourceIds) {
                    if (id == current.getChannelContentSourcePK().getContentSource().getId()) {
                        toBeRemoved.add(current);
                        break;
                    }
                }
            }

            for (RepoContentSource doomed : toBeRemoved) {
                entityManager.remove(doomed);
            }

            currentSet.removeAll(toBeRemoved);
        }

        // note that we specifically do not disassociate package versions from the channel, even if those
        // package versions come from the content source that is being removed

        return;
    }

    @SuppressWarnings("unchecked")
    public void subscribeResourceToChannels(Subject subject, int resourceId, int[] channelIds) {
        if ((channelIds == null) || (channelIds.length == 0)) {
            return; // nothing to do
        }

        // make sure the user has permissions to subscribe this resource
        if (!authzManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("[" + subject
                + "] does not have permission to subscribe this resource to channels");
        }

        // find the resource - abort if it does not exist
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new RuntimeException("There is no resource with the ID [" + resourceId + "]");
        }

        // find all the channels and subscribe the resource to each of them
        // note that if the length of the ID array doesn't match, then one of the channels doesn't exist
        // and we abort altogether - we do not subscribe to anything unless all channel IDs are valid
        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_IDS);
        List<Integer> idList = new ArrayList<Integer>(channelIds.length);
        for (Integer id : channelIds) {
            idList.add(id);
        }

        q.setParameter("ids", idList);
        List<Repo> repos = q.getResultList();

        if (repos.size() != channelIds.length) {
            throw new RuntimeException("One or more of the channels do not exist [" + idList + "]->[" + repos + "]");
        }

        for (Repo repo : repos) {
            ResourceChannel mapping = repo.addResource(resource);
            entityManager.persist(mapping);
        }

        return;
    }

    @SuppressWarnings("unchecked")
    public void unsubscribeResourceFromChannels(Subject subject, int resourceId, int[] channelIds) {
        if ((channelIds == null) || (channelIds.length == 0)) {
            return; // nothing to do
        }

        // make sure the user has permissions to unsubscribe this resource
        if (!authzManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("[" + subject
                + "] does not have permission to unsubscribe this resource from channels");
        }

        // find the resource - abort if it does not exist
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new RuntimeException("There is no resource with the ID [" + resourceId + "]");
        }

        // find all the channels and unsubscribe the resource from each of them
        // note that if the length of the ID array doesn't match, then one of the channels doesn't exist
        // and we abort altogether - we do not unsubscribe from anything unless all channel IDs are valid
        Query q = entityManager.createNamedQuery(Repo.QUERY_FIND_BY_IDS);
        List<Integer> idList = new ArrayList<Integer>(channelIds.length);
        for (Integer id : channelIds) {
            idList.add(id);
        }

        q.setParameter("ids", idList);
        List<Repo> repos = q.getResultList();

        if (repos.size() != channelIds.length) {
            throw new RuntimeException("One or more of the channels do not exist [" + idList + "]->[" + repos + "]");
        }

        for (Repo repo : repos) {
            ResourceChannel mapping = repo.removeResource(resource);
            entityManager.remove(mapping);
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromChannel(Subject subject, String filter, int channelId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_BY_CHANNEL_ID_FILTERED);

        countQuery.setParameter("channelId", channelId);
        countQuery.setParameter("filter", (filter == null) ? null : ("%" + filter.toUpperCase() + "%"));

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageVersionCountFromChannel(Subject subject, int channelId) {
        return getPackageVersionCountFromChannel(subject, null, channelId);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Repo> findChannelsByCriteria(Subject subject, ChannelCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Repo> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PackageVersion> findPackageVersionsInChannelByCriteria(Subject subject,
        PackageVersionCriteria criteria) {
        Integer channelId = criteria.getFilterChannelId();

        if ((null == channelId) || (channelId < 1)) {
            throw new IllegalArgumentException("Illegal filterResourceId: " + channelId);
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        Query query = generator.getQuery(entityManager);
        Query countQuery = generator.getCountQuery(entityManager);

        long count = (Long) countQuery.getSingleResult();
        List<PackageVersion> packageVersions = query.getResultList();

        return new PageList<PackageVersion>(packageVersions, (int) count, criteria.getPageControl());
    }

}