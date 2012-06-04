<!--
  ~ RHQ Management Platform
  ~ Copyright (C) 2005-2012 Red Hat, Inc.
  ~ All rights reserved.
  ~
  ~ This program is free software; you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation version 2 of the License.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program; if not, write to the Free Software
  ~ Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  -->

<!--
 Taken from https://github.com/pilhuhn/swagger-core/blob/apt/modules/java-jaxrs-apt/src/main/xsl/apiout2html.xsl
 and modified for docbook
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://docbook.org/ns/docbook"
    xml:lang="en">

  <xsl:param name="basePath"/>
  <xsl:output xml:lang="en"/>

  <xsl:template match="/api">
    <xsl:element name="section">
      <xsl:attribute name="version">5.0</xsl:attribute>
      <!--<xsl:attribute name="xmlns">http://docbook.org/ns/docbook</xsl:attribute>-->
      <title>REST Api documentation</title>
      <subtitle>Base path (if not otherwise specified) : <xsl:value-of select="$basePath"/></subtitle>
      <toc>
        <xsl:for-each select="class">
          <xsl:sort select="@basePath"/>
          <xsl:sort select="@path"/>
          <xsl:element name="tocentry">
            <link>
            <xsl:attribute name="linkend">
              <xsl:value-of select="@path"/>
            </xsl:attribute>
            <xsl:if test="@basePath">
              <xsl:value-of select="@basePath"/>
            </xsl:if>
            /<xsl:value-of select="@path"/>
            </link>
          </xsl:element>
        </xsl:for-each>
      </toc>
      <xsl:apply-templates>
        <xsl:sort select="@basePath"/>
        <xsl:sort select="@path"/>
      </xsl:apply-templates>
    </xsl:element>
  </xsl:template>

  <xsl:template match="class">
    <xsl:element name="section">
      <xsl:attribute name="xml:id">
        <xsl:value-of select="@path"/>
      </xsl:attribute>
      <title>
        /<xsl:value-of select="@path"/>
        <xsl:if test="@shortDesc">
        : <xsl:value-of select="@shortDesc"/>
        </xsl:if>
      </title>
      <subtitle><xsl:value-of select="@description"/></subtitle>
      <simpara>
        Defining class:
        <xsl:value-of select="@name"/>
      </simpara>
      <para>
        <xsl:if test="method">
          <itemizedlist>
            <title>Methods</title>
            <xsl:apply-templates/>
          </itemizedlist>
        </xsl:if>
      </para>
    </xsl:element>

  </xsl:template>

  <xsl:template match="method">
    <listitem>
      <simpara>
        <emphasis role="bold"><xsl:value-of select="@method"/><xsl:text xml:space="preserve"> </xsl:text><xsl:value-of
          select="../@path"/>/<xsl:value-of select="@path"/>
        </emphasis>
      </simpara>
      <!--<subtitle><xsl:value-of select="@description"/></subtitle>-->
      <xsl:choose>
        <xsl:when test="param">
          <table>
            <title>Parameters:</title>
            <tr>
              <th>Name</th>
              <th>P.Type</th>
              <th>Description</th>
              <th>Required</th>
              <th>Type</th>
              <th>Allowed values</th>
              <th>Default value</th>
            </tr>
            <xsl:apply-templates select="param"/>
          </table>
        </xsl:when>
        <xsl:otherwise>
          <simpara>
            This method has no parameters
          </simpara>
        </xsl:otherwise>
      </xsl:choose>
      <simpara>
        Return type:
        <xsl:value-of select="@returnType"/>
      </simpara>
      <xsl:if test="error">
        <table>
          <title>Error codes:</title>
          <tr>
              <th>Code</th>
              <th>Reason</th>
            </tr>
          <xsl:apply-templates select="error"/>
        </table>
      </xsl:if>
    </listitem>
  </xsl:template>

  <xsl:template match="param">
    <tr>
      <td>
        <xsl:choose>
          <xsl:when test="@name">
          <xsl:value-of select="@name"/>
          </xsl:when>
          <xsl:otherwise><emphasis>implicit</emphasis></xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:value-of select="@paramType"/>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="@description">
          <xsl:value-of select="@description"/>
          </xsl:when>
          <xsl:otherwise><emphasis>none</emphasis></xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:value-of select="@required"/>
      </td>
      <td>
        <xsl:value-of select="@type"/>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="@allowableValues">
            <xsl:value-of select="@allowableValues"/>
          </xsl:when>
          <xsl:otherwise><emphasis>-all-</emphasis></xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="@defaultValue">
            <xsl:value-of select="@defaultValue"/>
          </xsl:when>
          <xsl:otherwise><emphasis>none</emphasis></xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="error">
    <tr>
      <td>
        <xsl:value-of select="@code"/>
      </td>
      <td>
        <xsl:value-of select="@reason"/>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>