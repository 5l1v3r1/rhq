<%@ page import="java.io.ByteArrayOutputStream" %>
<%@ page import="java.io.PrintStream" %>
<%@ page import="org.rhq.enterprise.server.test.CoreTestLocal" %>
<%@ page import="org.rhq.enterprise.server.test.DiscoveryTestLocal" %>
<%@ page import="org.rhq.enterprise.server.test.MeasurementTestLocal" %>
<%@ page import="org.rhq.enterprise.server.test.ResourceGroupTestBeanLocal" %>
<%@ page import="org.rhq.enterprise.server.test.SubjectRoleTestBeanLocal" %>
<%@ page import="org.rhq.enterprise.server.cluster.instance.ServerManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.test.ResourceGroupTestBeanLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="javax.naming.NamingException" %>
<%@ page import="org.rhq.core.domain.util.PersistenceUtility" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<head><title>RHQ Test Control Page</title></head>
<body>

[<a href="/Dashboard.do">Go to Dashboard</a>][<a href="sql.jsp">Go to sql.jsp</a>]

<hr>

<%
   CoreTestLocal coreTestBean;
   DiscoveryTestLocal discoveryTestBean;
   MeasurementTestLocal measurementTestBean;
   ResourceGroupTestBeanLocal resourceGroupTestBean;
   SubjectRoleTestBeanLocal subjectRoleTestBean;
   ServerManagerLocal serverManager;
   
   coreTestBean = LookupUtil.getCoreTest();
   discoveryTestBean = LookupUtil.getDiscoveryTest();
   measurementTestBean = LookupUtil.getMeasurementTest();
   resourceGroupTestBean = LookupUtil.getResourceGroupTestBean();
   subjectRoleTestBean = LookupUtil.getSubjectRoleTestBean();
   serverManager = LookupUtil.getServerManager();

   String result = null;
   String mode = pageContext.getRequest().getParameter("mode");
   String failure = null;
   try
   {
      if ("registerTestAgent".equals(mode))
      {
         coreTestBean.registerTestAgent();
      }
      else if ("registerTestPluginAndTypeInfo".equals(mode))
      {
         discoveryTestBean.registerTestPluginAndTypeInfo();
      }
      else if ("removeTestPluginAndTypeInfo".equals(mode))
      {
         discoveryTestBean.removeTestPluginAndTypeInfo();
      }
      else if ("sendTestFullInventoryReport".equals(mode))
      {
         discoveryTestBean.sendTestFullInventoryReport();
      }
      else if ("sendTestRuntimeInventoryReport".equals(mode))
      {
         discoveryTestBean.sendTestRuntimeInventoryReport();
      }
      else if ("sendTestMeasurementReport".equals(mode))
      {
         measurementTestBean.sendTestMeasurementReport();
      }
      else if ("sendNewPlatform".equals(mode))
      {
         String address = request.getParameter("address");
         int servers = Integer.parseInt(request.getParameter("servers"));
         int servicesPerServer = Integer.parseInt(request.getParameter("servicesPerServer"));
         discoveryTestBean.sendNewPlatform(address, servers, servicesPerServer);
      }
      else if ("setupCompatibleGroups".equals(mode))
      {
         resourceGroupTestBean.setupCompatibleGroups();
      }
      else if ("setupUberMixedGroup".equals(mode))
      {
         resourceGroupTestBean.setupUberMixedGroup();
      }
      else if ("startStats".equals(mode))
      {
         coreTestBean.enableHibernateStatistics();
      }
      else if ("addProblemResource".equals(mode))
      {
         measurementTestBean.addProblemResource();
      }
      else if ("setAgentCurrentlyScheduledMetrics".equals(mode))
      {
         String value = pageContext.getRequest().getParameter("v");
         measurementTestBean.setAgentCurrentlyScheduledMetrics(Double.valueOf(value));
      }
      else if ("addSubjectsAndRoles".equals(mode))
      {
         String roleCount = pageContext.getRequest().getParameter("roleCount");
         String usersInRoleCount = pageContext.getRequest().getParameter("usersInRoleCount");
         subjectRoleTestBean.createRolesAndUsers(Integer.parseInt(roleCount), Integer.parseInt(usersInRoleCount));
      }
      else if ("clusterGetIdentity".equals(mode))
      {
         String serverName = serverManager.getIdentity();
         pageContext.setAttribute("serverName", "(serverName = " + serverName + ")");
      }
   }
   catch (Exception e)
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      e.printStackTrace(new PrintStream(baos));
      failure = baos.toString();
   }

   pageContext.setAttribute("executed", mode);
   pageContext.setAttribute("result", result);
   pageContext.setAttribute("failure", failure);
   pageContext.setAttribute("testAgentReported", Boolean.valueOf(coreTestBean.isTestAgentReported()));

