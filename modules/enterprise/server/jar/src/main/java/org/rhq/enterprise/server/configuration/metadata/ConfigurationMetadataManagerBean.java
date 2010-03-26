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
package org.rhq.enterprise.server.configuration.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.enterprise.server.RHQConstants;

/**
 * Used to work with metadata defining generic configurations.
 */
@Stateless
public class ConfigurationMetadataManagerBean implements ConfigurationMetadataManagerLocal {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public void updateConfigurationDefinition(ConfigurationDefinition newDefinition,
        ConfigurationDefinition existingDefinition) {
        /*
         * handle grouped and ungrouped properties separately. for ungrouped, we don't need to care about the group, but
         * for the grouped ones we need to start at group level and then look at the properties. This is done below.
         *
         * First look at the ungrouped ones.
         */

        List<PropertyDefinition> existingPropertyDefinitions = existingDefinition.getNonGroupedProperties();
        List<PropertyDefinition> newPropertyDefinitions = newDefinition.getNonGroupedProperties();
        if (existingPropertyDefinitions != null) {
            for (PropertyDefinition newProperty : newPropertyDefinitions) {
                PropertyDefinition existingProp = existingDefinition.get(newProperty.getName());
                if (existingProp != null) {
                    updatePropertyDefinition(existingProp, newProperty);
                } else {
                    existingDefinition.put(newProperty);
                }
            }

            // delete outdated properties
            removeNolongerUsedProperties(newDefinition, existingDefinition, existingPropertyDefinitions, null);
        } else {
            // TODO what if exisitingDefinitions is null?
            // we probably don't run in here, as the initial persisting is done
            // somewhere else.
        }

        entityManager.flush();

        /*
         * Now update / delete contained groups We need to be careful here, as groups are present in PropertyDefinition
         * as "backlink" from PropertyDefinition to group
         */
        List<PropertyGroupDefinition> existingGroups = existingDefinition.getGroupDefinitions();
        List<PropertyGroupDefinition> newGroups = newDefinition.getGroupDefinitions();

        List<PropertyGroupDefinition> toPersist = missingInFirstList(existingGroups, newGroups);
        List<PropertyGroupDefinition> toDelete = missingInFirstList(newGroups, existingGroups);
        List<PropertyGroupDefinition> toUpdate = intersection(existingGroups, newGroups);

        // delete groups no longer present
        for (PropertyGroupDefinition group : toDelete) {
            List<PropertyDefinition> groupedDefinitions = existingDefinition.getPropertiesInGroup(group.getName());

            // first look for contained stuff
            for (PropertyDefinition def : groupedDefinitions) {
                existingPropertyDefinitions.remove(def);
                existingDefinition.getPropertyDefinitions().remove(def.getName());
                def.setPropertyGroupDefinition(null);
                entityManager.remove(def);
            }

            // then remove the definition itself
            existingGroups.remove(group);
            entityManager.remove(group);
        }

        entityManager.flush();

        // update existing groups that stay
        for (PropertyGroupDefinition group : toUpdate) {
            String groupName = group.getName();

            //         System.out.println("Group to update: " + groupName + ", id=" + group.getId());
            List<PropertyDefinition> newGroupedDefinitions = newDefinition.getPropertiesInGroup(groupName);
            for (PropertyDefinition nDef : newGroupedDefinitions) {
                PropertyDefinition existingProperty = existingDefinition.getPropertyDefinitions().get(nDef.getName());
                if (existingProperty != null) {
                    updatePropertyDefinition(existingProperty, nDef);
                } else {
                    existingDefinition.put(nDef);
                }
            }

            // delete outdated properties of this group
            removeNolongerUsedProperties(newDefinition, existingDefinition, existingDefinition
                .getPropertiesInGroup(groupName), group);
        }

        entityManager.flush();

        // persist new groups
        for (PropertyGroupDefinition group : toPersist) {
            /*
             * First persist a new group definition and then link the properties to it
             */
            entityManager.persist(group);
            existingGroups.add(group); // iterating over this does not update the underlying crap

            List<PropertyDefinition> defs = newDefinition.getPropertiesInGroup(group.getName());
            Map<String, PropertyDefinition> exPDefs = existingDefinition.getPropertyDefinitions();
            for (PropertyDefinition def : defs) {
                entityManager.persist(def);
                def.setPropertyGroupDefinition(group);
                def.setConfigurationDefinition(existingDefinition);
                exPDefs.put(def.getName(), def);
            }
        }

        /*
         * Now work on the templates.
         */
        Map<String, ConfigurationTemplate> existingTemplates = existingDefinition.getTemplates();
        Map<String, ConfigurationTemplate> newTemplates = newDefinition.getTemplates();
        List<String> toRemove = new ArrayList<String>();
        for (String name : existingTemplates.keySet()) {
            ConfigurationTemplate exTemplate = existingTemplates.get(name);
            if (newTemplates.containsKey(name)) {
                //                System.out.println("updating template with name " + name);
                updateTemplate(exTemplate, newTemplates.get(name));
            } else {
                // template in newTemplates not there -> delete old stuff
                //                System.out.println("Deleting template with name " + name);
                entityManager.remove(exTemplate);
                toRemove.add(name);
            }
        }
        // Remove the deleted ones from the existing templates map
        for (String name : toRemove) {
            existingTemplates.remove(name);
        }
        entityManager.flush();

        for (String name : newTemplates.keySet()) {
            // add completely new templates
            if (!existingTemplates.containsKey(name)) {

                //                System.out.println("Persisting new template with name " + name);
                ConfigurationTemplate newTemplate = newTemplates.get(name);

                // we need to set a valid configurationDefinition, where we will live on.
                newTemplate.setConfigurationDefinition(existingDefinition);

                entityManager.persist(newTemplate);
                existingTemplates.put(name, newTemplate);
            }
        }

        entityManager.flush();
    }

