/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.LazyInitializationException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Mazzitelli
 */
@Test
public class ServerPluginsBeanTest extends AbstractEJB3Test {
    private ServerPluginsLocal serverPluginsBean;

    @BeforeMethod
    public void beforeMethod() {
        TestGenericServerPluginService pluginService;
        pluginService = new TestGenericServerPluginService();
        prepareCustomServerPluginService(pluginService);

        serverPluginsBean = LookupUtil.getServerPlugins();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {

        EntityManager em = null;

        try {
            unprepareServerPluginService();
        } catch (Exception e) {
        }

        try {
            getTransactionManager().begin();
            em = getEntityManager();

            Query q = em.createQuery("SELECT p FROM Plugin p WHERE p.name LIKE 'serverplugintest%'");
            List<Plugin> doomed = q.getResultList();
            for (Plugin plugin : doomed) {
                em.remove(plugin);
            }

            getTransactionManager().commit();
            em.close();
            em = null;
        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        } finally {
            if (null != em) {
                em.close();
            }
        }
    }

    public void testGetPlugins() throws Exception {
        Plugin p1 = registerPlugin(1);
        Plugin p2 = registerPlugin(2);
        List<String> pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(true);
        assert pluginNames.contains(p1.getName()) : pluginNames;
        assert pluginNames.contains(p2.getName()) : pluginNames;
        pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(false);
        assert !pluginNames.contains(p1.getName()) : pluginNames;
        assert !pluginNames.contains(p2.getName()) : pluginNames;

        Plugin plugin = this.serverPluginsBean.getServerPlugin(p1.getName());
        assert plugin.getId() == p1.getId() : plugin;
        assert plugin.getName().equals(p1.getName()) : plugin;
        assetLazyInitializationException(plugin);

        plugin = this.serverPluginsBean.getServerPluginRelationships(plugin);
        assert plugin.getPluginConfiguration() != null;
        assert plugin.getScheduledJobsConfiguration() != null;
        assert plugin.getPluginConfiguration().equals(p1.getPluginConfiguration());
        assert plugin.getScheduledJobsConfiguration().equals(p1.getScheduledJobsConfiguration());

        List<Plugin> plugins = this.serverPluginsBean.getServerPlugins();
        assert plugins.contains(p1) : plugins;
        assert plugins.contains(p2) : plugins;
        assetLazyInitializationException(plugins.get(0));
        assetLazyInitializationException(plugins.get(1));

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        plugins = this.serverPluginsBean.getServerPluginsById(ids);
        assert plugins.size() == 2 : plugins;
        assert plugins.contains(p1) : plugins;
        assert plugins.contains(p2) : plugins;
        assetLazyInitializationException(plugins.get(0));
        assetLazyInitializationException(plugins.get(1));
    }

    public void testDisableEnablePlugins() throws Exception {
        Plugin p1 = registerPlugin(1);
        Plugin p2 = registerPlugin(2);

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        List<Plugin> disabled = this.serverPluginsBean.disableServerPlugins(getOverlord(), ids);
        assert disabled.size() == 2 : disabled;
        assert disabled.contains(p1) : disabled;
        assert disabled.contains(p2) : disabled;
        assert disabled.get(0).getStatus() == PluginStatusType.INSTALLED; // still installed
        assert disabled.get(1).getStatus() == PluginStatusType.INSTALLED; // still installed
        List<String> pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(false);
        assert pluginNames.contains(p1.getName()) : pluginNames;
        assert pluginNames.contains(p2.getName()) : pluginNames;
        pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(true);
        assert !pluginNames.contains(p1.getName()) : pluginNames;
        assert !pluginNames.contains(p2.getName()) : pluginNames;

        // re-enable them
        this.serverPluginsBean.enableServerPlugins(getOverlord(), ids);
        pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(true);
        assert pluginNames.contains(p1.getName()) : pluginNames;
        assert pluginNames.contains(p2.getName()) : pluginNames;
        pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(false);
        assert !pluginNames.contains(p1.getName()) : pluginNames;
        assert !pluginNames.contains(p2.getName()) : pluginNames;

        // make sure none of these enable/disable settings lost our config
        Plugin plugin = this.serverPluginsBean.getServerPlugin(p1.getName());
        plugin = this.serverPluginsBean.getServerPluginRelationships(plugin);
        assert plugin.getPluginConfiguration() != null; // no LazyInitException should be thrown!
        assert plugin.getPluginConfiguration().equals(p1.getPluginConfiguration());
        plugin = this.serverPluginsBean.getServerPlugin(p2.getName());
        plugin = this.serverPluginsBean.getServerPluginRelationships(plugin);
        assert plugin.getPluginConfiguration() != null; // no LazyInitException should be thrown!
        assert plugin.getPluginConfiguration().equals(p1.getPluginConfiguration());
    }

    public void testUndeployPlugins() throws Exception {

        Plugin p1 = registerPlugin(1);
        Plugin p2 = registerPlugin(2);

        List<Integer> ids = new ArrayList<Integer>(2);
        ids.add(p1.getId());
        ids.add(p2.getId());
        List<Plugin> undeployed = this.serverPluginsBean.undeployServerPlugins(getOverlord(), ids);
        assert undeployed.size() == 2 : undeployed;
        assert undeployed.contains(p1) : undeployed;
        assert undeployed.contains(p2) : undeployed;
        assert undeployed.get(0).getStatus() == PluginStatusType.DELETED;
        assert undeployed.get(1).getStatus() == PluginStatusType.DELETED;

        List<String> pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(false);
        assert !pluginNames.contains(p1.getName()) : "deleted plugins should not be returned even here" + pluginNames;
        assert !pluginNames.contains(p2.getName()) : "deleted plugins should not be returned even here" + pluginNames;
        pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(true);
        assert !pluginNames.contains(p1.getName()) : pluginNames;
        assert !pluginNames.contains(p2.getName()) : pluginNames;

        // re-install them - need to implement this feature
        // this.serverPluginsBean.enableServerPlugins(getOverlord(), ids);
        // pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(true);
        // assert pluginNames.contains(p1.getName()) : pluginNames;
        // assert pluginNames.contains(p2.getName()) : pluginNames;
        // assert undeployed.get(0).getStatus() == PluginStatusType.INSTALLED;
        // assert undeployed.get(1).getStatus() == PluginStatusType.INSTALLED;
        // pluginNames = this.serverPluginsBean.getPluginNamesByEnabled(false);
        // assert !pluginNames.contains(p1.getName()) : pluginNames;
        // assert !pluginNames.contains(p2.getName()) : pluginNames;
    }

    private Plugin registerPlugin(int index) throws Exception {
        Plugin plugin = new Plugin(0, "serverplugintest" + index, "path", "displayName", true,
            PluginStatusType.INSTALLED, "description", "help", "md5", "version", "ampsVersion",
            PluginDeploymentType.SERVER, createPluginConfiguration(), createScheduledJobsConfiguration(), System
                .currentTimeMillis(), System.currentTimeMillis());

        plugin = this.serverPluginsBean.registerPlugin(getOverlord(), plugin, null);
        assert plugin.getId() > 0;
        assert plugin.isEnabled() == true;
        assert plugin.getDeployment() == PluginDeploymentType.SERVER;
        assert plugin.getStatus() == PluginStatusType.INSTALLED;
        assert plugin.getPluginConfiguration() != null;
        assert plugin.getScheduledJobsConfiguration() != null;
        return plugin;
    }

    private void assetLazyInitializationException(Plugin plugin) {
        try {
            plugin.getPluginConfiguration().toString();
            assert false : "Should have thrown a lazy-initialization exception - we didn't load config";
        } catch (LazyInitializationException ok) {
        }
        try {
            plugin.getScheduledJobsConfiguration().toString();
            assert false : "Should have thrown a lazy-initialization exception - we didn't load config";
        } catch (LazyInitializationException ok) {
        }
    }

    private Configuration createPluginConfiguration() {
        Configuration config = new Configuration();
        config.put(new PropertySimple("simpleprop1", "simpleprop1value"));
        return config;
    }

    private Configuration createScheduledJobsConfiguration() {
        Configuration config = new Configuration();
        PropertyMap map = new PropertyMap("jobname");
        config.put(map);
        map.put(new PropertySimple("methodName", "methodNameValue"));
        return config;
    }

    private Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }
}
