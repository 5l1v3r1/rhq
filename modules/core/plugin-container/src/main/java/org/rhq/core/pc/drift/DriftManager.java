package org.rhq.core.pc.drift;

import static org.rhq.core.util.ZipUtil.zipFileOrDirectory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;

public class DriftManager extends AgentService implements DriftAgentService, DriftClient, ContainerService {

    private final Log log = LogFactory.getLog(DriftManager.class);

    private PluginContainerConfiguration pluginContainerConfiguration;

    private File changeSetsDir;

    private ScheduledThreadPoolExecutor driftThreadPool;

    private ScheduleQueue schedulesQueue = new ScheduleQueueImpl();

    private ChangeSetManager changeSetMgr;

    public DriftManager() {
        super(DriftAgentService.class);
    }

    @Override
    public void setConfiguration(PluginContainerConfiguration configuration) {
        pluginContainerConfiguration = configuration;
        changeSetsDir = new File(pluginContainerConfiguration.getDataDirectory(), "changesets");
        changeSetsDir.mkdir();
    }

    @Override
    public void initialize() {
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);

        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setScheduleQueue(schedulesQueue);
        driftDetector.setChangeSetManager(changeSetMgr);
        driftDetector.setDriftClient(this);

        driftThreadPool = new ScheduledThreadPoolExecutor(5);
        driftThreadPool.scheduleAtFixedRate(driftDetector, 30, 1800, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        driftThreadPool.shutdown();
        driftThreadPool = null;

        schedulesQueue.clear();
        schedulesQueue = null;

        changeSetMgr = null;
    }

    @Override
    public void sendChangeSetToServer(int resourceId, DriftConfiguration driftConfiguration) {
        try {
            File changeSetFile = changeSetMgr.findChangeSet(resourceId, driftConfiguration.getName());
            if (changeSetFile == null) {
                log
                    .warn("changeset[resourceId: " + resourceId + ", driftConfiguration: "
                        + driftConfiguration.getName()
                        + "] was not found. Cancelling request to send change set to server");
                return;
            }

            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();

            // TODO Include the version in the change set file name to ensure the file name is unique
            File zipFile = new File(pluginContainerConfiguration.getTemporaryDirectory(), "changeset-" + resourceId
                + driftConfiguration.getName() + ".zip");
            zipFileOrDirectory(changeSetFile, zipFile);

            driftServer.sendChangesetZip(resourceId, zipFile.length(), remoteInputStream(new BufferedInputStream(
                new FileInputStream(zipFile))));
        } catch (IOException e) {
            log.error("An error occurred while trying to send changeset[resourceId: " + resourceId
                + ", driftConfiguration: " + driftConfiguration.getName() + "]", e);
        }
    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, File contentDir) {
        try {
            File zipFile = new File(pluginContainerConfiguration.getTemporaryDirectory(), "content.zip");
            zipFileOrDirectory(contentDir, zipFile);

            DriftServerService driftServer = pluginContainerConfiguration.getServerServices().getDriftServerService();
            driftServer.sendFilesZip(resourceId, zipFile.length(), remoteInputStream(new BufferedInputStream(
                new FileInputStream(zipFile))));
        } catch (IOException e) {
            log.error("An error occurred while trying to send content for changeset[resourceId: " + resourceId +
                ", driftConfiguration: " + driftConfigurationName + "]", e);
        }
    }

    @Override
    public void detectDrift(int resourceId, DriftConfiguration driftConfiguration) {
        ScheduleQueue queue = new ScheduleQueue() {
            DriftDetectionSchedule schedule;

            @Override
            public DriftDetectionSchedule dequeue() {
                DriftDetectionSchedule removedSchedule = schedule;
                schedule = null;
                return removedSchedule;
            }

            @Override
            public boolean enqueue(DriftDetectionSchedule schedule) {
                this.schedule = schedule;
                return true;
            }

            @Override
            public void clear() {
                schedule = null;
            }
        };
        queue.enqueue(new DriftDetectionSchedule(resourceId, driftConfiguration));

        DriftDetector driftDetector = new DriftDetector();
        driftDetector.setChangeSetManager(changeSetMgr);
        driftDetector.setScheduleQueue(queue);
        driftDetector.setDriftClient(this);

        driftThreadPool.execute(driftDetector);
    }

    @Override
    public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        schedulesQueue.enqueue(new DriftDetectionSchedule(resourceId, driftConfiguration));
    }

    @Override
    public boolean requestDriftFiles(int resourceId, Headers headers, List<DriftFile> driftFiles) {
        DriftFilesSender sender = new DriftFilesSender();
        sender.setResourceId(resourceId);
        sender.setDriftClient(this);
        sender.setDriftFiles(driftFiles);
        sender.setHeaders(headers);
        sender.setChangeSetManager(changeSetMgr);

        driftThreadPool.execute(sender);

        return true;
    }


    @Override
    public void unscheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
    }

    @Override
    public void updateDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
    }

}
