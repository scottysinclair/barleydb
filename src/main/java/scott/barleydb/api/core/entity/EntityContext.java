package scott.barleydb.api.core.entity;

import static scott.barleydb.api.core.entity.EntityContextHelper.toParents;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.barleydb.api.audit.AuditInformation;
import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.QueryRegistry;
import scott.barleydb.api.core.entity.FetchHelper.EntityPath;
import scott.barleydb.api.core.entity.context.Entities;
import scott.barleydb.api.core.entity.context.EntityInfo;
import scott.barleydb.api.core.proxy.ProxyList;
import scott.barleydb.api.core.util.EnvironmentAccessor;
import scott.barleydb.api.dependency.diagram.DependencyDiagram;
import scott.barleydb.api.dependency.diagram.Link;
import scott.barleydb.api.dependency.diagram.LinkType;
import scott.barleydb.api.exception.constraint.EntityConstraintMismatchException;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.OptimisticLockMismatchException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.exception.execution.query.ForUpdateNotSupportedException;
import scott.barleydb.api.exception.execution.query.IllegalQueryStateException;
import scott.barleydb.api.exception.model.ProxyCreationException;
import scott.barleydb.api.persist.OperationType;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.api.stream.EntityData;
import scott.barleydb.api.stream.EntityStreamException;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.api.stream.QueryEntityDataInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;
import scott.barleydb.server.jdbc.query.QueryGenerator;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.server.jdbc.query.QueryGenerator.Param;

/**
 * Contains a set of entities.<br/>
 * and has a session for performing operations.<br/>
 *<br/>
 * Has either status autocommit true or false.<br/>
 *<br/>
 * Transaction Handling:<br/>
 * Client Side:<br/>
 *   - only autocommit == true is supported, no connection resources are associated
 *   with the entity context
 *<br/>
 * Server Side:<br/>
 *     - autocommit == true, typically means no connection is associated with this entity context.<br/>
 *     - autocommit == false, a connection is associated and has autocommit set to false.<br/>
 *
 *
 * @author scott
 *
 */
