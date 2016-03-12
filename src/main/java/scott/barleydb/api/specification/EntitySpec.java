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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import scott.barleydb.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.barleydb.api.specification.constraint.PrimaryKeyConstraintSpec;
import scott.barleydb.api.specification.constraint.UniqueConstraintSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class EntitySpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlID
    @XmlAttribute
    private String className;

    @XmlElement(name = "queryClass")
    private String queryClassName;

    @XmlAttribute
    private String tableName;

    @XmlAttribute(name="abstract")
    private boolean abstractEntity;
    
    @XmlIDREF
    @XmlElement(name = "parent")
    private EntitySpec parentEntitySpec;

    @XmlJavaTypeAdapter(NodeSpecAdapter.class)
    @XmlElement(name="NodeSpecs")
    private final LinkedHashMap<String,NodeSpec> nodeSpecs = new LinkedHashMap<>();

    @XmlElement(name = "Constraints")
    private final Contraints constraints = new Contraints();

    private static class Contraints {
        @XmlElement(name="PrimaryKey")
        private PrimaryKeyConstraintSpec primaryKeyConstraint;

        @XmlElement(name="ForeignKey")
        private final List<ForeignKeyConstraintSpec> foreignKeyConstraints = new LinkedList<>();

        @XmlElement(name="UniqueConstraint")
        private final List<UniqueConstraintSpec> uniqueConstraints = new LinkedList<>();
    }

    public void afterUnmarshal(Unmarshaller unmarshall, Object parent) {
        for (NodeSpec nodeSpec: nodeSpecs.values()) {
            nodeSpec.setEntity(this);
        }
    }

    public boolean isAbstractEntity() {
        return abstractEntity;
    }

    public void setAbstractEntity(boolean abstractEntity) {
        this.abstractEntity = abstractEntity;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public EntitySpec getParentEntity() {
        return parentEntitySpec;
    }

    public void setParentEntitySpec(EntitySpec parentEntity) {
        this.parentEntitySpec = parentEntity;
    }

    public String getQueryClassName() {
        return queryClassName;
    }

    public void setQueryClassName(String queryClassName) {
        this.queryClassName = queryClassName;
    }
    
    public void add(NodeSpec nodeSpec) {
        nodeSpecs.put(nodeSpec.getName(), nodeSpec);
    }

    public Collection<NodeSpec> getNodeSpecs() {
        return Collections.unmodifiableCollection( nodeSpecs.values() );
    }

    public List<UniqueConstraintSpec> getUniqueConstraints() {
        return constraints.uniqueConstraints;
    }

    public void add(UniqueConstraintSpec uniqueConstraintSpec) {
        constraints.uniqueConstraints.add(uniqueConstraintSpec);
    }

    public int indexOf(UniqueConstraintSpec uniqueConstraintSpec) {
        return constraints.uniqueConstraints.indexOf(uniqueConstraintSpec);
    }

    public void add(ForeignKeyConstraintSpec foreignKeyConstraintSpec) {
        constraints.foreignKeyConstraints.add(foreignKeyConstraintSpec);
    }

    public List<ForeignKeyConstraintSpec> getForeignKeyConstraints() {
        return constraints.foreignKeyConstraints;
    }

    public NodeSpec getNodeSpec(String name) {
        return nodeSpecs.get(name);
    }

    public void verify() {
        verifyEachNodeHasAName();
        verifyColumnNamesAreUnique();
        verifyEachColumnHasAJdbcTypeAndNullable();
    }


    private void verifyColumnNamesAreUnique() {
        Set<String> columnNames = new HashSet<>();
        for (NodeSpec spec: nodeSpecs.values()) {
            if (spec.getColumnName() != null && !columnNames.add(spec.getColumnName())) {
                throw new IllegalStateException("2 or more properties have column name " + spec.getColumnName());
            }
        }
    }

    private void verifyEachColumnHasAJdbcTypeAndNullable() {
        for (NodeSpec spec: nodeSpecs.values()) {
            if (spec.getColumnName() != null) {
                if (spec.getJdbcType() == null) {
                    throw new IllegalStateException("Each column must specify a JDBC type: " + spec);
                }
                if (spec.getNullable() == null) {
                    throw new IllegalStateException("Each column must specify nullable: " + spec);
                }
            }
        }
    }

    private void verifyEachNodeHasAName() {
        for (NodeSpec spec: nodeSpecs.values()) {
            if (spec.getName() == null) {
                throw new IllegalStateException("Each column must have a name " + spec);
            }
        }
    }

    public Collection<NodeSpec> getPrimaryKeyNodes(boolean checkParent) {
        Collection<NodeSpec> key = null;
        for (NodeSpec spec: nodeSpecs.values()) {
            if (spec.isPrimaryKey()) {
                if (key == null) {
                    key = new LinkedList<NodeSpec>();
                }
                key.add(spec);
            }
        }
        if (key == null && checkParent && parentEntitySpec != null) {
            key = parentEntitySpec.getPrimaryKeyNodes(true);
        }
        return key;
    }

    public PrimaryKeyConstraintSpec getPrimaryKeyConstraint() {
        return constraints.primaryKeyConstraint;
    }

    public void setPrimaryKeyConstraint(PrimaryKeyConstraintSpec primaryKeyConstraint) {
        this.constraints.primaryKeyConstraint = primaryKeyConstraint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EntitySpec [");
        sb.append("\nClass name: ");
        sb.append(className);
        sb.append("\nTable name: ");
        sb.append(tableName);
        if (parentEntitySpec != null) {
            sb.append("\nParentSpec: ");
            sb.append(parentEntitySpec.getClassName());
        }
        if (!nodeSpecs.isEmpty()) {
            sb.append("\nNodes:\n");
            for (NodeSpec spec: nodeSpecs.values()) {
                sb.append(spec.toString());
                sb.append('\n');
            }
            sb.setLength(sb.length()-1);
        }
        sb.append("\nConstraints:\n");
        if (constraints.primaryKeyConstraint != null) {
            sb.append(constraints.primaryKeyConstraint.toString());
            sb.append('\n');
        }
        if (!constraints.foreignKeyConstraints.isEmpty()) {
            for (ForeignKeyConstraintSpec spec: constraints.foreignKeyConstraints) {
                sb.append(spec.toString());
                sb.append('\n');
            }
        }
        if (!constraints.uniqueConstraints.isEmpty()) {
            for (UniqueConstraintSpec spec: constraints.uniqueConstraints) {
                sb.append(spec.toString());
                sb.append('\n');
            }
            sb.setLength(sb.length()-1);
        }
        sb.append("\n]");
        return sb.toString();
    }

    public static class NodeSpecList {
        @XmlElement(name="NodeSpec")
        private final List<NodeSpec> data = new LinkedList<NodeSpec>();
    }

    public static class NodeSpecAdapter extends XmlAdapter<NodeSpecList, LinkedHashMap<String,NodeSpec>> {
        @Override
        public LinkedHashMap<String, NodeSpec> unmarshal(NodeSpecList nodeSpecs) throws Exception {
            LinkedHashMap<String, NodeSpec> map = new LinkedHashMap<String, NodeSpec>();
            for (NodeSpec spec: nodeSpecs.data) {
                map.put(spec.getName(), spec);
            }
            return map;
        }

        @Override
        public NodeSpecList marshal(LinkedHashMap<String, NodeSpec> nodeSpecs) throws Exception {
            NodeSpecList list = new NodeSpecList();
            list.data.addAll(nodeSpecs.values());
            return list;
        }
    }

}
