/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.drift.mongodb;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFSDBFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.drift.dto.DriftChangeSetDTO;
import org.rhq.core.domain.drift.dto.DriftDTO;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.ChangeSetDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.FileDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import static org.rhq.enterprise.server.util.LookupUtil.getAgentManager;
import static org.rhq.enterprise.server.util.LookupUtil.getResourceManager;
import static org.rhq.enterprise.server.util.LookupUtil.getSubjectManager;

public class MongoDBDriftServer implements DriftServerPluginFacet, ServerPluginComponent {

    private Log log = LogFactory.getLog(MongoDBDriftServer.class);

    private Mongo connection;

    private Morphia morphia;

    private Datastore ds;
    
    private ChangeSetDAO changeSetDAO;

    private FileDAO fileDAO;

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        connection = new Mongo("127.0.0.1");
        morphia = new Morphia().map(MongoDBChangeSet.class).map(MongoDBChangeSetEntry.class).map(MongoDBFile.class);
        ds = morphia.createDatastore(connection, "rhq");
        changeSetDAO =  new ChangeSetDAO(morphia, connection, "rhq");
        fileDAO = new FileDAO(ds.getDB());
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public DriftChangeSetSummary saveChangeSet(final Subject subject, final int resourceId, final File changeSetZip)
        throws Exception {

        final DriftChangeSetSummary summary = new DriftChangeSetSummary();

        ZipUtil.walkZipFile(changeSetZip, new ZipUtil.ZipEntryVisitor() {
            @Override
            public boolean visit(ZipEntry zipEntry, ZipInputStream stream) throws Exception {
                ChangeSetReader reader = new ChangeSetReaderImpl(new BufferedReader(new InputStreamReader(stream)));
                Headers headers = reader.getHeaders();

                List<DriftFileDTO> missingContent = new LinkedList<DriftFileDTO>();

                MongoDBChangeSet changeSet = new MongoDBChangeSet();
                changeSet.setCategory(headers.getType());
                changeSet.setResourceId(resourceId);
                changeSet.setDriftDefinitionId(headers.getDriftDefinitionId());
                changeSet.setDriftDefinitionName(headers.getDriftDefinitionName());
                changeSet.setDriftHandlingMode(DriftHandlingMode.normal);
                changeSet.setVersion(headers.getVersion());

                summary.setCategory(headers.getType());
                summary.setResourceId(resourceId);
                summary.setDriftDefinitionName(headers.getDriftDefinitionName());
                summary.setCreatedTime(changeSet.getCtime());

                for (FileEntry fileEntry : reader) {
                    String path = FileUtil.useForwardSlash(fileEntry.getFile());
                    MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry(path, fileEntry.getType());

                    switch (fileEntry.getType()) {
                        case FILE_ADDED:
                            entry.setNewFileHash(fileEntry.getNewSHA());
                            if (fileDAO.findOne(fileEntry.getNewSHA()) == null) {
                                missingContent.add(newDriftFile(fileEntry.getNewSHA()));
                            }
                            break;
                        case FILE_CHANGED:
                            entry.setOldFileHash(fileEntry.getOldSHA());
                            entry.setNewFileHash(fileEntry.getNewSHA());
                            if (fileDAO.findOne(fileEntry.getNewSHA()) == null) {
                                missingContent.add(newDriftFile(fileEntry.getNewSHA()));
                            }
                            if (fileDAO.findOne(fileEntry.getOldSHA()) == null) {
                                missingContent.add(newDriftFile(fileEntry.getNewSHA()));
                            }
                            break;
                        default: // FILE_REMOVED
                            entry.setOldFileHash(fileEntry.getOldSHA());
                            if (fileDAO.findOne(fileEntry.getOldSHA()) == null) {
                                missingContent.add(newDriftFile(fileEntry.getOldSHA()));
                            }
                    }
                    changeSet.add(entry);

                    // we are taking advantage of the fact that we know the summary is only used by the server
                    // if the change set is a DRIFT report. If its a coverage report, it is not used (we do
                    // not alert on coverage reports) - so don't waste memory by collecting all the paths
                    // when we know they aren't going to be used anyway.
                    if (headers.getType() == DriftChangeSetCategory.DRIFT) {
                        summary.addDriftPathname(path);
                    }
                }

                ds.save(changeSet);

                // The following section of code really should not be part of the plugin
                // implementation, but it is currently required due to a flaw in the design
                // of the drift plugin framework. Two things are done here. First, if we
                // successfully persist the change set, then we need to send an
                // acknowledgement to the agent. This is critical because the agent will
                // in effect suspend drift detection (for the particular definition) until
                // it receives the ACK. Secondly, we need to tell the agent to send the
                // actual file bits for any change set content we do not have.
                AgentClient agent = getAgentManager().getAgentClient(getSubjectManager().getOverlord(), resourceId);
                DriftAgentService driftService = agent.getDriftAgentService();
                driftService.ackChangeSet(headers.getResourceId(), headers.getDriftDefinitionName());
                if (!missingContent.isEmpty()) {
                    driftService.requestDriftFiles(resourceId, headers, missingContent);
                }

                return true;
            }
        });

        return summary;
    }

