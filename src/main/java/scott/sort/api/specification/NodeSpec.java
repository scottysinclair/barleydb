package scott.sort.api.specification;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.core.types.Nullable;

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

    @XmlAttribute
    private Class<? extends Enum<?>> enumType;

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
    private boolean optimisticLock;


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

    public Class<? extends Enum<?>> getEnumType() {
        return enumType;
    }

    public void setEnumType(Class<? extends Enum<?>> enumType) {
        this.enumType = enumType;
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

    public boolean isOptimisticLock() {
        return optimisticLock;
    }

    public void setOptimisticLock(boolean optimisticLock) {
        this.optimisticLock = optimisticLock;
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
            sb.append("\n\tprimaryKey=true");
        }
        if (name != null) {
            sb.append("\n\tname=");
            sb.append(name);
        }
        if (javaType != null) {
            sb.append("\n\tjavaType=");
            sb.append(javaType);
        }
        if (enumType != null) {
            sb.append("\n\tenumType=");
            sb.append(enumType);
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
