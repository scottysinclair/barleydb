package scott.sort.api.core.entity.context;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.EntityType;
import scott.sort.api.core.entity.Entity;

public final class Entities implements Iterable<Entity>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(Entities.class);

    private volatile boolean allowGarbageCollection;

    private Collection<Entity> collectionPreventingGarbageCollection;

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
        this.entityInfos = new WeakHashMap<>();
        this.entityByUuid = new HashMap<>();
        this.entityByPk = new HashMap<>();
        this.entitiesByType = new HashMap<>();
    }

    public boolean isAllowGarbageCollection() {
        return allowGarbageCollection;
    }

    public void add(Entity entity) {
        EntityInfo entityInfo = entityInfos.get(entity);
        if (entityInfo == null) {
            entityInfo = new EntityInfo(entity);
            entityInfos.put(entity, entityInfo);
            addEntityByType(entityInfo);
            entityByUuid.put(entity.getUuid(), entityInfo);
            if (entity.getKey().getValue() != null) {
                entityByPk.put(new EntityPkKey(entity), entityInfo);
            }
            if (!allowGarbageCollection) {
                collectionPreventingGarbageCollection.add(entity);
            }
        }
    }

    public void remove(Entity entity) {
        final Object pk = entity.getKey().getValue();
        if (pk != null) {
            entityByPk.remove( pk );
        }
        entityByUuid.remove( entity.getUuid() );
        EntityInfo entityInfo = entityInfos.remove(entity);
        removeEntityByType(entityInfo);
        if (!collectionPreventingGarbageCollection.isEmpty()) {
            collectionPreventingGarbageCollection.remove(entity);
        }
    }

    public EntityInfo getByUuid(UUID uuid, boolean mustExist) {
        EntityInfo entityInfo = entityByUuid.get(uuid);
        if (entityInfo != null) {
            return entityInfo;
        }
        if (mustExist) {
            throw new IllegalStateException("Entity with uuid " + uuid + " must exist");
        }
        System.out.println("ENTITY INFO NOT IN UUID MAP");
        return null;
    }

    public EntityInfo getByKey(EntityType entityType, Object key) {
        final EntityPkKey pkKey = new EntityPkKey(entityType.getInterfaceName(), key);
        return entityByPk.get(pkKey);
    }

    public EntityInfo keyChanged(Entity entity, Object origKey) {
        LOG.trace("Key changed from {} for entity {}", origKey, entity);
        final String iname = entity.getEntityType().getInterfaceName();
        final Object key = entity.getKey().getValue();
        EntityInfo entityInfo = entityByUuid.get(entity.getUuid());
        if (origKey == null && key != null) {
            entityByPk.put(new EntityPkKey(iname, key), entityInfo);
        }
        else if (origKey != null && key == null) {
            entityByPk.remove(new EntityPkKey(iname, origKey));
        }
        else {
            throw new IllegalStateException("Primary keys cannot be changed.");
        }
        return entityInfo;
    }

    public Collection<Entity> getEntitiesByType(EntityType entityType) {
        Collection<EntityInfo> infos = entitiesByType.get(entityType);
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

    public int size() {
        return entityInfos.size();
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

    private void removeEntityByType(EntityInfo entityInfo) {
        Set<EntityInfo> set = entitiesByType.get(entityInfo.getEntityType());
        if (set != null) {
            set.remove(entityInfo);
        }
    }


}
