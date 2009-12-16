package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIPanel;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;

public class RawConfigUIComponent extends UIComponentBase {

    private boolean readOnly;
    private boolean showRaw;

    @Override
    public void decode(FacesContext context) {
        super.decode(context);
        setSelectedPath(FacesContextUtility.getOptionalRequestParameter("path"));
        readOnly = Boolean.valueOf(FacesContextUtility.getOptionalRequestParameter("readOnly"));

    }

    @Override
    public String getFamily() {
        return "rhq";
    }

    private Configuration configuration;
    private ConfigurationDefinition configurationDefinition;
    private FacesComponentIdFactory idFactory;
    private String selectedPath;
    private HtmlInputTextarea inputTextarea;
    private Map<String, RawConfiguration> rawMap;

    public String getSelectedPath() {
        if (null == selectedPath) {
            if (configuration.getRawConfigurations().iterator().hasNext()) {
                selectedPath = configuration.getRawConfigurations().iterator().next().getPath();
            } else {
                selectedPath = "";
            }
        }
        return selectedPath;
    }

    public void setSelectedPath(String selectedPath) {
        if (selectedPath == null)
            return;
        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            if (raw.getPath().equals(selectedPath)) {
                this.selectedPath = selectedPath;
            }
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        for (UIComponent kid : getChildren()) {
            kid.encodeAll(context);
        }
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    Vector<UIParameter> readOnlyParms = new Vector<UIParameter>();

    public RawConfigUIComponent(Configuration configuration, ConfigurationDefinition configurationDefinition,
        FacesComponentIdFactory componentIdFactory, boolean readOnly) {

        this.configuration = configuration;
        this.configurationDefinition = configurationDefinition;
        this.idFactory = componentIdFactory;
        this.readOnly = readOnly;

        UIPanel rawPanel = FacesComponentUtility.addBlockPanel(this, idFactory, "");

        addToolbar(configurationDefinition, rawPanel);

        HtmlPanelGrid grid = FacesComponentUtility.addPanelGrid(rawPanel, idFactory, "summary-props-table");
        grid.setParent(this);
        grid.setColumns(2);
        grid.setColumnClasses("raw-config-table");

        HtmlPanelGrid panelLeft = FacesComponentUtility.addPanelGrid(grid, idFactory, "summary-props-table");
        panelLeft.setBgcolor("#a4b2b9");
        panelLeft.setColumns(1);

        FacesComponentUtility.addOutputText(panelLeft, idFactory, "Raw Configurations Paths", "");

        int rawCount = 0;

        ArrayList<String> configPathList = new ArrayList<String>();

        rawMap = new HashMap<String, RawConfiguration>();

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            configPathList.add(raw.getPath());
            rawMap.put(raw.getPath(), raw);
        }
        Collections.sort(configPathList);
        String oldDirname = "";
        for (String s : configPathList) {
            RawConfiguration raw = rawMap.get(s);
            String dirname = raw.getPath().substring(0, raw.getPath().lastIndexOf("/") + 1);
            String basename = raw.getPath().substring(raw.getPath().lastIndexOf("/") + 1, raw.getPath().length());
            if (!dirname.equals(oldDirname)) {
                FacesComponentUtility.addOutputText(panelLeft, idFactory, dirname, "");
                oldDirname = dirname;
            }
            UIPanel nextPath = FacesComponentUtility.addBlockPanel(panelLeft, idFactory, "");
            HtmlCommandLink link = FacesComponentUtility.addCommandLink(nextPath, idFactory);
            FacesComponentUtility.addOutputText(link, idFactory, "[]" + basename, "");
            FacesComponentUtility.addParameter(link, idFactory, "path", raw.getPath());
            FacesComponentUtility.addParameter(link, idFactory, "whichRaw", Integer.toString(rawCount++));
            FacesComponentUtility.addParameter(link, idFactory, "showRaw", Boolean.TRUE.toString());
            readOnlyParms.add(FacesComponentUtility.addParameter(link, idFactory,
                AbstractConfigurationComponent.READ_ONLY_ATTRIBUTE, Boolean.toString(readOnly)));

        }

        UIPanel panelRight = FacesComponentUtility.addBlockPanel(grid, idFactory, "summary-props-table");

        UIPanel editPanel = FacesComponentUtility.addBlockPanel(panelRight, idFactory, "summary-props-table");
        inputTextarea = new HtmlInputTextarea();
        editPanel.getChildren().add(inputTextarea);
        inputTextarea.setParent(editPanel);
        inputTextarea.setCols(80);
        inputTextarea.setRows(40);
        inputTextarea.setValue(rawMap.get(getSelectedPath()).getContentString());
        inputTextarea.setReadonly(readOnly);
    }

