/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.service.ManagementService;

/**
 * Get information about RHQ's underlying AS Server.
 */
public class CoreServer implements CoreServerMBean {
    private static final String PRODUCT_INFO_PROPERTIES_RESOURCE_PATH =
            "org/rhq/enterprise/server/core/ProductInfo.properties";

    private final Log log = LogFactory.getLog(CoreServer.class);

    /**
     * The name of the version file as found in this class's classloader
     */
    private static final String VERSION_FILE = "rhq-server-version.properties";

    /**
     * Version property whose value is the product version.
     */
    private static final String PROP_PRODUCT_VERSION = "Product-Version";

    /**
     * Version property whose value is the source code revision number used to make the build.
     */
    private static final String PROP_BUILD_NUMBER = "Build-Number";

    /**
     * Version property whose value is the date when this version of the product was built.
     */
    private static final String PROP_BUILD_DATE = "Build-Date";

    private Properties buildProps;

    private Date bootTime;

    protected void start() throws Exception {
        this.buildProps = loadBuildProperties();
        this.bootTime = new Date();

        // make sure our log file has an indication of the version of this server
        String version = getVersion();
        String buildNumber = getBuildNumber();
        String buildDate = this.buildProps.getProperty(PROP_BUILD_DATE, "?");
        log.info("Version=[" + version + "], Build Number=[" + buildNumber + "], Build Date=[" + buildDate + "]");
    }

    public String getName() {
        return "RHQ Server";
    }

    @Override
    public String getVersion() {
        return this.buildProps.getProperty(PROP_PRODUCT_VERSION, "?");
    }

    @Override
    public String getBuildNumber() {
        return this.buildProps.getProperty(PROP_BUILD_NUMBER, "?");
    }

    @Override
    public Date getBootTime() {
        return bootTime;
    }

    @Override
    public File getInstallDir() {
        String rhqHome = System.getProperty("rhq.server.home");
        if (rhqHome != null) {
            return new File(rhqHome);
        }

        // I think that sysprop should always be set, but just in case, fallback on using
        // logDir explicitly - log dir is always under our own installation directory
        File homeDir = new File(getServerEnvironmentAttribute("logDir"));
        return homeDir.getParentFile(); // logDir is "rhq-install-dir/logs", so the install dir is .. from there
    }

    @Override
    public File getJBossServerHomeDir() {
        File baseDir = new File(getServerEnvironmentAttribute("baseDir"));
        return baseDir;
    }

    @Override
    public File getJBossServerDataDir() {
        File dataDir = new File(getServerEnvironmentAttribute("dataDir"));
        return dataDir;
    }

    @Override
    public File getJBossServerTempDir() {
        File tempDir = new File(getServerEnvironmentAttribute("tempDir"));
        return tempDir;
    }

    @Override
    public File getEarDeploymentDir() {
        ModelControllerClient mcc = ManagementService.getClient();
        try {
            DeploymentJBossASClient client = new DeploymentJBossASClient(mcc);
            String earPath = client.getDeploymentPath(RHQConstants.EAR_FILE_NAME);
            return new File(earPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                mcc.close();
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    public ProductInfo getProductInfo() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(PRODUCT_INFO_PROPERTIES_RESOURCE_PATH);
        if (inputStream != null) {
            Properties props = new Properties();
            try {
                try {
                    props.load(inputStream);
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load product info properties from class loader resource ["
                        + PRODUCT_INFO_PROPERTIES_RESOURCE_PATH + "].");
            }
            ProductInfo productInfo = new ProductInfo();
            // TODO: Using reflection below might be nicer.
            productInfo.setBuildNumber(props.getProperty("buildNumber"));
            productInfo.setFullName(props.getProperty("fullName"));
            productInfo.setHelpDocRoot(props.getProperty("helpDocRoot"));
            productInfo.setName(props.getProperty("name"));
            productInfo.setSalesEmail(props.getProperty("salesEmail"));
            productInfo.setShortName(props.getProperty("shortName"));
            productInfo.setSupportEmail(props.getProperty("supportEmail"));
            productInfo.setUrlDomain(props.getProperty("urlDomain"));
            productInfo.setUrl(props.getProperty("url"));
            productInfo.setVersion(props.getProperty("version"));

            HashMap<String, String> helpViewContent = new HashMap<String, String>();

            for (String propertyName : props.stringPropertyNames()) {
                if (propertyName.startsWith("view_help_section")) {
                    helpViewContent.put(propertyName, props.getProperty(propertyName));
                }
            }

            productInfo.setHelpViewContent(helpViewContent);

            return productInfo;
        } else {
            throw new IllegalStateException("Failed to find class loader resource ["
                    + PRODUCT_INFO_PROPERTIES_RESOURCE_PATH + "].");
        }
    }

    private MBeanServer getMBeanServer() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        return mbs;
    }

    private String getServerEnvironmentAttribute(String attributeName) {
        try {
            MBeanServer mbs = getMBeanServer();
            ObjectName name = ObjectNameFactory.create("jboss.as:core-service=server-environment");
            Object value = mbs.getAttribute(name, attributeName);
            return (value != null) ? value.toString() : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Properties loadBuildProperties() {
        Properties buildProps = new Properties();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            InputStream stream = classLoader.getResourceAsStream(VERSION_FILE);
            try {
                buildProps.load(stream);
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            log.fatal("Failed to load [" + VERSION_FILE + "] via class loader [" + classLoader + "]");
        }

        return buildProps;
    }
}