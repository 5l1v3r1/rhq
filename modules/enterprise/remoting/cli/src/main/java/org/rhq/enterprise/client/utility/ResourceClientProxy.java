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
package org.rhq.enterprise.client.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import javax.jws.WebParam;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.Summary;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * Implements a local object that exposes resource related data as
 * if it were local.
 *
 * @author Greg Hinkle
 */
public class ResourceClientProxy {

    private RemoteClient remoteClient;
    private int resourceId;
    private Resource resource;

    Map<String, Object> allProperties = new HashMap<String, Object>();

    // Metadata
    private List<MeasurementDefinition> measurementDefinitions;
    private Map<String, Measurement> measurementMap = new HashMap<String, Measurement>();

    private List<OperationDefinition> operationDefinitions;
    private Map<String, Operation> operationMap = new HashMap<String, Operation>();

    private Map<String, ContentType> contentTypes = new HashMap<String, ContentType>();

    private List<ResourceClientProxy> children;
    private ConfigurationDefinition resourceConfigurationDefinition;
    private ConfigurationDefinition pluginConfigurationDefinition;

    public ResourceClientProxy() {
    }

    public ResourceClientProxy(ResourceClientProxy parentProxy) {
        this.remoteClient = parentProxy.remoteClient;
        this.resourceId = parentProxy.resourceId;
        this.resource = parentProxy.resource;
        this.allProperties = parentProxy.allProperties;
        this.measurementDefinitions = parentProxy.measurementDefinitions;
        this.measurementMap = parentProxy.measurementMap;
        this.children = parentProxy.children;

    }

    public ResourceClientProxy(RemoteClient remoteClient, int resourceId) {
        this.remoteClient = remoteClient;
        this.resourceId = resourceId;

        init();
    }

    @Summary(index = 0)
    public int getId() {
        return resourceId;
    }

    @Summary(index = 1)
    public String getName() {
        return resource.getName();
    }

    public String getDescription() {
        return resource.getDescription();
    }

    @Summary(index = 2)
    public String getVersion() {
        return resource.getVersion();
    }

    @Summary(index = 3)
    public ResourceType getResourceType() {
        return resource.getResourceType();
    }

    public Date getCreatedDate() {
        return new Date(resource.getCtime());
    }

    public Date getModifiedDate() {
        return new Date(resource.getCtime());
    }

    public Measurement getMeasurement(String name) {
        return this.measurementMap.get(name);
    }

    public Collection<Measurement> getMeasurements() {
        return this.measurementMap.values();
    }

    public Map<String, Measurement> getMeasurementMap() {
        return this.measurementMap;
    }

    public Collection<Operation> getOperations() {
        return this.operationMap.values();
    }

    public Map<String, ContentType> getContentTypes() {
        return contentTypes;
    }

    public List<ResourceClientProxy> getChildren() {
        if (children == null) {
            children = new ArrayList<ResourceClientProxy>();

            initChildren();

        }
        return children;
    }

    public ResourceClientProxy getChild(String name) {
        for (ResourceClientProxy child : getChildren()) {
            if (name.equals(child.getName()))
                return child;
        }
        return null;
    }

    public String toString() {
        return "[" + resourceId + "] " + resource.getName() + " (" + resource.getResourceType().getName() + "::"
            + resource.getResourceType().getPlugin() + ")";
    }

    public void init() {

        this.resource = remoteClient.getResourceManagerRemote().getResource(remoteClient.getSubject(), resourceId);

        // Lazy init children, not here
        initMeasurements();
        initOperations();
        initConfigDefs();
        initContent();
    }

    private void initConfigDefs() {
        this.resourceConfigurationDefinition = remoteClient.getConfigurationManagerRemote()
            .getResourceConfigurationDefinitionWithTemplatesForResourceType(remoteClient.getSubject(),
                resource.getResourceType().getId());
        this.pluginConfigurationDefinition = remoteClient.getConfigurationManagerRemote()
            .getPluginConfigurationDefinitionForResourceType(remoteClient.getSubject(),
                resource.getResourceType().getId());
    }

    private void initChildren() {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterParentResourceId(resourceId);
        PageList<Resource> childResources = remoteClient.getResourceManagerRemote().findResourcesByCriteria(
            remoteClient.getSubject(), criteria);

        for (Resource child : childResources) {
            this.children.add(new Factory(remoteClient).getResource(child.getId()));
        }
    }

