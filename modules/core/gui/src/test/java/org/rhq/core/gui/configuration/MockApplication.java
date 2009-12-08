package org.rhq.core.gui.configuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.ActionListener;
import javax.faces.validator.Validator;

public class MockApplication extends Application {

    @Override
    public void addComponent(String componentType, String componentClass) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void addConverter(String converterId, String converterClass) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void addConverter(Class targetClass, String converterClass) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void addValidator(String validatorId, String validatorClass) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public UIComponent createComponent(String componentType) throws FacesException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public UIComponent createComponent(ValueBinding componentBinding, FacesContext context, String componentType)
        throws FacesException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Converter createConverter(String converterId) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Converter createConverter(Class targetClass) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public MethodBinding createMethodBinding(String ref, Class[] params) throws ReferenceSyntaxException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Validator createValidator(String validatorId) throws FacesException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ValueBinding createValueBinding(String ref) throws ReferenceSyntaxException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ActionListener getActionListener() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<String> getComponentTypes() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<String> getConverterIds() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<Class> getConverterTypes() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Locale getDefaultLocale() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getDefaultRenderKitId() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getMessageBundle() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public NavigationHandler getNavigationHandler() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public PropertyResolver getPropertyResolver() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public StateManager getStateManager() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<Locale> getSupportedLocales() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<String> getValidatorIds() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public VariableResolver getVariableResolver() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ViewHandler getViewHandler() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setActionListener(ActionListener listener) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setDefaultLocale(Locale locale) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setDefaultRenderKitId(String renderKitId) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setMessageBundle(String bundle) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setNavigationHandler(NavigationHandler handler) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setPropertyResolver(PropertyResolver resolver) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setStateManager(StateManager manager) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setSupportedLocales(Collection<Locale> locales) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setVariableResolver(VariableResolver resolver) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setViewHandler(ViewHandler handler) {
        throw new RuntimeException("Function not implemented");

    }

}
