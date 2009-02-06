<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.configuration.ConfigurationUpdateStatus" %>
<%@ page import="org.rhq.core.domain.configuration.ResourceConfigurationUpdate" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.configuration.ConfigurationManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    PageList<ResourceConfigurationUpdate> configurationUpdates = configurationManager.getResourceConfigurationUpdates(subject, resourceId, new PageControl(0,100));
%>


{ "events": [

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (ResourceConfigurationUpdate configUpdate : configurationUpdates) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = null;
            switch (configUpdate.getStatus()) {
                case SUCCESS:
                    icon = "/images/icn_config_success.png";
                    break;
                case FAILURE:
                    icon = "/images/icn_config_error.png";
                    break;
                case INPROGRESS:
                    icon = "/images/icn_config.png";
                    break;
            }

            String link = "/rhq/resource/configuration/history.xhtml?id=" + resourceId + "&configId=" + configUpdate.getId();

//            out.write(
//                    "    <event start=\"" + sdf.format(configUpdate.getCreatedTime()) + "\" " +
//                    "title=\"Configuration Change\" \n" +
//                    "link=\"/rhq/resource/configuration/history.xhtml?id=" + resourceId + "&amp;configId=" + configUpdate.getId() + "\" " +
//                    "icon=\"" + icon + "\" >\n" +
//                            "&lt;b&gt;User:&lt;/b&gt; " + configUpdate.getSubjectName() + "&lt;br/&gt;" +
//                            "&lt;b&gt;Status:&lt;/b&gt; " + configUpdate.getStatus() +
//                    "    </event>\n\n");
            %>

{ "start" : "<%=sdf.format(configUpdate.getCreatedTime())%>",
  "title" : "Configuration Change",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <%=configUpdate.getSubjectName()%><br/><b>Status:</b> <%=configUpdate.getStatus()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(configUpdate.getStatus() != ConfigurationUpdateStatus.FAILURE ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }
    %>

]}