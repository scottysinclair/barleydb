package scott.barleydb.server.jdbc.persist;

import java.util.Collection;

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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextHelper;
import scott.barleydb.api.core.entity.FetchHelper;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.jdbc.SortJdbcException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.query.ConditionVisitor;
import scott.barleydb.api.query.QCondition;
import scott.barleydb.api.query.QExists;
import scott.barleydb.api.query.QLogicalOp;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
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

    //the max number of conditions a query can have
    private static final Integer MAX_QUERY_SIZE = 500;

    private final boolean loadKeysOnly;
    private final EntityContext myentityContext;

    public DatabaseDataSet(EntityContext entityContext) {
        this(entityContext, false);
    }

    /**
     * @param entityContext the entity context to share a transaction with
     * @param loadKeysOnly if only the keys should be loaded.
     */
    public DatabaseDataSet(EntityContext entityContext, boolean loadKeysOnly) {
        myentityContext = entityContext.newEntityContextSharingTransaction();
        myentityContext.setAllowGarbageCollection(false);
        this.loadKeysOnly = loadKeysOnly;
    }

    public void prepopulate(EntityContext other) {
        List<Entity> copied = EntityContextHelper.addEntities(other.getEntities(), myentityContext, true, false);
        EntityContextHelper.copyRefStates(other, myentityContext, copied, new EntityContextHelper.EntityFilter() {
            @Override
            public boolean includesEntity(Entity entity) {
                return true;
            }
        });
    }

    public EntityContext getOwnEntityContext() {
        return myentityContext;
    }

    public Entity getEntity(EntityType entityType, Object key) {
        return myentityContext.getEntity(entityType, key, false);
    }

    public void loadEntities(Collection<Entity> toSave) throws SortServiceProviderException, BarleyDBQueryException {
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
        OperationGroup optimized = new OperationGroup(toSave).optimizedForInsertCopy();

        batchLoader.addEntities(optimized.getEntities());

        LOG.debug("-------------------------------");
        LOG.debug("Performing database load ...");
        /*
         * actually perform the queries, loading the entityContext
         */
        batchLoader.load();
    }

    /**
     * Load all of the entities in the groups into the dataset
     * @throws SortJdbcException
     * @throws BarleyDBQueryException
     */
    public void loadEntities(OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) throws SortServiceProviderException, BarleyDBQueryException  {

        /*
         * Build queries to load all of these entites
         */
        BatchEntityLoader batchLoader = new BatchEntityLoader();

        /*
         * We optimise the order for insert, this helps prevent table deadlock
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

    /**
     * Loads entities from the database in batches
     * the order of loading on the different tables is fixed
     * by the order of the entities we receive.
     */
    private class BatchEntityLoader {
        private final LinkedHashMap<EntityType, List<QueryObject<Object>>> map;

        public BatchEntityLoader() {
            this.map = new LinkedHashMap<>();
        }

        public void addEntities(List<Entity> entities) {
            for (Entity entity : entities) {
                if (!myentityContext.containsKey(entity)) {
                    addKeyCondition(entity);
                }
            }
        }

        public void load() throws SortServiceProviderException, BarleyDBQueryException  {
            Database database = ConnectionResources.getMandatoryForQuery(myentityContext).getDatabase();
            if (!database.supportsBatchUpdateCounts()) {
                if (database.supportsSelectForUpdate()) {
                    addForUpdatePessimistickLockToQueries(database);
                }
            }

            QueryBatcher batcher = new QueryBatcher();
            for (Map.Entry<EntityType, List<QueryObject<Object>>> entry : map.entrySet()) {
                for (QueryObject<Object> qo: entry.getValue()) {
                    batcher.addQuery(qo);
                }
            }
            myentityContext.performQueries(batcher);
        }

        /**
         * Adds a filter for a specific entity key
         */
        private void addKeyCondition(Entity entity) {
            addKeyConditions(entity.getEntityType(), entity.getKeyValue());
        }

        /**
         * Adds a filter for a specific entity key
         * @param entityType
         */
        private void addKeyConditions(final EntityType entityType, Object key) {
            final QueryObject<Object> query = getQueryForEntityType(entityType, true);
            final QCondition condition = FetchHelper.getKeyConditions(entityType, query, key);
            if (query.getCondition() == null) {
                query.where(condition);
            }
            else {
                query.or(condition);
            }
        }


        private QueryObject<Object> getQueryForEntityType(EntityType entityType, boolean newQueryIfMaxSizeReached) {
            List<QueryObject<Object>> queries = map.get(entityType);
            if (queries == null) {
                queries = new LinkedList<>();
                QueryObject<Object> qo = new QueryObject<>(entityType.getInterfaceName());
                if (loadKeysOnly) {
                   for (String keyNodeName : entityType.getKeyNodeNames()) {
                      QProperty<?> keyProp = new QProperty<>(qo, keyNodeName);
                      qo.select(keyProp);
                   }
                }
                queries.add(qo);
                map.put(entityType, queries);
            }
            else {
                QueryObject<Object> qo = queries.get(queries.size()-1);
                if (newQueryIfMaxSizeReached && countPropertyConditions(qo) >= MAX_QUERY_SIZE) {
                    qo = new QueryObject<>(entityType.getInterfaceName());
                    if (loadKeysOnly) {
                       for (String keyNodeName : entityType.getKeyNodeNames()) {
                          QProperty<?> keyProp = new QProperty<>(qo, keyNodeName);
                          qo.select(keyProp);
                       }
                    }
                    queries.add(qo);
                }
            }
            return queries.get(queries.size()-1);
        }

        private int countPropertyConditions(QueryObject<?> query) {
            PropertyConditionCounter counter = new PropertyConditionCounter();
            try { query.getCondition().visit(counter); }
            catch(Exception x) { throw new IllegalStateException("Could not count the property conditions in query object"); }
            return counter.getCount();
        }

        private void addForUpdatePessimistickLockToQueries(Database database) {
            for (Map.Entry<EntityType, List<QueryObject<Object>>> entry : map.entrySet()) {
                for (QueryObject<Object> query: entry.getValue()) {
                    if (database.supportsSelectForUpdateWaitN()) {
                        query.forUpdateWait(10);
                    } else {
                        query.forUpdate();
                    }
                }
            }
        }

    }
}

class PropertyConditionCounter implements ConditionVisitor {
    private int count = 0;
    public int getCount() { return count; }
    @Override
    public void visitPropertyCondition(final QPropertyCondition qpc) throws IllegalQueryStateException {
        count++;
    }
    @Override
    public void visitLogicalOp(final QLogicalOp qlo) throws IllegalQueryStateException, ForUpdateNotSupportedException  {
        qlo.getLeft().visit(this);
        qlo.getRight().visit(this);
    }
    @Override
    public void visitExists(final QExists exists) throws IllegalQueryStateException, ForUpdateNotSupportedException {
        exists.getSubQueryObject().getCondition().visit(this);
    }
}
