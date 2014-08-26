package com.smartstream.morf.server.jdbc.queryexecution;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.config.Definitions;
import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.EntityContext;
import com.smartstream.morf.api.core.entity.EntityState;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ToManyNode;
import com.smartstream.morf.api.query.QJoin;
import com.smartstream.morf.api.query.QueryObject;
import com.smartstream.morf.server.jdbc.helper.PreparedStatementHelper;
import com.smartstream.morf.server.jdbc.queryexecution.QueryGenerator.Param;

/**
 * Builds the SQL for a query
 * Processes the resultset creating the loaded entities in the entity context
 * @author scott
 *
 * @param <T>
 */
public class QueryExecution<T> {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExecution.class);

    private final EntityContext entityContext;
    private final QueryObject<T> query;
    private final Definitions definitions;
    private final Projection projection;
    private QueryResult<T> queryResult;
    private EntityLoaders entityLoaders;
    private QueryGenerator qGen;

	public QueryExecution(EntityContext entityContext, QueryObject<T> query,
			Definitions definitions) {
		this.entityContext = entityContext;
		this.query = query;
		this.definitions = definitions;
		this.projection = new Projection(definitions);
		projection.build(query);
		queryResult = new QueryResult<>(entityContext, query.getTypeClass());
	}

    public String getSql() {
        return getSql(new LinkedList<Param>());
    }
    public String getSql(List<Param> existingParams) {
        qGen = new QueryGenerator(query, definitions, existingParams);
        return qGen.generateSQL(projection);
    }

    public boolean hasParameters() {
    	return !qGen.getParameters().isEmpty();
    }

    public void setParameters(PreparedStatement stmt) throws SQLException {
       int i = 1;
       PreparedStatementHelper helper = new PreparedStatementHelper(definitions);
       for (QueryGenerator.Param param: qGen.getParameters())  {
          helper.setParameter(stmt, i++, param.getNodeDefinition(), param.getValue());
       }
    }

    public String getPrimaryTableName() {
    	EntityType entityType = definitions.getEntityTypeMatchingInterface( query.getTypeClass().getName(), true );
    	return entityType.getTableName();
    }

    void processResultSet(ResultSet resultSet) throws SQLException {
    	int row = 1;
    	while(resultSet.next()) {
    		LOG.debug("======== row " + row + " =======");
    		processRow(resultSet);
    		row++;
    	}
    }

    /**
     *  called after the resultset data
     *  has been loaded to correctly
     *  set the final state
     */
    void finish() {
    	if (entityLoaders != null) {
	    	LOG.debug("Finishing query execution....");
	    	processToManyRelations(query);
	    	setEntityStateToLoadedAndRefresh();
	    	verifyAllFetchedDataIsLinked();
	    	prepareQueryResult();
	    	LOG.debug("Finished query execution.");
    	}
    }

    /**
     * Sets all ToMany relations which had query joins to 'fetched'
     * @param queryObject
     */
    private void processToManyRelations(QueryObject<?> queryObject) {
    	//get the entities which this query object loaded
    	List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject( queryObject );
    	if (loadedEntities.isEmpty()) {
    		return;
    	}
    	//for each tomany ref set fetched to true iff the queryobject had a join for that property
    	for (Entity loadedEntity: loadedEntities) {
	    	for (ToManyNode toManyNode: loadedEntity.getChildren(ToManyNode.class)) {
	    		if (findJoin(toManyNode.getName(), queryObject)) {
	    			toManyNode.setFetched(true);
	    		}
	    	}
    	}
    	//repeat the process for tomany relations in the joined queryobjects
    	for (QJoin join: queryObject.getJoins()) {
    		processToManyRelations(join.getTo());
    	}
    }

    private void verifyAllFetchedDataIsLinked() {
    	verifyRefs(query);


    	  /*
         * todo: add back in some kind of ToMany validation

        for (LoadedToMany loadedToMany: loadedToManys) {
            loadedToMany.validate();
        }
        */
    }

    private void prepareQueryResult() {
    	//get the entities from the top level query object
    	List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject( query );
    	//add them to the result
    	queryResult.addEntities( loadedEntities );
    }

    /**
     * Verifies that all joined refs with a FK value were loaded
     * @param queryObject
     */
    private void verifyRefs(QueryObject<?> queryObject) {
    	List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject( queryObject );
    	if (loadedEntities.isEmpty()) {
    		return;
    	}
    	List<RefNode> refNodes = loadedEntities.get(0).getChildren(RefNode.class);
    	for (RefNode refNode: refNodes) {
    		if (refNode.getEntityKey() != null && findJoin(refNode.getName(), queryObject)) {
    			if (refNode.getReference() == null) {
    				throw new IllegalStateException("Joined FK key was not loaded: " + refNode);
    			}
    		}
    	}

    }

    /**
     * Sets the entitystate to loaded and refreshes each entity.
     */
    private void setEntityStateToLoadedAndRefresh() {
    	for (EntityLoader entityLoader: entityLoaders) {
	   		 for (Entity entity: entityLoader.getLoadedEntities()) {
	   			 entity.setEntityState( EntityState.LOADED );
	   			 entity.refresh();
	   		 }
    	}
    }

    /**
     * looks for a join on the given property for the queryobject
     * @param propertyName
     * @param queryObject
     * @return true iff the join was found
     */
    private boolean findJoin(String propertyName, QueryObject<?> queryObject) {
    	for (QJoin join: queryObject.getJoins()) {
    		if (join.getFkeyProperty().equals(propertyName)) {
    			return true;
    		}
    	}
    	return false;
    }

    public QueryResult<T> getResult() {
    	return queryResult;
    }

    /**
     * Processes a row of the resultset
     * @param resultSet
     * @throws SQLException
     */
    private void processRow(ResultSet resultSet) throws SQLException {
    	if (entityLoaders == null) {
    		entityLoaders = new EntityLoaders(projection, resultSet, entityContext);
    	}
    	else {
    		entityLoaders.clearRowCache();
    	}
    	for (EntityLoader entityLoader: entityLoaders) {
    		 if (entityLoader.isEntityThere()) {
    		     //todo: handle entity nodes which are lazy
    		     //current logic sees that the entity exists & is loaded and does nothing
    			 if (entityLoader.isNotYetLoaded()) {
    				 entityLoader.load();
    			 }
    			 else {
    				 /*
    				  * The entity is already loaded in our context
    				  * so don't update it, and associate the existing entity
    				  * with our loader so that it can be part of the QueryResult
    				  */
    				 entityLoader.associateExistingEntity();
    			 }
    		 }
    	}
    	//todo process join table columns
    }
}

