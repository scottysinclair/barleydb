package scott.sort.api.specification.constraint;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import scott.sort.api.specification.NodeSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class PrimaryKeyConstraintSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@XmlAttribute
	private String name;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> nodes;

	public PrimaryKeyConstraintSpec() {
		this.nodes = new LinkedList<NodeSpec>();
	}

	public PrimaryKeyConstraintSpec(String name, Collection<NodeSpec> nodes) {
		this.name = name;
		this.nodes = nodes;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<NodeSpec> getNodes() {
		return Collections.unmodifiableCollection( nodes );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Primary Key Constraint '");
		sb.append(name);
		sb.append("' [ ");
		for (NodeSpec spec: nodes) {
			sb.append(spec.getColumnName());
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		sb.append(" ]");
		return sb.toString();
	}
	
	
}
