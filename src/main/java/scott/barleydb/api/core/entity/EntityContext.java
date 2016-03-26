package scott.barleydb.api.core.entity;

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

import static scott.barleydb.api.core.entity.EntityContextHelper.toParents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.QueryRegistry;
import scott.barleydb.api.core.entity.context.Entities;
import scott.barleydb.api.core.entity.context.EntityInfo;
import scott.barleydb.api.core.proxy.ProxyList;
import scott.barleydb.api.core.util.EnvironmentAccessor;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.OptimisticLockMismatchException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.exception.model.ProxyCreationException;
import scott.barleydb.api.persist.PersistAnalyser;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.query.RuntimeProperties;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextState;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.entity.KeySetEvent;
import scott.barleydb.api.core.entity.NodeEvent;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;

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
public class EntityContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EntityContext.class);

    private String namespace;
    private Entities entities;
    private EntityContextState entityContextState;

    private Environment env;
    private QueryRegistry userQueryRegistry;
    private Definitions definitions;
    /**
     * weak hashmap used to allow garbage collection of the entity
     */
    private WeakHashMap<UUID, WeakReference<Object>> proxies;
    /**
     * A place to store extra resources like transaction information
     */
    private Map<String,Object> resources;

    public EntityContext(Environment env, String namespace) {
        this.env = env;
        this.namespace = namespace;
        init(true);
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

    public void unload(Object object, boolean includeOwnedEntities) {
        if (object instanceof ProxyController) {
            ProxyController pc = (ProxyController)object;
            pc.getEntity().unload( includeOwnedEntities );
        }
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
                    Comparable<Object> e1Key = e1.getKey().getValue();
                    Comparable<Object> e2Key = e2.getKey().getValue();
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
     * Handles events in the underlying entity model, like PKs being set.
     * @param event
     */
    public void handleEvent(NodeEvent event) {
        EntityContextState prev = entityContextState;
        try {
            entityContextState = EntityContextState.INTERNAL;
            if (event.getType() == NodeEvent.Type.KEYSET) {

                final Entity entity = (Entity) event.getSource().getParent();

                final EntityInfo entityInfo = entities.keyChanged(entity, ((KeySetEvent) event).getOriginalKey());
                if (entityInfo == null) {
                    throw new IllegalStateException("Could not find entity, to change the key: " + entity.getUuid());
                }

                /*
                 * update any matching refs
                 */
                for (RefNode refNode : entityInfo.getFkReferences()) {
                    refNode.handleEvent(event);
                }
            }
        } finally {
            entityContextState = prev;
        }
    }

    /**
     * Creates a new entity of the given type.
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T newModel(Class<T> type) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(type.getName(), true);
        Entity entity = new Entity(this, EntityState.NEW, entityType);
        add(entity);
        return (T) getProxy(entity);
    }

    /**
     * Creates a new entity of the given type. The model is either new, or in the database but not loaded.
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T newOrNotLoadedModel(Class<T> type) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(type.getName(), true);
        Entity entity = new Entity(this, EntityState.IS_PERHAPS_IN_DATABASE, entityType);
        add(entity);
        return (T) getProxy(entity);
    }

    /**
     * Create a new model (not existing in database, with the given key.
     * The entity type + key combination must be new for the entity context
     *
     * If the key is not autogenerated and known upfront then this is the preferred method  of creating
     * the new entity, to prevent duplicate entities with the same PK in the same context
     *
     * @param type
     * @param key
     * @throws IllegalStateException if an entity with the given key already exists
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T newModel(Class<T> type, Object key) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(type.getName(), true);
        Entity entity = new Entity(this, EntityState.NEW, entityType);
        entity.getKey().setValueNoEvent( key );
        add(entity);
        return (T) getProxy(entity);
    }

    /**
     * creates a model with a PK which may be new or may already be in the database.
     * @param type
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T newOrNotLoadedModel(Class<T> type, Object key) {
        EntityType entityType = definitions.getEntityTypeMatchingInterface(type.getName(), true);
        Entity entity = new Entity(this, EntityState.IS_PERHAPS_IN_DATABASE, entityType);
        entity.getKey().setValueNoEvent( key );
        add(entity);
        return (T) getProxy(entity);
    }

    /**
     * Removes all entities of the given type
     * todo: should support base types as well (syntax removes xml and csv syntax)
     * @param javaType
     */
    public void removeAll(Class<?> javaType) {
        final String typeName = javaType.getName();
        for (Entity entity : getEntitiesSafeIterable()) {
            if (entity.getEntityType().getInterfaceName().equals(typeName)) {
                remove(entity);
            }
        }
    }

    public void remove(Entity entity) {
        remove(entity, Collections.<Entity> emptyList());
    }

    public void remove(Object object) {
        if (object instanceof ProxyController) {
            ProxyController pc = (ProxyController)object;
            remove(pc.getEntity());
        }
    }

    public void remove(List<Entity> allEntitiesToBeRemoved) {
        for (Entity e : allEntitiesToBeRemoved) {
            remove(e, allEntitiesToBeRemoved);
        }
    }

    private void remove(Entity entity, List<Entity> allEntitiesToBeRemoved) {
        EntityInfo entityInfo = entities.getByUuid(entity.getUuid(), true);
        Collection<RefNode> refNodes = entityInfo.getFkReferences();
        if (!refNodes.isEmpty() && !allEntitiesToBeRemoved.containsAll(toParents(refNodes))) {
            entity.unload(false);
            LOG.debug("Unloaded entity " + entity + " " + entity.getUuid());
        }
        else {
            for (RefNode refNode: entity.getChildren(RefNode.class)) {
                if (refNode.getReference() != null) {
                    removeReference(refNode, refNode.getReference());
                }
            }
            entities.remove(entity);
            proxies.remove(entity.getUuid());
            LOG.debug("Removed from entityContext " + entity + " " + entity.getUuid());
        }
    }

    /**
     * gets a proxy for the given entity, creating it if required.
     * @param entity
     * @return
     */
    public Object getProxy(Entity entity) {
        WeakReference<Object> p = proxies.get(entity.getUuid());
        if (p == null || p.get() == null ) {
            try {
                Object o = env.generateProxy(entity);
                proxies.put(entity.getUuid(), new WeakReference<>(o));
                return o;
            } catch (Exception x) {
                throw new IllegalStateException("Could not generated proxy", x);
            }
        }
        return p.get();
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

    public void beginLoading() {
        this.entityContextState = EntityContextState.INTERNAL;
    }

    public void endLoading() {
        setAllLoadingEntitiesToLoaded();
        this.entityContextState = EntityContextState.USER;
    }

    public void beginSaving() {
        this.entityContextState = EntityContextState.INTERNAL;
    }

    public void beginDeleting() {
        this.entityContextState = EntityContextState.INTERNAL;
    }

    public void endSaving() {
        this.entityContextState = EntityContextState.USER;
    }

    public void endDeleting() {
        this.entityContextState = EntityContextState.USER;
    }

    public <T> void performQueries(QueryBatcher queryBatcher) throws SortServiceProviderException, SortQueryException {
        performQueries(queryBatcher, null);
    }
    public <T> void performQueries(QueryBatcher queryBatcher, RuntimeProperties runtimeProperties) throws SortServiceProviderException, SortQueryException {
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

    public <T> QueryResult<T> performQuery(QueryObject<T> queryObject) throws SortServiceProviderException, SortQueryException {
        return performQuery(queryObject, null);
    }
    public <T> QueryResult<T> performQuery(QueryObject<T> queryObject, RuntimeProperties runtimeProperties) throws SortServiceProviderException, SortQueryException {
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

    public void persist(PersistRequest persistRequest) throws SortServiceProviderException, SortPersistException  {
        persist(persistRequest, null);
    }
    public void persist(PersistRequest persistRequest, RuntimeProperties runtimeProperties) throws SortServiceProviderException, SortPersistException  {
        beginSaving();
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
            endSaving();
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
        return getEntity(entity.getEntityType(), entity.getKey().getValue(), false) != null;
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

    public void add(Entity entity) {
        if (getEntityByUuid(entity.getUuid(), false) != null) {
            throw new IllegalStateException("Entity with uuid '" + entity.getUuid() + "' already exists.");
        }
        if (entity.getKey().getValue() != null && getEntity(entity.getEntityType(), entity.getKey().getValue(), false) != null) {
            throw new IllegalStateException("Entity with key '" + entity.getKey().getValue() + "' already exists.");
        }
        entities.add(entity);
        LOG.debug("Added to entityContext " + entity);
    }

    /**
     * Copies a single entity into our context
     * If the entity already exists, then the values and the fk refs are updated accordingly
     * @param entity
     *            return the new entity
     */
    public Entity copyInto(Entity entity) {
        beginLoading();
        entity.getEntityContext().beginLoading();
        try {
            if (entity.getEntityContext() == this) {
                throw new IllegalStateException("Cannot copy entity into same context");
            }
            Entity ours = null;
            if (entity.getUuid() != null) {
                ours = getEntityByUuid(entity.getUuid(), false);
            }
            if (ours == null && entity.getKey().getValue() != null) {
                ours = getEntity(entity.getEntityType(), entity.getKey().getValue(), false);
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
                ours.getChild(refNode.getName(), RefNode.class).setEntityKey(refNode.getEntityKey());
            }
            return ours;
        } finally {
            entity.getEntityContext().endLoading();
            endLoading();
        }
    }

    /**
     * Gets the entity if it exists or creates a non-loaded entity
     * in the context.<br/>
     *
     * This means that the caller is expecting the entity to exist in the datasase already.
     *
     * @return the entity
     */
    public Entity getOrCreate(EntityType entityType, Object key) {
        Entity entity = getEntity(entityType, key, false);
        if (entity == null) {
            entity = new Entity(this, entityType, key);
            entity.setEntityState(EntityState.NOTLOADED);
            add(entity);
        }
        return entity;
    }

    /**
     * Gets an entity from the context, or creates a new one with state NEW
     * @param class1
     * @param key
     * @return
     * @throws ProxyCreationException
     */
    public <T> T getOrCreateAsNew(Class<T> class1, Object key) throws ProxyCreationException {
        EntityType entityType = getDefinitions().getEntityTypeForClass(class1, true);
        Entity entity = getEntity(entityType, key, false);
        if (entity == null) {
            entity = new Entity(this, entityType, key);
            entity.setEntityState(EntityState.NEW);
            add(entity);
        }
        return env.generateProxy(entity);
    }


    /**
     * creates a new entity context which shares the same transaction
     * as the current entity context.
     * @return
     */
    public EntityContext newEntityContext() {
        EntityContext entityContext = new EntityContext(env, namespace);
        return entityContext;
    }

    /**
     * creates a new entity context which shares the same transaction
     * as the current entity context.
     * @return
     */
    public EntityContext newEntityContextSharingTransaction() {
        EntityContext entityContext = newEntityContext();
        env.joinTransaction(entityContext, this);
        return entityContext;
    }

    /**
     * gets the entity from the context or lazily loads it into
     * the context if it does not exist.
     * @param type
     * @param key
     * @param ensureExistsInDb actually query the database, to confirm it exists.
     * @return
     * @throws ProxyCreationException
     */
    public <T> T getOrLazilyLoad(Class<T> type, String key, boolean ensureExistsInDb) throws ProxyCreationException {
        if (ensureExistsInDb) {
            T object = getOrLoad(type, key);
            Entity e = ((ProxyController)object).getEntity();
            e.unload(false);
            return object;
        }
        else {
            T object = newModel(type, key);
            Entity e = ((ProxyController)object).getEntity();
            e.setEntityState(EntityState.NOTLOADED);
            return object;
        }
    }


    /**
     * gets the entity from the context loading it first if required.
     * @param type
     * @param key
     * @return
     * @throws ProxyCreationException
     */
    public <T> T getOrLoad(Class<T> type, Object key) throws ProxyCreationException {
      EntityType entityType = definitions.getEntityTypeForClass(type, true);
      Entity e = getOrLoad(entityType, key);
      return e != null ? (T)env.generateProxy(e) : null;
    }

    /**
     * Returns the entity from the context, loading it first if required.
     * @param entityType
     * @param key
     * @return the entity of null if it does not exist in the context or database
     */
    public Entity getOrLoad(EntityType entityType, Object key) {
        Entity entity = getEntity(entityType, key, false);
        if (entity != null) {
            return entity;
        }
        EntityContext tmp = newEntityContextSharingTransaction();
        entity = new Entity(tmp, entityType, key);
        tmp.add(entity);
        tmp.fetchSingle(entity, true);
        if (entity.isLoaded()) {
            return copyInto(entity);
        }
        return null;
    }

    public <T> QueryObject<T> getQuery(Class<T> clazz) {
        return userQueryRegistry.getQuery(clazz);
    }

    /**
     * Gets the standard unit query with no joins
     * @param clazz
     * @return
     */
    public QueryObject<Object> getUnitQuery(EntityType entityType) {
        return definitions.getQuery(entityType);
    }

    private QueryObject<Object> getQuery(EntityType entityType, boolean fetchInternal) {
        return fetchInternal ? definitions.getQuery(entityType) : userQueryRegistry.getQuery(entityType);
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

    /**
     *
     * @param toManyNode the node to fetch
     * @param override if we should ignore the node context state
     */
    public void fetch(ToManyNode toManyNode, boolean override) {
        fetch(toManyNode, override, false);
    }

    public void fetchSingle(ToManyNode toManyNode, boolean override) {
        fetch(toManyNode, override, true);
    }

    /**
     *
     * @param toManyNode
     * @param override if true we fetch even if in internal mode.
     * @param fetchInternal
     */
    private void fetch(ToManyNode toManyNode, boolean override, boolean fetchInternal) {
        if (!override && toManyNode.getEntityContext().isInternal()) {
            return;
        }

        final NodeType toManyDef = toManyNode.getNodeType();

        //get the name of the node/property which we need to filter on to get the correct entities back on the many side
        final String foreignNodeName = toManyDef.getForeignNodeName();
        if (foreignNodeName != null) {
            final QueryObject<Object> qo = getQuery(toManyNode.getEntityType(), fetchInternal);
            if (qo == null) {
                throw new IllegalStateException("No query registered for '" + toManyNode.getEntityType().getInterfaceName() + "'");
            }

            final QProperty<Object> manyFk = new QProperty<Object>(qo, foreignNodeName);
            final Object primaryKeyOfOneSide = toManyNode.getParent().getKey().getValue();
            qo.where(manyFk.equal(primaryKeyOfOneSide));

            /*
             * If a user call is causing a fetch to a "join entity"
             * then join from the join entity to the referenced entity.
             *
             * So if the ToManyNode joins from Template to TemplateDatatype and has joinProperty "datatype"
             * then we will make the QTemplateBusinessType outer join to QBusinessType so that QBusinessType is automatically pulled in
             *
             * Template.datatypes == the ToManyNode
             * TemplateDatatype.datatype == the RefNode on the "join entity"
             *
             * the if below uses these nouns for better clarification
             *
             */
            if (!fetchInternal && toManyDef.getJoinProperty() != null) {
                NodeType datatypeNodeType = toManyNode.getEntityType().getNodeType(toManyDef.getJoinProperty(), true);
                EntityType datatype = definitions.getEntityTypeMatchingInterface(datatypeNodeType.getRelationInterfaceName(), true);
                QueryObject<Object> qdatatype = getQuery(datatype, fetchInternal);
                //TODO: if the tomany relation has a non-key sort column, then make sure that column is also fetched
                qo.addLeftOuterJoin(qdatatype, datatypeNodeType.getName());
            }

            try {
                performQuery(qo);
                toManyNode.setFetched(true);
                toManyNode.refresh();
            } catch (Exception x) {
                throw new IllegalStateException("Error performing fetch", x);
            }
        } else {
            /*
             * get the query for loading the entity which has the relation we want to fetch
             * ie the template query for when we want to fetch the datatypes.
             */
            final QueryObject<Object> fromQo = definitions.getQuery(toManyNode.getParent().getEntityType());
            /*
             * gets the query for loading the entity which the relation fulfills
             * ie the datatype query which the template wants to load
             */
            final QueryObject<Object> toQo = definitions.getQuery(toManyNode.getEntityType());
            /*
             * add the join from template to query
             * the query generator knows to include the join table
             */
            fromQo.addLeftOuterJoin(toQo, toManyNode.getName());
            /*
             * constrain from query to only return data for the entity we are fetching for
             * ie contrain the template query by the id of the template we are fetching for
             */
            final QProperty<Object> fromPk = new QProperty<Object>(fromQo, toManyNode.getParent().getKey().getName());
            fromQo.where(fromPk.equal(toManyNode.getParent().getKey().getValue()));

            try {
                performQuery(fromQo);
                toManyNode.setFetched(true);
                toManyNode.refresh();
            } catch (Exception x) {
                throw new IllegalStateException("Error performing fetch", x);
            }
        }
    }

    /**
     *
     * @param entity
     * @param force if true we fetch even we are in internal mode
     * @param fetchInternal if true we use the internal query registry
     */
    public void fetch(Entity entity, boolean force, boolean fetchInternal) {
        fetch(entity, force, fetchInternal, false, null);
    }

    public void fetch(Entity entity, boolean force, boolean fetchInternal, boolean evenIfLoaded, String singlePropertyName) {
        if (!force && entity.getEntityContext().isInternal()) {
            return;
        }
        if (!evenIfLoaded && entity.getEntityState() == EntityState.LOADED) {
            return;
        }
        final QueryObject<Object> qo = getQuery(entity.getEntityType(), fetchInternal);
        if (qo == null) {
            throw new IllegalStateException("No query registered for loading " + entity.getEntityType());
        }

        final QProperty<Object> pk = new QProperty<Object>(qo, entity.getEntityType().getKeyNodeName());
        qo.where(pk.equal(entity.getKey().getValue()));

        try {
            if (singlePropertyName != null) {
                QProperty<?> property =  qo.getMandatoryQProperty( singlePropertyName );
                qo.select(property);
            }
            QueryResult<Object> result = performQuery(qo);
            /*
             * Some cleanup required.
             * the executer will set all loading entities to LOADED
             * but we set the state to LOADING ourselves
             * so check the result, and manually set the entity state
             */
            if (!result.getList().isEmpty()) {
                entity.setEntityState(EntityState.LOADED);
            }
            else {
                entity.setEntityState(EntityState.NOTLOADED);
            }
        } catch (Exception x) {
            throw new IllegalStateException("Error performing fetch", x);
        }
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

    private void setAllLoadingEntitiesToLoaded() {
        for (Entity entity : entities) {
            if (entity.getEntityState() == EntityState.LOADING) {
                entity.setEntityState(EntityState.LOADED);
            }
        }
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
        env = EnvironmentAccessor.get();
        Objects.requireNonNull(env, "Could not get environment");
        boolean allowGarbageCollection = stream.readBoolean();
        init(allowGarbageCollection);
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeUTF(namespace);
        stream.writeObject(entityContextState);
        stream.writeBoolean(entities.isAllowGarbageCollection());
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

}
