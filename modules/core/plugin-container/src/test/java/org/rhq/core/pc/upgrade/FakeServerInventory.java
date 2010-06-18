/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.pc.upgrade;

import static org.testng.Assert.fail;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class FakeServerInventory {

    private Resource platform;
    private Map<String, Resource> resourceStore = new HashMap<String, Resource>();
    private int counter;

    private static final Comparator<Resource> ID_COMPARATOR = new Comparator<Resource>() {
        public int compare(Resource o1, Resource o2) {
            return o1.getId() - o2.getId();
        }
    };
    
    private static final Comparator<Resource> RESOURCE_TYPE_COMPARATOR = new Comparator<Resource>() {
        public int compare(Resource o1, Resource o2) {
            return o1.getResourceType().equals(o2.getResourceType()) ? 0 : o1.getId() - o2.getId();
        }
    };
    
    //need to synchronize, because resource upgrade is async and can overlap with
    //resource discovery.
    public synchronized CustomAction mergeInventoryReport(final InventoryStatus requiredInventoryStatus) {
        return new CustomAction("updateServerSideInventory") {
            public Object invoke(Invocation invocation) throws Throwable {
                InventoryReport inventoryReport = (InventoryReport) invocation.getParameter(0);
                
                for(Resource res : inventoryReport.getAddedRoots()) {
                    if (res.getParentResource() == Resource.ROOT) {
                        platform = fakePersist(res, requiredInventoryStatus, new HashSet<String>());
                        break;
                    }
                }
                return getSyncInfo();
            }  
        };
    }
    
    public synchronized CustomAction upgradeResources() {
        return new CustomAction("upgradeServerSideInventory") {
            @SuppressWarnings({"serial", "unchecked"})
            public Object invoke(Invocation invocation) throws Throwable {
                Set<ResourceUpgradeReport> reports = (Set<ResourceUpgradeReport>) invocation.getParameter(0);

                for (final ResourceUpgradeReport report : reports) {
                    Resource resource = findResource(platform, new Resource() {
                        public int getId() {
                            return report.getResourceId();
                        }
                    }, ID_COMPARATOR);
                    if (resource != null) {
                        if (report.getNewDescription() != null) {
                            resource.setDescription(report.getNewDescription());
                        }
                        if (report.getNewName() != null) {
                            resource.setName(report.getNewName());
                        }
                        
                        if (report.getNewResourceKey() != null) {
                            resource.setResourceKey(report.getNewResourceKey());
                        }
                    }
                }
                return true;
            }
        };
    }
    
    public synchronized CustomAction getResources() {
        return new CustomAction("getResources") {
            @SuppressWarnings("unchecked")
            public Object invoke(Invocation invocation) throws Throwable {
                Set<Integer> resourceIds = (Set<Integer>)invocation.getParameter(0);
                boolean includeDescendants = (Boolean) invocation.getParameter(1);
                
                return getResources(resourceIds, includeDescendants);
            }
        };        
    }
    
    @SuppressWarnings("serial")
    public synchronized Set<Resource> findResourcesByType(final ResourceType type) {
        Set<Resource> result = new HashSet<Resource>();
        if (platform != null) {
            findResources(platform, new Resource() {
                public ResourceType getResourceType() {
                    return type;
                }
            }, result, RESOURCE_TYPE_COMPARATOR);
        }
        return result;
    }
    
    @SuppressWarnings("serial")
    private Set<Resource> getResources(Set<Integer> resourceIds, boolean includeDescendants) {        
        Set<Resource> result = new HashSet<Resource>();
        
        for(final Integer id : resourceIds) {
            Resource r = findResource(platform, new Resource() {
                public int getId() {
                    return id;
                }
            }, ID_COMPARATOR);
            if (r != null) {
                result.add(r);
                
                if (includeDescendants) {
                    for(Resource child : r.getChildResources()) {
                        result.addAll(getResources(Collections.singleton(child.getId()), true));
                    }
                }
            }
        }
        
        return result;
    }
    
    private Resource fakePersist(Resource agentSideResource, InventoryStatus requiredInventoryStatus, Set<String> inProgressUUIds) {
        Resource persisted = resourceStore.get(agentSideResource.getUuid());
        if (!inProgressUUIds.add(agentSideResource.getUuid())) {
            return persisted;
        }
        if (persisted == null) {
            persisted = new Resource();
            persisted.setId(++counter);
            persisted.setUuid(agentSideResource.getUuid());
            resourceStore.put(persisted.getUuid(), persisted);
        }
        persisted.setAgent(agentSideResource.getAgent());
        persisted.setCurrentAvailability(agentSideResource.getCurrentAvailability());
        persisted.setDescription(agentSideResource.getDescription());
        persisted.setName(agentSideResource.getName());
        persisted.setPluginConfiguration(agentSideResource.getPluginConfiguration().clone());
        persisted.setResourceConfiguration(agentSideResource.getResourceConfiguration().clone());
        persisted.setVersion(agentSideResource.getVersion());
        persisted.setInventoryStatus(requiredInventoryStatus);
        persisted.setResourceKey(agentSideResource.getResourceKey());
        persisted.setResourceType(agentSideResource.getResourceType());
        
        Resource parent = agentSideResource.getParentResource();
        if (parent != null && parent != Resource.ROOT) {
            persisted.setParentResource(fakePersist(agentSideResource.getParentResource(), requiredInventoryStatus, inProgressUUIds));
        } else {
            persisted.setParentResource(parent);
        }

        Set<Resource> childResources = new HashSet<Resource>();
        persisted.setChildResources(childResources);
        for(Resource child : agentSideResource.getChildResources()) {
            childResources.add(fakePersist(child, requiredInventoryStatus, inProgressUUIds));
        }
        
        inProgressUUIds.remove(agentSideResource.getUuid());
        
        return persisted;
    }
    
    private ResourceSyncInfo getSyncInfo() {
        return platform != null ? convert(platform) : null;
    }
    
    private static ResourceSyncInfo convert(Resource root) {
        try {
            ResourceSyncInfo ret = new ResourceSyncInfo();
            
            Class<ResourceSyncInfo> clazz = ResourceSyncInfo.class;

            getPrivateField(clazz, "id").set(ret, root.getId());
            getPrivateField(clazz, "uuid").set(ret, root.getUuid());
            getPrivateField(clazz, "mtime").set(ret, root.getMtime());
            getPrivateField(clazz, "inventoryStatus").set(ret, root.getInventoryStatus());
            getPrivateField(clazz, "parent").set(ret, null);
                        
            Set<ResourceSyncInfo> children = new LinkedHashSet<ResourceSyncInfo>();
            for(Resource child : root.getChildResources()) {
                ResourceSyncInfo syncChild = convert(child);
                getPrivateField(clazz, "parent").set(syncChild, ret);
                
                children.add(convert(child));
            }            
            getPrivateField(clazz, "childSyncInfos").set(ret, children);
            
            return ret;
        } catch (Exception e) {
            fail("Failed to convert resource " + root + " to a ResourceSyncInfo. This should not happen.", e);
            return null;
        }
    }    
    
    private static Field getPrivateField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        
        return field;
    }
    
    private static Resource findResource(Resource root, Resource template, Comparator<Resource> comparator) {
        if (root == null) return null;
        if (comparator.compare(root, template) == 0) {
            return root;
        } else {
            for(Resource child : root.getChildResources()) {
                Resource found = findResource(child, template, comparator);
                if (found != null) return found;
            }
        }
        
        return null;
    }    
    
    private static void findResources(Resource root, Resource template, Set<Resource> result, Comparator<Resource> comparator) {
        if (root == null) return;
        if (comparator.compare(root, template) == 0) {
            result.add(root);
        } else {
            for(Resource child : root.getChildResources()) {
                findResources(child, template, result, comparator);
            }
        }        
    }
}
