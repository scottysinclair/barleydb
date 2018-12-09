package scott.barleydb.build.specification.graphql;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.EnumValueSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.SpecRegistry;

public class GenerateGrapqlSDL {
	private final SpecRegistry specRegistry;	
	
	
	public GenerateGrapqlSDL(SpecRegistry specRegistry) {
		this.specRegistry = specRegistry;
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
		.append("\n")
		.append(printEnumDefinitions())
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
	
	private String printEnumDefinitions() {
		return streamEnumDefinitions()
				.map(this::printEnumDefinition)
				.collect(Collectors.joining("\n\n"));
	}
	
	private Stream<EnumSpec> streamEnumDefinitions() {
		return specRegistry.getDefinitions().stream()
				.map(DefinitionsSpec::getEnumSpecs)
				.flatMap(Collection::stream);
	}


	private String printEnumDefinition(EnumSpec enumSpec) {
		return new StringBuilder()
				.append("enum ")
				.append(getSimpleName(enumSpec.getClassName()))
				.append(" {\n")
				.append(printEnumValues(enumSpec.getEnumValues()))
				.append("\n}")
				.toString();
	}
	
	private String printEnumValues(List<EnumValueSpec> enumValues) {
		return enumValues.stream()
		.map(ev -> "  " + ev.getName())
		.collect(Collectors.joining("\n"));
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
		return specRegistry.getDefinitions().stream()
				.map(DefinitionsSpec::getEntitySpecs)
				.flatMap(Collection::stream)
				.map(TypeDefinition::new);
	}
	
	private Stream<SchemaQueryField> streamSchemaQueryFields() {
		return specRegistry.getDefinitions().stream()
				.map(DefinitionsSpec::getEntitySpecs)
				.flatMap(Collection::stream)
				.map(SchemaQueryField::new);
	}

	private String getGraphQlTypeName(EntitySpec et) {
		return getSimpleName(et.getClassName());
	}
	
	private String getSimpleName(String name) {
		int i = name.lastIndexOf('.');
		return i == -1 ? name : name.substring(i+1);
	}
	
	private String getGraphQlTypeName(NodeSpec ns) {
		if (ns.getJavaType() != null) {
			return getGraphQlTypeName(ns, ns.getJavaType());
		}
		return getGraphQlTypeName(ns.getRelation().getEntitySpec());
	}

	private String getGraphQlTypeName(NodeSpec nodeSpec, JavaType javaType) {
		switch(javaType) {
		case INTEGER: return "Int";
		case BIGDECIMAL: return "Float";
		case STRING: return "String";
		case LONG: return "Int";
		case BOOLEAN: return "Boolean";
		case BYTE_ARRAY: return "String"; 
		case ENUM: return getSimpleName(nodeSpec.getEnumSpec().getClassName());
		case SHORT:return "Int";
		case SQL_DATE: throw new UnsupportedOperationException();
		case UTIL_DATE: throw new UnsupportedOperationException();
		case UUID: return "String";
		default: throw new UnsupportedOperationException(javaType.toString());
		}
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