    /**
     * Updates a template 
     * @param existingDT
     * @param newDT
     */
    private void updateTemplate(ConfigurationTemplate existingDT, ConfigurationTemplate newDT) {

        try {
            Configuration existConf = existingDT.getConfiguration();
            Configuration newConf = newDT.getConfiguration();
            Collection<String> exNames = existConf.getNames();
            Collection<String> newNames = newConf.getNames();
            List<String> toRemove = new ArrayList<String>();
            for (String name : exNames) {
                Property prop = newConf.get(name);
                if (prop instanceof PropertySimple) {
                    PropertySimple ps = newConf.getSimple(name);
                    if (ps != null) {
                        Property eprop = existConf.get(name);
                        if (eprop instanceof PropertySimple) {
                            PropertySimple exps = existConf.getSimple(name);
                            if (ps.getStringValue() != null) {
                                exps.setStringValue(ps.getStringValue());
                                //                                System.out.println("  updated " + name + " to value " + ps.getStringValue());
                            }
                        } else {
                            if (eprop != null) {
                                //                                System.out.println("Can't yet handle target prop: " + eprop);
                            }
                        }
                    } else { // property not in new template -> deleted
                        toRemove.add(name);
                    }
                } else {
                    if (prop != null) {
                        //                        System.out.println("Can't yet handle source prop: " + prop);
                    }
                }
            }
            for (String name : toRemove)
                existConf.remove(name);

            // now check for new names and add them
            for (String name : newNames) {
                if (!exNames.contains(name)) {
                    Property prop = newConf.get(name);
                    if (prop instanceof PropertySimple) {
                        PropertySimple ps = newConf.getSimple(name);
                        if (ps.getStringValue() != null) {
                            // TODO add a new property
                            //                            Collection<Property> properties = existConf.getProperties();
                            //                            properties = new ArrayList<Property>(properties);
                            //                            properties.add(ps);
                            //                            existConf.setProperties(properties);
                            Property property = ps.deepCopy(false);
                            existConf.put(property);
                        }
                    }
                }
            }
            entityManager.flush();
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * Removes PropertyDefinition items from the configuration
     *
     * @param newDefinition      new configuration to persist
     * @param existingDefinition existing persisted configuration
     * @param existingProperties list of existing properties
     * @param groupDef TODO
     */
    private void removeNolongerUsedProperties(ConfigurationDefinition newDefinition,
        ConfigurationDefinition existingDefinition, List<PropertyDefinition> existingProperties,
        PropertyGroupDefinition groupDef) {

        List<PropertyDefinition> definitionsToDelete = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition exDef : existingProperties) {
            PropertyDefinition nDef = newDefinition.get(exDef.getName());
            if (nDef == null) {
                // not in new configuration
                definitionsToDelete.add(exDef);
            }
        }
        //        System.out.println("Props to delete " + definitionsToDelete);

        for (PropertyDefinition def : definitionsToDelete) {
            existingDefinition.getPropertyDefinitions().remove(def.getName());
            existingProperties.remove(def); // does not operate on original list!!            
        }
    }

    /**
     * Update objects of type on:property (simple-property, map-property, list-property
     *
     * @param existingProperty
     * @param newProperty
     */
    private void updatePropertyDefinition(PropertyDefinition existingProperty, PropertyDefinition newProperty) {

        existingProperty.setDescription(newProperty.getDescription());
        existingProperty.setDisplayName(newProperty.getDisplayName());
        existingProperty.setActivationPolicy(newProperty.getActivationPolicy());
        existingProperty.setVersion(newProperty.getVersion());
        existingProperty.setRequired(newProperty.isRequired());
        existingProperty.setReadOnly(newProperty.isReadOnly());
        existingProperty.setSummary(newProperty.isSummary());

        /*
         * After the general things have been set, go through the subtypes of PropertyDefinition. If the new type is the
         * same as the old, we update. Else we simply replace
         */

        if (existingProperty instanceof PropertyDefinitionMap) {
            if (newProperty instanceof PropertyDefinitionMap) {
                // remove outdated maps
                Map<String, PropertyDefinition> existingPropDefs = ((PropertyDefinitionMap) existingProperty)
                    .getPropertyDefinitions();
                Set<String> existingKeys = existingPropDefs.keySet();
                Set<String> newKeys = ((PropertyDefinitionMap) newProperty).getPropertyDefinitions().keySet();
                for (String key : existingKeys) {
                    if (!newKeys.contains(key)) {
                        entityManager.remove(existingPropDefs.get(key));
                        existingPropDefs.remove(key);
                    }
                }

                // store update new ones
                for (PropertyDefinition newChild : ((PropertyDefinitionMap) newProperty).getPropertyDefinitions()
                    .values()) {
                    PropertyDefinition existingChild = ((PropertyDefinitionMap) existingProperty).get(newChild
                        .getName());
                    if (existingChild != null) {
                        updatePropertyDefinition(existingChild, newChild);
                    } else {
                        ((PropertyDefinitionMap) existingProperty).put(newChild);
                        entityManager.persist(newChild);
                    }
                }
            } else // different type
            {
                replaceProperty(existingProperty, newProperty);
            }
        } else if (existingProperty instanceof PropertyDefinitionList) {
            PropertyDefinitionList exList = (PropertyDefinitionList) existingProperty;
            if (newProperty instanceof PropertyDefinitionList) {
                PropertyDefinitionList newList = (PropertyDefinitionList) newProperty;
                exList.setMemberDefinition(newList.getMemberDefinition());
                exList.setMax(newList.getMax());
                exList.setMin(newList.getMax());
                // what about parentPropertyListDefinition ?

                // TODO recursively update the member ?
            } else // simple property or map-property
            {
                replaceProperty(existingProperty, newProperty);
            }
        } else if (existingProperty instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple existingPDS = (PropertyDefinitionSimple) existingProperty;

            if (newProperty instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple newPDS = (PropertyDefinitionSimple) newProperty;

                // handle <property-options>?
                List<PropertyDefinitionEnumeration> existingOptions = existingPDS.getEnumeratedValues();
                List<PropertyDefinitionEnumeration> newOptions = newPDS.getEnumeratedValues();

                List<PropertyDefinitionEnumeration> toPersist = missingInFirstList(existingOptions, newOptions);
                List<PropertyDefinitionEnumeration> toDelete = missingInFirstList(newOptions, existingOptions);
                List<PropertyDefinitionEnumeration> changed = intersection(existingOptions, newOptions);

                // TODO GH: This still doesn't properly reorder options, but at least it doesn't leave any nulls
                // and therefore doesn't blow up the renderer
                // delete old ones (first so we don't leave index holes later)
                for (PropertyDefinitionEnumeration pde : toDelete) {
                    existingOptions.remove(pde);
                    entityManager.remove(pde);
                }

                // save new ones
                for (PropertyDefinitionEnumeration pde : toPersist) {
                    existingPDS.addEnumeratedValues(pde);
                    entityManager.persist(pde);
                }

                for (PropertyDefinitionEnumeration pde : changed) {
                    for (PropertyDefinitionEnumeration nPde : newOptions) {
                        if (nPde.equals(pde)) {
                            pde.setDefault(nPde.isDefault());
                            pde.setOrderIndex(nPde.getOrderIndex());
                            pde.setValue(nPde.getValue());
                            entityManager.merge(pde);
                        }
                    }
                }

                // handle <constraint> [0..*]

                entityManager.flush();
                Set<Constraint> exCon = existingPDS.getConstraints();
                if (exCon.size() > 0) {
                    for (Constraint con : exCon) {
                        con.setPropertyDefinitionSimple(null);
                        entityManager.remove(con);
                    }

                    existingPDS.getConstraints().clear(); // clear out existing
                }

                for (Constraint con : newPDS.getConstraints()) {
                    existingPDS.addConstraints(con);
                }

                // handle <defaultValue> [0..1]
                existingPDS.setDefaultValue(newPDS.getDefaultValue());
            } else {
                // other type
                replaceProperty(existingProperty, newProperty);
            }
        }
    }

    /**
     * Replace the existing property of a given type with a new property of a (possibly) different type
     *
     * @param existingProperty
     * @param newProperty
     */
    private void replaceProperty(PropertyDefinition existingProperty, PropertyDefinition newProperty) {
        // take id and definition from the existing one
        newProperty.setId(existingProperty.getId());
        newProperty.setConfigurationDefinition(existingProperty.getConfigurationDefinition());

        //  need to remove the old crap
        existingProperty.getConfigurationDefinition().getPropertyDefinitions().remove(existingProperty.getName());
        existingProperty.setConfigurationDefinition(null);
        entityManager.remove(existingProperty);

        // persist the new one
        entityManager.merge(newProperty);
        entityManager.flush();
    }

    /**
     * Return a list containing those element that are in reference, but not in first. Both input lists are not modified
     *
     * @param  first
     * @param  reference
     *
     * @return list containing those element that are in reference, but not in first
     */
    private <T> List<T> missingInFirstList(List<T> first, List<T> reference) {
        List<T> result = new ArrayList<T>();

        if (reference != null) {
            // First collection is null -> everything is missing
            if (first == null) {
                result.addAll(reference);
                return result;
            }

            // else loop over the set and sort out the right items.
            for (T item : reference) {
                if (!first.contains(item)) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Return a new List with elements both in first and second passed collection.
     *
     * @param  first  First list
     * @param  second Second list
     *
     * @return a new set (depending on input type) with elements in first and second
     */
    private <T> List<T> intersection(List<T> first, List<T> second) {
        List<T> result = new ArrayList<T>();

        if ((first != null) && (second != null)) {
            result.addAll(first);
            result.retainAll(second);
        }

        return result;
    }
}