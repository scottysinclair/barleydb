package scott.sort.api.config;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;

import scott.sort.api.core.util.EnvironmentAccessor;

/**
 * The static configuration information about an entity.
 *
 * Entity types are serializable and can be sent to the client on startup of a client environment.
 *
 * Runtime data which refer to entity types, should just send the namespace and interface name of the entity type
 * instead of the actual object
 *
 *
 * @author scott
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class EntityType implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlTransient
    private Definitions definitions;

    @XmlAttribute(name = "interface")
    private String interfaceName;

    private String tableName;

    @XmlAttribute(name = "abstract")
    private boolean abstractEntity;

    @XmlAttribute(name = "parentType")
    private String parentTypeName;

    @XmlAttribute(name = "key")
    private String keyNodeName;

    @XmlElement(name = "node")
    private List<NodeDefinition> nodeDefinitions = new LinkedList<NodeDefinition>();

    public EntityType() {}

    public EntityType(Definitions definitions, String interfaceName, boolean abstractEntity, String parentTypeName, String tableName, String keyNodeName, List<NodeDefinition> nodeDefinitions) {
        if (tableName == null) {
            throw new IllegalArgumentException("Entity type must have a table name");
        }
        if (keyNodeName == null) {
            throw new IllegalArgumentException("Entity type must have a key node");
        }
        this.definitions = definitions;
        this.interfaceName = interfaceName;
        this.parentTypeName = parentTypeName;
        this.abstractEntity = abstractEntity;
        this.tableName = tableName;
        this.keyNodeName = keyNodeName;
        this.nodeDefinitions = nodeDefinitions;
        for (NodeDefinition nd : nodeDefinitions) {
            nd.setEntityType(this);
        }
    }

    public void afterUnmarshal(Unmarshaller unmarshall, Object parent) {
        this.definitions = (Definitions) parent;
    }

    /**
     *
     * @param interfaceName
     * @return the matching node definition or null
     * @throws IllegalStateException if more than one match is found
     */
    public NodeDefinition getNodeWithRelationTo(String interfaceName) {
        NodeDefinition result = null;
        for (NodeDefinition nd : nodeDefinitions) {
            if (interfaceName.equals(nd.getRelationInterfaceName())) {
                if (result != null) {
                    throw new IllegalStateException("More than one node definition relates to interface '" + interfaceName + "'");
                }
                result = nd;
            }
        }
        return result;
    }

    public Definitions getDefinitions() {
        return definitions;
    }

    /**
     * The provided type must be the direct parent
     * @param type
     * @return
     */
    public boolean isDirectChildOf(EntityType type) {
        return parentTypeName != null && parentTypeName.equals(type.getInterfaceName());
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getInterfaceShortName() {
        return interfaceName.substring(interfaceName.lastIndexOf('.') + 1, interfaceName.length());
    }

    public boolean isAbstract() {
        return abstractEntity;
    }

    public String getTableName() {
        return tableName;
    }

    public String getKeyNodeName() {
        return keyNodeName;
    }

    public String getKeyColumn() {
        return getNode(keyNodeName, true).getColumnName();
    }

    public List<NodeDefinition> getNodeDefinitions() {
        return nodeDefinitions;
    }

    public boolean supportsOptimisticLocking() {
        for (NodeDefinition nd : nodeDefinitions) {
            if (nd.isOptimisticLock()) {
                return true;
            }
        }
        return false;
    }

    public NodeDefinition getNode(String name, boolean mustExist) {
        for (NodeDefinition nd : nodeDefinitions) {
            if (nd.getName().equals(name)) {
                return nd;
            }
        }
        throw new IllegalStateException("Node '" + name + "' must exist in entity '" + interfaceName);
    }

    public void write(ObjectOutputStream out) throws IOException {
        out.writeUTF(definitions.getNamespace());
        out.writeUTF(interfaceName);
    }

    public static EntityType read(ObjectInputStream ois) throws IOException {
        final String namespace = ois.readUTF();
        return EnvironmentAccessor.get().getDefinitions(namespace).getEntityTypeMatchingInterface(ois.readUTF(), true);
    }

    @Override
    public String toString() {
        return "EntityType [ " + interfaceName + " ]";
    }

}
