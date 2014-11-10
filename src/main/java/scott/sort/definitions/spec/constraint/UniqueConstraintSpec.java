package scott.sort.definitions.spec.constraint;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import scott.sort.definitions.spec.EntitySpec;
import scott.sort.definitions.spec.NodeSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class UniqueConstraintSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@XmlAttribute
	private String name;
	
	@XmlIDREF
	@XmlAttribute
	private final Collection<NodeSpec> nodes;
	
	public UniqueConstraintSpec() {
		this.nodes = new LinkedList<NodeSpec>();
	}

	public UniqueConstraintSpec(NodeSpec ...nodes) {
		this.nodes = new LinkedList<NodeSpec>( Arrays.asList(nodes) );
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
	
	public UniqueConstraintSpec newCopyFor(EntitySpec entitySpec) {
		UniqueConstraintSpec copy = new UniqueConstraintSpec();
		for (NodeSpec nodeSpec: nodes) {
			NodeSpec nodeSpecForCopy = entitySpec.getNodeSpec(nodeSpec.getName());
			if (nodeSpecForCopy == null) {
				throw new IllegalStateException("Could not find node spec in entity when creating a copy of a unique constraint");
			}
			copy.nodes.add( nodeSpecForCopy );
		}
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Unique Constraint '" + name + "' ");
		sb.append("[ ");
		for (NodeSpec spec: nodes) {
			sb.append(spec.getColumnName());
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		sb.append(" ]");
		return sb.toString();
	}
	
	
}
