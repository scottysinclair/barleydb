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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import graphql.execution.MergedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
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
import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QMathOps;
import scott.barleydb.api.query.QParameter;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.helper.CollectQParameters;

/**
 * Fetches data for a graphql query.
 * @author scott
 *
 */
public class EntityDataFetcher implements DataFetcher<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(EntityDataFetcher.class);

  private final Environment env;
  private final String namespace;

  public EntityDataFetcher(Environment env, String namespace) {
    this.env = env;
    this.namespace = namespace;
  }

	@Override
	public Object get(DataFetchingEnvironment graphEnv) throws Exception {
		Entity entity;
		if (graphEnv.getSource() instanceof ProxyController) {
			entity = ((ProxyController) graphEnv.getSource()).getEntity();
		} else if (graphEnv.getSource() instanceof Entity) {
			entity = (Entity) graphEnv.getSource();
		} else {
			throw new IllegalStateException("Unknown source " + graphEnv.getSource().getClass());
		}

		EntityContext ctx = entity.getEntityContext();
		MergedField fieldToFetch = graphEnv.getExecutionStepInfo().getField();
		Node node = entity.getChild(fieldToFetch.getName());
		if (node == null) {
			throw new IllegalStateException("Could not find node matching graphql field: " + fieldToFetch);
		}
		if (node instanceof ValueNode) {
			LOG.debug("Direct value access {} {}", entity, fieldToFetch.getName());
			return ((ValueNode) node).getValue();
		} else if (node instanceof RefNode) {
			RefNode refNode = (RefNode) node;
			Entity ref = refNode.getReference();
			if (ref == null) {
				LOG.debug("FK ref is null {} {}", entity, fieldToFetch.getName());
				return null;
			}
			if (!ref.isFetchRequired()) {
				LOG.debug("FK ref is already fetched {} {}", entity, fieldToFetch.getName());
				return ref;
			}
			LOG.debug("FK ref requires fetch {} {}", entity, fieldToFetch.getName());
			QueryObject<Object> fetchQuery = getJoinAt(graphEnv, entity, fieldToFetch.getName());
			Objects.requireNonNull(fetchQuery, () -> "fetch query must exist for entity " + entity + " and field " + fieldToFetch.getName());
			ctx.register(fetchQuery);
			try {
				refNode.getReference().fetchIfRequiredAndAllowed();
				return ref;
			} finally {
				// reset fetch query
				ctx.register(new QueryObject<>(refNode.getEntityType().getInterfaceName()));
			}
		} else if (node instanceof ToManyNode) {
			ToManyNode tmNode = (ToManyNode) node;
			if (tmNode.isFetched()) {
				LOG.debug("1:N ref is already fetched {} {}", entity, fieldToFetch.getName());
				return tmNode.getList();
			}
			LOG.debug("1:N ref requires fetch {} {}", entity, fieldToFetch.getName());
			QueryObject<Object> fetchQuery = getJoinAt(graphEnv, entity, fieldToFetch.getName());
			Objects.requireNonNull(fetchQuery, () -> "fetch query must exist for entity " + entity + " and field " + fieldToFetch.getName());
			ctx.register(fetchQuery);
			try {
				fetchIfNeeded(tmNode);
				return tmNode.getList();
			} finally {
				// reset fetch query
				ctx.register(new QueryObject<>(tmNode.getEntityType().getInterfaceName()));
			}
		} else {
			throw new IllegalStateException("Unknown node type " + node.getClass());
		}
	}
  
	private QueryObject<Object> getJoinAt(DataFetchingEnvironment graphEnv, Entity entity, String property) {
		GraphQLContext gctx = graphEnv.getContext();
		QJoin join = gctx.getJoinBreakFor(entity, property);
	    return (QueryObject<Object> )(join != null ? join.getTo() : null);
	}

	/**
	 * 
	 * @param graphEnv
	 * @param entityPath can be null if root entity
	 * @param property
	 * @return
  private QueryObject<Object> getQueryFor(DataFetchingEnvironment graphEnv, EntityPath entityPath, String property) {
	GraphQLContext gctx = graphEnv.getContext();
    QJoin join = gctx.getJoinBreakFor(entityPath, property);
    return (QueryObject<Object> )(join != null ? join.getTo() : null);
  }
	 */

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

  private void setProjection(DataFetchingEnvironment graphEnv, EntityType entityType, QueryObject<Object> query) {
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
        LOG.debug("Added {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
      }
    }
    forceSelectForeignKeysAndSortNodes(query);
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
        LOG.debug("Added FK {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
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
      LOG.debug("Added FK {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
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
      LOG.debug("Created join from {} to {} for property {}", query.getTypeName(), result.getTypeName(), propertyName);
    }
    else {
      LOG.debug("Found join query from {} to {} for property {}", query.getTypeName(), result.getTypeName(), propertyName);
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

}
