package scott.sort.api.specification.constraint;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class ForeignKeyConstraintSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@XmlAttribute
	private String name;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> fromKey;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> toKey;
	
	public ForeignKeyConstraintSpec() {
		this.fromKey = new LinkedList<NodeSpec>();
		this.toKey = new LinkedList<NodeSpec>();
	}

	public ForeignKeyConstraintSpec(String name, Collection<NodeSpec> fromKey, EntitySpec toEntity, Collection<NodeSpec> toKey) {
		this.name = name;
		this.fromKey = fromKey;
		this.toKey = toKey;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<NodeSpec> getFromKey() {
		return fromKey;
	}

	public Collection<NodeSpec> getToKey() {
		return toKey;
	}
	
	private static String printNodes(Collection<NodeSpec> nodeSpecs) {
		StringBuilder sb = new StringBuilder();
		for (NodeSpec spec: nodeSpecs) {
			sb.append(spec.getColumnName());
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Foreign Key Constraint '");
		sb.append(name);
		sb.append("' (");
		sb.append(printNodes(fromKey));
		sb.append(") REFERENCES ");
		sb.append(toKey.iterator().next().getEntity().getTableName());
		sb.append("(");
		sb.append(printNodes(toKey));
		sb.append(")");
		return sb.toString();
	}
}
