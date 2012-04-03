package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentDriftHandler extends AbstractRestBean implements RecentDriftLocal {

    private final Log log = LogFactory.getLog(RecentDriftHandler.class);

    @EJB
    private DriftManagerLocal driftManager;

    @Override
    public StreamingOutput recentDrift(final String categories, final Integer snapshot, final String path,
        final String definitionName, final Long startTime, final Long endTime, final UriInfo uriInfo,
        final HttpServletRequest request, final HttpHeaders headers) {

        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                GenericDriftCriteria criteria  = new GenericDriftCriteria();
                // Need to fetch the change set so that we can the definition id which is
                // needed to build the details url.
                criteria.fetchChangeSet(true);
                criteria.addFilterChangeSetStartVersion(1);// always start at 1 for this report

                if(startTime != null){
                    criteria.addFilterStartTime(startTime);
                }
                if(endTime != null){
                    criteria.addFilterEndTime(endTime);
                }
                // lets default the end time for them to now if they didnt enter it
                if(startTime != null && endTime == null){
                    Date today = new Date();
                    criteria.addFilterEndTime(today.getTime());
                }


                if(snapshot != null) {
                    log.info("Drift Snapshot version Filter set for: " + snapshot);
                    criteria.addFilterChangeSetEndVersion(snapshot);
                }
                if(path != null) {
                    log.info("Drift Path Filter set for: " + path);
                    criteria.addFilterPath(path);
                }
                if(definitionName != null) {
                    log.info("Drift Definition Filter set for: " + definitionName);
                    //@todo: drift sorting is done in the resultset after no criteria for definition
                }

                criteria.addFilterCategories(getCategories());

                CriteriaQueryExecutor<DriftComposite, DriftCriteria> queryExecutor =
                        new CriteriaQueryExecutor<DriftComposite, DriftCriteria>() {
                            @Override
                            public PageList<DriftComposite> execute(DriftCriteria criteria) {

                                return driftManager.findDriftCompositesByCriteria(caller, criteria);
                            }
                        };

                CriteriaQuery<DriftComposite, DriftCriteria> query =
                        new CriteriaQuery<DriftComposite, DriftCriteria>(criteria, queryExecutor);

                stream.write((getHeader() + "\n").getBytes());
                if (definitionName != null) {
                    for (DriftComposite alert : query) {
                        if(alert.getDriftDefinitionName() != null && alert.getDriftDefinitionName().contains(
                            definitionName)){
                            String record = toCSV(alert)  + "\n";
                            stream.write(record.getBytes());
                        }
                    }
                } else {
                    for (DriftComposite alert : query) {
                        String record = toCSV(alert)  + "\n";
                        stream.write(record.getBytes());
                    }
                }
            }

            private DriftCategory[] getCategories() {
                List<DriftCategory> driftCategoryList = new ArrayList<DriftCategory>(10);
                String categoryArray[] = categories.split(",");
                for (String category : categoryArray) {
                    log.info("DriftCategories Filter set for: " + category);
                    driftCategoryList.add(DriftCategory.valueOf(category.toUpperCase()));
                }
                return (driftCategoryList.toArray(new DriftCategory[driftCategoryList.size()]));
            }

            private String toCSV(DriftComposite drift) {
                return formatDateTime(drift.getDrift().getCtime()) + "," +
                        cleanForCSV(drift.getDriftDefinitionName()) + "," +
                        drift.getDrift().getChangeSet().getVersion()+","+
                        drift.getDrift().getCategory() + "," +
                        drift.getDrift().getPath() + "," +
                        cleanForCSV(drift.getResource().getName()) +","+
                        cleanForCSV(ReportFormatHelper.parseAncestry(drift.getResource().getAncestry())) + "," +
                        getDetailsURL(drift);
            }

            private String getHeader(){
                return "Creation Time,Definition,Snapshot,Category,Path,Resource,Ancestry,Details URL";
            }

            private String getDetailsURL(DriftComposite driftDetails) {
                String protocol;
                if (request.isSecure()) {
                    protocol = "https";
                } else {
                    protocol = "http";
                }

                return protocol + "://" + request.getServerName() + ":" + request.getServerPort() +
                    "/coregui/#Resource/" + driftDetails.getResource().getId() + "/Drift/Definitions/" +
                    driftDetails.getDrift().getChangeSet().getDriftDefinitionId() + "/Drift/0id_" +
                    driftDetails.getDrift().getId();
            }

        };

    }

}
