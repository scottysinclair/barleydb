package scott.barleydb.api.config;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import scott.barleydb.api.core.util.EnvironmentAccessor;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;

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

    private Map<String,NodeType> nodeTypes = new LinkedHashMap<>();

    public static EntityType create(Definitions definitions, EntitySpec entityTypeSpec) {
        System.out.println("Creating entity type: " + entityTypeSpec.getClassName());
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
            NodeType nt = NodeType.create(entityType, nodeTypeSpec);
            entityType.nodeTypes.put(nt.getName(), nt);
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
        for (NodeType nd : nodeTypes.values()) {
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

    public Collection<NodeType> getNodeTypes() {
        return Collections.unmodifiableCollection(nodeTypes.values());
    }

    public boolean supportsOptimisticLocking() {
        for (NodeType nd : nodeTypes.values()) {
            if (nd.isOptimisticLock()) {
                return true;
            }
        }
        return false;
    }

    public NodeType getNodeType(String name, boolean mustExist) {
        NodeType nt  = nodeTypes.get(name);
        if (nt == null && mustExist) {
            throw new IllegalStateException("Node '" + name + "' must exist in entity '" + interfaceName);
        }
        return nt;
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
