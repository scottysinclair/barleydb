package scott.barleydb.build.specification.graphql;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.specification.DefinitionsSpec;

public class CreateGrapqlSdl {
	private final Definitions definition;	
	
	
	public CreateGrapqlSdl(Definitions definition) {
		this.definition = definition;
	}


	public String createSdl(DefinitionsSpec definitionsSpec) {
		StringBuilder sb = new StringBuilder();
		return sb.append("schema {\n")
		.append("query: Query \n")
		.append("mutation: Mutation \n")
		.append("} \n")
		.append("\n")
		.append("type Query {\n")
		.append(printSchemQueryFields())
		.append("}\n")
		.append("\n")
		.append("type Mutation {\n")
		.append("}\n")
		.append(printTypeDefinitions())
		.toString();
	}
	
	private String printSchemQueryFields() {
		return streamSchemaQueryFields()
		.map(f -> new StringBuilder()
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
			.collect(Collectors.joining("\n"));
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
		return definition.getEntityTypes().stream()
		.map(TypeDefinition::new);
	}
	
	private Stream<SchemaQueryField> streamSchemaQueryFields() {
		return definition.getEntityTypes().stream()
				.map(SchemaQueryField::new);
	}

	private String getGraphQlTypeName(NodeType nt) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String getGraphQlTypeName(EntityType et2) {
		// TODO Auto-generated method stub
		return null;
	}


	//how a query is defined as a field in the Query type
	public class SchemaQueryField {
		private final EntityType et;
		
		public SchemaQueryField(EntityType et) {
			this.et = et;
		}

		public String getName() {
			return et.getInterfaceShortName();
		}
		
		public List<Argument> getArguments() {
			NodeType nt = et.getNodeType(et.getKeyNodeName(), true);
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
		private final EntityType et;
		private final List<FieldDefinition> fields;
		public TypeDefinition(EntityType et) {
			this.et = et;
			this.fields = et.getNodeTypes().stream()
					.map(FieldDefinition::new)
					.collect(Collectors.toList());
			
		}
		
		public String getName() {
			return et.getInterfaceShortName();
		}

		public List<FieldDefinition> getFields() {
			return fields;
		}
	}
	
	private class FieldDefinition {
		private final NodeType nodeType;

		public FieldDefinition(NodeType nodeType) {
			this.nodeType = nodeType;
		}
		
		public boolean isArray() {
			//is a 1:N relation
			return nodeType.getColumnName() == null;
		}

		public String getName() {
			return nodeType.getName();
		}
		
		public String getType() {
			return getGraphQlTypeName(nodeType);
		}
		
		public boolean isMandatory() {
			return nodeType.isMandatory();
		}
	}

}
