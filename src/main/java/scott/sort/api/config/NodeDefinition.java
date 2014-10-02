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

import java.io.Serializable;

import javax.xml.bind.annotation.*;
import javax.xml.bind.*;

import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;

@XmlAccessorType(XmlAccessType.FIELD)
public class NodeDefinition implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    public static class Builder {
        private NodeDefinition nd = new NodeDefinition();

        public Builder ref(String name, String interfaceName, String columnName, JdbcType jdbcType) {
            nd.name = name;
            nd.relation = new Relation(interfaceName, RelationType.REFERS, null, null);
            nd.columnName = columnName;
            nd.jdbcType = jdbcType;
            return this;
        }

        public Builder many(String name, String interfaceName, String foreignNodeName, String joinProperty) {
            nd.name = name;
            nd.relation = new Relation(interfaceName, RelationType.REFERS, foreignNodeName, joinProperty);
            return this;
        }

        public Builder value(String name, JavaType javaType, String columnName, JdbcType jdbcType) {
            nd.name = name;
            nd.javaType = javaType;
            nd.columnName = columnName;
            nd.jdbcType = jdbcType;
            return this;
        }

        public Builder enumm(String name, Class<? extends Enum<?>> enumType, String columnName, JdbcType jdbcType) {
            nd.name = name;
            nd.enumType = enumType;
            nd.javaType = JavaType.ENUM;
            nd.columnName = columnName;
            nd.jdbcType = jdbcType;
            return this;
        }

        public Builder owns() {
            nd.relation = nd.relation.copy(RelationType.OWNS);
            return this;
        }

        public Builder dependsOn() {
            nd.relation = nd.relation.copy(RelationType.DEPENDS);
            return this;
        }

        public Builder optimisticLock() {
            nd.optimisticLock = true;
            return this;
        }

        public Builder fixedValue(Object value) {
            nd.fixedValue = value;
            return this;
        }

        public NodeDefinition end() {
            return nd;
        }
    }

    @XmlTransient
    private EntityType entityType;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private JavaType javaType;

    private Relation relation;

    private transient String columnName;

    private transient JdbcType jdbcType;

    private transient Boolean optimisticLock;

    private Class<?> enumType;

    private Object fixedValue;

    public NodeDefinition() {}

    public NodeDefinition(String name, String interfaceName, RelationType relationType, String columnName, JdbcType jdbcType, String foreignNodeName, String joinEntityName, Object fixedValue) {
        this.name = name;
        this.relation = new Relation(interfaceName, RelationType.REFERS, foreignNodeName, joinEntityName);
        this.columnName = columnName;
        this.jdbcType = jdbcType;
        this.fixedValue = fixedValue;
    }

    @Override
    public NodeDefinition clone() {
        try {
            return (NodeDefinition) super.clone();
        } catch (CloneNotSupportedException x) {
            throw new IllegalStateException("Could not clone node definition", x);
        }
    }

    void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public void afterUnmarshal(Unmarshaller unmarshall, Object parent) {
        this.entityType = (EntityType) parent;
    }

    public boolean isPrimaryKey() {
        return entityType.getKeyNodeName().equals(name);
    }

    public boolean isForeignKey() {
        return relation != null && columnName != null;
    }

    @XmlAttribute
    public boolean isOptimisticLock() {
        return optimisticLock != null && optimisticLock;
    }

    public Class<?> getEnumType() {
        return enumType;
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

    public String getRelationInterfaceName() {
        return relation != null ? relation.getInterfaceName() : null;
    }

    @XmlAttribute
    public String getColumnName() {
        return columnName;
    }

    @XmlAttribute
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
        return relation.getRelationType() == RelationType.OWNS;
    }

    public boolean isDependsOn() {
        return relation.getRelationType() == RelationType.DEPENDS;
    }

    public boolean dependsOrOwns() {
        return isOwns() || isDependsOn();
    }

    @Override
    public String toString() {
        return "NodeDefinition - " + entityType.getInterfaceShortName() + "." + name
                + " [javaType=" + javaType + ", relation=" + relation
                + ", columnName=" + columnName + ", jdbcType=" + jdbcType + "]";
    }

}
