package scott.barleydb.server.jdbc.persist;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.query.QCondition;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.resources.ConnectionResources;
import scott.barleydb.server.jdbc.vendor.Database;

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
        myentityContext.setAllowGarbageCollection(false);
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
    public void loadEntities(OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) throws SortServiceProviderException, SortQueryException  {

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

        public void load() throws SortServiceProviderException, SortQueryException  {
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
