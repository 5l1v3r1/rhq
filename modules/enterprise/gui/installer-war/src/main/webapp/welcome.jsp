<%@ page contentType="text/html" %>

<%@page import="org.rhq.enterprise.installer.ServerInformation"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>

   <f:subview id="header">
      <jsp:include page="/header.jsp" flush="true"/>
   </f:subview>

   <c:if test="<%= new ServerInformation().isFullyDeployed() %>">
      <p align="center"><h:outputText value="#{bundle.alreadyInstalled}" /></p>
      <table align="center">
         <tr>
            <td align="center">
               <h:graphicImage id="pleasewait-image" url="/images/pleasewait.gif" alt="..."/><br/>
            </td>
         </tr>
         <tr>
            <td align="center" id="progressBarMessage">
               <h:outputText styleClass="small" value="#{bundle.starting}" />
            </td>
         </tr>
      </table>
   </c:if>
   
   <c:if test="<%= !new ServerInformation().isFullyDeployed() %>">
      <p align="center"><h:outputText value="#{bundle.welcomeMessage}" /></p>
      <p align="center">
         <h:outputLink value="start.jsf"><h:outputText value="#{bundle.startInstallingLink}" /></h:outputLink>
      </p>
   </c:if>

   <hr>

   <p align="center">
      <h:outputText value="#{bundle.introduceHelpDocs}"/>
   </p>
   
   <table align="center"><tr><td>
   <ul>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocJonServerInstallGuide}"><h:outputText value="#{bundle.helpDocJonServerInstallGuideLabel}"/></h:outputLink></li>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocJonGuiConsoleUsersGuide}"><h:outputText value="#{bundle.helpDocJonGuiConsoleUsersGuideLabel}"/></h:outputLink></li>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocJonServerUsersGuide}"><h:outputText value="#{bundle.helpDocJonServerUsersGuideLabel}"/></h:outputLink></li>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocJonAgentUsersGuide}"><h:outputText value="#{bundle.helpDocJonAgentUsersGuideLabel}"/></h:outputLink></li>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocFaq}"><h:outputText value="#{bundle.helpDocFaqLabel}"/></h:outputLink></li>
   </ul>
   </td></tr></table>

   </body>
   
</html>

</f:view>