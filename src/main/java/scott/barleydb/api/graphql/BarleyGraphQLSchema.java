package scott.barleydb.api.graphql;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.SelectedField;

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
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.types.JavaType;
import scott.barleydb.api.query.JoinType;
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
	
	private void setProjection(QueryObject<Object> qo, EntityType entityType, DataFetchingEnvironment graphEnv) {
		setProjection(qo, entityType, graphEnv, null);
	}
	
	private Set<String> forceAddForeignKey(QueryObject<Object> qo, EntityType entityType) {
		Set<String> addedToProj = new HashSet<>();
		for (NodeType nt: entityType.getNodeTypes()) {
			if (nt.isForeignKey() || nt.getSortNode() != null) {
				QProperty<Object> prop = new QProperty<>(qo, nt.getName());
				qo.andSelect(prop);
				addedToProj.add(nt.getName());
			}
		}
		return addedToProj;
	}

	private void setProjection(QueryObject<Object> qo, EntityType entityType, DataFetchingEnvironment graphEnv, String forceAddNode) {
		//force include 1:1 relations and and nodes required for sorting
		//                    String sortNodeName = getNodeType().getSortNode();
		
		Set<String> addedToProj = forceAddForeignKey(qo, entityType);
		if (addedToProj.add(forceAddNode)) {
			QProperty<Object> prop = new QProperty<>(qo, forceAddNode);
			qo.andSelect(prop);
		}
		
		int countToManyJoins = 0;
		Map<String, QueryObject<Object>> joins = new LinkedHashMap<>();

		List<SelectedField> selectedFields = graphEnv.getSelectionSet().getFields();
		if (selectedFields != null) {
			for (SelectedField sf: selectedFields) {
				countToManyJoins += processSelectedField(qo, entityType, sf, addedToProj, joins, countToManyJoins);
			}
		}
	}
	
	
	private int processSelectedField(QueryObject<Object> qo, EntityType entityType, SelectedField sf, Set<String> addedToProj, Map<String, QueryObject<Object>> joins, int countToManyJoins ) {
		String parts[] = sf.getQualifiedName().split("/");
		NodeType nodeType = entityType.getNodeType(parts[0], false);
		
		if (nodeType != null && nodeType.getColumnName() != null) {
			if (nodeType.getRelationInterfaceName() == null) {
				if (addedToProj.add(parts[0])) {
					QProperty<Object> prop = new QProperty<>(qo, parts[0]);
					qo.andSelect(prop);
					LOG.debug("Added selected field {} to query projection", parts[0]);
				}
			}
			else {
				//1:1 FK join
				QueryObject<Object> joinQo = joins.get(parts[0]);
				if (joinQo == null) {
					joinQo = leftOuterJoinTo(parts[0], qo, entityType);
					joins.put(parts[0], joinQo);
				}
				if (joinQo != null && parts.length > 1) {
					QProperty<Object> prop = new QProperty<>(joinQo, parts[1]);
					joinQo.andSelect(prop);
				}
				else if (joinQo != null) {
					LOG.info("Created left outer join for {}", sf.getName());
				}
			}
		}
		else  {
			//node has no column name - must be 1:N relation
			//selected field qualified name has /
			QueryObject<Object> joinQo = joins.get(parts[0]);
			if (joinQo == null && countToManyJoins < 3) {
				joinQo = leftOuterJoinTo(parts[0], qo, entityType);
				if (nodeType.getSortNode() != null) {
					QProperty<Object> prop = new QProperty<>(joinQo, nodeType.getSortNode());
					joinQo.andSelect(prop);
				}
				joins.put(parts[0], joinQo);
				countToManyJoins++;
			}
			if (joinQo != null && parts.length > 1) {
				QProperty<Object> prop = new QProperty<>(joinQo, parts[1]);
				joinQo.andSelect(prop);
			}
			else if (joinQo != null) {
				LOG.info("Created left outer join for {}", sf.getName());
			}
			else {
				LOG.warn("Added selected field {} is not a database column of {}", sf.getName(), entityType.getInterfaceShortName());
			}
		}
		return countToManyJoins;	
		}

    private QueryObject<Object> leftOuterJoinTo(String fieldName, QueryObject<Object> qo, EntityType entityType) {
    	NodeType nodeType = entityType.getNodeType(fieldName, true);
    	QueryObject<Object> toQo = new QueryObject<>(nodeType.getRelationInterfaceName());
    	qo.addJoin(toQo, fieldName, JoinType.LEFT_OUTER);
    	EntityType joinEntityType = getEntityTypeByIterfaceName(nodeType.getRelationInterfaceName());
		forceAddForeignKey(toQo, joinEntityType);
    	return toQo;
	}
    
    private EntityType getEntityTypeByIterfaceName(String interfaceName) {
    	return env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(interfaceName);
    }

	private class BarleyDbDataFetcher2 implements DataFetcher<Object> {
    	private EntitySpec entitySpec;

		public BarleyDbDataFetcher2(EntitySpec entitySpec) {
			this.entitySpec = entitySpec;
		}

		@Override
		public Object get(DataFetchingEnvironment graphEnv) throws Exception {
			Entity entity;
			if (graphEnv.getSource() instanceof ProxyController) {
				entity = ((ProxyController)graphEnv.getSource()).getEntity();
			}
			else if (graphEnv.getSource() instanceof Entity) {
				entity = (Entity)graphEnv.getSource();
			}
			else {
				throw new IllegalStateException("Unknown source " + graphEnv.getSource().getClass());
			}
			EntityContext ctx = entity.getEntityContext();
			Field fieldToFetch = graphEnv.getExecutionStepInfo().getField();
			Node node = entity.getChild(fieldToFetch.getName());
			if (node == null) {
				throw new IllegalStateException("Could not find node matching graphql field: " + fieldToFetch);
			}
			if (node instanceof ValueNode) {
				return ((ValueNode)node).getValue();
			}
			else if (node instanceof RefNode) {
				RefNode refNode = (RefNode)node;
				Entity ref = refNode.getReference();
				if (ref != null) {
					if (ref.isFetchRequired()) {
						/*
						 * the entity it not yet fetched, so customize the fetch query so that only the required fields will be
						 * in the projection
						 */
						List<SelectedField> selectedFields = graphEnv.getSelectionSet().getFields();
						if (!selectedFields.isEmpty()) {
							QueryObject<Object> fetchQuery = new QueryObject<>(refNode.getEntityType().getInterfaceName());
							setProjection(fetchQuery, refNode.getEntityType(), graphEnv);
							ctx.register(fetchQuery);
						}
						try {
							refNode.getReference().fetchIfRequiredAndAllowed();
						}
						finally {
							//reset fetch query
							ctx.register(new QueryObject<>(refNode.getEntityType().getInterfaceName()));
						}
					}
				}
				return ref;
			}
			else if (node instanceof ToManyNode) {
				ToManyNode tmNode = (ToManyNode)node;
				if (!tmNode.isFetched()) {
					/*
					 * the list is not fetched, so customize the fetch query so that only the required fields will be in the projection 
					 */
					List<SelectedField> selectedFields = graphEnv.getSelectionSet().getFields();
					if (!selectedFields.isEmpty()) {
						QueryObject<Object> fetchQuery = new QueryObject<>(tmNode.getEntityType().getInterfaceName());
						setProjection(fetchQuery, tmNode.getEntityType(), graphEnv, tmNode.getNodeType().getSortNode());
						ctx.register(fetchQuery);
					}
					try {
						fetchIfNeeded(tmNode);
						return tmNode.getList();
					}
					finally {
						//reset fetch query
						ctx.register(new QueryObject<>(tmNode.getEntityType().getInterfaceName()));
					}
				}
				else {
					return tmNode.getList();
				}
			}
			else {
				throw new IllegalStateException("Unknown node type " + node.getClass());
			}
		}
    	
    }
    
    private void fetchIfNeeded(ToManyNode toManyNode) {
        if (toManyNode.getParent().isClearlyNotInDatabase()) {
            return;
        }
        if (toManyNode.getParent().getEntityState() == EntityState.LOADING) {
            return;
        }
        if (!toManyNode.isFetched()) {
            toManyNode.getEntityContext().fetch(toManyNode);
        }
    }    

	
    private class BarleyDbDataFetcher implements DataFetcher<Object> {
    
		@Override
		public Object get(DataFetchingEnvironment graphEnv) throws Exception {
			EntityContext ctx = new EntityContext(env, namespace);
			String entityName = graphEnv.getExecutionStepInfo().getType().getName();
			EntityType et = env.getDefinitions(namespace).getEntityTypeMatchingInterface(namespace + ".model." + entityName, true);
			QueryObject<Object> qo = new QueryObject<>(et.getInterfaceName());
			setProjection(qo, et, graphEnv);
			addConditions(et, qo, graphEnv.getArguments());
			return ctx.performQuery(qo).getSingleResult();
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
