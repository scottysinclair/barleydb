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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.core.types.Nullable;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.helper.CollectQParameters;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.EnumSpec;
import scott.barleydb.api.specification.EnumValueSpec;
import scott.barleydb.api.specification.NodeSpec;
import scott.barleydb.api.specification.SpecRegistry;

public class GenerateGrapqlSDL {
	private final SpecRegistry specRegistry;	
	private final CustomQueries customQueries;
	
	public GenerateGrapqlSDL(SpecRegistry specRegistry, CustomQueries customQueries) {
		this.specRegistry = specRegistry;
		this.customQueries = customQueries;
	}


	public String createSdl() {
		StringBuilder sb = new StringBuilder();
		sb.append("schema {\n");
		sb.append("  query: Query \n");
//		sb.append("  mutation: Mutation \n");
		sb.append("} \n");
		sb.append("\n");
		sb.append("type Query {\n");
		sb.append(printSchemQueryFields());
		sb.append("\n");
		String customQueries = printCustomQueries();
		if (!customQueries.isEmpty()) {
			sb.append(customQueries);
			sb.append("\n");
		}
		sb.append("}\n");
		sb.append("\n");
//		sb.append("type Mutation {\n");
//		sb.append(printSchemaMutationFields());
//		sb.append("}\n");
		sb.append("\n");
		sb.append(printQueryTypeDefinitions());
		sb.append("\n");
//		sb.append(printInputTypeDefinitions());
//		sb.append("\n");
		sb.append(printEnumDefinitions());
		return sb.toString();
	}

	private String printSchemQueryFields() {
		return streamSchemaQueryFields()
		.map(f -> new StringBuilder()
				.append("  ")
				.append(f.getName())
				.append("ById")
				.append(printArguments(f.getPrimaryKeyArguments()))
				.append(": ")
				.append(f.getType())
				.append("\n")
				.append("  ")
				.append(f.getName())
				.append("s")
				.append(printArguments(f.getNonPrimaryKeyArguments()))
				.append(": ")
				.append("[")
				.append(f.getType())
				.append("]"))
		.collect(Collectors.joining("\n"));
	}

	private String printSchemaMutationFields() {
        return streamSchemaMutationFields()
                .map(f -> new StringBuilder()
                        .append("  create")
                        .append(firstCharUpperCase(f.getName()))
                        .append(printMutationArguments(f.getArguments()))
                        .append(": ")
                        .append(f.getType())
                        .append("\n")
                        .append("  update")
                        .append(firstCharUpperCase(f.getName()))
                        .append(printMutationArguments(f.getArguments()))
                        .append(": ")
                        .append(f.getType())
                        .append("\n")
                        .append("  delete")
                        .append(f.getName())
                        .append(printArguments(f.getArguments()))
                        .append(": ")
                        .append(f.getType())
                        .append("\n"))
                .collect(Collectors.joining("\n"));
	}

