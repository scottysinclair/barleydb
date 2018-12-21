package scott.barleydb.api.graphql;

import static java.util.Objects.requireNonNull;
import static scott.barleydb.api.graphql.GraphQLTypeConversion.convertValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QMathOps;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;

public class QueryDataFetcher implements DataFetcher<Object> {
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryDataFetcher.class);
	
	private final Environment env;
	private final String namespace;
	
	public QueryDataFetcher(Environment env, String namespace) {
		this.env = env;
		this.namespace = namespace;
	}

	@Override
	public Object get(DataFetchingEnvironment graphEnv) throws Exception {
		EntityContext ctx = new EntityContext(env, namespace);
		EntityType entityType = getEntityTypeForQuery(graphEnv);
		QueryObject<Object> query = buildQuery(graphEnv, entityType);
		List<Object> result = ctx.performQuery(query).getList();
		if (result.size() == 1) {
			return result.get(0);
		}
		return result;
	}

	private QueryObject<Object> buildQuery(DataFetchingEnvironment graphEnv, EntityType entityType) {
		QueryObject<Object> query = new QueryObject<>(entityType.getInterfaceName());
		/*
		 * build the where clause
		 */
		for (Map.Entry<String, Object> argument: graphEnv.getArguments().entrySet()) {
			QPropertyCondition qcond = createCondition(query, entityType, argument.getKey(), QMathOps.EQ, argument.getValue());
			query.and(qcond);
			LOG.debug("Added query condition {}", qcond);
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
				LOG.debug("Added {} to select for {}", qprop.getName(), qprop.getQueryObject().getTypeName());
			}
		}
		forceSelectForeignKeysAndSortNodes(query);
		return query;
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

	private EntityType getEntityTypeForQuery(DataFetchingEnvironment graphEnv) {
		String entityName = graphEnv.getExecutionStepInfo().getType().getName();
		if (entityName == null) {
			entityName = graphEnv.getExecutionStepInfo().getType().getChildren().get(0).getName();
		}
		EntityType et = env.getDefinitionsSet().getFirstEntityTypeByInterfaceName(namespace + ".model." + entityName);
		requireNonNull(et, "EntityType must exist");
		return et;
	}
	
}
