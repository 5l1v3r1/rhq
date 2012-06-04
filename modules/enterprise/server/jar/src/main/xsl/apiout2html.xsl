<!--
 Taken from https://github.com/pilhuhn/swagger-core/blob/apt/modules/java-jaxrs-apt/src/main/xsl/apiout2html.xsl
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="basePath"/>
  <xsl:strip-space elements="a"/>

  <xsl:template match="/api">
    <html>
      <head>
        <title>Api documentation</title>
        <style type="text/css">
          h2 {background-color:#ADD8E6   }
          h3 {background-color:#C0C0C0   }
          th {font-weight:bold; font-size:120% }
          em {font-style:italic}
        </style>
      </head>
      <body>
        <h1>REST-api documentation</h1>
        <em>Base path (if not otherwise specified) : <xsl:value-of select="$basePath"/></em>
        <h2>Table of contents</h2>
        <ul>
          <xsl:for-each select="class">
            <xsl:sort select="@basePath"/>
            <xsl:sort select="@path"/>
            <li>
              <xsl:element name="a">
                <xsl:attribute name="href">#<xsl:value-of select="@path"/></xsl:attribute>
                 <xsl:if test="@basePath">
                   <xsl:value-of select="@basePath"/>
                 </xsl:if>/<xsl:value-of select="@path"/>
              </xsl:element>
            </li>
          </xsl:for-each>
        </ul>
        <xsl:apply-templates>
          <xsl:sort select="@basePath"/>
          <xsl:sort select="@path"/>
        </xsl:apply-templates>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="class">
    <xsl:element name="h2">
      <xsl:attribute name="id"><xsl:value-of select="@path"/></xsl:attribute>
      /<xsl:value-of select="@path"/>
      <xsl:if test="@shortDesc">
      : <xsl:value-of select="@shortDesc"/>
      </xsl:if>
    </xsl:element>
    <em><xsl:value-of select="@description"/></em>
    <p/>
    Defining class: <xsl:value-of select="@name"/><br/>
    <br/>
    <xsl:if test="method">
      Methods:<br/>
      <xsl:apply-templates>
        <xsl:sort select="@path"/>
      </xsl:apply-templates>
    </xsl:if>
    <p/>
  </xsl:template>

  <xsl:template match="method">
    <h3><xsl:value-of select="@method"/><xsl:text xml:space="preserve"> </xsl:text><xsl:value-of select="../@path"/>/<xsl:value-of select="@path"/></h3>
    <em><xsl:value-of select="@description"/></em>
    <br/>
    <xsl:choose>
    <xsl:when test="param">
    Parameters:
    <table>
        <tr><th>Name</th><th>P.Type</th><th>Description</th><th>Required</th><th>Type</th><th>Allowed values</th><th>Default value</th></tr>
      <xsl:apply-templates select="param"/>
    </table>
    </xsl:when>
      <xsl:otherwise>
        This method has no parameters
      </xsl:otherwise>
    </xsl:choose>
    <br/>
    Return type: <xsl:value-of select="@returnType"/>
    <p/>
    <xsl:if test="error">
      Error codes:<br/>
      <table>
          <tr>
            <th>Code</th><th>Reason</th>
          </tr>
        <xsl:apply-templates select="error"/>
      </table>
    </xsl:if>
  </xsl:template>

  <xsl:template match="param">
    <tr>
      <td><xsl:value-of select="@name"/></td>
      <td><xsl:value-of select="@paramType"/></td>
      <td><xsl:value-of select="@description"/></td>
      <td><xsl:value-of select="@required"/></td>
      <td><xsl:value-of select="@type"/></td>
      <td><xsl:value-of select="@allowableValues"/></td>
      <td><xsl:value-of select="@defaultValue"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="error">
    <tr>
        <td><xsl:value-of select="@code"/></td>
        <td><xsl:value-of select="@reason"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>