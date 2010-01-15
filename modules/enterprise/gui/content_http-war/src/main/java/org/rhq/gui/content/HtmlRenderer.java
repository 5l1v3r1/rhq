/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.gui.content;

import javax.servlet.http.HttpServletRequest;

public class HtmlRenderer {

    public static String formStart(String title, String currentURI) {
        StringBuffer sb = new StringBuffer();
        formStart(sb, title, currentURI);
        return sb.toString();
    }

    public static void formStart(StringBuffer sb, String title, String currentURI) {
        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<title>Index of " + currentURI + "</title>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<h1>Index of " + currentURI + "</h1>\n");
        sb.append("<table><tr><th><img src=\"" + getIconPath("/icons/blank.gif")
            + "\" alt=\"[ICO]\" width=\"20\" height=\"22\"></th>");
        sb.append("<th>Name</th><th>Last modified</th><th>Size</th></tr>\n");
        sb.append("<tr><th colspan=\"4\"><hr></th></tr>\n");
    }

    public static String formParentLink(String parentURI) {
        StringBuffer sb = new StringBuffer();
        formParentLink(sb, parentURI);
        return sb.toString();
    }

    public static void formParentLink(StringBuffer sb, String parentURI) {
        sb.append("<tr><td valign=\"top\"><img src=\"" + getIconPath("/icons/back.gif")
            + "\" alt=\"[DIR]\" width=\"20\" height=\"22\"></td><td><a href=\"" + parentURI
            + "\">Parent Directory</a></td><td>&nbsp;</td><td align=\"right\">  - </td><td>&nbsp;</td></tr>");
    }

    public static String formEnd() {
        StringBuffer sb = new StringBuffer();
        formEnd(sb);
        return sb.toString();
    }

    public static void formEnd(StringBuffer sb) {
        sb.append("<tr><th colspan=\"4\"><hr></th></tr>\n" + "</table>\n" + "</body></html>\n");
    }

    public static String formDirEntry(HttpServletRequest request, String dirName, String lastMod) {
        StringBuffer sb = new StringBuffer();
        formDirEntry(sb, request, dirName, lastMod);
        return sb.toString();
    }

    public static void formDirEntry(StringBuffer sb, HttpServletRequest request, String dirName, String lastMod) {
        if (!dirName.endsWith("/")) {
            dirName = dirName + "/";
        }
        sb.append("<tr><td valign=\"top\"><img src=\"" + getIconPath("/icons/folder.gif")
            + "\" alt=\"[DIR]\" width=\"20\" height=\"22\"></td>\n");
        sb.append("<td><a href=\"" + getLink(request, dirName) + "\">" + dirName + "</a></td>\n");
        sb.append("<td align=\"right\">" + lastMod + "</td><td align=\"right\">  - </td><td>&nbsp;</td></tr>\n");
    }

    public static String formFileEntry(HttpServletRequest request, String fileName, String lastModDate, long fileSize) {
        StringBuffer sb = new StringBuffer();
        formFileEntry(sb, request, fileName, lastModDate, fileSize);
        return sb.toString();
    }

    public static void formFileEntry(StringBuffer sb, HttpServletRequest request, String fileName, String lastMod,
        long fileSize) {

        String fSize = new Long(fileSize).toString();
        if (fileSize < 0) {
            fSize = "-";
        }
        sb.append("<tr><td valign=\"top\"><img src=\"" + getIconPath("/icons/unknown.gif")
            + "\" alt=\"[DIR]\" width=\"20\" height=\"22\"></td>\n");
        sb.append("<td><a href=\"" + getLink(request, fileName) + "\">" + fileName + "</a></td>\n");
        sb.append("<td align=\"right\">" + lastMod + "</td><td align=\"right\">" + fSize
            + "</td><td>&nbsp;</td></tr>\n");
    }

    protected static String getIconPath(String path) {
        if (!ContentHTTPServlet.CONTENT_URI.endsWith("/")) {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
        return ContentHTTPServlet.CONTENT_URI + path;
    }

    protected static String getLink(HttpServletRequest request, String path) {
        String reqUri = request.getRequestURI();
        if (!reqUri.endsWith("/")) {
            reqUri = reqUri + "/"; // we need to ensure this ends with a "/"
        }
        if (path.startsWith("/")) {
            path = path.substring(1); // this is just to make the link look nice, it's not "needed"
        }
        return reqUri + path;
    }
}