public class EntityContext implements Serializable, AutoCloseable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EntityContext.class);

    private String namespace;
    private Entities entities;
    private EntityContextState entityContextState;

    private Environment env;
    private QueryRegistry userQueryRegistry;
    private Definitions definitions;

    private FetchHelper fetchHelper;
    /**
     * weak hashmap used to allow garbage collection of the entity
     */
    private WeakHashMap<UUID, WeakReference<Object>> proxies;
    /**
     * A place to store extra resources like transaction information
     */
    private Map<String,Object> resources;

    private Statistics statistics = new Statistics();


    public EntityContext(Environment env, String namespace) {
        this.env = env;
        this.namespace = namespace;
        init(true);
        //fetch helper only holds a ref to the ctx (escaped this)
        this.fetchHelper = new FetchHelper(this);
        this.entityContextState = EntityContextState.USER;
    }

    private void init(boolean allowGarbageCollection) {
        this.definitions = env.getDefinitions(namespace);
        this.userQueryRegistry = definitions.newUserQueryRegistry();
        /*
         * store in a sorted-set ordered by primary key,
         * so that we always have proper ordering of Lists etc.
         */
        entities = new Entities(allowGarbageCollection);
        proxies = new WeakHashMap<>();
        resources = new HashMap<String, Object>();
    }
    /**
     * if set to true, unreferenced entities will be garbage collected.
     * @param allow
     */
    public void setAllowGarbageCollection(boolean allow) {
        entities.setAllowGarbageCollection(allow);
    }

    public boolean isAllowGarbageCollection() {
        return entities.isAllowGarbageCollection();
    }

    public Statistics getStatistics() {
      return statistics;
    }

    /**
     * registers the given queries to be used for fetching their respective objects.
     * @param qos
     */
    public void register(QueryObject<?>... qos) {
        userQueryRegistry.register(qos);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setAutocommit(boolean value) throws SortServiceProviderException {
        env.setAutocommit(this, value);
    }

    public boolean getAutocommit() throws SortServiceProviderException {
        return env.getAutocommit(this);
    }

    /**
     * Rolls back the current transaction
     * @throws SortServiceProviderException if there is no transaction or the rollback failed.
     */
    public void rollback() throws SortServiceProviderException {
        env.rollback(this);
    }

    public void commit() throws SortServiceProviderException {
        env.commit(this);
    }

    public void close() throws SortServiceProviderException {
        env.close(this);
    }

    /**
     * gets a resource associated with the entity context.
     * @param key
     * @param required
     * @return
     */
    public Object getResource(String key, boolean required) {
        Object value = resources.get(key);
        if (value == null && required) {
            throw new IllegalStateException("Could not get resource '" + key + "'");
        }
        return value;
    }

    public Object setResource(String key, Object resource) {
        return resources.put(key, resource);
    }

    public Object removeResource(String key) {
        return resources.remove(key);
    }

    public Environment getEnv() {
        return env;
    }

    /**
     *
     * @return the number of entities in the context.
     */
    public int size() {
        return entities.size();
    }

    /**
     * an iterable object of all the entities in the context.
     * @return
     */
    public Iterable<Entity> getEntities() {
        return entities;
    }

    public Iterable<Entity> getEntitiesSafeIterable() {
        return entities.safe();
    }


    public void clear() {
        entities.clear();
        proxies.clear();
    }

    public Element toXml(Document doc) {
        EntityContextState prev = entityContextState;
        entityContextState = EntityContextState.INTERNAL;
        try {
            Element element = doc.createElement("entityContext");
            List<Entity> entitiesList = new ArrayList<>(entities.safe());
            Collections.sort(entitiesList, new Comparator<Entity>() {
                public int compare(Entity e1, Entity e2) {
                    int value = e1.getEntityType().getInterfaceName().compareTo(e2.getEntityType().getInterfaceName());
                    if (value != 0) {
                        return value;
                    }
                    Comparable<Object> e1Key = e1.getKeyValue();
                    Comparable<Object> e2Key = e2.getKeyValue();
                    if (e1Key != null) {
                        return e2Key != null ? e1Key.compareTo(e2Key) : 1;
                    }
                    else if (e2Key != null) {
                        return -1;
                    }
                    else {
                        return e1.getUuid().compareTo(e2.getUuid());
                    }
                }
            });
            for (Entity en : entitiesList) {
                Element el = en.toXml(doc);
                element.appendChild(el);
            }
            return element;
        } finally {
            entityContextState = prev;
        }
    }



    /**
     * gets a proxy for the given entity, creating it if required.
     * @param entity
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Entity entity) {
        WeakReference<Object> p = proxies.get(entity.getUuid());
        if (p == null || p.get() == null ) {
            try {
                Object o = env.generateProxy(entity);
                proxies.put(entity.getUuid(), new WeakReference<>(o));
                return (T)o;
            } catch (Exception x) {
                throw new IllegalStateException("Could not generated proxy", x);
            }
        }
        return (T)p.get();
    }

    public Definitions getDefinitions() {
        return definitions;
    }

    public EntityContextState getEntityContextState() {
        return entityContextState;
    }

    public void setEntityContextState(EntityContextState entityContextState) {
        this.entityContextState = entityContextState;
    }

    public boolean isUser() {
        return entityContextState == EntityContextState.USER;
    }

    public boolean isInternal() {
        return entityContextState == EntityContextState.INTERNAL;
    }

    public EntityContextState switchToInternalMode() {
        EntityContextState old = this.entityContextState;
        this.entityContextState = EntityContextState.INTERNAL;
        return old;
    }

    public EntityContextState switchToExternalMode() {
        EntityContextState old = this.entityContextState;
        this.entityContextState = EntityContextState.USER;
        return old;
    }

    public EntityContextState switchToMode(EntityContextState mode) {
        EntityContextState old = this.entityContextState;
        this.entityContextState = mode;
        return old;
    }


    /*
     * =====================================================================
     *  BEGIN ENTITY OPERATIONS
     * =====================================================================
     */
    public Entity newEntity(EntityType entityType) {
        return newEntity(entityType, null, EntityConstraint.noConstraints());
    }

    public Entity newEntity(EntityType entityType, Object key) {
        return newEntity(entityType, key, EntityConstraint.noConstraints());
    }

    public Entity newEntity(EntityType entityType, Object key, EntityConstraint constraints) {
        if (key != null) {
            /*
             * the entity must be new to the context
             * we don't care if it exists in the database or not
             */
            Entity entity = getEntity(entityType, key, false);
            if (entity != null) {
                throw new IllegalStateException("Entity with key '" + entity.getKeyValue() + "' already exists.");
            }
        }

        Entity entity = new Entity(this, entityType, key, constraints);
        add(entity);
        return entity;
    }

    public Entity newEntity(EntityType entityType, Object key, EntityConstraint constraints, UUID uuid) {
      if (key != null) {
          /*
           * the entity must be new to the context
           * we don't care if it exists in the database or not
           */
          Entity entity = getEntity(entityType, key, false);
          if (entity == null) {
            entity = getEntityByUuid(uuid, false);
          }
          if (entity != null) {
              throw new IllegalStateException("Entity with key '" + entity.getKeyValue() + "' already exists.");
          }
      }

      Entity entity = new Entity(this, entityType, key, uuid, constraints);
      add(entity);
      return entity;
  }

    /**
     * Gets an existing entity from the context, or creates a new entity to be inserted into the database
     * @param class1
     * @param key
     * @param constrains the constraints to use if creating the Entity
     * @return
     * @throws ProxyCreationException
     * @throws EntityConstraintMismatchException
     */
    public Entity getEntityOrNewEntity(EntityType entityType, Object key, EntityConstraint constraints)  {
        Entity entity = getEntity(entityType, key, false);
        if (entity == null) {
            entity = new Entity(this, entityType, key, constraints);
            add(entity);
        }
        return entity;
    }

    /**
     * Returns the entity from the context, loading it first if required.
     * @param entityType
     * @param key
     * @return the entity of null if it does not exist in the context or database
     */
    public Entity getEntityOrLoadEntity(EntityType entityType, Object key, boolean mustExist) {
        Entity entity = getEntity(entityType, key, false);
        if (entity != null) {
            return entity;
        }
        /*
         * capture the must exist constraint so we can apply it to the entity
         * before we fetch.
         */
        EntityConstraint constraint = new EntityConstraint(mustExist, false);
        EntityContext tmp = newEntityContextSharingTransaction();
        entity = new Entity(tmp, entityType, key, constraint);
        tmp.add(entity);
        /*
         * fetching will fail, if the entity must exist but does not.
         */
        tmp.fetchSingle(entity, true);
        if (entity.isLoaded()) {
            return copyInto(entity);
        }
        return null;
    }

    /**
     * Returns the entity from the context if it exists
     * Otherwise loads it
     * Otherwise creates it
     *
     * @param entityType
     * @param key
     * @return the entity of null if it does not exist in the context or database
     */
    public Entity getEntityOrLoadEntityOrNewEntity(EntityType entityType, Object key, EntityConstraint constraints) {
        Entity entity = getEntityOrLoadEntity(entityType, key, false);
        if (entity == null) {
            entity = new Entity(this, entityType, key, constraints);
        }
        return entity;
    }

    @SuppressWarnings("unchecked")
    public <T> T getModel(Class<T> type, Object key, boolean mustExist)  {
        EntityType entityType = definitions.getEntityTypeForClass(type, true);
        Entity entity = getEntity(entityType, key, mustExist);
        return entity != null ? (T) getProxy(entity) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getModelOrLoadModel(Class<T> type, Object key, boolean mustExist) {
        EntityType entityType = definitions.getEntityTypeForClass(type, true);
        Entity entity = getEntityOrLoadEntity(entityType, key, mustExist);
        return (entity != null) ? (T)getProxy(entity) : null;
    }

    public <T> T getModelOrNewModel(Class<T> type, Object key)  {
        //hmm, if we have no constrants, perhaps we don't care if the entity is already
        //existing with constraints
        //we will find out if we are getting too many exceptions around this.....
        return getModelOrNewModel(type, key, EntityConstraint.noConstraints());
    }

    /**
     *
     * @param type
     * @param key
     * @param constraints the constrains to use if creating the model
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getModelOrNewModel(Class<T> type, Object key, EntityConstraint constraints)  {
        EntityType entityType = definitions.getEntityTypeForClass(type, true);
        Entity entity = getEntityOrNewEntity(entityType, key, constraints);
        if (entity != null) {
            return (T)getProxy(entity);
        }
        return null;
    }

    /**
     * Creates an Entity in the EntityContext for the given EntityData
     * @param entityData
     * @return
     */
    public Entity addEntityLoadedFromDB(EntityData entityData, QueryObject<?> optionalQuery) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(entityData.getEntityType(), true);
        Object key = entityData.getData().get( entityType.getKeyNodeName() );
        LOG.debug("Adding or creating Entity for EntityData {} with key {}", entityType, key);
        Entity entity = getEntity(entityType, key, false);
        if (entity == null) {
            entity = new Entity(this, entityType, key, entityData.getConstraints());
            add(entity, optionalQuery);
        }
        else {
            LOG.debug("Found entity already in ctx {}", entity);
            entity.getConstraints().set( entityData.getConstraints() );
            entity.getEntityContext().setAssociatedQuery(entity, optionalQuery);
        }
        if (entity.getEntityState() == EntityState.NOTLOADED) {
            for (Node child: entity.getChildren()) {
                if (entity.getKey() == child) {
                    continue;
                }
                if (child  instanceof ValueNode) {
                    ((ValueNode)child).setValue( NotLoaded.VALUE );
                }
                else if (child instanceof RefNode) {
                    ((RefNode)child).setLoaded(false);
                }
            }
        }
        entity.setEntityState(EntityState.LOADING);

        LOG.debug("--------------------------------------------------------");
        /*
         * apply the data from the EntityData object onto the Entitie's nodes.
         */
        for (Map.Entry<String, Object> entry: entityData.getData().entrySet()) {
            Node node = entity.getChild( entry.getKey() );
            Object value = entry.getValue();
            if (node == entity.getKey()) {
                continue;
            }
            if (node instanceof ValueNode) {
                LOG.trace("Setting value of {} to {}", node.getName(), value);
                ((ValueNode)node).setValueNoEvent( value );
            }
            else if (node instanceof RefNode) {
                RefNode refNode = (RefNode)node;
                refNode.setLoaded(true);
                if (value != null) {
                    LOG.trace("Processing RefNode {} with key {}", refNode.getName(), value);
                    Entity reffed = getEntity(refNode.getEntityType(), value, false);
                    if (reffed != null) {
                       //as we have a foreign key in the entity data we assume that FK entity must exist in the database
                      //(method is called addEntityLoadedFromDB)
                        reffed.getConstraints().setMustExistInDatabase();
                    }
                    else {
                        //MUST EXIST IN DATABASE BECAUSE of our method name
                        reffed = newEntity(refNode.getEntityType(), value, EntityConstraint.mustExistInDatabase());
                    }
                    refNode.setReference( reffed );
                }
                else {
                    refNode.setReference(null);
                }
            }
        }
        entity.setEntityState( entityData.getEntityState() );
        LOG.debug("--------------------------------------------------------");
        return entity;
    }


    private void setAssociatedQuery(Entity entity, QueryObject<?> query) {
    	entities.setAssociatedQuery(entity, query);
	}

	public QueryObject<?> getAssociatedQuery(Entity entity) {
    	return entities.getAssociatedQuery(entity);
    }

    void add(Entity entity) {
    	add(entity, null);
    }
    void add(Entity entity, QueryObject<?> optionalQuery) {
        if (getEntityByUuid(entity.getUuid(), false) != null) {
            throw new IllegalStateException("Entity with uuid '" + entity.getUuid() + "' already exists.");
        }
        if (entity.getKeyValue() != null && getEntity(entity.getEntityType(), entity.getKeyValue(), false) != null) {
            throw new IllegalStateException("Entity with key '" + entity.getKeyValue() + "' already exists.");
        }
        entities.add(entity, optionalQuery);
        LOG.debug("Added to entityContext " + entity);
    }

    /**
     * Copies a single entity into our context
     * If the entity already exists, then the values and the fk refs are updated accordingly, the
     * state of any ToMany nodes is left untouched
     *
     * @param entity
     *            return the new entity
     */
    public Entity copyInto(Entity entity) {
        EntityContextState ecs1 = switchToInternalMode();
        EntityContextState ecs2 = entity.getEntityContext().switchToInternalMode();
        try {
            if (entity.getEntityContext() == this) {
                throw new IllegalStateException("Cannot copy entity into same context");
            }
            Entity ours = null;
            if (entity.getUuid() != null) {
                ours = getEntityByUuid(entity.getUuid(), false);
            }
            if (ours == null && entity.getKeyValue() != null) {
                ours = getEntity(entity.getEntityType(), entity.getKeyValue(), false);
            }
            if (ours == null) {
                ours = new Entity(this, entity);
                add(ours);
                ours.setEntityState(entity.getEntityState());
            }
            for (ValueNode valueNode : entity.getChildren(ValueNode.class)) {
                ours.getChild(valueNode.getName(), ValueNode.class).setValueNoEvent(valueNode.getValue());
            }
            for (RefNode refNode : entity.getChildren(RefNode.class)) {
                Entity refEntity = refNode.getReference();
                if (refEntity != null && refEntity.getKeyValue() != null) {
                    Entity ourRefEntity = getEntity(refNode.getEntityType(), refEntity.getKeyValue(), false);
                    if (ourRefEntity == null) {
                        //copy the constraints from the original entity's referenced entity
                        ourRefEntity = newEntity(refNode.getEntityType(), refEntity.getKeyValue(), refEntity.getConstraints());
                    }
                    ours.getChild(refNode.getName(), RefNode.class).setReference( ourRefEntity );
                }
            }
            return ours;
        } finally {
            entity.getEntityContext().switchToMode(ecs2);
            switchToMode(ecs1);
        }
    }

    /**
     * Creates a new entity of the given type with no constraints
     * @param type
     * @return
     */
    public <T> T newModel(Class<T> type) {
        return newModel(type, EntityConstraint.noConstraints());
    }

    /**
     * Creates a new entity of the given type with the given constraints
     * @param type
     * @return
     */
    public <T> T newModel(Class<T> type, EntityConstraint constraints) {
        return newModel(type, null, constraints);
    }

    /**
     * Creates a new entity of the given type with the given key with no constraints.
     * <br/>
     * This means that it may or may not exist in the database
     * @param type
     * @return
     */
    public <T> T newModel(Class<T> type, Object key) {
        return  newModel(type, key, EntityConstraint.noConstraints());
    }

    /**
     * Creates a new entity of the given type with the given key and constraints
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T newModel(Class<T> type, Object key, EntityConstraint constraints) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(type.getName(), true);
        Entity entity = newEntity(entityType, key, constraints);
        return (T) getProxy(entity);
    }

    public void removeAll(Class<?> type) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(type.getName(), true);
        for (Entity entity: entities.getEntitiesByType(entityType)) {
            remove(entity);
        }
    }


    public void remove(Entity entity) {
       remove(entity, Collections.<Entity> emptyList());
    }

    private void remove(Entity entity, List<Entity> allEntitiesToBeRemoved) {
        EntityInfo entityInfo = entities.getByUuid(entity.getUuid(), true);
        Collection<RefNode> refNodes = entityInfo.getFkReferences();
        if (!refNodes.isEmpty() && !allEntitiesToBeRemoved.containsAll(toParents(refNodes))) {
            entity.unload(false);
            LOG.debug("Unloaded entity " + entity + " " + entity.getUuid());
        } else {
            for (RefNode refNode : entity.getChildren(RefNode.class)) {
                if (refNode.getReference(false) != null) {
                    removeReference(refNode, refNode.getReference(false));
                }
            }
            entities.remove(entity);
            proxies.remove(entity.getUuid());
            LOG.debug("Removed from entityContext " + entity + " " + entity.getUuid());
        }
    }

    public void unload(Object object, boolean includeOwnedEntities) {
        if (object instanceof ProxyController) {
            ProxyController pc = (ProxyController) object;
            entities.remove(pc.getEntity());
            pc.getEntity().unload(includeOwnedEntities);
        }
    }

    /*
     * =====================================================================
     *  END ENTITY OPERATIONS
     * =====================================================================
     */

    public void handleKeySet(Entity entity, Object originalKey, List<ValueNode> newKey) {
         EntityContextState prev = entityContextState;
         try {
             entityContextState = EntityContextState.INTERNAL;
             final EntityInfo entityInfo = entities.keyChanged(entity, originalKey);
             if (entityInfo == null) {
                 throw new IllegalStateException("Could not find entity, to change the key: " + entity.getUuid());
             }
         }
         finally {
             entityContextState = prev;
         }
    }

    public <T> void performQueries(QueryBatcher queryBatcher) throws SortServiceProviderException, BarleyDBQueryException {
        performQueries(queryBatcher, null);
    }
    public <T> void performQueries(QueryBatcher queryBatcher, RuntimeProperties runtimeProperties) throws SortServiceProviderException, BarleyDBQueryException {
        /*
         * We can perform the query in a fresh context which is copied back to us
         * it gives us control over any replace vs merge logic.
         *
         * It's also means that we don't potentially send our full entity context across the wire
         *
         * But we let the runtime properties decide.
         */

        runtimeProperties = env.overrideProps( runtimeProperties  );
        EntityContext opContext = getOperationContext(this, runtimeProperties);

        QueryBatcher result = env.services().execute(opContext, queryBatcher, runtimeProperties);
        /*
         * Copy the result into the original query batcher if required.
         */
        result.copyTo(this, queryBatcher);
    }

    public <T> ObjectInputStream<T> streamObjectQuery(QueryObject<T> queryObject) throws SortServiceProviderException, BarleyDBQueryException {
        return streamObjectQuery(queryObject, null, false);
    }

    public <T> ObjectInputStream<T> streamObjectQuery(QueryObject<T> queryObject, boolean createNewCtx) throws SortServiceProviderException, BarleyDBQueryException {
        return streamObjectQuery(queryObject, null, createNewCtx);
    }

    public <T> ObjectInputStream<T> streamObjectQuery(QueryObject<T> queryObject, RuntimeProperties runtimeProperties, boolean createNewCtx) throws SortServiceProviderException, BarleyDBQueryException {
        /*
         * We can perform the query in a fresh context which is copied back to us
         * it gives us control over any replace vs merge logic
         *
         * It's also means that we don't potentially send our full entity context across the wire
         *
         * But we let the runtime properties decide.
         */

        runtimeProperties = env.overrideProps( runtimeProperties  );
        EntityContext opContext = getOperationContext(this, runtimeProperties);

        return  new ObjectInputStream<T>( new QueryEntityInputStream(
                env.services().streamQuery(opContext, queryObject, runtimeProperties),
                this,
                createNewCtx));
    }

    public interface BatchPersistProcessor {
        public void beforePersist(EntityContext ctx);
    }

    /**
     * Saves the stream data to the database in batches specified by batchSize.<br/>
     * None of the entities are added to this EntityContext
     *
     * @param in
     * @param batchSize
     * @throws EntityStreamException
     * @throws SortServiceProviderException
     * @throws SortPersistException
     */
    public void batchPersistStreamData(QueryEntityDataInputStream in, int batchSize, OperationType operationType, BatchPersistProcessor processor) throws EntityStreamException, SortServiceProviderException, SortPersistException {
        EntityContext tmpCtx = this.newEntityContext();
        tmpCtx.setAllowGarbageCollection(false);

        try ( QueryEntityInputStream ein = new QueryEntityInputStream(in, tmpCtx, false); ) {
            PersistRequest request = new PersistRequest();
            Entity entity;
            int count = 1;
            while ( (entity = ein.read()) != null) {
                switch (operationType) {
                    case INSERT: request.insert( entity ); break;
                    case UPDATE: request.update( entity ); break;
                    case SAVE: request.save( entity ); break;
                    default: throw new SortPersistException("Unknown operation type " + operationType);
                }
                if (count % batchSize == 0) {
                    /*
                     * persist all of the entities in the request in one transaction.
                     */
                    if (processor != null) {
                        processor.beforePersist( tmpCtx );
                    }
                    tmpCtx.persist( request );
                    /*
                     * start a new request for a new batch.
                     */
                    request = new PersistRequest();
                    /*
                     * clear all entities collected in the context
                     */
                    tmpCtx.clear();
                }
                count++;
            }
            /*
             * save the last entities.
             */
            if (!request.isEmpty()) {
                if (processor != null) {
                    processor.beforePersist( tmpCtx );
                }
                tmpCtx.persist( request );
            }
        }
    }

    public QueryEntityDataInputStream streamQueryEntityData(QueryObject<?> queryObject, RuntimeProperties runtimeProperties) throws SortServiceProviderException, BarleyDBQueryException {
        /*
         * We can perform the query in a fresh context which is copied back to us
         * it gives us control over any replace vs merge logic
         *
         * It's also means that we don't potentially send our full entity context across the wire
         *
         * But we let the runtime properties decide.
         */

        runtimeProperties = env.overrideProps( runtimeProperties  );
        EntityContext opContext = getOperationContext(this, runtimeProperties);

        return env.services().streamQuery(opContext, queryObject, runtimeProperties);
    }



    public <T> QueryResult<T> performQuery(QueryObject<T> queryObject) throws SortServiceProviderException, BarleyDBQueryException {
        return performQuery(queryObject, null);
    }
    public <T> QueryResult<T> performQuery(QueryObject<T> queryObject, RuntimeProperties runtimeProperties) throws SortServiceProviderException, BarleyDBQueryException {
        /*
         * We can perform the query in a fresh context which is copied back to us
         * it gives us control over any replace vs merge logic
         *
         * It's also means that we don't potentially send our full entity context across the wire
         *
         * But we let the runtime properties decide.
         */

        runtimeProperties = env.overrideProps( runtimeProperties  );
        EntityContext opContext = getOperationContext(this, runtimeProperties);

        QueryResult<T> queryResult = env.services().execute(opContext, queryObject, runtimeProperties);
        return queryResult.copyResultTo(this);
    }

    public AuditInformation comapreWithDatabase(ProxyController ...models) throws SortServiceProviderException, SortPersistException  {
      return compareWithDatabase(Arrays.asList(models));
    }

    public AuditInformation compareWithDatabase(List<? extends ProxyController> models) throws SortServiceProviderException, SortPersistException  {
        switchToInternalMode();
        RuntimeProperties runtimeProperties = env.overrideProps( null  );
        try {
            PersistRequest persistRequest = new PersistRequest();
            for (Object o: models) {
              persistRequest.save( o );
            }
            return env.services().compareWithDatabase(persistRequest, runtimeProperties);

        } finally {
            switchToExternalMode();
        }
    }

    public void persist(PersistRequest persistRequest) throws SortServiceProviderException, SortPersistException  {
        persist(persistRequest, null);
    }
    public void persist(PersistRequest persistRequest, RuntimeProperties runtimeProperties) throws SortServiceProviderException, SortPersistException  {
        if (persistRequest.isEmpty()) return;
        EntityContextState prev = switchToInternalMode();
        runtimeProperties = env.overrideProps( runtimeProperties );
        try {
            try {
                PersistAnalyser analyser = env.services().execute(persistRequest, runtimeProperties);
                if (analyser.getEntityContext() != this) {
                    analyser.applyChanges(this);
                }
                LOG.debug("Persist completed successfully");
            }
            catch (OptimisticLockMismatchException x) {
                x.switchEntitiesAndThrow(this);
            }
        } finally {
            switchToMode( prev );
        }
    }

    /**
     * Decides on the entity context to use for a query or persist based on the runtime properties.
     * @param entityContext
     * @param runtimeProperties
     * @return
     */
    private static EntityContext getOperationContext(EntityContext entityContext, RuntimeProperties runtimeProperties) {
        if (runtimeProperties.getExecuteInSameContext() == null || !runtimeProperties.getExecuteInSameContext()) {
            return entityContext.newEntityContextSharingTransaction();
        }
        return entityContext;
    }

    /**
     * @return true if there is an entity in the context with the same type and key
     */
    public boolean containsKey(Entity entity) {
        return getEntity(entity.getEntityType(), entity.getKeyValue(), false) != null;
    }

    /**
     * @return true if there is an entity in the context with the same type and key
     */
    public boolean containsKey(EntityType entityType, Object key) {
        return getEntity(entityType, key, false) != null;
    }

    public boolean contains(Entity entity) {
        for (Entity en : entities) {
            if (en == entity) {
                return true;
            }
        }
        return false;
    }


    /**
     * creates a new entity context which shares the same transaction
     * as the current entity context.
     * @return
     */
    public EntityContext newEntityContext() {
        EntityContext entityContext = new EntityContext(env, namespace);
        entityContext.setAllowGarbageCollection( this.isAllowGarbageCollection() );
        return entityContext;
    }

    /**
     * creates a new entity context which shares the same transaction
     * as the current entity context.
     * @return
     */
    public EntityContext newEntityContextSharingTransaction() {
        EntityContext entityContext = newEntityContext();
        entityContext.statistics = this.statistics;
        env.joinTransaction(entityContext, this);
        return entityContext;
    }



    public <T> QueryObject<T> getQuery(Class<T> clazz) {
        return userQueryRegistry.getQuery(clazz);
    }

    /**
     * Gets a copy of the standard unit query with no joins
     * @param clazz
     * @return
     */
    public QueryObject<Object> getUnitQuery(EntityType entityType) {
        return definitions.getQuery(entityType, false);
    }

    /**
     * Gets a copy of the standard unit query with no joins
     * @param clazz
     * @return
     */
    public QueryObject<Object> getUnitQuery(EntityType entityType, boolean mustExist) {
        return definitions.getQuery(entityType, mustExist);
    }

    QueryObject<Object> getQuery(EntityType entityType, boolean fetchInternal) {
        return fetchInternal ? definitions.getQuery(entityType, false) : userQueryRegistry.getQuery(entityType);
    }

    Set<RefNode> getFKReferences(Entity entity, boolean mustExist) {
        return entities.getByUuid(entity.getUuid(), mustExist).getFkReferences();
    }

    public void fetch(Entity entity) {
        fetch(entity, false, false);
    }

    public void fetchSingle(Entity entity, boolean force) {
        fetch(entity, force, true);
    }

    public void fetch(ToManyNode toManyNode) {
        fetch(toManyNode, false);
    }

    public void batchFetchDescendants(ProxyController pc) {
        fetchHelper.batchFetchDescendants(pc.getEntity());
    }

    public void batchFetchDescendants(Entity entity) {
        fetchHelper.batchFetchDescendants(entity);
    }

	public void batchFetchDescendants(Collection<Entity> entites) {
		fetchHelper.batchFetchDescendants(entites);
	}

    /**
     *
     * @param toManyNode the node to fetch
     * @param override if we should ignore the node context state
     */
    public void fetch(ToManyNode toManyNode, boolean override) {
        fetchHelper.fetch(toManyNode, override, false);
    }

    public void fetchSingle(ToManyNode toManyNode, boolean override) {
        fetchHelper.fetch(toManyNode, override, true);
    }


    /**
     *
     * @param entity
     * @param force if true we fetch even we are in internal mode
     * @param fetchInternal if true we use the internal query registry
     */
    public void fetch(Entity entity, boolean force, boolean fetchInternal) {
        fetchHelper.fetchEntity(entity, force, fetchInternal, false, null);
    }

    void fetch(Entity entity, boolean force, boolean fetchInternal, boolean evenIfLoaded, String singlePropertyName) {
        fetchHelper.fetchEntity(entity, force, fetchInternal, evenIfLoaded, singlePropertyName);
    }

    /**
     * Gets entities by UUI or key from the entity context.<br/>
     * No fetching is performed.
     * @param uuid
     * @param mustExist
     * @return
     */
    public Entity getEntityByUuidOrKey(UUID uuid, EntityType entityType, Object key, boolean mustExist) {
        Entity e = getEntityByUuid(uuid, false);
        if (e != null) {
            return e;
        }
        return getEntity(entityType, key, mustExist);
    }

    /**
     * Gets entities by UUI from the entity context.<br/>
     * No fetching is performed.
     * @param uuid
     * @param mustExist
     * @return
     */
    public Entity getEntityByUuid(UUID uuid, boolean mustExist) {
        EntityInfo ei = entities.getByUuid(uuid, mustExist);
        return ei != null ? ei.getEntity(mustExist) : null;
    }

    public boolean isCompletelyEmpty() {
        return entities.isCompletelyEmpty();
    }

    /**
     * Gets an entity by it's type and id from the entity context.<br/>
     * No fetching is performed.
     *
     * @return the entity or null if it is not in the context
     * @throws IllegalStateException if mustExist but not found.
     */
    public Entity getEntity(EntityType entityType, Object key, boolean mustExist) {
        final EntityInfo ei = entities.getByKey(entityType, key);
        if (ei != null) {
            return ei.getEntity(mustExist);
        }
        if (mustExist) {
            throw new IllegalStateException("Could not find entity of type '" + entityType.getInterfaceName() + "' with key '" + key + "'");
        }
        return null;
    }

    public void addReference(RefNode refNode, Entity entity) {
        EntityInfo entityInfo = entities.getByUuid(entity.getUuid(), true);
        entityInfo.addAssociation(refNode);
    }

    public void removeReference(RefNode refNode, Entity entity) {
        EntityInfo entityInfo = entities.getByUuid(entity.getUuid(), true);
        entityInfo.removeAssociation(refNode);
    }

    public void refresh() {
        for (Entity entity : entities) {
            entity.refresh();
        }
    }

    /**
     * Builds a list of entities of a given type which refer to another entity of a given type and id
     *
     * For example all mappings which refer to syntax with key value '30'
     *
     * @param entityType the entity type to find
     * @param nodeName the name of the refnode (mapping's syntax property)
     * @param refType the type of ref node (the syntax type)
     * @param refKey the FK key value (mapping's syntax-id)
     * @return
     */
    public List<Entity> getEntitiesWithReferenceKey(final EntityType entityType, final String nodeName, final EntityType refType, final Object refKey) {
        final List<Entity> list = new LinkedList<Entity>();
        /*
         * lookup the EntityInfo for 'syntax'
         */
        EntityInfo entityInfo = entities.getByKey(refType, refKey);
        if (entityInfo == null) {
            throw new NullPointerException("No entity info for " + refType + " " + refKey);
        }
        /*
         * list all of the entities which refer to it
         */
        for (RefNode refNode : entityInfo.getFkReferences()) {
            final Entity refEntity = refNode.getParent();
            /*
             * filter on the entity which has the required type and refers on the required property
             */
            if (refEntity.isOfType(entityType) && refNode.getName().equals(nodeName)) {
                list.add(refEntity);
            }
        }
        return list;
    }

    public <T> Collection<T> getByType(Class<T> type) {
        EntityType et = definitions.getEntityTypeForClass(type, true);
        return new ProxyList<T>(this, entities.getEntitiesByType(et));
    }

    public Collection<Entity> getEntitiesByType(Class<?> type) {
        EntityType et = definitions.getEntityTypeForClass(type, true);
        return entities.getEntitiesByType(et);
    }

    public Collection<Entity> getEntitiesByType(EntityType et) {
        return entities.getEntitiesByType(et);
    }


    /**
     * The entity context does not write it's contents to the stream.
     *
     * entities are transmitted based on their relationships to each other.
     *
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        namespace = stream.readUTF();
        entityContextState = (EntityContextState)stream.readObject();
        statistics = (Statistics)stream.readObject();
        env = EnvironmentAccessor.get();
        Objects.requireNonNull(env, "Could not get environment");
        boolean allowGarbageCollection = stream.readBoolean();
        init(allowGarbageCollection);
        //the fetch helper on the serialized server side doesn't need any state
        //so we just create a new one
        fetchHelper = new FetchHelper(this);
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeUTF(namespace);
        stream.writeObject(entityContextState);
        stream.writeObject(statistics);
        stream.writeBoolean(entities.isAllowGarbageCollection());
    }

    public DependencyDiagram generateDependencyDiagram(Entity entity) throws IOException {
        DependencyDiagram diag = new DependencyDiagram();
        generateDiagram(diag, entity, new HashSet<Entity>());
        return diag;
    }

    private Link generateDiagram(DependencyDiagram diag, Entity entity, Set<Entity> processed) {
        if (!processed.add(entity)) {
            return null;
        }
        Link firstLink = null;
        /*
         * first add links for all refnodes
         */
        for (RefNode refNode: entity.getChildren(RefNode.class)) {
            Entity reffed = refNode.getReference(false);
            if (reffed != null) {
                Link l = diag.link(entity.toString(), reffed.toString(), genLinkName(refNode), LinkType.DEPENDENCY);
                if (firstLink == null) {
                    firstLink = l;
                }
            }
        }


        /*
         * first add links for all tomany
         */
        for (ToManyNode toManyNode: entity.getChildren(ToManyNode.class)) {
            if (!toManyNode.isFetched()) {
                continue;
            }
            for (Entity reffed: toManyNode.getList()) {
                Link l = diag.link(entity.toString(), reffed.toString(), genLinkName(toManyNode), LinkType.DEPENDENCY);
                if (firstLink == null) {
                    firstLink = l;
                }

            }
        }

        /*
         * recurse into the ref nodes
         */
        for (RefNode refNode: entity.getChildren(RefNode.class)) {
            Entity reffed = refNode.getReference(false);
            if (reffed != null) {
                generateDiagram(diag, reffed, processed);
            }
        }

        /*
         * recurse into the to many nodes
         */
        for (ToManyNode toManyNode: entity.getChildren(ToManyNode.class)) {
            if (!toManyNode.isFetched()) {
                continue;
            }
            for (Entity reffed: toManyNode.getList()) {
                generateDiagram(diag, reffed, processed);
            }
        }
        return firstLink;
    }

    private String genLinkName(ToManyNode toManyNode) {
        if (toManyNode.getNodeType().isOwns()) {
            return "owns " + toManyNode.getName();
        }
        else if (toManyNode.getNodeType().isDependsOn()) {
            return "depends on " + toManyNode.getName();
        }
        return toManyNode.getName();
    }

    private String genLinkName(RefNode refNode) {
        if (refNode.getNodeType().isOwns()) {
            return "owns " + refNode.getName();
        }
        else if (refNode.getNodeType().isDependsOn()) {
            return "depends on " + refNode.getName();
        }
        return refNode.getName();
    }

    public String printXml() {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element el = toXml(doc);
            doc.appendChild(el);

            final String transformXml = "<?xml version=\"1.0\"?>" +
                    "<xsl:stylesheet version=\"1.0\" " +
                    "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                    "<xsl:strip-space elements=\"*\" />" +
                    "<xsl:output method=\"xml\" indent=\"yes\" />" +
                    "" +
                    "<xsl:template match=\"node() | @*\">" +
                    "<xsl:copy>" +
                    "<xsl:apply-templates select=\"node() | @*\" />" +
                    "</xsl:copy>" +
                    "</xsl:template>" +
                    "</xsl:stylesheet>";

            Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(new ByteArrayInputStream(transformXml.getBytes("UTF-8"))));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return new String(out.toByteArray(), "UTF-8");
        } catch (Exception x) {
            throw new IllegalStateException("Could not print xml", x);
        }
    }

	public EntityPath getPathToBatchFetchRoot(Entity entity) {
		return fetchHelper.findShortestPath(entity);
	}
	
	public String debugQueryString(QueryObject<Object> query) {
	  return env.services().debugQueryString(query, namespace);
	}


}
