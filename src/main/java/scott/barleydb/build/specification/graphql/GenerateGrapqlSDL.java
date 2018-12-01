package scott.barleydb.build.specification.graphql;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;

public class GenerateGrapqlSDL {
	private final DefinitionsSpec definition;	
	
	
	public GenerateGrapqlSDL(DefinitionsSpec definition) {
		this.definition = definition;
	}


	public String createSdl() {
		StringBuilder sb = new StringBuilder();
		return sb.append("schema {\n")
		.append("  query: Query \n")
		.append("  mutation: Mutation \n")
		.append("} \n")
		.append("\n")
		.append("type Query {\n")
		.append(printSchemQueryFields())
		.append("\n}\n")
		.append("\n")
		.append("type Mutation {\n")
		.append("}\n")
		.append("\n")
		.append(printTypeDefinitions())
		.toString();
	}
	
	private String printSchemQueryFields() {
		return streamSchemaQueryFields()
		.map(f -> new StringBuilder()
				.append("  ")
				.append(f.getName())
				.append(printArguments(f.getArguments()))
				.append(": ")
				.append(f.getType()))
		.collect(Collectors.joining("\n"));
	}
	
	private String printArguments(Collection<Argument> args) {
		StringBuilder sb = new StringBuilder("(");
		for (Argument a : args) {
			sb.append(a.name)
			  .append(": ")
			  .append(a.type)
			  .append(a.mandatory ? "!" : "");
		}
		sb.append(")");
		return sb.toString();
	}
	
	private String printTypeDefinitions() {
		return streamTypeDefinitions()
			.map(this::printTypeDefinition)
			.collect(Collectors.joining("\n\n"));
	}
	
	private String printTypeDefinition(TypeDefinition type) {
		return new StringBuilder()
				.append("type ")
				.append(type.getName())
				.append(" {\n")
				.append(printFieldDefinitions(type))
				.append("\n}")
				.toString();
	}
	
	private String printFieldDefinitions(TypeDefinition type) {
		return type.getFields().stream()
		.map(this::printFieldDefinition)
		.collect(Collectors.joining("\n"));
	}

	private String printFieldDefinition(FieldDefinition field) {
		return new StringBuilder()
				.append("  ")
				.append(field.getName())
				.append(": ")
				.append(toFieldTypeDeclr(field))
				.toString();
	}

	private String toFieldTypeDeclr(FieldDefinition field) {
		if (field.isArray()) {
			return "[" + field.getType() + "]" + (field.isMandatory() ? "!" : "");
		}
		return field.getType() + (field.isMandatory() ? "!" : "");
	}


	private Stream<TypeDefinition> streamTypeDefinitions() {
		return definition.getEntitySpecs().stream()
		.map(TypeDefinition::new);
	}
	
	private Stream<SchemaQueryField> streamSchemaQueryFields() {
		return definition.getEntitySpecs().stream()
				.map(SchemaQueryField::new);
	}

	private String getGraphQlTypeName(EntitySpec et) {
		String name = et.getClassName();
		int i = name.lastIndexOf('.');
		return i == -1 ? name : name.substring(i+1);
	}
	
	private String getGraphQlTypeName(NodeSpec ns) {
		if (ns.getJavaType() != null) {
			return getGraphQlTypeName(ns.getJavaType());
		}
		return getGraphQlTypeName(ns.getRelation().getEntitySpec());
	}

	private String getGraphQlTypeName(JavaType javaType) {
		return javaType.name().toLowerCase();
	}


	//how a query is defined as a field in the Query type
	public class SchemaQueryField {
		private final EntitySpec et;
		
		public SchemaQueryField(EntitySpec et) {
			this.et = et;
		}

		public String getName() {
			String s = getGraphQlTypeName(et);
			return Character.toLowerCase(s.charAt(0)) + s.substring(1, s.length());
		}
		
		public List<Argument> getArguments() {
			NodeSpec nt = et.getPrimaryKeyNodes(true).iterator().next();
			return Collections.singletonList(new Argument(nt.getName(), getGraphQlTypeName(nt), true));
		} 
		
		public String getType() {
			return getGraphQlTypeName(et);
		}
	}
	
	public static class Argument {
		private String name;
		private String type;
		private boolean mandatory;
		public Argument(String name, String type, boolean mandatory) {
			this.name = name;
			this.type = type;
			this.mandatory = mandatory;
		}
		
	}
	
	public class TypeDefinition {
		private final EntitySpec et;
		private final List<FieldDefinition> fields;
		public TypeDefinition(EntitySpec et) {
			this.et = et;
			this.fields = et.getNodeSpecs().stream()
					.map(FieldDefinition::new)
					.collect(Collectors.toList());
			
		}
		
		public String getName() {
			return getGraphQlTypeName(et);
		}

		public List<FieldDefinition> getFields() {
			return fields;
		}
	}
	
	private class FieldDefinition {
		private final NodeSpec nodeSpec;

		public FieldDefinition(NodeSpec nodeSpec) {
			this.nodeSpec = nodeSpec;
		}
		
		public boolean isArray() {
			//is a 1:N relation
			return nodeSpec.getColumnName() == null;
		}

		public String getName() {
			return nodeSpec.getName();
		}
		
		public String getType() {
			return getGraphQlTypeName(nodeSpec);
		}
		
		public boolean isMandatory() {
			return nodeSpec.getNullable() == Nullable.NOT_NULL;
		}
	}

}
