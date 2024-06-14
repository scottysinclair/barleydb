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
import scott.barleydb.api.specification.KeyGenSpec;
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

    private String dtoClassName;

    private boolean abstractEntity;

    private String parentTypeName;

    private String keyNodeName;

    private KeyGenSpec keyGenSpec;

    private Map<String,NodeType> nodeTypes = new LinkedHashMap<>();

    public static EntityType create(Definitions definitions, EntitySpec entityTypeSpec) {
//        System.out.println("Creating entity type: " + entityTypeSpec.getClassName());
        if (entityTypeSpec.getTableName() == null) {
            throw new IllegalArgumentException("Entity type must have a table name");
        }
        Collection<NodeSpec> keyNodes = entityTypeSpec.getPrimaryKeyNodes(true);
        if (keyNodes.size() > 1) {
            throw new IllegalArgumentException("Entity type " + entityTypeSpec.getTableName() +  " must currently have a single key node");
        }
        EntityType entityType = new EntityType(definitions);
        entityType.interfaceName = entityTypeSpec.getClassName();
        entityType.tableName = entityTypeSpec.getTableName();
        entityType.dtoClassName = entityTypeSpec.getDtoClassName();
        entityType.abstractEntity = entityTypeSpec.isAbstractEntity();
        if (entityTypeSpec.getParentEntity() != null) {
            entityType.parentTypeName = entityTypeSpec.getParentEntity().getClassName();
        }
        if (!keyNodes.isEmpty()) {
           entityType.keyNodeName = keyNodes.iterator().next().getName();
        }
        /*
         * if there is only 1 primary key then we check for the generation policy.
         * if there is any other number of keys, then we assume it is a business key.
         */
        if (entityTypeSpec.getPrimaryKeyNodes(true).size() == 1) {
          entityType.keyGenSpec = entityTypeSpec.getPrimaryKeyNodes(true).iterator().next().getKeyGenSpec();
        }
        if (entityType.keyGenSpec == null) {
          //we default to framework
          entityType.keyGenSpec = KeyGenSpec.CLIENT;
        }
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

    public KeyGenSpec getKeyGenSpec() {
	    return keyGenSpec;
	  }

  public boolean isAbstract() {
        return abstractEntity;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean hasPk() {
       return keyNodeName != null;
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

    public String getDtoClassName() {
      return dtoClassName;
    }

}
