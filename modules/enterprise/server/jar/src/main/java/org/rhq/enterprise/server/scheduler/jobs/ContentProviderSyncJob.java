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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is a Quartz scheduler job whose job is to synchronize one particular {@link ContentSource}. After synchronizing
 * the metadata, this will also attempt to load the content if the content source is not configured for
 * {@link ContentSource#isLazyLoad() lazy loading}.
 *
 * <p>This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't need multiple instances of this job running at the same time.</p>
 *
 * @author John Mazzitelli
 * @author Jason Dobies
 */
public class ContentProviderSyncJob implements StatefulJob {
    private static final String DATAMAP_CONTENT_SOURCE_NAME = "contentSourceName";
    private static final String DATAMAP_CONTENT_SOURCE_TYPE_NAME = "contentSourceTypeName";
    public static final String DATAMAP_SYNC_IMPORTED_REPOS = "syncImportedRepos";

    private static final Log log = LogFactory.getLog(ContentProviderSyncJob.class);
    private static final String SEPARATOR = "--";

    public int getPercentComplete() {
        int retval = (int) (Math.random() * 100);
        return retval;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDetail jobDetail = context.getJobDetail();
            if (jobDetail == null) {
                throw new IllegalStateException("The job does not have any details");
            }

            JobDataMap dataMap = jobDetail.getJobDataMap();
            if (dataMap == null) {
                throw new IllegalStateException("The job does not have any data in its details");
            }

            String name = dataMap.getString(DATAMAP_CONTENT_SOURCE_NAME);
            String typeName = dataMap.getString(DATAMAP_CONTENT_SOURCE_TYPE_NAME);
            boolean syncImportedRepos = dataMap.getBoolean(DATAMAP_SYNC_IMPORTED_REPOS);

            if (name == null) {
                throw new IllegalStateException("Missing the content source name in details data");
            }

            if (typeName == null) {
                throw new IllegalStateException("Missing the content source type name in details data");
            }

            ContentSource contentSource = synchronizeAndLoad(name, typeName);

            if (syncImportedRepos) {
                // Synchronize all repos associated with the content source - these may be repos we just
                // discovered during this sync or ones that were already associated.
                syncImportedRepos(contentSource);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to sync content source in job [" + context.getJobDetail() + "]";
            log.error(errorMsg, e);
            JobExecutionException jobExecutionException = new JobExecutionException(errorMsg, e, false);

            // should we unschedule so we never attempt to sync again?
            // That would mean any error will cause this sync to never occur again automatically until
            // we restart the server, restart the server-side content plugin container or somehow manually create
            // the schedule again.  I will assume we will allow this schedule to trigger again, not sure if
            // that is what we want, but we can flip this to true if we want the other behavior.
            // NOTE: we do NOT retrigger if we threw IllegalStateException because we know it'll never work anyway
            if (!(e instanceof IllegalStateException)) {
                jobExecutionException.setUnscheduleAllTriggers(false);
            }

            throw jobExecutionException;
        }
    }

    private void syncImportedRepos(ContentSource contentSource) throws InterruptedException {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        final Subject overlord = subjectManager.getOverlord();
        final RepoCriteria repoCriteria = new RepoCriteria();
        repoCriteria.addFilterContentSourceIds(contentSource.getId());
        repoCriteria.addFilterCandidate(false);

        final RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<Repo, RepoCriteria> queryExecutor = new CriteriaQueryExecutor<Repo, RepoCriteria>() {
            @Override
            public PageList<Repo> execute(RepoCriteria criteria) {
                return repoManager.findReposByCriteria(overlord, repoCriteria);
            }
        };

        CriteriaQuery<Repo, RepoCriteria> repos = new CriteriaQuery<Repo, RepoCriteria>(repoCriteria, queryExecutor);


        ArrayList<Integer> repoIdList = new ArrayList<Integer>();
        for (Repo repo : repos) {
            repoIdList.add(repo.getId());
        }
        Integer[] repoIds = new Integer[repoIdList.size()];
        repoIds = repoIdList.toArray(repoIds);

        repoManager.internalSynchronizeRepos(overlord, repoIds);
    }

    /**
     * This will synchronize the identified content source such that its package version information is updated and, if
     * not lazy-loading, its package bits are downloaded.
     *
     * <p>Note that this method executes outside of any transaction. This is very important since this job is
     * potentially very long running (on the order of hours potentially). We do our processing in here with this in
     * mind. We make sure we never do any one thing that potentially could timeout a transaction.</p>
     *
     * @param  contentSourceName     name of the {@link org.rhq.core.domain.content.ContentSource}
     * @param  contentSourceTypeName name of the {@link org.rhq.core.domain.content.ContentSourceType}
     *
     * @throws Exception if either the sync failed or one of the packages failed to download
     */
    private ContentSource synchronizeAndLoad(String contentSourceName, String contentSourceTypeName) throws Exception {
        // note that we will keep calling getOverlord on this subject manager - the overlord
        // has a very short session lifespan so we need to keep asking for a new one, due to the possibility
        // that some of the methods we call here take longer than the overlord's lifespan
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord;
        ContentSourceManagerLocal contentManager = LookupUtil.getContentSourceManager();
        ContentSource contentSource;

        overlord = subjectManager.getOverlord();
        contentSource = contentManager
            .getContentSourceByNameAndType(overlord, contentSourceName, contentSourceTypeName);

        if (contentSource == null) {
            throw new Exception("Sync job was asked to sync an unknown content source: " + contentSourceName + "|"
                + contentSourceTypeName);
        }

        int contentSourceId = contentSource.getId();

        // If 'completed' is false, there was already a synchronization taking place,
        // so we should abort and let that already running sync take care of everything.
        boolean completed = contentManager.internalSynchronizeContentSource(contentSourceId);

        if (!completed) {
            log.info("Content source [" + contentSourceName + "] is currently being synchronized already. "
                + "Please wait for the current sync job to finish.");
        }

        return contentSource;
    }

    /**
     * All content source sync jobs must have specified data prepared
     * in their job details data map. This creates that data map. You must
     * call this method everytime you schedule a content source sync job.
     * If the given details is not <code>null</code>, this will place the
     * created data map in the details for you. Otherwise, you must ensure
     * the returned data map gets associated with the job when it is created.
     *
     * @param contentSource the content source whose sync job's details is being prepared
     * @param details where the job's data map will be stored (may be <code>null</code>)
     *
     * @return the data map with the data necessary to execute a content sync job
     */
    public static JobDataMap createJobDataMap(ContentSource contentSource, JobDetail details) {
        JobDataMap dataMap;

        if (details != null) {
            dataMap = details.getJobDataMap();
        } else {
            dataMap = new JobDataMap();
        }

        dataMap.put(DATAMAP_CONTENT_SOURCE_NAME, contentSource.getName());
        dataMap.put(DATAMAP_CONTENT_SOURCE_TYPE_NAME, contentSource.getContentSourceType().getName());

        return dataMap;
    }

    /**
     * Creates the name for the scheduled content source's sync job. Calling this
     * method multiple times with the same content source always produces the same name.
     *
     * @param cs the content source whose scheduled job name is to be returned
     *
     * @return the scheduled job name for the given content source
     */
    public static String createJobName(ContentSource cs) {
        // the quartz table has a limited column width of 80 - but we need to use the names to make jobs unique
        // so encode the names' hashcodes to ensure we fix into the quartz job name column.
        String nameEncoded = Integer.toHexString(cs.getName().hashCode());
        String typeNameEncoded = Integer.toHexString(cs.getContentSourceType().getName().hashCode());

        String jobName = nameEncoded + SEPARATOR + typeNameEncoded;

        if (jobName.length() > 80) {
            throw new IllegalArgumentException("Job names max size is 80 chars due to DB column size restrictions: "
                + jobName);
        }

        return jobName;
    }

    /**
     * Creates a unique name for a new content source sync job. Calling this
     * method multiple times with the same content source always produces a different name
     * which is useful if you want to schedule an new job that is separate and distinct
     * from any other job in the system.
     *
     * @param cs the content source
     *
     * @return a unique job name that can be used for a new job to sync a given content source
     */
    public static String createUniqueJobName(ContentSource cs) {
        // the quartz table has a limited column width of 80 - but we need to use the names to make jobs unique
        // so encode the names' hashcodes to ensure we fix into the quartz job name column.
        // appendStr is used to make the job unique among others for the same content source.
        String nameEncoded = Integer.toHexString(cs.getName().hashCode());
        String typeNameEncoded = Integer.toHexString(cs.getContentSourceType().getName().hashCode());
        String appendStr = Long.toHexString(System.currentTimeMillis());

        String jobName = nameEncoded + SEPARATOR + typeNameEncoded + SEPARATOR + appendStr;

        if (jobName.length() > 80) {
            throw new IllegalArgumentException("Job names max size is 80 chars due to DB column size restrictions: "
                + jobName);
        }

        return jobName;
    }
}