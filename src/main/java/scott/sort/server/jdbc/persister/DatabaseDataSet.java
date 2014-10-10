package scott.sort.server.jdbc.persister;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.*;
import scott.sort.api.core.*;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.SortJdbcException;
import scott.sort.api.exception.query.SortQueryException;
import scott.sort.api.query.*;
import scott.sort.server.jdbc.database.Database;
import scott.sort.server.jdbc.resources.ConnectionResources;

/**
 * Used to load the original database data for all entities which we are updating, deleting or depending on.
 *
 * All of the data is loaded into a separate context.
 *
 * @author scott
 *
 */
public class DatabaseDataSet {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseDataSet.class);

    private final EntityContext myentityContext;

    public DatabaseDataSet(EntityContext entityContext) {
        myentityContext = entityContext.newEntityContextSharingTransaction();
    }

    public EntityContext getOwnEntityContext() {
        return myentityContext;
    }

    public Entity getEntity(EntityType entityType, Object key) {
        return myentityContext.getEntity(entityType, key, false);
    }

    /**
     * Load all of the entities in the groups into the dataset
     * @throws SortJdbcException
     * @throws SortQueryException
     */
    public void loadEntities(OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) throws SortJdbcException, SortQueryException  {

        /*
         * Build queries to load all of these entites
         */
        BatchEntityLoader batchLoader = new BatchEntityLoader();

        /*
         * We optimize the order for insert, this helps prevent table deadlock
         * as delete operations would lock in the reverse order.
         * Note: this only helps prevent deadlock if REPEATABLE_READ is used
         * since REPEATABLE_READ ensures that read operations also lock rows.
         */
        LOG.debug("Reordering entities for database retrival to reduce table deadlock scenarios.");
        //the dependsOnGroup ordering is the same as for create/update
        //so merge it first
        OperationGroup mergedAndOptimized = dependsOnGroup.mergedCopy(updateGroup, deleteGroup.reverse()).optimizedForInsertCopy();

        batchLoader.addEntities(mergedAndOptimized.getEntities());

        LOG.debug("-------------------------------");
        LOG.debug("Performing database load ...");
        /*
         * actually perform the queries, loading the entityContext
         */
        batchLoader.load();
    }

    public Entity loadEntity(EntityType entityType, Object entityKey) throws Exception {
        BatchEntityLoader batchLoader = new BatchEntityLoader();
        batchLoader.addEntityKey(entityType, entityKey);
        batchLoader.load();
        return myentityContext.getEntity(entityType, entityKey, false);
    }

    /**
     * Loads entities from the database in batches
     * the order of loading on the different tables is fixed
     * by the order of the entities we receive.
     */
    private class BatchEntityLoader {
        private final LinkedHashMap<EntityType, QueryObject<Object>> map;

        public BatchEntityLoader() {
            this.map = new LinkedHashMap<>();
        }

        public void addEntities(List<Entity> entities) {
            for (Entity entity : entities) {
                addKeyCondition(entity);
            }
        }

        public void addEntityKey(EntityType entityType, Object key) {
            addKeyCondition(entityType, key);
        }

        public void load() throws SortJdbcException, SortQueryException  {
            Database database = ConnectionResources.getMandatoryForQuery(myentityContext).getDatabase();
            if (!database.supportsBatchUpdateCounts()) {
                if (database.supportsSelectForUpdate()) {
                    addForUpdatePessimistickLockToQueries(database);
                }
            }

            QueryBatcher batcher = new QueryBatcher();
            for (Map.Entry<EntityType, QueryObject<Object>> entry : map.entrySet()) {
                batcher.addQuery(entry.getValue());
            }
            myentityContext.performQueries(batcher);
        }

        /**
         * Adds a filter for a specific entity key
         * @param entityType
         * @param entityKey
         */
        private void addKeyCondition(Entity entity) {
            addKeyCondition(entity.getEntityType(), entity.getKey().getValue());
        }

        /**
         * Adds a filter for a specific entity key
         * @param entityType
         * @param entityKey
         */
        private void addKeyCondition(final EntityType entityType, Object entityKey) {
            final QueryObject<Object> query = getQueryForEntityType(entityType);
            final QCondition condition = getKeyCondition(entityType, query, entityKey);
            if (query.getCondition() == null) {
                query.where(condition);
            }
            else {
                query.or(condition);
            }
        }

        private QCondition getKeyCondition(EntityType entityType, QueryObject<Object> query, Object key) {
            final QProperty<Object> pk = new QProperty<Object>(query, entityType.getKeyNodeName());
            return pk.equal(key);
        }

        private QueryObject<Object> getQueryForEntityType(EntityType entityType) {
            QueryObject<Object> qo = map.get(entityType);
            if (qo == null) {
                qo = myentityContext.getUnitQuery(entityType);
                map.put(entityType, qo);
            }
            return qo;
        }

        private void addForUpdatePessimistickLockToQueries(Database database) {
            for (Map.Entry<EntityType, QueryObject<Object>> entry : map.entrySet()) {
                QueryObject<Object> query = entry.getValue();
                if (database.supportsSelectForUpdateWaitN()) {
                    query.forUpdateWait(10);
                }
                else {
                    query.forUpdate();
                }
            }
        }

    }
}
