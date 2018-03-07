/*
 * RHQ Management Platform
 * Copyright (C) 2014-2016 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.wfly10patch;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.rhq.common.wildfly.PatchProductType;
import org.rhq.common.wildfly.Patch;
import org.rhq.common.wildfly.PatchBundle;
import org.rhq.common.wildfly.PatchInfo;
import org.rhq.common.wildfly.PatchParser;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.core.util.updater.DestinationComplianceMode;
import org.rhq.enterprise.server.bundle.BundleDistributionInfo;
import org.rhq.enterprise.server.bundle.RecipeParseResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.bundle.UnknownRecipeException;

/**
 * @author Lukas Krejci
 * @author Michael Burman
 * @since 4.13
 */
public class Wildfly10PatchBundleServerPluginComponent implements ServerPluginComponent, BundleServerPluginFacet {

    @Override
    public RecipeParseResults parseRecipe(String recipe) throws UnknownRecipeException {
        throw new UnknownRecipeException(
            "The Wildfly 10 patches cannot be dealt with using only recipes - the whole distribution file is needed.");
    }

    @Override
    public BundleDistributionInfo processBundleDistributionFile(File distributionFile)
        throws Exception {

        if (null == distributionFile) {
            throw new IllegalArgumentException("distributionFile == null");
        }

        String fileName = null;
        String recipe = null;
        RecipeParseResults parseResults = null;

        FileInputStream in = new FileInputStream(distributionFile);
        try {
            PatchInfo patchInfo = PatchParser.parse(in, true);

            if (patchInfo == null) {
                throw new UnknownRecipeException();
            }

            if (patchInfo.is(Patch.class)) {
                Patch patch = patchInfo.as(Patch.class);

                if(!PatchProductType.EAP7.equals(patch.getProductType())) {
                    throw new UnknownRecipeException();
                }

                String version = patch.getType() == Patch.Type.ONE_OFF ? patch.getTargetVersion() + "+" + patch.getId()
                    : patch.getTargetVersion();

                DeploymentProperties props = new DeploymentProperties(0, patch.getIdentityName(),
                    version, patch.getDescription(), DestinationComplianceMode.full);

                ConfigurationDefinition config = new ConfigurationDefinition("wildfly-patch", null);
                PropertyDefinitionSimple patchIdProp = new PropertyDefinitionSimple("patchId", "The ID of the patch",
                    true,
                    PropertySimpleType.STRING);
                patchIdProp.setDefaultValue(patch.getId());
                patchIdProp.setReadOnly(true);
                PropertyDefinitionSimple patchTypeProp = new PropertyDefinitionSimple("patchType",
                    "The type of the patch",
                    true, PropertySimpleType.STRING);
                patchTypeProp.setDefaultValue(patch.getType().toString());
                patchTypeProp.setReadOnly(true);

                config.put(patchIdProp);
                config.put(patchTypeProp);
                addCommonProperties(config);

                parseResults = new RecipeParseResults(props, config, null);
                fileName = patch.getId();
                recipe = patch.getContents();
            } else if (patchInfo.is(PatchBundle.class)) {
                PatchBundle patchBundle = patchInfo.as(PatchBundle.class);

                Patch lastPatch = null;
                StringBuilder allPatchIds = new StringBuilder();

                for (PatchBundle.Element p : patchBundle) {
                    lastPatch = p.getPatch();
                    allPatchIds.append(p.getPatch().getId()).append("#");
                }
                allPatchIds.replace(allPatchIds.length() - 1, allPatchIds.length(), "");

                if (lastPatch == null) {
                    throw new UnknownRecipeException("Not a Wildfly patch");
                } else if (!PatchProductType.EAP7.equals(lastPatch.getProductType())) {
                    throw new UnknownRecipeException();
                }

                DeploymentProperties props = new DeploymentProperties(0, lastPatch.getIdentityName(),
                    lastPatch.getTargetVersion(), lastPatch.getDescription(), DestinationComplianceMode.full);

                ConfigurationDefinition config = new ConfigurationDefinition("wildfly-patch", null);
                PropertyDefinitionSimple allPatchIdsProp = new PropertyDefinitionSimple("allPatchIds",
                    "Hash-separated list of all individual patches the patch bundle is composed of.", true,
                    PropertySimpleType.STRING);
                allPatchIdsProp.setDefaultValue(allPatchIds.toString());
                allPatchIdsProp.setReadOnly(true);
                PropertyDefinitionSimple patchTypeProp = new PropertyDefinitionSimple("patchType",
                    "The type of the patch", true, PropertySimpleType.STRING);
                patchTypeProp.setDefaultValue("patch-bundle");
                patchTypeProp.setReadOnly(true);

                config.put(allPatchIdsProp);
                config.put(patchTypeProp);
                addCommonProperties(config);

                parseResults = new RecipeParseResults(props, config, null);
                fileName = lastPatch.getIdentityName();
                recipe = patchBundle.getContents();
            }
        } finally {
            in.close();
        }

        fileName += ".zip";

        Map<String, File> patchFiles = new HashMap<String, File>();
        patchFiles.put(fileName, distributionFile);

        return new BundleDistributionInfo(recipe, parseResults, patchFiles);
    }

    void addCommonProperties(ConfigurationDefinition config) {
        PropertyDefinitionSimple overrideProp = new PropertyDefinitionSimple("override",
            "The value is a comma-separated list of the miscellaneous items in the patch that can be overridden on the server whether the item reports a conflict or not.",
            false, PropertySimpleType.LONG_STRING);
        PropertyDefinitionSimple overrideAllProp = new PropertyDefinitionSimple("override-all",
            "The argument does not expect any value and is optional. The default is 'false'. It signifies to bypass any content verification on the miscellaneous items changed by the patch.",
            false, PropertySimpleType.BOOLEAN);
        PropertyDefinitionSimple overrideModules = new PropertyDefinitionSimple("override-modules",
            "The argument does not expect any value and is optional. The default is 'false'. It signifies to bypass any content verification on the modules and OSGi bundles affected by the patch.",
            false, PropertySimpleType.LONG_STRING);
        PropertyDefinitionSimple preserve = new PropertyDefinitionSimple("preserve",
            "The value is a comma-separated list of the miscellaneous items that must be preserved and not modified by applying or rolling back a patch.",
            false, PropertySimpleType.LONG_STRING);
        PropertyDefinitionSimple restart = new PropertyDefinitionSimple("restart",
            "This only applies if the server being deployed to is running. When true, the server is stopped before the patch application and started back up afterwards.",
        true, PropertySimpleType.BOOLEAN);
        restart.setDefaultValue(Boolean.toString(true));
        PropertyDefinitionSimple takeOver = new PropertyDefinitionSimple("takeOver",
            "Set this to true ONLY in cases where you want this destination to become the new originating destination for the patch deployments to the servers in the resource group. Each server can have at most 1 originating destination for its patch deployments at a time. As an example, you might want to use this in cases where a destination that was used for prior patch deployments was deleted, yet the target servers weren't purged and thus still relate to the deleted destination as their originating destination.",
            true, PropertySimpleType.BOOLEAN);
        takeOver.setDefaultValue(Boolean.toString(false));

        config.put(overrideProp);
        config.put(overrideAllProp);
        config.put(overrideModules);
        config.put(preserve);
        config.put(restart);
        config.put(takeOver);
    }

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void shutdown() {
    }
}
