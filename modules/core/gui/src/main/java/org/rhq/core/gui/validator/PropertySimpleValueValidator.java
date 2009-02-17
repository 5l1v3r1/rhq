 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui.validator;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.DoubleRangeValidator;
import javax.faces.validator.LengthValidator;
import javax.faces.validator.LongRangeValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.configuration.ConfigRenderer;
import org.ajax4jsf.context.AjaxContext;

 /**
 * A JSF validator that validates a String, which represents the value of a JON simple property (i.e.
 * {@link PropertySimple}). A definition for the simple property {@link PropertyDefinitionSimple} can optionally be
 * specified when this validator is instantiated. If a definition was specified, the value will be validated against the
 * definition's type (i.e. {@link org.rhq.core.domain.configuration.definition.PropertySimpleType}) and any constraints
 * (i.e. {@link Constraint}s) included in the definition.
 *
 * @author Ian Springer
 */
public class PropertySimpleValueValidator implements Validator, StateHolder {    
    private static final String INPUT_ERROR_STYLE_CLASS = "inputerror";

    private PropertyDefinitionSimple propertyDefinition;
    private boolean transientValue;

    // A public no-arg constructor is required by the JSF spec.
    public PropertySimpleValueValidator() {
    }

    public PropertySimpleValueValidator(@Nullable
    PropertyDefinitionSimple propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    public void validate(FacesContext facesContext, UIComponent component, Object value) throws ValidatorException {
        String stringValue = (String) value;        

        String refresh = FacesContextUtility.getOptionalRequestParameter("refresh");
        if (refresh != null && refresh.equals(ConfigRenderer.PROPERTY_SET_COMPONENT_ID))
            return;                    

        if (!FacesComponentUtility.isOverride(component)) {
            // don't validate if it's not being updated
            return;
        }

        List<Validator> subValidators = createSubValidators();

        for (Validator subValidator : subValidators) {
            try {
                subValidator.validate(facesContext, component, stringValue);
            }
            catch (ValidatorException e) {
                component.getAttributes().put("styleClass", INPUT_ERROR_STYLE_CLASS);
                if (" ".equals(stringValue)) {
                    ((UIInput)component).setSubmittedValue("");
                }
                throw e;
            }
        }
    }

    private List<Validator> createSubValidators() {
        List<Validator> subValidators = new ArrayList<Validator>();
        subValidators.add(new LengthValidator(PropertySimple.MAX_VALUE_LENGTH));
        if (this.propertyDefinition != null) {
            switch (this.propertyDefinition.getType()) {
            case INTEGER: {
                subValidators.add(new LongRangeValidator(Integer.MAX_VALUE, Integer.MIN_VALUE));
                break;
            }

            case LONG: {
                subValidators.add(new LongRangeValidator(Long.MAX_VALUE, Long.MIN_VALUE));
                break;
            }

            case FLOAT: {
                subValidators.add(new DoubleRangeValidator(Float.MAX_VALUE, Float.MIN_VALUE));
                break;
            }

            case DOUBLE: {
                subValidators.add(new DoubleRangeValidator(Double.MAX_VALUE, Double.MIN_VALUE));
                break;
                // There is no need for type-based validators for booleans or enums, because the UI input controls
                // (e.g. radio buttons or pulldown menus) prevent invalid values from being entered.
            }
            }

            for (Constraint constraint : this.propertyDefinition.getConstraints()) {
                subValidators.add(createValidator(constraint));
            }
        }
        return subValidators;
    }

    @NotNull
    public static Validator createValidator(Constraint constraint) {
        Validator validator;
        if (constraint instanceof IntegerRangeConstraint) {
            IntegerRangeConstraint integerRangeConstraint = (IntegerRangeConstraint) constraint;
            LongRangeValidator longRangeValidator = new LongRangeValidator();
            Long minValue = integerRangeConstraint.getMinimum();
            if (minValue != null) {
                longRangeValidator.setMinimum(minValue);
            }

            Long maxValue = integerRangeConstraint.getMaximum();
            if (maxValue != null) {
                longRangeValidator.setMaximum(maxValue);
            }

            validator = longRangeValidator;
        } else if (constraint instanceof FloatRangeConstraint) {
            FloatRangeConstraint floatRangeConstraint = (FloatRangeConstraint) constraint;
            DoubleRangeValidator doubleRangeValidator = new DoubleRangeValidator();
            Double minValue = floatRangeConstraint.getMinimum();
            if (minValue != null) {
                doubleRangeValidator.setMinimum(minValue);
            }

            Double maxValue = floatRangeConstraint.getMaximum();
            if (maxValue != null) {
                doubleRangeValidator.setMaximum(maxValue);
            }

            validator = doubleRangeValidator;
        } else if (constraint instanceof RegexConstraint) {
            RegexConstraint regexConstraint = (RegexConstraint) constraint;
            validator = new RegexValidator(regexConstraint.getDetails());
        } else {
            throw new IllegalArgumentException("Unknown constraint type: " + constraint.getClass().getName());
        }

        return validator;
    }

    public void restoreState(FacesContext facesContext, Object state) {
        Object[] values = (Object[]) state;
        this.propertyDefinition = (PropertyDefinitionSimple) values[0];
    }

    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[1];
        values[0] = this.propertyDefinition;
        return (values);
    }

    public boolean isTransient() {
        return transientValue;
    }

    public void setTransient(boolean transientValue) {
        this.transientValue = transientValue;
    }
}