package org.rhq.embeddedagent.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class AgentSubsystemExtension implements Extension {

    private final Logger log = Logger.getLogger(AgentSubsystemExtension.class);

    public static final String NAMESPACE = "urn:org.rhq:embeddedagent:1.0";
    public static final String SUBSYSTEM_NAME = "embeddedagent";

    private final SubsystemParser parser = new SubsystemParser();

    private static final String RESOURCE_NAME = AgentSubsystemExtension.class.getPackage().getName()
        + ".LocalDescriptions";

    protected static final String PLUGINS_ELEMENT = "plugins";
    protected static final String PLUGIN_ELEMENT = "plugin";
    protected static final String PLUGIN_NAME = "name";
    protected static final String PLUGIN_ENABLED = "enabled";
    protected static final String AGENT_ENABLED = "enabled";
    protected static final boolean AGENT_ENABLED_DEFAULT = false;
    protected static final boolean PLUGIN_ENABLED_DEFAULT = true;

    protected static final String AGENT_RESTART_OP = "restart";
    protected static final String AGENT_STOP_OP = "stop";
    protected static final String AGENT_STATUS_OP = "status";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME,
            AgentSubsystemExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        log.info("Initializing embedded agent subsystem");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem
            .registerSubsystemModel(AgentSubsystemDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * The subsystem parser, which uses stax to read and write to and from xml
     */
    private static class SubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // The agent "enabled" attribute is required
            ParseUtils.requireSingleAttribute(reader, AGENT_ENABLED);

            // Add the main subsystem 'add' operation
            final ModelNode opAdd = new ModelNode();
            opAdd.get(OP).set(ADD);
            opAdd.get(OP_ADDR).set(PathAddress.pathAddress(SUBSYSTEM_PATH).toModelNode());
            String agentEnabledValue = reader.getAttributeValue(null, AGENT_ENABLED);
            if (agentEnabledValue != null) {
                opAdd.get(AGENT_ENABLED).set(agentEnabledValue);
            }

            ModelNode pluginsAttributeNode = opAdd.get(PLUGINS_ELEMENT);

            // Read the children elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (!reader.getLocalName().equals(PLUGINS_ELEMENT)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                    if (reader.isStartElement()) {
                        readPlugin(reader, pluginsAttributeNode);
                    }
                }
            }

            list.add(opAdd);
        }

        private void readPlugin(XMLExtendedStreamReader reader, ModelNode pluginsAttributeNode)
            throws XMLStreamException {

            if (!reader.getLocalName().equals(PLUGIN_ELEMENT)) {
                throw ParseUtils.unexpectedElement(reader);
            }

            String pluginName = null;
            boolean pluginEnabled = PLUGIN_ENABLED_DEFAULT;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attr = reader.getAttributeLocalName(i);
                String value = reader.getAttributeValue(i);
                if (attr.equals(PLUGIN_ENABLED)) {
                    pluginEnabled = Boolean.parseBoolean(value);
                } else if (attr.equals(PLUGIN_NAME)) {
                    pluginName = value;
                } else {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
            ParseUtils.requireNoContent(reader);
            if (pluginName == null) {
                throw ParseUtils.missingRequiredElement(reader, Collections.singleton(PLUGIN_NAME));
            }

            // Add the plugin to the plugins attribute node
            pluginsAttributeNode.add(pluginName, pluginEnabled);
        }

        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {
            ModelNode node = context.getModelNode();

            // <subsystem>
            context.startSubsystemElement(AgentSubsystemExtension.NAMESPACE, false);
            writer.writeAttribute(AGENT_ENABLED,
                String.valueOf(node.get(AGENT_ENABLED).asBoolean(AGENT_ENABLED_DEFAULT)));

            // <plugins>
            writer.writeStartElement(PLUGINS_ELEMENT);
            ModelNode plugins = node.get(PLUGINS_ELEMENT);
            if (plugins != null && plugins.isDefined()) {
                for (Property property : plugins.asPropertyList()) {
                    // <plugin>
                    writer.writeStartElement(PLUGIN_ELEMENT);
                    writer.writeAttribute(PLUGIN_NAME, property.getName());
                    writer.writeAttribute(PLUGIN_ENABLED, property.getValue().asString());
                    // </plugin>
                    writer.writeEndElement();
                }
            }
            // </plugins>
            writer.writeEndElement();
            // </subsystem>
            writer.writeEndElement();
        }
    }
}
