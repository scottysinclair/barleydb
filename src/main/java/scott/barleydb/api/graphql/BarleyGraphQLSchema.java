package scott.barleydb.api.graphql;

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

import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.SelectedField;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.query.QCondition;
import scott.barleydb.api.query.QMathOps;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.graphql.GenerateGrapqlSDL;

public class BarleyGraphQLSchema {
	
	private static final Logger LOG = LoggerFactory.getLogger(BarleyGraphQLSchema.class);
	
	private SpecRegistry specRegistry;
	private Environment env;
	private String namespace;
	private GraphQLSchema graphQLSchema;
	
	public BarleyGraphQLSchema(SpecRegistry specRegistry, Environment env, String namespace) {
		this.specRegistry = specRegistry;
		this.env = env;
		this.namespace = namespace;
		
        GenerateGrapqlSDL graphSdl = new GenerateGrapqlSDL(specRegistry);
        
        SchemaParser schemaParser = new SchemaParser();
        String sdlString = graphSdl.createSdl();
        LOG.info(sdlString);
        
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(sdlString);
        RuntimeWiring.Builder wiringBuilder = newRuntimeWiring()
                .type("Query", builder -> builder.defaultDataFetcher(new BarleyDbDataFetcher()));
        
        specRegistry.getDefinitions().stream()
        .map(DefinitionsSpec::getEntitySpecs)
        .flatMap(Collection::stream)
        .forEach(eSpec -> wiringBuilder.type(getSimpleName(eSpec.getClassName()), builder ->  builder.defaultDataFetcher(new BarleyDbDataFetcher2(eSpec))));
        
         RuntimeWiring runtimeWiring = wiringBuilder.build();        
        
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        this.graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
	}
	
	private String getSimpleName(String name) {
		int i = name.lastIndexOf('.');
		return i == -1 ? name : name.substring(i+1);
	}

    private class BarleyDbDataFetcher2 implements DataFetcher<Object> {
    	private EntitySpec entitySpec;

		public BarleyDbDataFetcher2(EntitySpec entitySpec) {
			this.entitySpec = entitySpec;
		}

		@Override
		public Object get(DataFetchingEnvironment graphEnv) throws Exception {
			return null;
		}
    	
    }

	
    private class BarleyDbDataFetcher implements DataFetcher<Object> {
    
		@Override
		public Object get(DataFetchingEnvironment graphEnv) throws Exception {
			EntityContext ctx = new EntityContext(env, namespace);
			String entityName = graphEnv.getExecutionStepInfo().getType().getName();
			EntityType et = env.getDefinitions(namespace).getEntityTypeMatchingInterface(namespace + ".model." + entityName, true);
			QueryObject<Object> qo = new QueryObject<>(et.getInterfaceName());
			setProjection(qo, graphEnv);
			addConditions(et, qo, graphEnv.getArguments());
			return ctx.performQuery(qo).getSingleResult();
		}

		private void setProjection(QueryObject<Object> qo,DataFetchingEnvironment graphEnv) {
			List<SelectedField> selectedFields = graphEnv.getSelectionSet().getFields();
			if (selectedFields != null) {
				for (SelectedField sf: selectedFields) {
					QProperty<Object> prop = new QProperty<>(qo, sf.getName());
					qo.andSelect(prop);
				}
			}
		}

		private void addConditions(EntityType et, QueryObject<Object> qo, Map<String, Object> arguments) {
			for (Map.Entry<String, Object> entry: arguments.entrySet()) {
				QProperty<Object> prop = new QProperty<>(qo, entry.getKey());
				Object value = convertIfRequired(et, entry.getKey(), entry.getValue());
				QPropertyCondition qcond = new QPropertyCondition(prop, QMathOps.EQ, value);
				qo.and(qcond);
			}
		}

		private Object convertIfRequired(EntityType et, String nodeName, Object value) {
			NodeType nt = et.getNodeType(nodeName, true);
			return convertIfRequired(value, nt.getJavaType());
		}

		private Object convertIfRequired(Object value, JavaType javaType) {
			switch (javaType) {
				case LONG:
					return convertToLong(value);
				default: return value;
			}
		}
    	
    }

	private Long convertToLong(Object value) {
		if (value instanceof Integer) {
			return ((Integer)value).longValue();
		}
		throw new IllegalStateException(String.format("Cannot convert %s to Long", value));
	}

	public GraphQLContext newContext() {
		return new MyGraphQLContext();	
	}
	
	class MyGraphQLContext implements GraphQLContext {

		private GraphQL graphql;
		
		public MyGraphQLContext() {
			this.graphql = GraphQL.newGraphQL(graphQLSchema).build();
		}
		
		@Override
		public <T> T execute(String body) {
			ExecutionResult result = graphql.execute(body);
			if (!result.getErrors().isEmpty()) {
				result.getErrors().forEach(System.out::println);
			}
			return result.getData();
		}
		
	}
	
}
