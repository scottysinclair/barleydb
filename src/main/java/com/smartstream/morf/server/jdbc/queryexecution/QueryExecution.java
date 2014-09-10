package com.smartstream.morf.server.jdbc.queryexecution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.config.Definitions;
import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.config.NodeDefinition;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.EntityContext;
import com.smartstream.morf.api.core.entity.EntityState;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ToManyNode;
import com.smartstream.morf.api.core.entity.ValueNode;
import com.smartstream.morf.api.query.QJoin;
import com.smartstream.morf.api.query.QueryObject;
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

    public String getSql(List<Param> queryParameters) {
        qGen = new QueryGenerator(query, definitions);
        return qGen.generateSQL(projection, queryParameters);
    }

    public String getPrimaryTableName() {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(query.getTypeClass().getName(), true);
        return entityType.getTableName();
    }

    void processResultSet(ResultSet resultSet) throws SQLException {
        int row = 1;
        while (resultSet.next()) {
            LOG.debug("======== row " + row + " =======");
            processRow(resultSet);
            row++;
        }
    }

    /**
     * called after the resultset data
     * has been loaded to correctly
     * set the final state
     */
    void finish() {
        if (entityLoaders != null) {
            LOG.debug("Finishing query execution....");
            downcastAbstractEntities(query);
            processToManyRelations(query);
            setEntityStateToLoadedAndRefresh();
            verifyAllFetchedDataIsLinked();
            prepareQueryResult();
            LOG.debug("Finished query execution.");
        }
    }

    /**
     * Finds the concrete entity type for any abstract entities
     */
    private void downcastAbstractEntities(QueryObject<?> queryObject) {
        List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject(queryObject);
        for (Entity e : loadedEntities) {
            if (e.getEntityType().isAbstract()) {
                LOG.debug("Attempting to downcast abstract entity {}", e);
                downcastEntity(e);
            }
        }

        for (QJoin join : queryObject.getJoins()) {
            downcastAbstractEntities(join.getTo());
        }
    }

    private void downcastEntity(Entity entity) {
        List<EntityType> candidateChildTypes = definitions.getEntityTypesExtending(entity.getEntityType());
        EntityType entityType = null;
        for (EntityType et : candidateChildTypes) {
            if (canDowncastEntity(entity, et)) {
                if (entityType != null) {
                    throw new IllegalStateException("Multiple downcast paths for entity '" + entity + "'");
                }
                entityType = et;
            }
        }
        if (entityType == null) {
            throw new IllegalStateException("No suitable type for downcast for entity '" + entity + "'");
        }
        entity.downcast(entityType);
    }

    /**
     * This is based on checking for any fixed value nodes in
     * the child type which "lock" the entity for us
     * @param entity
     * @return
     */
    private boolean canDowncastEntity(Entity entity, EntityType entityType) {
        int fvFound = 0;
        int fvMatch = 0;
        for (NodeDefinition nd : entityType.getNodeDefinitions()) {
            final Object fv = nd.getFixedValue();
            if (fv != null) {
                fvFound++;
                final ValueNode node = entity.getChild(nd.getName(), ValueNode.class, false);
                if (node != null && fv.equals(node.getValue())) {
                    fvMatch++;
                }
            }
        }
        if (fvFound == 0) {
            throw new IllegalStateException("Invalid downcast candidate '" + entityType + "', no fixed values defined");
        }
        return fvFound == fvMatch;
    }

    /**
     * Sets all ToMany relations which had query joins to 'fetched'
     * @param queryObject
     */
    private void processToManyRelations(QueryObject<?> queryObject) {
        //get the entities which this query object loaded
        List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject(queryObject);
        if (loadedEntities.isEmpty()) {
            return;
        }
        //for each tomany ref set fetched to true iff the queryobject had a join for that property
        for (Entity loadedEntity : loadedEntities) {
            for (ToManyNode toManyNode : loadedEntity.getChildren(ToManyNode.class)) {
                if (findJoin(toManyNode.getName(), queryObject) != null) {
                    toManyNode.setFetched(true);
                }
            }
        }
        //repeat the process for tomany relations in the joined queryobjects
        for (QJoin join : queryObject.getJoins()) {
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
        List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject(query);
        //add them to the result
        queryResult.addEntities(loadedEntities);
    }

    /**
     * Verifies that all joined refs with a FK value were loaded
     * @param queryObject
     */
    private void verifyRefs(QueryObject<?> queryObject) {
        List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject(queryObject);
        if (loadedEntities.isEmpty()) {
            return;
        }
        for (Entity e : loadedEntities) {
            List<RefNode> refNodes = e.getChildren(RefNode.class);
            for (RefNode refNode : refNodes) {
                QJoin join = findJoin(refNode.getName(), queryObject);
                if (refNode.getEntityKey() != null && join != null) {
                    if (refNode.getReference() == null) {
                        throw new IllegalStateException("Joined FK key was not loaded: " + refNode);
                    }
                }
            }
        }
        //repeat the process for refs relations in the joined queryobjects
        for (QJoin join : queryObject.getJoins()) {
            verifyRefs(join.getTo());
        }
    }

    /**
     * Sets the entitystate to loaded and refreshes each entity.
     */
    private void setEntityStateToLoadedAndRefresh() {
        for (EntityLoader entityLoader : entityLoaders) {
            for (Entity entity : entityLoader.getLoadedEntities()) {
                entity.setEntityState(EntityState.LOADED);
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
    private QJoin findJoin(String propertyName, QueryObject<?> queryObject) {
        for (QJoin join : queryObject.getJoins()) {
            if (join.getFkeyProperty().equals(propertyName)) {
                return join;
            }
        }
        return null;
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
        for (EntityLoader entityLoader : entityLoaders) {
            if (entityLoader.isEntityThere()) {
                //todo: handle entity nodes which are lazy
                //current logic sees that the entity exists & is loaded and does nothing
                //this isNotYetLoaded only makes sense if we are loading into a fresh entity context.
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