    private void initMeasurements() {
        MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeName(resource.getResourceType().getName());

        this.measurementDefinitions = remoteClient.getMeasurementDefinitionManagerRemote()
            .findMeasurementDefinitionsByCriteria(remoteClient.getSubject(), criteria);

        this.measurementMap = new HashMap<String, Measurement>();
        for (MeasurementDefinition def : measurementDefinitions) {
            Measurement m = new Measurement(def);

            String name = def.getDisplayName().replaceAll("\\W", "");
            name = decapitalize(name);

            this.measurementMap.put(name, m);
            this.allProperties.put(name, m);
        }
    }

    public void initOperations() {
        OperationDefinitionCriteria criteria = new OperationDefinitionCriteria();
        criteria.addFilterResourceIds(resourceId);
        criteria.fetchParametersConfigurationDefinition(true);
        criteria.fetchResultsConfigurationDefinition(true);

        this.operationDefinitions = remoteClient.getOperationManagerRemote().findOperationDefinitionsByCriteria(
            remoteClient.getSubject(), criteria);

        for (OperationDefinition def : operationDefinitions) {
            Operation o = new Operation(def);
            this.operationMap.put(o.getName(), o);
            this.allProperties.put(o.getName(), o);
        }
    }

    private void initContent() {
        ContentManagerRemote contentManager = remoteClient.getContentManagerRemote();
        List<PackageType> types = null;
        try {
            types = contentManager.findPackageTypes(remoteClient.getSubject(), resource.getResourceType().getName(),
                resource.getResourceType().getPlugin());

            for (PackageType packageType : types) {
                contentTypes.put(packageType.getName(), new ContentType(packageType));
            }
        } catch (ResourceTypeNotFoundException e) {
            e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public class ContentType {

        private PackageType packageType;

        public ContentType(PackageType packageType) {
            this.packageType = packageType;
        }

        public PackageType getPackageType() {
            return this.packageType;
        }

        public List<PackageVersion> getInstalledPackages() {
            ContentManagerRemote contentManager = remoteClient.getContentManagerRemote();

            PackageVersionCriteria criteria = new PackageVersionCriteria();
            criteria.addFilterResourceId(resourceId);
            // criteria.addFilterPackageTypeId()  TODO ADD this when the filter is added

            return contentManager.findInstalledPackageVersionsByCriteria(remoteClient.getSubject(), criteria);
        }

        public String toString() {
            return this.packageType.getDisplayName();
        }

    }

    public class Measurement {

        MeasurementDefinition definition;

        public Measurement(MeasurementDefinition definition) {
            this.definition = definition;
        }

        @Summary(index = 0)
        public String getName() {
            return definition.getDisplayName();
        }

        @Summary(index = 1)
        public String getDisplayValue() {
            Object val = getValue();
            if (val instanceof Number) {
                return MeasurementConverter.format(((Number) val).doubleValue(), getUnits(), true);
            } else {
                return String.valueOf(val);
            }
        }

        @Summary(index = 2)
        public String getDescription() {
            return definition.getDescription();
        }

        public DataType getDataType() {
            return definition.getDataType();
        }

        public MeasurementCategory getCategory() {
            return definition.getCategory();
        }

        public MeasurementUnits getUnits() {
            return definition.getUnits();
        }

        public Object getValue() {
            try {
                Set<MeasurementData> d = remoteClient.getMeasurementDataManagerRemote().findLiveData(
                    remoteClient.getSubject(), resourceId, new int[] { definition.getId() });
                MeasurementData data = d.iterator().next();
                return data.getValue();
            } catch (Exception e) {
                return "?";
            }
        }

        public String toString() {
            return getDisplayValue();
        }
    }

    public class Operation {
        OperationDefinition definition;

        public Operation(OperationDefinition definition) {
            this.definition = definition;
        }

        @Summary(index = 0)
        public String getName() {
            return simpleName(this.definition.getDisplayName());
        }

        @Summary(index = 1)
        public String getDescription() {
            return this.definition.getDescription();
        }

        public OperationDefinition getDefinition() {
            return definition;
        }

        public Object invoke(Object[] args) throws Exception {
            Configuration parameters = ConfigurationClassBuilder.translateParametersToConfig(definition
                .getParametersConfigurationDefinition(), args);

            ResourceOperationSchedule schedule = remoteClient.getOperationManagerRemote().scheduleResourceOperation(
                remoteClient.getSubject(), resourceId, definition.getName(), 0, 0, 0, 30000, parameters,
                "Executed from commandline");

            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());
            criteria.addFilterResourceIds(resourceId);
            criteria.addSortStartTime(PageOrdering.DESC); // put most recent at top of results
            criteria.setPaging(0, 1); // only return one result, in effect the latest
            criteria.fetchOperationDefinition(true);
            criteria.fetchParameters(true);
            criteria.fetchResults(true);

            int retries = 10;
            ResourceOperationHistory history = null;
            while (history == null && retries-- > 0) {
                Thread.sleep(1000);
                PageList<ResourceOperationHistory> histories = remoteClient.getOperationManagerRemote()
                    .findResourceOperationHistoriesByCriteria(remoteClient.getSubject(), criteria);
                if (histories.size() > 0 && histories.get(0).getStatus() != OperationRequestStatus.INPROGRESS) {
                    history = histories.get(0);
                }
            }

            Configuration result = (history != null ? history.getResults() : null);

            Object returnResults = ConfigurationClassBuilder.translateResults(definition
                .getResultsConfigurationDefinition(), result);

            return returnResults;
        }
    }

    public static class ClientProxyMethodHandler implements MethodHandler, ContentBackedResource, PluginConfigurable,
        ResourceConfigurable {

        ResourceClientProxy resourceClientProxy;
        RemoteClient remoteClient;

        public ClientProxyMethodHandler(ResourceClientProxy resourceClientProxy, RemoteClient remoteClient) {
            this.resourceClientProxy = resourceClientProxy;
            this.remoteClient = remoteClient;
        }

        // Methods here are optional and only accessible if their declared on the custom resource interface class

        public Configuration getPluginConfiguration() {
            return remoteClient.getConfigurationManagerRemote().getPluginConfiguration(remoteClient.getSubject(),
                resourceClientProxy.resourceId);
        }

        public ConfigurationDefinition getPluginConfigurationDefinition() {
            return resourceClientProxy.pluginConfigurationDefinition;
        }

        public Configuration getResourceConfiguration() {
            return remoteClient.getConfigurationManagerRemote().getResourceConfiguration(remoteClient.getSubject(),
                resourceClientProxy.resourceId);
        }

        public ConfigurationDefinition getResourceConfigurationDefinition() {
            return resourceClientProxy.resourceConfigurationDefinition;
        }

        public InstalledPackage getBackingContent() {
            InstalledPackage result = null;

            PackageVersionCriteria criteria = new PackageVersionCriteria();
            criteria.addFilterResourceId(resourceClientProxy.resourceId);
            criteria.fetchInstalledPackages(true);
            PageList<PackageVersion> pvs = remoteClient.getContentManagerRemote()
                .findInstalledPackageVersionsByCriteria(remoteClient.getSubject(), criteria);

            if (!((null == pvs) || pvs.isEmpty() || pvs.get(0).getInstalledPackages().isEmpty())) {
                // Do we want to check for more than 1 result?
                result = pvs.get(0).getInstalledPackages().iterator().next();
            }

            return result;
        }

        public Object invoke(Object proxy, Method method, Method proceedMethod, Object[] args) throws Throwable {

            if (proceedMethod != null) {
                Method realMethod = ResourceClientProxy.class.getMethod(method.getName(), method.getParameterTypes());
                return realMethod.invoke(resourceClientProxy, args);
            } else {

                try {
                    Method localMethod = ClientProxyMethodHandler.class.getDeclaredMethod(method.getName(), method
                        .getParameterTypes());
                    return localMethod.invoke(this, args);
                } catch (NoSuchMethodException nsme) {

                    String name = method.getName();
                    Object key = resourceClientProxy.allProperties.get(name);
                    if (key == null) {
                        name = decapitalize(method.getName().substring(3, method.getName().length()));
                        key = resourceClientProxy.allProperties.get(name);
                    }

                    if (key != null) {
                        if (key instanceof Measurement) {
                            return key;
                        } else if (key instanceof Operation) {
                            System.out.println("Invoking operation " + ((Operation) key).getName());

                            return ((Operation) key).invoke(args);

                        }
                    }
                }

                throw new RuntimeException("Can't find custom method: " + method);
            }
        }
    }

    public static class Factory {
        private RemoteClient remoteClient;

        public Factory(RemoteClient remoteClient) {
            this.remoteClient = remoteClient;
        }

        private static AtomicInteger classIndex = new AtomicInteger();

        public ResourceClientProxy getResource(int resourceId) {

            ResourceClientProxy proxy = new ResourceClientProxy(remoteClient, resourceId);
            Class customInterface = null;
            try {
                // define the dynamic class
                ClassPool pool = ClassPool.getDefault();
                CtClass customClass = pool.makeInterface(ResourceClientProxy.class.getName() + "__Custom__"
                    + classIndex.getAndIncrement());

                for (String key : proxy.allProperties.keySet()) {
                    Object prop = proxy.allProperties.get(key);

                    if (prop instanceof Measurement) {
                        Measurement m = (Measurement) prop;
                        String name = getterName(key);

                        try {
                            ResourceClientProxy.class.getMethod(name);
                        } catch (NoSuchMethodException nsme) {
                            CtMethod method = CtNewMethod.abstractMethod(pool.get(Measurement.class.getName()),
                                getterName(key), new CtClass[0], new CtClass[0], customClass);
                            customClass.addMethod(method);
                        }
                    } else if (prop instanceof Operation) {
                        Operation o = (Operation) prop;

                        LinkedHashMap<String, CtClass> types = ConfigurationClassBuilder.translateParameters(o
                            .getDefinition().getParametersConfigurationDefinition());

                        CtClass[] params = new CtClass[types.size()];
                        int x = 0;
                        for (String param : types.keySet()) {
                            params[x++] = types.get(param);
                        }

                        CtMethod method = CtNewMethod.abstractMethod(ConfigurationClassBuilder.translateConfiguration(o
                            .getDefinition().getResultsConfigurationDefinition()), simpleName(key), params,
                            new CtClass[0], customClass);

                        // Setup @WebParam annotations so the signatures have the config prop names
                        javassist.bytecode.annotation.Annotation[][] newAnnotations = new javassist.bytecode.annotation.Annotation[params.length][1];
                        int i = 0;
                        for (String paramName : types.keySet()) {
                            newAnnotations[i] = new Annotation[1];

                            newAnnotations[i][0] = new Annotation(WebParam.class.getName(), method.getMethodInfo()
                                .getConstPool());
                            newAnnotations[i][0].addMemberValue("name", new StringMemberValue(paramName, method
                                .getMethodInfo().getConstPool()));
                            i++;
                        }

                        ParameterAnnotationsAttribute newAnnotationsAttribute = new ParameterAnnotationsAttribute(
                            method.getMethodInfo().getConstPool(), ParameterAnnotationsAttribute.visibleTag);
                        newAnnotationsAttribute.setAnnotations(newAnnotations);
                        method.getMethodInfo().addAttribute(newAnnotationsAttribute);

                        customClass.addMethod(method);
                    }
                }

                customInterface = customClass.toClass();
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (customInterface != null) {

                List<Class> interfaces = new ArrayList<Class>();
                interfaces.add(customInterface);
                if (proxy.resourceConfigurationDefinition != null) {
                    interfaces.add(ResourceConfigurable.class);
                }
                if (proxy.pluginConfigurationDefinition != null) {
                    interfaces.add(PluginConfigurable.class);
                }

                if (proxy.getResourceType().getCreationDataType() == ResourceCreationDataType.CONTENT) {
                    interfaces.add(ContentBackedResource.class);
                }

                ProxyFactory proxyFactory = new ProxyFactory();
                proxyFactory.setInterfaces(interfaces.toArray(new Class[interfaces.size()]));
                proxyFactory.setSuperclass(ResourceClientProxy.class);
                ResourceClientProxy proxied = null;
                try {
                    proxied = (ResourceClientProxy) proxyFactory.create(new Class[] {}, new Object[] {},
                        new ClientProxyMethodHandler(proxy, remoteClient));
                } catch (InstantiationException e) {
                    e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                } catch (NoSuchMethodException e) {
                    e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                } catch (InvocationTargetException e) {
                    e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                }
                return proxied;
            }
            return proxy;

        }
    }

    private static String simpleName(String name) {
        return decapitalize(name.replaceAll("\\W", ""));
    }

    private static String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
    }

    private static String getterName(String name) {
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1, name.length());
    }

    public static interface PluginConfigurable {
        public Configuration getPluginConfiguration();

        public ConfigurationDefinition getPluginConfigurationDefinition();
    }

    public static interface ResourceConfigurable {
        public Configuration getResourceConfiguration();

        public ConfigurationDefinition getResourceConfigurationDefinition();
    }

    public static interface ContentBackedResource {

        public InstalledPackage getBackingContent();

    }

    public static void main(String[] args) throws Exception {
        RemoteClient rc = new RemoteClient("localhost", 7080);

        rc.login("rhqadmin", "rhqadmin");

        Factory factory = new Factory(rc);

        ResourceClientProxy resource = factory.getResource(10571);

        for (Measurement m : resource.getMeasurements()) {
            System.out.println(m.toString());
        }
    }
}
