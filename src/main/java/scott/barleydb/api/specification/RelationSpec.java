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
import javax.xml.bind.annotation.XmlIDREF;

import scott.barleydb.api.config.RelationType;


/**
 * @author scott
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class RelationSpec implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private RelationType type;

    /**
     * An initial EntitySpec identifier allowing the real EntitySpec
     * to be resolved at a later time.
     */
    private transient Object entitySpecIdentifier;

    /**
     * entitySpec is usually set in a second pass when loading / defining specs
     * ie
     * 1st pass: setup all entity specs and their node specs
     * 2nd pass: resolve relations
     */
    @XmlIDREF
    @XmlAttribute
    private EntitySpec entitySpec;

    /**
     * For a many relationship.
     * the reference back to us
     */
    @XmlIDREF
    @XmlAttribute
    private NodeSpec backReference;

    /**
     * For a many relationship.
     * the node to sort on
     */
    @XmlIDREF
    @XmlAttribute
    private NodeSpec sortNode;


    /**
     * For reference to a join table
     * we can specify the onwards join out past the join table
     * ie onwards to datatype from template_datatype join table.
     */
    @XmlIDREF
    @XmlAttribute
    private NodeSpec ownwardJoin;

    @XmlAttribute
    private JoinTypeSpec joinType;

    public RelationSpec() {
    }

    public RelationSpec(RelationType type, Object entitySpecIdentifier, NodeSpec backReference, NodeSpec onwardJoin) {
        this.type = type;
        this.entitySpecIdentifier = entitySpecIdentifier;
        this.backReference = backReference;
        this.ownwardJoin = onwardJoin;
    }

    public boolean isForeignKeyRelation() {
        return backReference == null;
    }

    public JoinTypeSpec getJoinType() {
        return joinType;
    }

    public void setJoinType(JoinTypeSpec joinType) {
        this.joinType = joinType;
    }

    public NodeSpec getSortNode() {
        return sortNode;
    }

    public void setSortNode(NodeSpec sortNode) {
        this.sortNode = sortNode;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
    }

    public EntitySpec getEntitySpec() {
        return entitySpec;
    }

    public void setEntitySpec(EntitySpec entitySpec) {
        this.entitySpec = entitySpec;
    }

    public Object getEntitySpecIdentifier() {
        return entitySpecIdentifier;
    }

    public void setEntitySpecIdentifier(Object entitySpecIdentifier) {
        this.entitySpecIdentifier = entitySpecIdentifier;
    }

    public NodeSpec getBackReference() {
        return backReference;
    }

    public void setBackReference(NodeSpec backReference) {
        this.backReference = backReference;
    }

    public NodeSpec getOwnwardJoin() {
        return ownwardJoin;
    }

    public void setOwnwardJoin(NodeSpec ownwardJoin) {
        this.ownwardJoin = ownwardJoin;
    }

    @Override
    protected RelationSpec clone()  {
        try {
            /*
             * The entitySpec and backReference should not be cloned
             */
            return (RelationSpec)super.clone();
        }
        catch (CloneNotSupportedException x) {
          throw new IllegalStateException("Could not clone relation spec", x);
        }
    }

    @Override
    public String toString() {
        if (entitySpec != null) {
            return "RelationSpec [type=" + type + ", entitySpec=" + entitySpec.getClassName() + "]";
        }
        return "RelationSpec [type=" + type + ", id=" + entitySpecIdentifier + "]";
    }

}
