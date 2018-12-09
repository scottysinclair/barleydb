package scott.barleydb.api.graphql;

import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.specification.DefinitionsSpec;
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
        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", builder -> builder.defaultDataFetcher(new BarleyDbDataFetcher()))
                .build();        
        
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        this.graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
	}
	
    private class BarleyDbDataFetcher implements DataFetcher<Object> {
    
		@Override
		public Object get(DataFetchingEnvironment graphEnv) throws Exception {
			EntityContext ctx = new EntityContext(env, namespace);
			return "Hello";
		}
    	
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
