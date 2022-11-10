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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.core.util.EnvironmentAccessor;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.EnumValueSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.RelationSpec;
import scott.barleydb.api.specification.SuppressionSpec;

public class NodeType implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(NodeType.class);

    private final EntityType entityType;

    private String name;

    private JavaType javaType;

    private Relation relation;

    private String columnName;

    private JdbcType jdbcType;

    private String typeConverterFqn;

    private Boolean optimisticLock;

    /**

     * if this node type is required to have a value.
     */
    private boolean mandatory;

    private Class<?> enumType;
    private EnumSpec enumSpec;
    private SuppressionSpec suppressionSpec;

    private Object fixedValue;

    public static NodeType create(EntityType entityType, NodeSpec nodeSpec) {
        NodeType nodeType = new NodeType(entityType);
        nodeType.name = nodeSpec.getName();
        nodeType.javaType = nodeSpec.getJavaType();
//        System.out.println(nodeSpec.getEntity().getClassName()  + "." + nodeSpec.getName() + "  " + nodeSpec.getRelationSpec());
        if (nodeSpec.getRelationSpec() != null) {
            RelationSpec spec = nodeSpec.getRelationSpec();
            NodeSpec backReference = spec.getBackReference();
            NodeSpec sortNode = spec.getSortNode();
            NodeSpec onwardJoin = spec.getOwnwardJoin();
            nodeType.relation = new Relation(spec.getEntitySpec().getClassName(),
                    spec.getType(),
                    backReference != null ? backReference.getName() : null,
                    sortNode != null ? sortNode.getName() : null,
                    onwardJoin != null ? onwardJoin.getName() : null);
        }
        nodeType.columnName = nodeSpec.getColumnName();
        nodeType.jdbcType = nodeSpec.getJdbcType();
        nodeType.mandatory = nodeSpec.getNullable() == Nullable.NOT_NULL;
        nodeType.optimisticLock = nodeSpec.isOptimisticLock();
        nodeType.enumSpec = nodeSpec.getEnumSpec();
        if (nodeType.getEnumSpec() != null) {
            try {
                nodeType.enumType = NodeType.class.getClassLoader().loadClass( nodeSpec.getEnumSpec().getClassName() );
            } catch (ClassNotFoundException e) {
                LOG.info("Enum class " + nodeSpec.getEnumSpec().getClassName() + " is not available.");
            }
        }
        if (nodeSpec.getFixedValue() != null) {
            if (nodeType.getEnumSpec() != null) {
                nodeType.fixedValue = convertToEnum(nodeType, nodeSpec.getFixedValue());
            }
            else {
                nodeType.fixedValue = nodeSpec.getFixedValue();
            }
        }
        nodeType.suppressionSpec = nodeSpec.getSuppression();
        nodeType.typeConverterFqn = nodeSpec.getTypeConverter();
        return nodeType;
    }

    /**
     *
     * @param nd
     * @param value
     * @return
     */
    public static <E extends Enum<E>> Object convertToEnum(NodeType nd, Object value) {
        for (EnumValueSpec enumValue : nd.getEnumSpec().getEnumValues()) {
            if (enumValue.getId().equals(value)) {
                if (nd.getEnumType() != null) {
                    /*
                     * ok we have the enum class, so we will return the real enum value
                     */
                    Class<E> enumType = nd.getEnumType();
                    try {
                        Object result = Enum.valueOf(enumType, enumValue.getName());
                        LOG.debug("Converted " + value + " to enum value " + enumValue.getName());
                        return result;
                    }
                    catch(IllegalArgumentException x) {
                        LOG.warn("Could not find enum value with name '" + enumValue.getName() + "'");
                        return null;
                    }
                }
                else {
                    /*
                     * we do not have the enum class so we return the string name of the enum value
                     */
                    LOG.trace("Converted " + value + " to enum string value " + enumValue.getName());
                    return enumValue.getName();
                }
            }
        }
        return null;
    }

    private NodeType(EntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public NodeType clone() {
        try {
            return (NodeType) super.clone();
        } catch (CloneNotSupportedException x) {
            throw new IllegalStateException("Could not clone node definition", x);
        }
    }

    public boolean isPrimaryKey() {
        return entityType.getKeyNodeName().equals(name);
    }

    public boolean isForeignKey() {
        return relation != null && columnName != null;
    }

    public boolean isOptimisticLock() {
        return optimisticLock != null && optimisticLock;
    }

    public String getTypeConverterFqn() {
        return typeConverterFqn;
    }

    public String getSortNode() {
        return relation != null ? relation.getSortNodeName() : null;
    }

    public boolean isSuppressedFromDto() {
      return suppressionSpec == SuppressionSpec.DTO;
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getEnumType() {
        return (Class<E>)enumType;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public EnumSpec getEnumSpec() {
        return enumSpec;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getName() {
        return name;
    }

    public Object getFixedValue() {
        return fixedValue;
    }

    public JavaType getJavaType() {
        return javaType;
    }

	public boolean isForeignKeyColumn() {
		return columnName != null && relation != null;
	}
	
	public boolean isOneToManyRelation() {
		return columnName == null && relation != null;
	}

    public Relation getRelation() {
        return relation;
    }

    public String getRelationInterfaceName() {
        return relation != null ? relation.getInterfaceName() : null;
    }

    public String getColumnName() {
        return columnName;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public String getForeignNodeName() {
        return relation != null ? relation.getForeignNodeName() : null;
    }

    public String getJoinProperty() {
        return relation != null ? relation.getJoinProperty() : null;
    }

    public boolean isOwns() {
        return relation != null && relation.getRelationType() == RelationType.OWNS;
    }

    public boolean isRefers() {
        return relation != null && relation.getRelationType() == RelationType.REFERS;
    }

    public boolean isDependsOn() {
        return relation != null && relation.getRelationType() == RelationType.DEPENDS;
    }

    public boolean dependsOrOwns() {
        return isOwns() || isDependsOn();
    }

    public void write(ObjectOutputStream out) throws IOException {
        out.writeUTF(entityType.getDefinitions().getNamespace());
        out.writeUTF(entityType.getInterfaceName());
        out.writeUTF(name);
    }

    public static NodeType read(ObjectInputStream ois) throws IOException {
        String namespace = ois.readUTF();
        String entityTypeName = ois.readUTF();
        String nodeName = ois.readUTF();
        return EnvironmentAccessor.get().getDefinitions(namespace).getEntityTypeMatchingInterface(entityTypeName, true).getNodeType(nodeName, true);
    }

    /**
     * Handy string representation
     * @return
     */
    public String getShortId() {
        return entityType.getInterfaceShortName() + "." + name;
    }
        @Override
    public String toString() {
        return "NodeType - " + entityType.getInterfaceShortName() + "." + name
                + " [javaType=" + javaType + ", relation=" + relation
                + ", columnName=" + columnName + ", jdbcType=" + jdbcType + "]";
    }
}
