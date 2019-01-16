package scott.barleydb.api.graphql;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import scott.barleydb.api.core.entity.Statistics;
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
	
	public BarleyGraphQLSchema(SpecRegistry specRegistry, Environment env, String namespace, CustomQueries customQueries) {
		this.specRegistry = specRegistry;
		this.env = env;
		this.namespace = namespace;
		
        GenerateGrapqlSDL graphSdl = new GenerateGrapqlSDL(specRegistry, customQueries);
        
        SchemaParser schemaParser = new SchemaParser();
        sdlString = graphSdl.createSdl();
        LOG.info(sdlString);
        System.out.println(sdlString);
        
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(sdlString);
        RuntimeWiring.Builder wiringBuilder = newRuntimeWiring()
                .type("Query", builder -> builder.defaultDataFetcher(new QueryDataFetcher(env, namespace, customQueries)));
        
//        specRegistry.getDefinitions().stream()
//        .map(DefinitionsSpec::getEntitySpecs)
//        .flatMap(Collection::stream)
//        .forEach(eSpec -> wiringBuilder.type(getSimpleName(eSpec.getClassName()), builder ->  builder.defaultDataFetcher(new BarleyDbDataFetcher2(eSpec))));
        
         RuntimeWiring runtimeWiring = wiringBuilder.build();        
        
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        this.graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
	}

	public String getSdlString() {
		return sdlString;
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
				throw new GraphQLExecutionException(result.getErrors());
			}
			return result.getData();
		}
	}
	
}
