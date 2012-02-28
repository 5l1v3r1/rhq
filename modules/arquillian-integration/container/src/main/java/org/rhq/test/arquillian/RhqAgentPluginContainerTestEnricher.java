package org.rhq.test.arquillian;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

public class RhqAgentPluginContainerTestEnricher implements TestEnricher {

    @Inject
    @SuiteScoped
    private Instance<PluginContainer> pluginContainer;

    @Override
    public void enrich(Object testCase) {
        boolean pcConfigured = false;
        PluginContainerInstance config = testCase.getClass().getAnnotation(PluginContainerInstance.class);
        if (config != null) {
            configurePc(config);
            pcConfigured = true;
        }
        Set<Field> discoveredResourceFields = new HashSet<Field>();
        Set<Field> resourceComponentFields = new HashSet<Field>();
        
        for (Field f : testCase.getClass().getDeclaredFields()) {
            enrichPluginContainerInstance(testCase, f, pcConfigured);
            collectDiscoveredResourceFields(f, discoveredResourceFields);
            collectResourceComponentFields(f, resourceComponentFields);
        }
        
        for(Field f : discoveredResourceFields) {
            assignDiscoveredResourceField(testCase, f);
        }
        
        for(Field f : resourceComponentFields) {
            assignResourceComponentField(testCase, f);
        }
    }

    @Override
    public Object[] resolve(Method method) {
        return new Object[method.getParameterTypes().length];
    }
    
    private void enrichPluginContainerInstance(Object testCase, Field f, boolean pcConfigured) {
        PluginContainerInstance config = f.getAnnotation(PluginContainerInstance.class);
        if (config != null && f.getType().equals(PluginContainer.class)) {
            if (!pcConfigured) {
                configurePc(config);
                pcConfigured = true;
            }
            f.setAccessible(true);
            try {
                f.set(testCase, pluginContainer.get());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                    "Could not enrich the test class with the plugin container instance", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                    "Could not enrich the test class with the plugin container instance", e);
            }
        }
    }
    
    private void configurePc(PluginContainerInstance config) {
        if (config.discoverServers()) {
            pluginContainer.get().getInventoryManager().executeServerScanImmediately();
        }
        if (config.discoverServices()) {
            pluginContainer.get().getInventoryManager().executeServiceScanImmediately();
        }
    }
    
    private void collectDiscoveredResourceFields(Field f, Set<Field> fields) {
        if (f.isAnnotationPresent(DiscoveredResources.class)) {
            Type type = f.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) type;
                
                if (Set.class.equals(ptype.getRawType())) {
                    Type[] typeArgs = ptype.getActualTypeArguments();
                    if (typeArgs.length == 1 && Resource.class.equals(typeArgs[0])) {
                        fields.add(f);
                    }
                }
            }
        }
    }
    
    private void collectResourceComponentFields(Field f, Set<Field> fields) {
        if (f.isAnnotationPresent(ResourceComponentInstances.class)) {
            Type type = f.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) type;
                
                if (Set.class.equals(ptype.getRawType())) {
                    Type[] typeArgs = ptype.getActualTypeArguments();
                    if (typeArgs.length == 1 && ResourceComponent.class.isAssignableFrom((Class<?>)typeArgs[0])) {
                        fields.add(f);
                    }
                }
            }
        }
    }
    
    private void assignDiscoveredResourceField(Object testCase, Field f) {
        DiscoveredResources config = f.getAnnotation(DiscoveredResources.class);
        
        String pluginName = config.plugin();
        String resourceTypeName = config.resourceType();
        
        ResourceType resourceType = pluginContainer.get().getPluginManager().getMetadataManager().getType(resourceTypeName, pluginName);
        if (resourceType == null) {
            return;
        }
        
        Set<Resource> resources = pluginContainer.get().getInventoryManager().getResourcesWithType(resourceType);
        
        f.setAccessible(true);
        try {
            f.set(testCase, resources);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        }
    }

    private void assignResourceComponentField(Object testCase, Field f) {
        ResourceComponentInstances config = f.getAnnotation(ResourceComponentInstances.class);
        
        String pluginName = config.plugin();
        String resourceTypeName = config.resourceType();
        
        PluginMetadataManager pmm = pluginContainer.get().getPluginManager().getMetadataManager();
        ResourceType resourceType = pmm.getType(resourceTypeName, pluginName);
        if (resourceType == null) {
            return;
        }
        
        String componentClassName = pmm.getComponentClass(resourceType);
        
        //now find out the resource component type declared at the field
        Type type = f.getGenericType();
        if (!(type instanceof ParameterizedType)) {
            return;
        }
        
        ParameterizedType ptype = (ParameterizedType) type;
        
        if (!Set.class.equals(ptype.getRawType())) {
            return;
        }
        
        Type[] typeArgs = ptype.getActualTypeArguments();
        if (typeArgs.length != 1) {
            return;
        }
        
        Type expectedResourceComponentClass = typeArgs[0];
        if (!(expectedResourceComponentClass instanceof Class)) {
            return;
        }

        String expectedTypeName = ((Class<?>) expectedResourceComponentClass).getName();
        if (!componentClassName.equals(expectedTypeName)) {
            return;
        }
            
        InventoryManager im = pluginContainer.get().getInventoryManager();
        Set<Resource> resources = im.getResourcesWithType(resourceType);
        
        Set<ResourceComponent<?>> components = new HashSet<ResourceComponent<?>>(resources.size());
        for(Resource r : resources) {
            components.add(im.getResourceComponent(r));
        }
        
        f.setAccessible(true);
        try {
            f.set(testCase, components);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Could not enrich the test class with the discovered resources on field " + f, e);
        }
    }
}
