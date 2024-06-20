package scott.barleydb.api.core.entity.context;

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

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.query.QueryObject;

public final class Entities implements Iterable<Entity>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(Entities.class);
    public static final Logger GC_LOG = LoggerFactory.getLogger("gc." + Entities.class.getName());

    private volatile boolean allowGarbageCollection;

    private Collection<Entity> collectionPreventingGarbageCollection;

    /**
     * Tracks when references get collected.
     */
    private final ReferenceQueue<Entity> entityReferenceQueue;

    /**
     * A WeakHashMap linking the entity to the entity info.
     *
     * The WeakHashMap does not prevent the entity key from being collected.
     *
     * EntityInfo has a WeakReference to the entity and also does not prevent the
     * entity from being collected.
     */
    private WeakHashMap<Entity, EntityInfo> entityInfos;

    private Map<UUID, EntityInfo> entityByUuid;
    private Map<EntityPkKey,EntityInfo> entityByPk;
    private Map<EntityType,Set<EntityInfo>> entitiesByType;

    public Entities(boolean allowGarbageCollection) {
        this.allowGarbageCollection = allowGarbageCollection;
        this.collectionPreventingGarbageCollection = new HashSet<Entity>();
        this.entityReferenceQueue = new ReferenceQueue<>();
        this.entityInfos = new WeakHashMap<>();
        this.entityByUuid = new HashMap<>();
        this.entityByPk = new HashMap<>();
        this.entitiesByType = new HashMap<>();
    }

    public boolean isAllowGarbageCollection() {
        return allowGarbageCollection;
    }

    public void add(Entity entity, QueryObject<?> optionalQuery) {
        pollForCollectedEntities();
        EntityInfo entityInfo = entityInfos.get(entity);
        if (entityInfo == null) {
            entityInfo = new EntityInfo(entity, entityReferenceQueue, optionalQuery);
            entityInfos.put(entity, entityInfo);
            addEntityByType(entityInfo);
            entityByUuid.put(entity.getUuid(), entityInfo);
            if (entity.getKeyValue() != null) {
                entityByPk.put(new EntityPkKey(entity), entityInfo);
            }
            if (!allowGarbageCollection) {
                collectionPreventingGarbageCollection.add(entity);
            }
        }
    }

    public void remove(Entity entity) {
        pollForCollectedEntities();
        final Object pk = entity.getKeyValue();
        if (pk != null) {
            entityByPk.remove( new EntityPkKey(entity) );
        }
        entityByUuid.remove( entity.getUuid() );
        EntityInfo entityInfo = entityInfos.remove(entity);
        removeEntityByType(entityInfo);
        if (!collectionPreventingGarbageCollection.isEmpty()) {
            collectionPreventingGarbageCollection.remove(entity);
        }
    }

    public EntityInfo getByUuid(UUID uuid, boolean mustExist) {
        pollForCollectedEntities();
        EntityInfo entityInfo = entityByUuid.get(uuid);
        if (entityInfo != null) {
            return entityInfo;
        }
        if (mustExist) {
            throw new IllegalStateException("Entity with uuid " + uuid + " must exist");
        }
        return null;
    }

    public EntityInfo getByKey(EntityType entityType, Object key) {
        pollForCollectedEntities();
        final EntityPkKey pkKey = new EntityPkKey(entityType.getInterfaceName(), key);
        return entityByPk.get(pkKey);
    }

    public EntityInfo keyChanged(Entity entity, Object origKey) {
        LOG.trace("Key changed from {} for entity {}", origKey, entity);
        final String iname = entity.getEntityType().getInterfaceName();
        final Object key = entity.getKeyValue();
        EntityInfo entityInfo = entityByUuid.get(entity.getUuid());
        if (origKey == null && key != null) {
            entityByPk.put(new EntityPkKey(iname, key), entityInfo);
            entityInfo.setPrimaryKey(entity.getKeyValue());
        }
        else if (origKey != null && key == null) {
            entityByPk.remove(new EntityPkKey(iname, origKey));
            entityInfo.setPrimaryKey(null);
        }
        else {
            throw new IllegalStateException("Primary keys cannot be changed.");
        }
        return entityInfo;
    }

    public Collection<Entity> getEntitiesByType(EntityType entityType) {
        Collection<EntityInfo> infos = entitiesByType.get(entityType);
        if (infos == null) {
            return Collections.emptyList();
        }
        Collection<Entity> result = new ArrayList<>(infos.size());
        for (EntityInfo entityInfo: infos) {
            Entity entity = entityInfo.getEntity(false);
            if (entity != null) {
                result.add( entity );
            }
        }
        return result;
    }

    public void setAllowGarbageCollection(boolean allow) {
        if (allow) {
            this.allowGarbageCollection = allow;
            collectionPreventingGarbageCollection.clear();
        }
        else {
            this.allowGarbageCollection = allow;
            for (Entity entity: safe()) {
                collectionPreventingGarbageCollection.add(entity);
            }
        }
    }


    @Override
    public Iterator<Entity> iterator() {
        //wrap in a list so there is no garbage collection during the course
        //of the iteration.
        return new ArrayList<>(entityInfos.keySet()).iterator();
    }

    public Collection<Entity> safe() {
        return new ArrayList<>(entityInfos.keySet());
    }

    public void clear() {
        entityInfos.clear();
        entityByPk.clear();
        entityByUuid.clear();
        entitiesByType.clear();
        collectionPreventingGarbageCollection.clear();
    }

    public boolean isCompletelyEmpty() {
        if (!entityInfos.isEmpty()) {
            return false;
        }
        if (!entityByPk.isEmpty()) {
            return false;
        }
        if (!entityByUuid.isEmpty()) {
            return false;
        }
        if (!entitiesByType.isEmpty()) {
            return false;
        }
        if (!collectionPreventingGarbageCollection.isEmpty()) {
            return false;
        }
        return true;
    }

    public int size() {
        pollForCollectedEntities();
        return entityInfos.size();
    }

    private void pollForCollectedEntities() {
        EntityInfo entityInfo = null;
        while((entityInfo = (EntityInfo)entityReferenceQueue.poll()) != null) {
            if (entityInfo.getPrimaryKey() != null) {
                if (entityByPk.remove(new EntityPkKey(entityInfo)) == null) {
                    GC_LOG.debug("Failed to remove EntityInfo from primary key lookup for {}", entityInfo);
                }
            }
            if (entityByUuid.remove(entityInfo.getUuid()) == null) {
                GC_LOG.debug("Failed to remove EntityInfo from UUID lookup for {}", entityInfo);
            }
            if (!removeEntityByType(entityInfo)) {
                GC_LOG.debug("Failed to remove EntityInfo from set of entities by type {}", entityInfo);
            }
            //calling size on WeakHashMap will force any stale references to be cleared.
            //this is required for our EnityInfo which is stored as a map value.
            entityInfos.size();
            GC_LOG.debug("Removed entity info " + entityInfo + " for garbage collected entity");
        }
    }

    private void addEntityByType(EntityInfo entityInfo) {
        //LOG.debug("add entity by type " + entity.getEntityType() + " " + entity.getUuid() + " " + System.identityHashCode(entity));
        Entity entity = entityInfo.getEntity(false);
        /*
         * Check if garbage collected
         */
        if (entity == null) {
            return;
        }
        final EntityType entityType = entity.getEntityType();
        Set<EntityInfo> infos = entitiesByType.get(entityType);
        if (infos == null) {
            entitiesByType.put(entityType, infos = new HashSet<>());
        }
        infos.add(entityInfo);
    }

    /**
     *
     * @param entityInfo
     * @return true if the entity was removed,.
     */
    private boolean removeEntityByType(EntityInfo entityInfo) {
        Set<EntityInfo> set = entitiesByType.get(entityInfo.getEntityType());
        if (set != null) {
            boolean modified = set.remove(entityInfo);
            /*
             * We also remove the set if it is empty, allows entitiesByType.isEmpty() to
             * be more accurate.
             */
            if (set.isEmpty()) {
                entitiesByType.remove(entityInfo.getEntityType());
            }
            return modified;
        }
        return false;
    }

	public QueryObject<?> getAssociatedQuery(Entity entity) {
		EntityInfo ei = entityInfos.get(entity);
		return ei != null ? ei.getFromQuery() : null;
	}

	public void setAssociatedQuery(Entity entity, QueryObject<?> query) {
		EntityInfo ei = entityInfos.get(entity);
		if (ei != null) {
			ei.setFromQuery(query);
		}
		
	}


}
