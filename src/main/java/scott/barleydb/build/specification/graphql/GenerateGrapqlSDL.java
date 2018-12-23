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
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.ConditionVisitor;
import scott.barleydb.api.query.QCondition;
import scott.barleydb.api.query.QExists;
import scott.barleydb.api.query.QLogicalOp;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
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
		return sb.append("schema {\n")
		.append("  query: Query \n")
		.append("  mutation: Mutation \n")
		.append("} \n")
		.append("\n")
		.append("type Query {\n")
		.append(printSchemQueryFields())
		.append("\n")
		.append(printCustomQueries())
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
	
	private Stream<SchemaCustomQueryField> streamSchemaCustomQueryFields() {
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
	
	private String getGraphQlTypeName(NodeSpec ns) {
		if (ns.getJavaType() != null) {
			return getGraphQlTypeName(ns, ns.getJavaType());
		}
		return getGraphQlTypeName(ns.getRelation().getEntitySpec());
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
		
		public List<Argument> getPrimaryKeyArguments() {
			NodeSpec nt = et.getPrimaryKeyNodes(true).iterator().next();
			return Collections.singletonList(new NodeArgument(nt));
		} 

		public List<Argument> getNonPrimaryKeyArguments() {
			return et.getNodeSpecs(true)
			.stream()
			.filter(ns -> !ns.isPrimaryKey())
			.filter(ns -> ns.getRelation() == null)
			.map(NodeArgument::new)
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
			Collection<QParameter<?>> params = new LinkedList<>();
			collect(query, params);
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
	
	public class NodeArgument implements Argument{
		private NodeSpec nodeSpec;
		public NodeArgument(NodeSpec nodeSpec) {
			this.nodeSpec = nodeSpec;
		}
		
		public String getType() {
			return getGraphQlTypeName(nodeSpec);
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
	
	
	
	private void addAllNodes(EntitySpec et, LinkedHashMap<String, FieldDefinition> fds) {
		if (et.getParentEntity() != null) {
			addAllNodes(et.getParentEntity(), fds);
		}
		fds.putAll(et.getNodeSpecs().stream()
			.map(FieldDefinition::new)
			.collect(Collectors.toMap(FieldDefinition::getName, fd -> fd)));		
	}

	public void collect(QueryObject<?> query, Collection<QParameter<?>> params) {
		ConditionVisitor visitor = new ConditionVisitor() {
			@Override
			public void visitPropertyCondition(QPropertyCondition qpc) throws IllegalQueryStateException {
				if (qpc.getValue() instanceof QParameter<?>) {
					params.add((QParameter<?>)qpc.getValue());
				}
			}
			
			@Override
			public void visitLogicalOp(QLogicalOp qlo) throws IllegalQueryStateException, ForUpdateNotSupportedException {
			}
			
			@Override
			public void visitExists(QExists exists) throws IllegalQueryStateException, ForUpdateNotSupportedException {
				exists.getSubQueryObject().getCondition().visit(this);
			}
		};
		QCondition cond = query.getCondition();
		try {
			cond.visit(visitor);
		} catch (IllegalQueryStateException e) {
			e.printStackTrace();
		} catch (ForUpdateNotSupportedException e) {
			e.printStackTrace();
		}
	}


	public class TypeDefinition {
		private final EntitySpec et;
		private final List<FieldDefinition> fields;
		public TypeDefinition(EntitySpec et) {
			this.et = et;
			LinkedHashMap<String, FieldDefinition> tmp = new LinkedHashMap<>();
			addAllNodes(et, tmp);
			this.fields = new LinkedList<>(tmp.values());
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
