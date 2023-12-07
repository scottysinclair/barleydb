package scott.barleydb.api.graphql;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

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
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.graphql.CustomQueries;
import scott.barleydb.build.specification.graphql.GenerateGrapqlSDL;

public class BarleyGraphQLSchema {
	
	private static final Logger LOG = LoggerFactory.getLogger(BarleyGraphQLSchema.class);
	
	private final SpecRegistry specRegistry;
	private final Environment env;
	private final String namespace;
	private final GraphQLSchema graphQLSchema;
	private final String sdlString;
	private final GraphQLQueryCustomizations queryCustomizations;
	
	public BarleyGraphQLSchema(SpecRegistry specRegistry, Environment env, String namespace, CustomQueries customQueries) {
		this.specRegistry = specRegistry;
		this.env = env;
		this.namespace = namespace;
		this.queryCustomizations = new GraphQLQueryCustomizations();
		this.queryCustomizations.setShouldBreakPredicate(new DefaultQueryBreaker(env, namespace, 3, 4));

        GenerateGrapqlSDL graphSdl = new GenerateGrapqlSDL(specRegistry, customQueries);
        
        SchemaParser schemaParser = new SchemaParser();
        sdlString = graphSdl.createSdl();
        LOG.info(sdlString);
        
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(sdlString);
        RuntimeWiring.Builder wiringBuilder = newRuntimeWiring()
                .type("Query", builder -> builder.defaultDataFetcher(new QueryDataFetcher(env, namespace, customQueries)));
        
        specRegistry.getDefinitions().stream()
        .map(DefinitionsSpec::getEntitySpecs)
        .flatMap(Collection::stream)
        .forEach(eSpec -> wiringBuilder.type(getSimpleName(eSpec.getClassName()), builder ->  builder.defaultDataFetcher(new EntityDataFetcher(env, namespace))));
        
         RuntimeWiring runtimeWiring = wiringBuilder.build();        
        
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        this.graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
	}
	
	private String getSimpleName(String className) {
		int i = className.lastIndexOf('.');
		return i == -1 ? className : className.substring(i+1, className.length());
	}

	public GraphQLQueryCustomizations getQueryCustomizations() {
		return queryCustomizations;
	}

	public String getSdlString() {
		return sdlString;
	}

	public GraphQLContext newContext() {
		return new BarleyGraphQLContext();
	}
	
	public class BarleyGraphQLContext implements GraphQLContext {

		private final GraphQL graphql;
		private final GraphQLQueryCustomizations queryCustomizations;
		private final Set<QJoin> joinBreaks = new HashSet<>();
		private final Map<String, Object> attributes = new HashMap<>();
		private boolean batchFetchEnabled = true;

		private List<Entity> rootEntities = new LinkedList<>();

		private EntityContext entityContext;

		public BarleyGraphQLContext() {
			this.graphql = GraphQL.newGraphQL(graphQLSchema).build();
			this.queryCustomizations = BarleyGraphQLSchema.this.queryCustomizations.copy();
		}

		public List<Entity> getRootEntities() {
			return rootEntities;
		}

		public void addRootEntity(final Entity rootEntity) {
			this.rootEntities.add(rootEntity);
		}

		@Override
		public <T> T execute(String body) {
			ExecutionResult result = graphql.execute(ExecutionInput.newExecutionInput()
	                .query(body)
	                .context(this)
	                .build());
			
			if (!result.getErrors().isEmpty()) {
				throw new GraphQLExecutionException(result.getErrors());
			}
			return result.getData();
		}

		public List<Entity> executeAndGetEntities(String body) {
			ExecutionResult result = graphql.execute(ExecutionInput.newExecutionInput()
																					 .query(body)
																					 .context(this)
																					 .build());

			if (!result.getErrors().isEmpty()) {
				throw new GraphQLExecutionException(result.getErrors());
			}
			return rootEntities;
		}

		public <T> List<T> executeAndGetProxies(String body) {
			ExecutionResult result = graphql.execute(ExecutionInput.newExecutionInput()
																					 .query(body)
																					 .context(this)
																					 .build());

			if (!result.getErrors().isEmpty()) {
				throw new GraphQLExecutionException(result.getErrors());
			}
			List<T> proxies = new LinkedList<>();
			for (Entity e: rootEntities) {
				proxies.add(e.getEntityContext().getProxy(e));
			}
			return proxies;
		}

		@Override
		public GraphQLQueryCustomizations getQueryCustomizations() {
			return queryCustomizations;
		}
		
		@Override
		public void registerJoinBreak(QJoin join) {
			this.joinBreaks.add(join);
		}

		@Override
		public QJoin getJoinBreakFor(Entity entity, String property) {
			QueryObject<?> query = entity.getEntityContext().getAssociatedQuery(entity);
			Objects.requireNonNull(query, "query must exist for loaded entity " + entity);
			return getJoinBreakFor(query, property);
		}
		
		private QJoin getJoinBreakFor(QueryObject<?> query, String property) {
			for (QJoin joinBreak: joinBreaks) {
				/*
				 * compare UUIDs because registered queries are cloned and then executed
				 * UUIDs are kept across cloning.
				 */
				if (joinBreak.getFrom().getUuid().equals(query.getUuid()) && joinBreak.getFkeyProperty().equals(property)) {
					return joinBreak;
				}
			}
			return null;
		}

		@Override
		public <T> T get(String key) {
			return (T)attributes.get(key);
		}

		@Override
		public Object put(String key, Object value) {
			return attributes.put(key, value);
		}

		@Override
		public void setBatchFetchEnabled(boolean batchFetchEnabled) {
			this.batchFetchEnabled = batchFetchEnabled;
		}

		@Override
		public boolean isBatchFetchEnabled() {
			return batchFetchEnabled;
		}

		public void setEntityContext(EntityContext ctx) {
			this.entityContext = ctx;
		}

		public EntityContext getEntityContext() {
			return entityContext;
		}
	}
	
}
