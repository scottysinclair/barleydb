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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.util.EnvironmentAccessor;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;

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
public class EntityType implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Definitions definitions;

    private String interfaceName;

    private String tableName;

    private boolean abstractEntity;

    private String parentTypeName;

    private String keyNodeName;

    private List<NodeType> nodeTypes = new LinkedList<NodeType>();

    public static EntityType create(Definitions definitions, EntitySpec entityTypeSpec) {
        if (entityTypeSpec.getTableName() == null) {
            throw new IllegalArgumentException("Entity type must have a table name");
        }
        Collection<NodeSpec> keyNodes = entityTypeSpec.getPrimaryKeyNodes(true);
        if (keyNodes == null || keyNodes.isEmpty()) {
            throw new IllegalArgumentException("Entity type must have a key node");
        }
        if (keyNodes.size() > 1) {
            throw new IllegalArgumentException("Entity type must currently must have a single key node");
        }
        EntityType entityType = new EntityType(definitions);
        entityType.interfaceName = entityTypeSpec.getClassName();
        entityType.tableName = entityTypeSpec.getTableName();
        entityType.abstractEntity = entityTypeSpec.isAbstractEntity();
        if (entityTypeSpec.getParentEntity() != null) {
            entityType.parentTypeName = entityTypeSpec.getParentEntity().getClassName();
        }
        entityType.keyNodeName = keyNodes.iterator().next().getName();
        createNodeTypesFromEntitySpec(entityType, entityTypeSpec);
        return entityType;
      }

    private static void createNodeTypesFromEntitySpec(EntityType entityType, EntitySpec entityTypeSpec) {
        if (entityTypeSpec.getParentEntity() != null) {
            createNodeTypesFromEntitySpec(entityType, entityTypeSpec.getParentEntity());
        }
        for (NodeSpec nodeTypeSpec: entityTypeSpec.getNodeSpecs()) {
            entityType.nodeTypes.add( NodeType.create(entityType, nodeTypeSpec) );
        }
    }

    private EntityType(Definitions definitions) {
        this.definitions = definitions;
    }

    /**
     *
     * @param interfaceName
     * @return the matching node definition or null
     * @throws IllegalStateException if more than one match is found
     */
    public NodeType getNodeTypeWithRelationTo(String interfaceName) {
        NodeType result = null;
        for (NodeType nd : nodeTypes) {
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
        return getNodeType(keyNodeName, true).getColumnName();
    }

    public List<NodeType> getNodeTypes() {
        return nodeTypes;
    }

    public boolean supportsOptimisticLocking() {
        for (NodeType nd : nodeTypes) {
            if (nd.isOptimisticLock()) {
                return true;
            }
        }
        return false;
    }

    public NodeType getNodeType(String name, boolean mustExist) {
        for (NodeType nd : nodeTypes) {
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
