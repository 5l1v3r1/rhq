/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.core.gui.configuration;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;

/**
 * A class that provides static methods for masking and unmasking password properties within {@link Configuration}s,
 * The reason for masking a property's value is so that the current values of such properties cannot be viewed by a
 * user by viewing the HTML source of a Configuration GUI page, e.g.:
 *
 * &lt;input type="password" value="MASKED" .../&gt;
 *
 * would be rendered, rather than:
 *
 * &lt;input type="password" value="actual_password" .../&gt;
 *
 * @author Ian Springer
 */
public abstract class ConfigurationMaskingUtility {

    private static final String MASKED_PROPERTY_VALUE = "\u0002MASKED\u0003";

    /**
     * Mask the values of all simple properties of type PASSWORD in the configuration.
     *
     * @param configuration the configuration to be masked
     * @param configurationDefinition the configuration definition corresponding to the specified configuration
     */
    public static void maskConfiguration(@NotNull
    Configuration configuration, @NotNull
    ConfigurationDefinition configurationDefinition) {
        Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            maskProperty(childPropertyDefinition, configuration);
        }
    }

    /**
     * Unmask the values of all masked simple properties of type PASSWORD in the configuration.
     *
     * @param configuration the configuration to be unmasked
     * @param configurationDefinition the configuration definition corresponding to the specified configuration
     */
    public static void unmaskConfiguration(@NotNull
    Configuration configuration, @NotNull
    ConfigurationDefinition configurationDefinition) {
        Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            unmaskProperty(childPropertyDefinition, configuration);
        }
    }

    private static void maskProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple) propertyDefinition;
            if (propertyDefinitionSimple.getType() == PropertySimpleType.PASSWORD) {
                // It's a password - squirrel away the unmasked value, then mask it.
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                if (propertySimple == null) {
                    // This could theoretically happen in the case of handling a template, though it's very unlikely that a
                    // template would supply a password.
                    //throw new IllegalStateException("Property '" + propertyDefinitionSimple.getName() + "' is not present in the Configuration.");
                    return;
                }
                if (propertySimple.getStringValue() == null) {
                    // Don't mask properties with null values (i.e. unset properties), otherwise they will appear to have a
                    // value when rendered in the GUI (see http://jira.jboss.com/jira/browse/JBNADM-2248).
                    return;
                }
                if (MASKED_PROPERTY_VALUE.equals(propertySimple.getStringValue())) {
                    throw new IllegalStateException(
                        "maskConfiguration() was called more than once on the same Configuration.");
                }
                propertySimple.setUnmaskedStringValue(propertySimple.getStringValue());
                propertySimple.setStringValue(MASKED_PROPERTY_VALUE);
            }
        }
        // If the property is a Map, recurse into it and mask its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap) {
            PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
            PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
            maskPropertyMap(propertyMap, propertyDefinitionMap);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            // If the property is a List of Maps, iterate the list, and recurse into each Map and mask its child
            // properties.
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                for (Property property : propertyList.getList()) {
                    PropertyMap propertyMap = (PropertyMap) property;
                    maskPropertyMap(propertyMap, propertyDefinitionMap);
                }
            }
        }
    }

    private static void maskPropertyMap(AbstractPropertyMap propertyMap, PropertyDefinitionMap propertyDefinitionMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            maskProperty(childPropertyDefinition, propertyMap);
        }
    }

    private static void unmaskProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple) propertyDefinition;
            if (propertyDefinitionSimple.getType() == PropertySimpleType.PASSWORD) {
                // It's a password - if it's masked, unmask it.
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                if (propertySimple == null) {
                    // This could theoretically happen in the case of handling a template, though it's very unlikely that a
                    // template would supply a password.
                    //throw new IllegalStateException("Property '" + propertyDefinitionSimple.getName() + "' is not present in the Configuration.");
                    return;
                }
                if (MASKED_PROPERTY_VALUE.equals(propertySimple.getStringValue())) {
                    if (MASKED_PROPERTY_VALUE.equals(propertySimple.getUnmaskedStringValue())) {
                        throw new IllegalStateException("Unmasked string value of property '"
                            + propertySimple.getName()
                            + "' is set to MASKED_PROPERTY_VALUE - something went very wrong.");
                    }
                    propertySimple.setStringValue(propertySimple.getUnmaskedStringValue());
                }
            }
        }
        // If the property is a Map, recurse into it and unmask its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap) {
            PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
            PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
            unmaskPropertyMap(propertyMap, propertyDefinitionMap);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            // If the property is a List of Maps, iterate the list, and recurse into each Map and unmask its child
            // properties.
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                for (int i = 0; i < propertyList.getList().size(); i++) {
                    PropertyMap propertyMap = (PropertyMap) propertyList.getList().get(i);
                    unmaskPropertyMap(propertyMap, propertyDefinitionMap);
                }
            }
        }
    }

    private static void unmaskPropertyMap(AbstractPropertyMap propertyMap, PropertyDefinitionMap propertyDefinitionMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            unmaskProperty(childPropertyDefinition, propertyMap);
        }
    }

}
