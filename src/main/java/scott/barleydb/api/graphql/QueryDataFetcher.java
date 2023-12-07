package scott.barleydb.api.graphql;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2019 Scott Sinclair
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

import static java.util.Objects.requireNonNull;
import static scott.barleydb.api.graphql.GraphQLTypeConversion.convertValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.SelectedField;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.graphql.BarleyGraphQLSchema.BarleyGraphQLContext;
import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QMathOps;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.helper.CollectQParameters;
import scott.barleydb.build.specification.graphql.CustomQueries;
import scott.barleydb.server.jdbc.query.QueryResult;

/**
 * Fetches data for a graphql query.
 * @author scott
 *
 */
public class QueryDataFetcher implements DataFetcher<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(QueryDataFetcher.class);

  private final Environment env;
  private final String namespace;
  private final CustomQueries customQueries;

  public QueryDataFetcher(Environment env, String namespace, CustomQueries customQueries) {
    this.env = env;
    this.namespace = namespace;
    this.customQueries = customQueries;
  }

  @Override
  public Object get(DataFetchingEnvironment graphEnv) throws Exception {
    EntityContext ctx = new EntityContext(env, namespace);
    ((BarleyGraphQLContext)graphEnv.getContext()).setEntityContext(ctx);


    QueryObject<Object> query = null;
    if (customQueries != null) {
      query = (QueryObject<Object>)customQueries.getQuery(graphEnv.getField().getName());
    }
    if (query == null) {
      EntityType entityType = getEntityTypeForQuery(graphEnv);
      query = buildQuery(graphEnv, entityType);
    }
    else {
      buildQuery(graphEnv, query);
    }
    LOG.debug("Built full query which will be broken into chunks, query => {}", ctx.debugQueryString(query));
    
    breakQuery(graphEnv, query);

    BarleyGraphQLContext gctx = graphEnv.getContext();
    QueryResult<Object> queryResult = ctx.performQuery(query);
    
    List<Entity> result = queryResult.getEntityList();
    LOG.debug("Processed {} rows", ctx.getStatistics().getNumberOfRowsRead());
    if (graphEnv.getExecutionStepInfo().getType() instanceof GraphQLList) {
      if (gctx.isBatchFetchEnabled()) {
    	  ctx.batchFetchDescendants(result);
      }
      for (Entity e: result) {
        gctx.addRootEntity(e);
      }
      return result; //Entity2Map.toListOfMaps(result);
    }
    else if (result.size() == 1) {
      Entity e = result.get(0);//Entity2Map.toMap(result.get(0));
      if (gctx.isBatchFetchEnabled()) {
        ctx.batchFetchDescendants(e);
      }
      gctx.addRootEntity(e);
      return e;
    }
    else if (result.size() == 0) {
    	return null;
    }
    throw new IllegalStateException("too many results");
  }

  private void breakQuery(DataFetchingEnvironment graphEnv, QueryObject<Object> query) {
	  GraphQLContext graphCtx = graphEnv.getContext();	  
	  for (QJoin join: new ArrayList<>(query.getJoins())) {
		  if (graphCtx.getQueryCustomizations().shouldBreakJoin(join, graphCtx)) {
			  query.removeJoin(join);
			  GraphQLContext gctx = graphEnv.getContext();
			  gctx.registerJoinBreak(join);
		  }
		breakQuery(graphEnv, (QueryObject<Object>)join.getTo());
	  }
  }

  private QueryObject<Object> buildQuery(DataFetchingEnvironment graphEnv, EntityType entityType) {
    QueryObject<Object> query = new QueryObject<>(entityType.getInterfaceName());
    return buildQuery(graphEnv, query, entityType);
  }

  private QueryObject<Object> buildQuery(DataFetchingEnvironment graphEnv, QueryObject<Object> query) {
    EntityType entityType = env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(query.getTypeName());
    return buildQuery(graphEnv, query, entityType);
  }

  private QueryObject<Object> buildQuery(DataFetchingEnvironment graphEnv, QueryObject<Object> query, EntityType entityType) {
    /*
     * build the where clause
     */
    for (Map.Entry<String, Object> argument: graphEnv.getArguments().entrySet()) {
       /*
        *  the condition if from a custom query can have any name and must match a query parameter	
        */
      QParameter<Object> param = findQueryParameter(query, argument.getKey());
      Object value = argument.getValue();
      if (param != null) {
    	  if (param.getType() != null) {
    		  /*
    		   * if the QParameter has a type then try type conversion (graphql layer type conversion)
    		   */
    		  value = convertValue(value, param.getType());
    	  }
        param.setValue(value);
        LOG.trace("Set query parameter {}", param.getName());
      }
      else {
    	  /*
    	   * otherwise the parameter must match exactly a node.
    	   */
        QPropertyCondition qcond = createCondition(query, entityType, argument.getKey(), QMathOps.EQ, value);
        query.and(qcond);
        LOG.trace("Added query condition {}", qcond);
      }
    }

    /*
     * set the projection
     */
    List<SelectedField> selectedFields = graphEnv.getSelectionSet().getFields();
    if (selectedFields != null) {
      for (SelectedField sf: selectedFields) {
        String parts[] = sf.getQualifiedName().split("/");
        QueryObject<Object> q = getQueryForPath(query, entityType, Arrays.asList(parts).subList(0, parts.length - 1));
        QProperty<Object> qprop = createProperty(q, parts[ parts.length - 1]);
        q.andSelect(qprop);
        LOG.trace("Added {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
      }
    }
    forceSelectForeignKeysAndSortNodes(query);
    return query;
  }

  private Object typeConvertValue(EntityType entityType, String property, Object value) {
	 NodeType nodeType = entityType.getNodeType(property, false);
	 if (nodeType == null) {
		 return value;
	 }
	 if (nodeType.getJavaType() != null) {
		 switch(nodeType.getJavaType()) {
		 case BIGDECIMAL: if (value instanceof Double) {
			 return new BigDecimal((Double)value);
		 }
		 }
	 }
	return value;
}

private QParameter<Object> findQueryParameter(QueryObject<?> query, String parameterName) {
    return CollectQParameters.forQuery(query, parameterName);
  }

  private void forceSelectForeignKeysAndSortNodes(QueryObject<Object> query) {
    EntityType entityType = env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(query.getTypeName());
    for (NodeType nodeType: entityType.getNodeTypes()) {
      if (nodeType.isForeignKeyColumn() && !query.isProjected(nodeType.getName())) {
        QProperty<Object> qprop = createProperty(query, nodeType.getName());
        query.andSelect(qprop);
        LOG.trace("Added FK {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
      }
    }

    //if this query is joined to, then check the property of the joining entity to
    //see if it has a sorting requirement.
    addSortNodeIfRequiredByJoin(query);

    for (QJoin join: query.getJoins()) {
      forceSelectForeignKeysAndSortNodes((QueryObject<Object>)join.getTo());
    }
  }

  private void addSortNodeIfRequiredByJoin(QueryObject<Object> query) {
    QJoin joined = query.getJoined();
    if (joined == null) {
      return;
    }
    NodeType nodeType = getNodeType(joined.getFrom(), joined.getFkeyProperty());
    if (nodeType.getSortNode() != null && !query.isProjected(nodeType.getSortNode())) {
      QProperty<Object> qprop = createProperty(query, nodeType.getSortNode());
      query.andSelect(qprop);
      LOG.trace("Added FK {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
    }
  }

  private NodeType getNodeType(QueryObject<?> queryObject, String propertyName) {
    EntityType entityType = env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(queryObject.getTypeName());
    return entityType.getNodeType(propertyName, true);
  }

  private QueryObject<Object> getQueryForPath(QueryObject<Object> query, EntityType entityType, List<String> path) {
    for (String propName: path) {
      query = getOrCreateJoinQuery(query, entityType, propName);
      entityType = getEntityTypeForProperty(entityType, propName);
    }
    return query;
  }

  private EntityType getEntityTypeForProperty(EntityType entityType, String propName) {
    NodeType nt = entityType.getNodeType(propName, true);
    String intefaceName = nt.getRelationInterfaceName();
    requireNonNull(intefaceName, propName + " of " + entityType.getInterfaceShortName() + " must be a relation");
    return env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(intefaceName);
  }

  private QueryObject<Object> getOrCreateJoinQuery(QueryObject<Object> query, EntityType entityType, String propertyName) {
    QueryObject<Object> result = getJoinQuery(query, propertyName);
    if (result == null) {
      result = createJoinQuery(query, entityType, propertyName);
      LOG.trace("Created join from {} to {} for property {}", query.getTypeName(), result.getTypeName(), propertyName);
    }
    else {
      LOG.trace("Found join query from {} to {} for property {}", query.getTypeName(), result.getTypeName(), propertyName);
    }
    return result;
  }

  private QueryObject<Object> getJoinQuery(QueryObject<Object> query, String propertyName) {
    for (QJoin join: query.getJoins()) {
      if (join.getFkeyProperty().equals(propertyName)) {
        return (QueryObject<Object>)join.getTo();
      }
    }
    return null;
  }

  private QueryObject<Object> createJoinQuery(QueryObject<Object> query, EntityType entityType, String propertyName) {
    NodeType nodeType = entityType.getNodeType(propertyName, true);
    QueryObject<Object> joinQuery = new QueryObject<>(nodeType.getRelationInterfaceName());
    query.addJoin(joinQuery, propertyName, JoinType.LEFT_OUTER);
    return joinQuery;
  }

  private QProperty<Object> createProperty(QueryObject<Object> query, String propertyName) {
    return new QProperty<>(query, propertyName);
  }

  private QPropertyCondition createCondition(QueryObject<Object> query, EntityType entityType, String propertyName, QMathOps op, Object value) {
    QProperty<Object> prop = new QProperty<>(query, propertyName);
    value = convertValue(entityType.getNodeType(propertyName, true), value);
    return new QPropertyCondition(prop, QMathOps.EQ, value);
  }

  private EntityType getEntityTypeForQuery(DataFetchingEnvironment graphEnv) {
    String entityName = getEntityName(graphEnv.getExecutionStepInfo().getType());
    EntityType et = env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(namespace + ".model." + entityName);
    requireNonNull(et, "EntityType '" + entityName + "' must exist");
    return et;
  }

  private String getEntityName(GraphQLType type) {
    if (type instanceof GraphQLList) {
      return getEntityName(((GraphQLList) type).getWrappedType());
    }
    else if (type instanceof GraphQLObjectType) {
      return ((GraphQLObjectType)type).getName();
    }
    throw new IllegalStateException("Cannot find entity type from GraphQL type " + type);
  }

}
