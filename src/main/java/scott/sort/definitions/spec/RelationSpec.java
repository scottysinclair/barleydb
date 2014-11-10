package scott.sort.definitions.spec;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import scott.sort.definitions.type.RelationType;

@XmlAccessorType(XmlAccessType.NONE)
public class RelationSpec implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	@XmlAttribute
	private RelationType type;
	
	/**
	 * An initial EntitySpec identifier allowing the real EntitySpec
	 * to be resolved at a later time. 
	 */
	private Object entitySpecIdentifier;
	
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
	
	public RelationSpec() {
	}
	
	public RelationSpec(RelationType type, Object entitySpecIdentifier, NodeSpec backReference) {
		this.type = type;
		this.entitySpecIdentifier = entitySpecIdentifier;
		this.backReference = backReference;
	}
	
	public boolean isForeignKeyRelation() {
		return backReference == null;
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
 