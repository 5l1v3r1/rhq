package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import com.wordnik.swagger.annotations.Api;
import org.jboss.resteasy.annotations.GZIP;

@Path("/configurationHistory")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The configuration history report")
public interface ConfigurationHistoryLocal {

    @GZIP
    @GET
    @Produces({"text/csv"})
    StreamingOutput configurationHistory(@Context HttpServletRequest request);
}
