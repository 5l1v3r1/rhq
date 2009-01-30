<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>

<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Date"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="MINUTES_LABEL" var="CONST_MINUTES" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="HOURS_LABEL" var="CONST_HOURS" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="DAYS_LABEL" var="CONST_DAYS" />

  <logic:messagesPresent>
  <table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorField"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td colspan="3" align="left" class="ErrorField"><html:errors/></td>
  </tr>
  </table>
  </logic:messagesPresent>

<!--  BASE SERVER CONFIG TITLE -->
  <tr>
    <td colspan="4" class="BlockHeader">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.BaseConfigTab"/>
</tiles:insert>
    </td>
  </tr>
<!--  /  -->

<!--  BASE SERVER CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.BaseURL"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="baseUrl" /></td>
    <td width="20%" class="BlockContent" colspan="2"></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AgentMaxQuietTimeAllowed"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="agentMaxQuietTimeAllowedVal">
          <td class="ErrorField">
            <html:text size="2" property="agentMaxQuietTimeAllowedVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="agentMaxQuietTimeAllowed">
              <html:option value="${CONST_MINUTES}"><fmt:message key="admin.settings.Minutes"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="agentMaxQuietTimeAllowedVal">
          <td class="BlockContent">
            <html:text size="2" property="agentMaxQuietTimeAllowedVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="agentMaxQuietTimeAllowed">
              <html:option value="${CONST_MINUTES}"><fmt:message key="admin.settings.Minutes"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="agentMaxQuietTimeAllowedVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="agentMaxQuietTimeAllowedVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="agentMaxQuietTimeAllowedVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"/>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.EnableAgentAutoUpdate"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td align="left"><html:radio property="enableAgentAutoUpdate" value="true"/><fmt:message key="yesno.true"/></td>
          <td align="left"><html:radio property="enableAgentAutoUpdate" value="false"/><fmt:message key="yesno.false"/></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
<!--  /  -->

<!--  DATA MANAGER CONFIG TITLE -->
  <tr>
    <td colspan="4" class="BlockHeader">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.DataMangerConfigTab"/>
</tiles:insert>
    </td>
  </tr>
<!--  /  -->

<!--  DATA MANAGER CONFIG CONTENTS -->
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.DataMaintInterval"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="maintIntervalVal">
          <td class="ErrorField">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="maintInterval">
              <html:option value="${CONST_HOURS}"><fmt:message key="admin.settings.Hours"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="maintIntervalVal">
          <td class="BlockContent">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="maintInterval">
              <html:option value="${CONST_HOURS}"><fmt:message key="admin.settings.Hours"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="maintIntervalVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="maintIntervalVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="maintIntervalVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"/>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.RtDataPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="rtPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="rtPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="rtPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="rtPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="rtPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="rtPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="rtPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="rtPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="rtPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AlertPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="alertPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="alertPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="alertPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="alertPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="alertPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="alertPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="alertPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="alertPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="alertPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

    <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.EventPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="eventPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="eventPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="eventPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="eventPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="eventPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="eventPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="eventPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="eventPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="eventPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

    <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.TraitPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="traitPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="traitPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="traitPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="traitPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="traitPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="traitPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="traitPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="traitPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="traitPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

    <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AvailPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="availPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="availPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="availPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="availPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="availPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="availPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="availPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="availPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="availPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.Reindex"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td align="left"><html:radio property="reindex" value="true"/><fmt:message key="yesno.true"/></td>
          <td align="left"><html:radio property="reindex" value="false"/><fmt:message key="yesno.false"/></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>

</table>

<!--  BASELINE CONFIG TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.BaselineConfigTab"/>
</tiles:insert>
<!--  /  -->

<!--  Baseline configuration -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <!-- Baseline frequency -->
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.BaselineFrequencyLabel"/></td>
    <td width="30%" class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="baselineFrequencyVal">
          <td class="ErrorField">
            <html:text size="2" property="baselineFrequencyVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="baselineFrequency">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineFrequencyVal">
          <td class="BlockContent">
            <html:text size="2" property="baselineFrequencyVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="baselineFrequency">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="baselineFrequencyVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="baselineFrequencyVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineFrequencyVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td width="20%" class="BlockLabel"/>
    <td width="30%" class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <!-- Baseline dataset admin.settings.BaselineDataSet-->
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.BaselineDataSet"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="baselineDataSetVal">
          <td class="ErrorField">
            <html:text size="2" property="baselineDataSetVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="baselineDataSet">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineDataSetVal">
          <td class="BlockContent">
            <html:text size="2" property="baselineDataSetVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="baselineDataSet">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="baselineDataSetVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="baselineDataSetVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineDataSetVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"/>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>

<!--  SYSTEM-WIDE TITLE -->
  <tr>
    <td colspan="4" class="BlockHeader">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.SystemTab"/>
</tiles:insert>
    </td>
  </tr>
<!--  /  -->

<%

   org.rhq.enterprise.server.util.SystemInformation info = org.rhq.enterprise.server.util.SystemInformation.getInstance();

%>
<!--  SYSTEM-WIDE CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td nowrap="nowrap" width="30%" class="BlockLabel"><fmt:message key="admin.settings.TimeZone"/></td>
    <td width="40%" class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= java.util.TimeZone.getDefault().getDisplayName()%></td>
        </tr>
      </table>
    </td>
    <td width="15%" class="BlockLabel"></td>
    <td width="15%" class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.LocalTime"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL).format(new Date(System.currentTimeMillis()))%></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseURL"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= info.getDatabaseConnectionURL() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseProductName"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= info.getDatabaseProductName() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseProductVersion"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= info.getDatabaseProductVersion() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseDriverName"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= info.getDatabaseDriverName() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.DatabaseDriverVersion"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= info.getDatabaseDriverVersion() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.RawTable"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility.getCurrentRawTable() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td nowrap="nowrap" class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td nowrap="nowrap" class="BlockLabel"><fmt:message key="admin.settings.System.RotationTime"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td nowrap="nowrap" align="left"><%= org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility.getNextRotationTime() %></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>
<!--  /  -->





</table>
<!--  /  -->
