package scott.sort.server.jdbc.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeType;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.EntityState;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.exception.execution.jdbc.SortJdbcException;
import scott.sort.api.exception.execution.query.DowncastEntityException;
import scott.sort.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.sort.api.exception.execution.query.IllegalQueryStateException;
import scott.sort.api.exception.execution.query.QueryConnectionRequiredException;
import scott.sort.api.exception.execution.query.SortQueryException;
import scott.sort.api.query.QJoin;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.query.QueryGenerator.Param;
import scott.sort.server.jdbc.resources.ConnectionResources;
import scott.sort.server.jdbc.vendor.Database;

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
    private final Database database;
    private QueryResult<T> queryResult;
    private EntityLoaders entityLoaders;
    private QueryGenerator qGen;

    public QueryExecution(EntityContext entityContext, QueryObject<T> query, Definitions definitions) throws QueryConnectionRequiredException {
        this.entityContext = entityContext;
        this.query = query;
        this.definitions = definitions;
        this.projection = new Projection(definitions);
        this.database = ConnectionResources.getMandatoryForQuery(entityContext).getDatabase();
        projection.build(query);
        queryResult = new QueryResult<>(entityContext, query.getTypeClass());
    }

    public String getSql(List<Param> queryParameters) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        qGen = new QueryGenerator(database, query, definitions);
        return qGen.generateSQL(projection, queryParameters);
    }

    public String getPrimaryTableName() {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(query.getTypeClass().getName(), true);
        return entityType.getTableName();
    }

    void processResultSet(ResultSet resultSet) throws SortJdbcException, SortQueryException {
        int row = 1;
        try {
            while (resultSet.next()) {
                LOG.debug("======== row " + row + " =======");
                processRow(resultSet);
                row++;
            }
        }
        catch (SQLException x) {
            throw new SortJdbcException("SQLException getting next result set", x);
        }
    }

    /**
     * called after the resultset data
     * has been loaded to correctly
     * set the final state
     * @throws SortQueryException
     */
    void finish() throws SortQueryException {
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
     * @throws IllegalQueryStateException
     */
    private void downcastAbstractEntities(QueryObject<?> queryObject) throws IllegalQueryStateException {
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

    private void downcastEntity(Entity entity) throws IllegalQueryStateException  {
        LOG.debug("Downcasting entity {}", entity);
        List<EntityType> candidateChildTypes = definitions.getEntityTypesExtending(entity.getEntityType());
        LOG.debug("Found candidate types for downcast {}", candidateChildTypes);
        EntityType entityType = null;
        for (EntityType et : candidateChildTypes) {
            if (canDowncastEntity(entity, et)) {
                if (entityType != null) {
                    throw new DowncastEntityException("Multiple downcast paths for entity '" + entity + "'");
                }
                entityType = et;
            }
        }
        if (entityType == null) {
            throw new DowncastEntityException("No suitable type for downcast for entity '" + entity + "'");
        }
        entity.downcast(entityType);
    }

    /**
     * This is based on checking for any fixed value nodes in
     * the child type which "lock" the entity for us
     * @param entity
     * @return
     * @throws IllegalQueryStateException
     */
    private boolean canDowncastEntity(Entity entity, EntityType entityType) throws IllegalQueryStateException {
        int fvFound = 0;
        int fvMatch = 0;
        for (NodeType nd : entityType.getNodeTypes()) {
            final Object fv = nd.getFixedValue();
            if (fv != null) {
                fvFound++;
                final ValueNode node = entity.getChild(nd.getName(), ValueNode.class, false);
                if (node != null) {
                    LOG.debug("Looking for matching fixed value {} in candidate types",node.getValue());
                    if (fv.equals(node.getValue())) {
                        LOG.debug("Found matching fixed value for downcast {}", node.getValue());
                        fvMatch++;
                    }
                }
            }
        }
        if (fvFound == 0) {
            throw new IllegalQueryStateException("Invalid downcast candidate '" + entityType + "', no fixed values defined");
        }
        return fvFound == fvMatch;
    }

    /**
     * Sets all ToMany relations which had query joins to 'fetched'
     * @param queryObject
     * @throws IllegalQueryStateException
     */
    private void processToManyRelations(QueryObject<?> queryObject) throws IllegalQueryStateException {
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

    private void verifyAllFetchedDataIsLinked() throws IllegalQueryStateException {
        verifyRefs(query);

        /*
        * todo: add back in some kind of ToMany validation

        for (LoadedToMany loadedToMany: loadedToManys) {
          loadedToMany.validate();
        }
        */
    }

    private void prepareQueryResult() throws IllegalQueryStateException {
        //get the entities from the top level query object
        List<Entity> loadedEntities = entityLoaders.getEntitiesForQueryObject(query);
        //add them to the result
        queryResult.addEntities(loadedEntities);
    }

    /**
     * Verifies that all joined refs with a FK value were loaded
     * @param queryObject
     * @throws IllegalQueryStateException
     */
    private void verifyRefs(QueryObject<?> queryObject) throws IllegalQueryStateException {
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
                        throw new IllegalQueryStateException("Joined FK key was not loaded: " + refNode);
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
     * @throws SortQueryException
     * @throws SQLException
     */
    private void processRow(ResultSet resultSet) throws SortJdbcException, SortQueryException {
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
