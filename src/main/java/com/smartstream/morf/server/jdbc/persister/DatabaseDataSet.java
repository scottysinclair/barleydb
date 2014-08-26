package com.smartstream.morf.server.jdbc.persister;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.config.*;
import com.smartstream.morf.api.core.*;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.EntityContext;
import com.smartstream.morf.api.query.*;



public class DatabaseDataSet {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseDataSet.class);

    private final EntityContext myentityContext;

    public DatabaseDataSet(Environment env, String namespace) {
        myentityContext = new EntityContext(env, namespace);
    }

    public EntityContext getOwnEntityContext() {
        return myentityContext;
    }

    public Entity getEntity(EntityType entityType, Object key) {
        return myentityContext.getEntity(entityType, key, false);
    }

    /**
     * Load all of the entities in the groups into the dataset
     */
    public void loadEntities(OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) throws Exception {

        /*
         * Build queries to load all of these entites
         */
        BatchEntityLoader batchLoader = new BatchEntityLoader();

        /*
         * We optimize the order for insert, this helps prevent table deadlock
         * as delete operations would lock in the reverse order.
         * Note: this only helps prevent deadlock if REPEATABLE_READ is used
         * sicne REPEATABLE_READ ensures that read operations also lock rows.
         */
        LOG.debug("Reordering entities for database retrival to reduce table deadlock scenarios.");
        //the dependsOnGroup ordering is the same as for create/update
        //so merge it first
        OperationGroup mergedAndOptimized = dependsOnGroup.mergedCopy(updateGroup, deleteGroup.reverse() ).optimizedForInsertCopy();

        batchLoader.addEntities( mergedAndOptimized.getEntities() );

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
        private final LinkedHashMap<EntityType,QueryObject<Object>> map;
        public BatchEntityLoader() {
            this.map = new LinkedHashMap<>();
        }

        public void addEntities(List<Entity> entities) {
            for (Entity entity: entities) {
                addKeyCondition(entity);
            }
        }

        public void addEntityKey(EntityType entityType, Object key) {
        	addKeyCondition(entityType, key);
        }

        public void load() throws Exception {
        	QueryBatcher batcher = new QueryBatcher();
            for (Map.Entry<EntityType,QueryObject<Object>> entry: map.entrySet()) {
            	batcher.addQuery( entry.getValue() );
            }
            myentityContext.performQueries( batcher );
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
            final QueryObject<Object> query = getQueryForEntityType( entityType );
            final QCondition condition = getKeyCondition(entityType, query, entityKey );
            if (query.getCondition() == null) {
                query.where( condition );
            }
            else {
                query.or( condition );
            }
        }

        private QCondition getKeyCondition(EntityType entityType, QueryObject<Object> query, Object key) {
            final QProperty<Object> pk = new QProperty<Object>(query, entityType.getKeyNodeName());
            return pk.equal( key );
        }


        private QueryObject<Object> getQueryForEntityType(EntityType entityType) {
            QueryObject<Object> qo = map.get( entityType );
            if (qo == null) {
                qo = myentityContext.newQuery(entityType);
                map.put(entityType, qo);
            }
            return qo;
       }

    }
}