%>

<c:if test="${executed != null}">
   <b>Executed <c:out value="${executed}"/>: </b> <c:out value="${result}"/><br>
   <c:if test="${failure != null}">
      <pre style="background-color: yellow;"><c:out value="${failure}"/></pre>
   </c:if>
</c:if>

<h2>Administration</h2>

<c:url var="url" value="/admin/TestControl.jsp?mode=addSubjectsAndRoles"/>
Add Lots of Users and Roles
<form action="<c:out value="${url}"/>" method="get">
   <input type="hidden" name="mode" value="addSubjectsAndRoles"/>
   Number of Roles: <input type="text" name="roleCount" size="5"/><br/>
   Number of Users in each Role: <input type="text" name="usersInRoleCount" size="5"/><br/>
   <input type="submit" value="Send" name="Send"/>
</form>


<ul>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=addSubjectsAndRoles"/>
      <a href="<c:out value="${url}"/>">Report Test Agent</a> (done = <c:out value="${testAgentReported}"/>)</li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=registerTestPluginAndTypeInfo"/>
      <a href="<c:out value="${url}"/>">Register test plugin metadata</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=removeTestPluginAndTypeInfo"/>
      <a href="<c:out value="${url}"/>">Remove test plugin metadata</a></li>
</ul>

<h2>Core</h2>

<ul>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=registerTestAgent"/>
      <a href="<c:out value="${url}"/>">Report Test Agent</a> (done = <c:out value="${testAgentReported}"/>)</li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=registerTestPluginAndTypeInfo"/>
      <a href="<c:out value="${url}"/>">Register test plugin metadata</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=removeTestPluginAndTypeInfo"/>
      <a href="<c:out value="${url}"/>">Remove test plugin metadata</a></li>
</ul>

<h2>Cluster</h2>

<ul>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=clusterGetIdentity"/>
      <a href="<c:out value="${url}"/>">Get Identity</a> <c:out value="${serverName}"/></li>
</ul>

<h2>Inventory</h2>

<ul>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=sendTestFullInventoryReport"/>
      <a href="<c:out value="${url}"/>">Send Full Inventory Report</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=sendTestRuntimeInventoryReport"/>
      <a href="<c:out value="${url}"/>">Send Runtime Inventory Report</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=setupCompatibleGroups"/>
      <a href="<c:out value="${url}"/>">Setup Compatible Groups</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=setupUberMixedGroup"/>
      <a href="<c:out value="${url}"/>">Setup Uber Mixed Group</a></li>
</ul>

<c:url var="url" value="/admin/TestControl.jsp?mode=sendNewPlatform"/>
Send New Platform Inventory Report
<form action="<c:out value="${url}"/>" method="get">
   <input type="hidden" name="mode" value="sendNewPlatform"/>
   Address: <input type="text" name="address" size="30"/><br/>
   Servers: <input type="text" name="servers" size="5"/><br/>
   Services Per Server: <input type="text" name="servicesPerServer" size="5"/><br/>
   <input type="submit" value="Send" name="Send"/>
</form>


<h2>Measurement</h2>

<ul>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=sendTestMeasurementReport"/>
      <a href="<c:out value="${url}"/>">Send Measurement Report</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=addProblemResource"/>
      <a href="<c:out value="${url}"/>">Add problem Resource</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=setAgentCurrentlyScheduledMetrics&v=100"/>
      <a href="<c:out value="${url}"/>">Set RHQ Agent 'CurrentlyScheduleMetrics' to 100</a></li>
  <li><c:url var="url" value="/admin/TestControl.jsp?mode=setAgentCurrentlyScheduledMetrics&v=50"/>
      <a href="<c:out value="${url}"/>">Set RHQ Agent 'CurrentlyScheduleMetrics' to 50</a></li>
</ul>

<h2>Utilities</h2>
<ul>
   <li><c:url var="url" value="/admin/TestControl.jsp?mode=startStats"/>
      <a href="<c:out value="${url}"/>">Start Hibernate Statistics Collection</a></li>
</ul>

</body>
</html>
