package scott.barleydb.api.specification;

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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.JdbcType;
import scott.barleydb.api.core.types.Nullable;

@XmlAccessorType(XmlAccessType.NONE)
public class NodeSpec implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /**
     * the entity which we belong to
     */
    @XmlTransient
    private EntitySpec entity;

    @XmlAttribute
    private String name;

    private boolean primaryKey = false;

    @XmlAttribute
    private JavaType javaType;

    @XmlIDREF
    @XmlElement
    private EnumSpec enumSpec = null;

    private RelationSpec relation;

    @XmlAttribute
    private JdbcType jdbcType;

    @XmlAttribute
    private String columnName;

    @XmlAttribute
    private Nullable nullable;

    @XmlElement
    private Object fixedValue;

    @XmlAttribute
    private Integer length;

    @XmlAttribute
    private Integer precision;

    @XmlAttribute
    private Integer scale;

    @XmlAttribute
    private boolean optimisticLock;

    @XmlElement
    private SuppressionSpec suppression;

    @XmlElement
    private String typeConverter;

    @XmlAttribute
    private KeyGenSpec keyGenSpec;

    /**
     * key used to lookup the enumSpec, only required by static definition processing.
     */
    private transient Object enumSpecIdentifier;


    @XmlAttribute(name="pk")
    private Boolean getPrimaryKeyForJaxb() {
        return primaryKey ? primaryKey : null;
    }

    @SuppressWarnings("unused") //called by JAXB
    private void setPrimaryKeyForJaxb(Boolean pk) {
        primaryKey = pk != null && pk.booleanValue();
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public KeyGenSpec getKeyGenSpec() {
        return keyGenSpec;
    }

    public void setKeyGenSpec(KeyGenSpec keyGenSpec) {
        this.keyGenSpec = keyGenSpec;
    }

    @XmlID
    @XmlElement
    public String getId() {
        return entity.getClassName() + "." +  name;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public EntitySpec getEntity() {
        return entity;
    }

    public void setEntity(EntitySpec entity) {
        this.entity = entity;
    }

    public String getTypeConverter() {
        return typeConverter;
    }

    public void setTypeConverter(String typeConverter) {
        this.typeConverter = typeConverter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public void setJavaType(JavaType javaType) {
        this.javaType = javaType;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public EnumSpec getEnumSpec() {
        return enumSpec;
    }

    public void setEnumSpec(EnumSpec enumSpec) {
        this.enumSpec = enumSpec;
    }

    public void setEnumSpecIdentifier(Class<?> enumSpecIdentifier) {
        this.enumSpecIdentifier = enumSpecIdentifier;
    }

    public Object getEnumSpecIdentifier() {
        return enumSpecIdentifier;
    }

    @XmlElement(name="relation")
    public RelationSpec getRelationSpec() {
        return relation;
    }

    public void setRelationSpec(RelationSpec relation) {
        this.relation = relation;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Nullable getNullable() {
        return nullable;
    }

    public void setNullable(Nullable nullable) {
        this.nullable = nullable;
    }

    public Object getFixedValue() {
        return fixedValue;
    }

    public void setFixedValue(Object fixedValue) {
        this.fixedValue = fixedValue;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public RelationSpec getRelation() {
        return relation;
    }

    public void setRelation(RelationSpec relation) {
        this.relation = relation;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public boolean isOptimisticLock() {
        return optimisticLock;
    }

    public void setOptimisticLock(boolean optimisticLock) {
        this.optimisticLock = optimisticLock;
    }

    public SuppressionSpec getSuppression() {
        return suppression;
    }

    public void setSuppression(SuppressionSpec suppression) {
        this.suppression = suppression;
    }

    @Override
    public NodeSpec clone() {
        try {
            NodeSpec copy = (NodeSpec)super.clone();
            if (relation != null) {
                copy.relation = (RelationSpec)relation.clone();
            }
            return copy;
        }
        catch (CloneNotSupportedException x) {
            throw new IllegalStateException("Error cloning nodespec", x);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NodeSpec [");
        if (primaryKey) {
            sb.append("\n\tprimaryKey=true, keyGen=");
            sb.append(keyGenSpec);
        }
        if (name != null) {
            sb.append("\n\tname=");
            sb.append(name);
        }
        if (javaType != null) {
            sb.append("\n\tjavaType=");
            sb.append(javaType);
        }
        if (enumSpec != null) {
            sb.append("\n\tenumSpec=");
            sb.append(enumSpec);
        }
        if (relation != null) {
            sb.append("\n\trelation=");
            sb.append(relation);
        }
        if (jdbcType != null) {
            sb.append("\n\tjdbcType=");
            sb.append(jdbcType);
        }
        if (columnName != null) {
            sb.append("\n\tcolumnName=");
            sb.append(columnName);
        }
        if (nullable != null) {
            sb.append("\n\tnullable=");
            sb.append(nullable);
        }
        if (fixedValue != null) {
            sb.append("\n\tfixedValue=");
            sb.append(fixedValue);
        }
        if (length != null) {
            sb.append("\n\tlength=");
            sb.append(length);
        }
        sb.append("\n]");
        return sb.toString();
    }

}