    private HtmlOutputLink fullscreenLink;
    private UIParameter fullScreenResourceIdParam;

    private void addToolbar(ConfigurationDefinition configurationDefinition, UIPanel parent) {
        UIPanel toolbarPanel = FacesComponentUtility.addBlockPanel(parent, idFactory, "config-toolbar");
        if (readOnly) {
            HtmlCommandLink editLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
            FacesComponentUtility.addGraphicImage(editLink, idFactory, "/images/edit.png", "Edit");
            FacesComponentUtility.addOutputText(editLink, idFactory, "Edit", "");
            FacesComponentUtility.addParameter(editLink, idFactory, "showRaw", Boolean.TRUE.toString());
            FacesComponentUtility.addParameter(editLink, idFactory, AbstractConfigurationComponent.READ_ONLY_ATTRIBUTE,
                Boolean.FALSE.toString());
        } else {
            HtmlCommandLink saveLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
            FacesComponentUtility.addGraphicImage(saveLink, idFactory, "/images/save.png", "Save");
            FacesComponentUtility.addOutputText(saveLink, idFactory, "Save", "");
            FacesComponentUtility.addParameter(saveLink, idFactory, "showRaw", Boolean.FALSE.toString());
            FacesComponentUtility.addParameter(saveLink, idFactory, AbstractConfigurationComponent.READ_ONLY_ATTRIBUTE,
                Boolean.TRUE.toString());
        }
        {
            fullscreenLink = FacesComponentUtility.addOutputLink(toolbarPanel, idFactory, "view-full.xhtml");
            FacesComponentUtility
                .addGraphicImage(fullscreenLink, idFactory, "/images/viewfullscreen.png", "FullScreen");
            FacesComponentUtility.addOutputText(fullscreenLink, idFactory, "Full Screen", "");
            fullScreenResourceIdParam = FacesComponentUtility.addParameter(fullscreenLink, idFactory, "id", "");
        }
        if (!readOnly) {
            HtmlOutputLink uploadLink = FacesComponentUtility.addOutputLink(toolbarPanel, idFactory, "upload.xhtml");
            FacesComponentUtility.addGraphicImage(uploadLink, idFactory, "/images/upload.png", "Upload");
            FacesComponentUtility.addOutputText(uploadLink, idFactory, "Upload", "");
        }
        {
            HtmlOutputLink downloadLink = FacesComponentUtility
                .addOutputLink(toolbarPanel, idFactory, "download.xhtml");
            FacesComponentUtility.addGraphicImage(downloadLink, idFactory, "/images/download.png", "download");
            FacesComponentUtility.addOutputText(downloadLink, idFactory, "Download", "");
        }
        if (configurationDefinition.getConfigurationFormat().isStructuredSupported()) {
            HtmlCommandLink toStructureLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
            FacesComponentUtility.addGraphicImage(toStructureLink, idFactory, "/images/structured.png",
                "showStructured");
            FacesComponentUtility.addOutputText(toStructureLink, idFactory, "showStructured", "");
            FacesComponentUtility.addParameter(toStructureLink, idFactory, "showRaw", Boolean.FALSE.toString());
            readOnlyParms.add(FacesComponentUtility.addParameter(toStructureLink, idFactory, "readOnly", Boolean
                .toString(readOnly)));
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        // TODO Auto-generated method stub
        super.encodeBegin(context);

        inputTextarea.setValue(rawMap.get(getSelectedPath()).getContentString());
        for (UIParameter param : readOnlyParms) {
            param.setValue(Boolean.toString(readOnly));
        }

        String resourceId = FacesContextUtility.getOptionalRequestParameter("id");

        if (null == resourceId) {
            if (fullscreenLink != null) {
                fullscreenLink.setRendered(false);
            }
        } else {
            if (fullScreenResourceIdParam != null) {
                fullScreenResourceIdParam.setValue(resourceId);
            }
        }

    }
}