	private String firstCharUpperCase(String name) {
	    if (name.length() == 0) {
	        return name;
        }
        else if (name.length() == 1) {
            return "" + Character.toUpperCase(name.charAt(0));
        }
	    return Character.toUpperCase(name.charAt(0)) + name.substring(1, name.length());
    }

	
	private String printCustomQueries() {
		return streamSchemaCustomQueryFields()
		.map(f -> new StringBuilder()
				.append("  ")
				.append(f.getName())
				.append(printArguments(f.getArguments()))
				.append(": ")
				.append("[")
				.append(f.getType())
				.append("]"))
		.collect(Collectors.joining("\n"));
	}

	
	private String printArguments(Collection<Argument> args) {
		if (args.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder("(");
		for (Argument a : args) {
			sb.append(a.getName())
			  .append(": ")
			  .append(a.getType());
	//		  .append(a.mandatory ? "!" : "");
			  sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append(")");
		return sb.toString();
	}

	private String printMutationArguments(Collection<Argument> args) {
		if (args.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder("(");
		for (Argument a : args) {
			sb.append(a.getName())
					.append(": ")
					.append(a.getType());
			//		  .append(a.mandatory ? "!" : "");
			sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append(")");
		return sb.toString();
	}


	private String printQueryTypeDefinitions() {
		return streamQueryTypeDefinitions()
			.map(td -> printQueryTypeDefinition(td))
			.collect(Collectors.joining("\n\n"));
	}

	private String printInputTypeDefinitions() {
		return streamInputTypeDefinitions()
				.map(td -> printInputTypeDefinition(td))
				.collect(Collectors.joining("\n\n"));
	}

	private String printQueryTypeDefinition(TypeDefinition type) {
		return new StringBuilder()
				.append("type ")
				.append(type.getName())
				.append(" {\n")
				.append(printFieldDefinitions(type))
				.append("\n}")
				.toString();
	}

    private String printInputTypeDefinition(TypeDefinition type) {
        return new StringBuilder()
                .append("input ")
                .append(type.getName() + "Input")
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


	private Stream<TypeDefinition> streamQueryTypeDefinitions() {
		return specRegistry.getDefinitions().stream()
				.map(DefinitionsSpec::getEntitySpecs)
				.flatMap(Collection::stream)
				.map(QueryTypeDefinition::new);
	}

	private Stream<TypeDefinition> streamInputTypeDefinitions() {
		return specRegistry.getDefinitions().stream()
				.map(DefinitionsSpec::getEntitySpecs)
				.flatMap(Collection::stream)
				.map(InputTypeDefinition::new);
	}

	private Stream<SchemaQueryField> streamSchemaQueryFields() {
		return specRegistry.getDefinitions().stream()
				.map(DefinitionsSpec::getEntitySpecs)
				.flatMap(Collection::stream)
				.map(SchemaQueryField::new);
	}

    private Stream<SchemaMutationField> streamSchemaMutationFields() {
        return specRegistry.getDefinitions().stream()
                .map(DefinitionsSpec::getEntitySpecs)
                .flatMap(Collection::stream)
                .map(SchemaMutationField::new);
    }

    private Stream<SchemaCustomQueryField> streamSchemaCustomQueryFields() {
		if (customQueries == null) {
			return Collections.<SchemaCustomQueryField>emptyList().stream();
		}
		return customQueries.queries().stream()
		.map(e -> new SchemaCustomQueryField(e.getKey(), e.getValue()));
	}

	private String getGraphQlTypeName(EntitySpec et) {
		return getSimpleName(et.getClassName());
	}

	private String getGraphQlTypeName(QueryObject<?> query) {
		return getSimpleName(query.getTypeName());
	}

	private String getSimpleName(String name) {
		int i = name.lastIndexOf('.');
		return i == -1 ? name : name.substring(i+1);
	}
	
	private String getGraphQlTypeName(NodeSpec ns, boolean inputType) {
		if (ns.getJavaType() != null) {
			return getGraphQlTypeName(ns, ns.getJavaType());
		}
		return getGraphQlTypeName(ns.getRelation().getEntitySpec()) + (inputType ? "Input" : "");
	}

	private String getGraphQlTypeName(JavaType javaType) {
		return getGraphQlTypeName(null, javaType);
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
		case UTIL_DATE: return "String";
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
		
		public List<Argument> getPrimaryKeyArguments() {
			NodeSpec nt = et.getPrimaryKeyNodes(true).iterator().next();
			return Collections.singletonList(new NodeQueryArgument(nt));
		} 

		public List<Argument> getNonPrimaryKeyArguments() {
			return et.getNodeSpecs(true)
			.stream()
			.filter(ns -> !ns.isPrimaryKey())
			.filter(ns -> ns.getRelationSpec() == null || ns.getRelationSpec().getBackReference() == null) //not a one to many reference
			.map(NodeQueryArgument::new)
			.collect(Collectors.toList());
		} 

		public String getType() {
			return getGraphQlTypeName(et);
		}
	}
	
	public class SchemaCustomQueryField {
		private final String name;
		private final QueryObject<?> query;
		public SchemaCustomQueryField(String name, QueryObject<?> query) {
			this.name = name;
			this.query = query;
		}
		
		public String getType() {
			return getGraphQlTypeName(query);
		}
		public Collection<Argument> getArguments() {
			Collection<QParameter<?>> params = collect(query);
			return params.stream()
					.map(QueryParameterArgument::new)
					.collect(Collectors.toList());
					
		}
		public String getName() {
			return name;
		}
	}

	public interface Argument {
		String getName();
		String getType();
	}
	
	public class NodeQueryArgument implements Argument{
		private NodeSpec nodeSpec;
		public NodeQueryArgument(NodeSpec nodeSpec) {
			this.nodeSpec = nodeSpec;
		}
		
		public String getType() {
			if (nodeSpec.getRelation() != null) {
				return getGraphQlTypeName(nodeSpec.getRelation().getEntitySpec().getPrimaryKeyNodes(true).iterator().next(), false);
			}
			else {
				return getGraphQlTypeName(nodeSpec, false);
			}
		}

		public String getName() {
			if (nodeSpec.getRelation() == null) {
				return nodeSpec.getName();
			}
			else {
				return nodeSpec.getName();
			}
		}
		
		private String getPrimaryKeyName(EntitySpec entitySpec) {
			return entitySpec.getPrimaryKeyNodes(true).iterator().next().getName();
		}
	}

	public class NodeMutationArgument implements Argument{
		private NodeSpec nodeSpec;
		public NodeMutationArgument(NodeSpec nodeSpec) {
			this.nodeSpec = nodeSpec;
		}

		public String getType() {
			return getGraphQlTypeName(nodeSpec, true);
		}

		public String getName() {
			if (nodeSpec.getRelation() == null) {
				return nodeSpec.getName();
			}
			else {
				return nodeSpec.getName() + getPrimaryKeyName(nodeSpec.getRelation().getEntitySpec());
			}
		}

		private String getPrimaryKeyName(EntitySpec entitySpec) {
			return entitySpec.getPrimaryKeyNodes(true).iterator().next().getName();
		}
	}

	public class QueryParameterArgument implements Argument {
		private QParameter<?> param;
		public QueryParameterArgument(QParameter<?> param) {
			this.param = param;
		}
		
		public String getType() {
			Objects.requireNonNull(param.getType(), "Query parameter must have JavaType specified");
			return getGraphQlTypeName(param.getType());
		}

		public String getName() {
			return param.getName();
		}
		
	}


    //how a query is defined as a field in the Query type
    public class SchemaMutationField {
        private final EntitySpec et;

        public SchemaMutationField(EntitySpec et) {
            this.et = et;
        }

        public String getName() {
            String s = getGraphQlTypeName(et);
            return Character.toLowerCase(s.charAt(0)) + s.substring(1, s.length());
        }

        public String getType() {
            return getGraphQlTypeName(et);
        }

        public Collection<Argument> getArguments() {
            return et.getNodeSpecs(true)
                    .stream()
                    .map(NodeMutationArgument::new)
                    .collect(Collectors.toList());
        }

    }


	
	
	
	private void addAllNodes(EntitySpec et, LinkedHashMap<String, FieldDefinition> fds, boolean inputFields) {
		if (et.getParentEntity() != null) {
			addAllNodes(et.getParentEntity(), fds, inputFields);
		}
		fds.putAll(et.getNodeSpecs().stream()
			.map( ns -> inputFields ? new InputFieldDefinition(ns) : new QueryFieldDefinition(ns))
			.collect(Collectors.toMap(FieldDefinition::getName, fd -> fd)));		
	}

	public Collection<QParameter<?>> collect(QueryObject<?> query) {
		return CollectQParameters.forQuery(query);
	}

	public interface TypeDefinition {
		String getName();
		List<FieldDefinition> getFields();
	}

	public class QueryTypeDefinition implements TypeDefinition {
		private final EntitySpec et;
		private final List<FieldDefinition> fields;
		public QueryTypeDefinition(EntitySpec et) {
			this.et = et;
			LinkedHashMap<String, FieldDefinition> tmp = new LinkedHashMap<>();
			addAllNodes(et, tmp, false);
			this.fields = new LinkedList<>(tmp.values());
		}

		public String getName() {
			return getGraphQlTypeName(et);
		}

		public List<FieldDefinition> getFields() {
			return fields;
		}
	}

	private interface FieldDefinition {
		boolean isArray();

		String getName();

		String getType();

		boolean isMandatory();

	}

	private class QueryFieldDefinition implements FieldDefinition{
		protected final NodeSpec nodeSpec;

		public QueryFieldDefinition(NodeSpec nodeSpec) {
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
			return getGraphQlTypeName(nodeSpec, false);
		}
		
		public boolean isMandatory() {
			return nodeSpec.getNullable() == Nullable.NOT_NULL;
		}
	}

	public class InputTypeDefinition implements TypeDefinition {
		private final EntitySpec et;
		private final List<FieldDefinition> fields;
		public InputTypeDefinition(EntitySpec et) {
			this.et = et;
			LinkedHashMap<String, FieldDefinition> tmp = new LinkedHashMap<>();
			addAllNodes(et, tmp, true);
			this.fields = new LinkedList<>(tmp.values());
		}

		public String getName() {
			return getGraphQlTypeName(et);
		}

		public List<FieldDefinition> getFields() {
			return fields;
		}
	}


	private class InputFieldDefinition implements FieldDefinition {
		protected final NodeSpec nodeSpec;
		public InputFieldDefinition(NodeSpec nodeSpec) {
			this.nodeSpec = nodeSpec;
		}


		public boolean isArray() {
			//is a 1:N relation
			return nodeSpec.getColumnName() == null;
		}

		public String getName() {
			return nodeSpec.getName() + "Input";
		}

		public String getType() {
			return getGraphQlTypeName(nodeSpec, true);
		}

		public boolean isMandatory() {
			return nodeSpec.getNullable() == Nullable.NOT_NULL;
		}

	}

}