    private DriftFileDTO newDriftFile(String hash) {
        DriftFileDTO file = new DriftFileDTO();
        file.setHashId(hash);
        return file;
    }

    private DriftFileDTO newDriftFile(String hash, DriftFileStatus status) {
        DriftFileDTO file = new DriftFileDTO();
        file.setHashId(hash);
        file.setStatus(status);
        return file;
    }

    @Override
    public void saveChangeSetFiles(final Subject subject, final File changeSetFilesZip) throws Exception {
        String zipFileName = changeSetFilesZip.getName();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File dir = new File(tmpDir, zipFileName.substring(0, zipFileName.indexOf(".")));
        dir.mkdir();

        ZipUtil.unzipFile(changeSetFilesZip, dir);
        for (File file : dir.listFiles()) {
            fileDAO.save(file);
            file.delete();
        }

    }

    @Override
    public PageList<? extends DriftChangeSet<?>> findDriftChangeSetsByCriteria(Subject subject,
        DriftChangeSetCriteria criteria) {
        List<MongoDBChangeSet> changeSets = changeSetDAO.findByChangeSetCritiera(criteria);
        PageList<DriftChangeSetDTO> results = new PageList<DriftChangeSetDTO>();
        for (MongoDBChangeSet changeSet : changeSets) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            Set<DriftDTO> entries = new HashSet<DriftDTO>();
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                entries.add(toDTO(entry, changeSetDTO));
            }
            changeSetDTO.setDrifts(entries);
            results.add(changeSetDTO);
        }

        return results;
    }

    @Override
    public PageList<? extends Drift<?, ?>> findDriftsByCriteria(Subject subject, DriftCriteria criteria) {
        List<MongoDBChangeSetEntry> entries = changeSetDAO.findEntries(criteria);
        PageList<DriftDTO> results = new PageList<DriftDTO>();
        for (MongoDBChangeSetEntry entry : entries) {
            results.add(toDTO(entry, toDTO(entry.getChangeSet())));
        }

        return results;
    }

    @Override
    public PageList<DriftComposite> findDriftCompositesByCriteria(Subject subject, DriftCriteria criteria) {
        List<MongoDBChangeSet> changeSets = changeSetDAO.findByDriftCriteria(criteria);
        Map<Integer, Resource> resources = loadResourceMap(subject, criteria.getFilterResourceIds());
        PageList<DriftComposite> results = new PageList<DriftComposite>();

        for (MongoDBChangeSet changeSet : changeSets) {
            DriftChangeSetDTO changeSetDTO = toDTO(changeSet);
            for (MongoDBChangeSetEntry entry : changeSet.getDrifts()) {
                results.add(new DriftComposite(toDTO(entry, changeSetDTO), resources.get(changeSet.getResourceId()),
                        changeSet.getDriftDefinitionName()));
            }
        }

        return results;
    }

    @Override
    public DriftFile getDriftFile(Subject subject, String hashId) throws Exception {
        GridFSDBFile gridFSDBFile = fileDAO.findById(hashId);
        if (gridFSDBFile == null) {
            return null;
        }
        return newDriftFile(hashId, DriftFileStatus.LOADED);
    }

    @Override
    public void purgeByDriftDefinitionName(Subject subject, int resourceId, String driftDefName) throws Exception {
        changeSetDAO.deleteChangeSets(resourceId, driftDefName);
    }

    @Override
    public int purgeOrphanedDriftFiles(Subject subject, long purgeMillis) {
        // TODO implement me!
        return 0;
    }

    @Override
    public String getDriftFileBits(Subject subject, String hash) {
        GridFSDBFile file = fileDAO.findById(hash);
        if (file == null) {
            return null;
        }
        return new String(StreamUtil.slurp(file.getInputStream()));
    }

    Map<Integer, Resource> loadResourceMap(Subject subject, Integer[] resourceIds) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterIds(resourceIds);

        ResourceManagerLocal resourceMgr = getResourceManager();
        PageList<Resource> resources = resourceMgr.findResourcesByCriteria(subject, criteria);

        Map<Integer, Resource> map = new HashMap<Integer, Resource>();
        for (Resource r : resources) {
            map.put(r.getId(), r);
        }

        return map;
    }

    DriftChangeSetDTO toDTO(MongoDBChangeSet changeSet) {
        DriftChangeSetDTO dto = new DriftChangeSetDTO();
        dto.setId(changeSet.getId());
        // TODO copy resource id
        dto.setDriftDefinitionId(changeSet.getDriftDefinitionId());
        dto.setVersion(changeSet.getVersion());
        dto.setCtime(changeSet.getCtime());
        dto.setCategory(changeSet.getCategory());

        return dto;
    }

    DriftDTO toDTO(MongoDBChangeSetEntry entry, DriftChangeSetDTO changeSetDTO) {
        DriftDTO dto = new DriftDTO();
        dto.setChangeSet(changeSetDTO);
        dto.setId(entry.getId());
        dto.setCtime(entry.getCtime());
        dto.setPath(entry.getPath());
        dto.setDirectory(entry.getDirectory());
        dto.setCategory(entry.getCategory());

        switch (entry.getCategory()) {
            case FILE_ADDED:
                dto.setNewDriftFile(newDriftFile(entry.getNewFileHash()));
                break;
            case FILE_CHANGED:
                dto.setNewDriftFile(newDriftFile(entry.getNewFileHash()));
                dto.setOldDriftFile(newDriftFile(entry.getOldFileHash()));
                break;
            default:  // FILE_REMOVED
                dto.setOldDriftFile(newDriftFile(entry.getOldFileHash()));
        }
        
        return dto;
    }

    @Override
    public String persistChangeSet(Subject subject, DriftChangeSet<?> changeSet) {
        // if this is a resource level change set we need to fetch the definition so that
        // we can persist the definition name; otherwise, we will not be able to delete
        // this and any other future change sets that belong to the definition.

        // TODO Refactor caller logic of persistChangeSet to pass the definition name
        // It might make sense to break persistChangeSet into two separate methods,
        // something along the lines of persistTemplateChangeSet and
        // persistDefinitionChangeSet where the definition is passed as an argument to
        // the latter. As it stands right now, each plugin implementation will have
        // duplicate logic for determining whether this is a template or definition change
        // set. That logic could be pulled up into DriftManagerBean.

        MongoDBChangeSet newChangeSet = new MongoDBChangeSet();
        newChangeSet.setResourceId(changeSet.getResourceId());
        newChangeSet.setDriftDefinitionId(changeSet.getDriftDefinitionId());
        newChangeSet.setDriftHandlingMode(changeSet.getDriftHandlingMode());
        newChangeSet.setCategory(changeSet.getCategory());
        newChangeSet.setDriftDefinitionId(changeSet.getDriftDefinitionId());
        
        if (!isTemplateChangeSet(changeSet)) {
            DriftDefinition driftDef = getDriftDef(subject, changeSet.getDriftDefinitionId());
            if (driftDef == null) {
                throw new IllegalArgumentException("Cannot persist change set. " +
                        DriftDefinition.class.getSimpleName() + " with id " + changeSet.getDriftDefinitionId() +
                        " cannot be found.");
            }
            newChangeSet.setDriftDefinitionName(driftDef.getName());
        }
        
        for (Drift drift : changeSet.getDrifts()) {
            MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry();
            entry.setPath(drift.getPath());
            entry.setCategory(drift.getCategory());
            // we only need to initialize the newFileHash property here since each drift
            // is going to be a FILE_ADDED entry.
            entry.setNewFileHash(drift.getNewDriftFile().getHashId());
            newChangeSet.add(entry);
        }

        changeSetDAO.save(newChangeSet);

        return newChangeSet.getId();
    }

    private boolean isTemplateChangeSet(DriftChangeSet<?> changeSet) {
        return changeSet.getResourceId() == 0 && changeSet.getDriftDefinitionId() == 0;
    }

    @Override
    public String copyChangeSet(Subject subject, String changeSetId, int driftDefId, int resourceId) {
        // TODO Pass definition name in as an argument
        // We need to store the definition name to support deletes. This method is invoked
        // from DriftManagerBean.updateDriftDefinition when creating a definition from a
        // pinned template. Because the transaction in which the definition is created has
        // not yet been committed when this method is invoked, we cannot look up the
        // definition.

        MongoDBChangeSet changeSet = changeSetDAO.findOne("id", new ObjectId(changeSetId));
        changeSet.setDriftDefinitionId(driftDefId);
        changeSet.setResourceId(resourceId);
        changeSet.setId(new ObjectId());

        changeSetDAO.save(changeSet);

        return changeSet.getId();
    }

    @Override
    public byte[] getDriftFileAsByteArray(Subject subject, String hash) {
        GridFSDBFile file = fileDAO.findById(hash);
        if (file == null) {
            return null;
        }
        return StreamUtil.slurp(file.getInputStream());
    }
    
    private DriftDefinition getDriftDef(Subject subject, int id) {
        DriftManagerLocal driftMgr = LookupUtil.getDriftManager();
        return driftMgr.getDriftDefinition(subject, id);
    }
}
