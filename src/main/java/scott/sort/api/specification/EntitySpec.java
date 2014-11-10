package scott.sort.api.specification;

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

import scott.sort.api.specification.constraint.ForeignKeyConstraintSpec;
import scott.sort.api.specification.constraint.PrimaryKeyConstraintSpec;
import scott.sort.api.specification.constraint.UniqueConstraintSpec;

@XmlAccessorType(XmlAccessType.NONE)
public class EntitySpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@XmlID
	@XmlAttribute
	private String className;

	@XmlAttribute
	private String tableName;
	
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
		Set<String> columnNames = new HashSet<>();
		for (NodeSpec spec: nodeSpecs.values()) {
			if (!columnNames.add(spec.getColumnName())) {
				throw new IllegalStateException("2 or more properties have column name " + spec.getColumnName());
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
