package com.smartstream.morf.api.core.entity.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.core.entity.Entity;

public final class Entities implements Iterable<Entity>, Serializable {
    private static final long serialVersionUID = 1L;
    /*
     * contains all entities
     */
    private Map<String, EntityInfo> entitiesByUuid = new HashMap<String, EntityInfo>();
    /*
     * only contains entities which have a PK
     */
    private Map<EntityPkKey, EntityInfo> entitiesByPk = new HashMap<EntityPkKey, EntityInfo>();

    private Map<EntityType,Set<Entity>> entitiesByType = new HashMap<EntityType, Set<Entity>>();

    public void add(Entity entity) {
        final String iname = entity.getEntityType().getInterfaceName();
        final String uuid = entity.getUuid();
        EntityInfo entityInfo = entitiesByUuid.get(uuid);
        if (entityInfo == null) {
            entityInfo = new EntityInfo(entity);
            entitiesByUuid.put(uuid, entityInfo);
            addEntityByType(entity);
        }
        final Object key = entity.getKey().getValue();
        if (key != null) {
            entitiesByPk.put(new EntityPkKey(iname, key), entityInfo);
        }
    }

    public void remove(Entity entity) {
        final String iname = entity.getEntityType().getInterfaceName();
        final String uuid = entity.getUuid();
        entitiesByUuid.remove(uuid);
        removeEntityByType(entity);
        final Object key = entity.getKey().getValue();
        if (key != null) {
            entitiesByPk.remove(new EntityPkKey(iname, key));
        }
    }

    public EntityInfo getByUuid(String uuid, boolean mustExist) {
        EntityInfo entityInfo = entitiesByUuid.get(uuid);
        if (entityInfo != null) {
            return entityInfo;
        }
        if (mustExist) {
            throw new IllegalStateException("Entity with uuid " + uuid + " must exist");
        }
        return null;
    }

    public EntityInfo getByKey(EntityType entityType, Object key) {
        final EntityPkKey pkKey = new EntityPkKey(entityType.getInterfaceName(), key);
        return entitiesByPk.get(pkKey);
    }

    public EntityInfo keyChanged(Entity entity, Object origKey) {
        final String iname = entity.getEntityType().getInterfaceName();
        final Object key = entity.getKey().getValue();
        EntityInfo entityInfo = entitiesByUuid.get(entity.getUuid());
        if (origKey == null && key != null) {
            entitiesByPk.put(new EntityPkKey(iname, key), entityInfo);
        }
        else if (origKey != null && key == null) {
            entitiesByPk.remove(new EntityPkKey(iname, origKey));
        }
        else {
            throw new IllegalStateException("Primary keys cannot be changed.");
        }
        return entityInfo;
    }

    public Collection<Entity> getEntitiesByType(EntityType entityType) {
        Collection<Entity> col = entitiesByType.get(entityType);
        return col != null ? col : Collections.<Entity>emptyList();
    }

    private void addEntityByType(Entity entity) {
        //LOG.debug("add entity by type " + entity.getEntityType() + " " + entity.getUuid() + " " + System.identityHashCode(entity));
        final EntityType et = entity.getEntityType();
        Set<Entity> set = entitiesByType.get(et);
        if (set == null) {
            entitiesByType.put(et, set = new LinkedHashSet<Entity>());
        }
        set.add(entity);
    }

    private void removeEntityByType(Entity entity) {
        Set<Entity> set = entitiesByType.get(entity.getEntityType());
        if (set != null) {
            set.remove(entity);
        }
    }

    public void clear() {
        entitiesByUuid.clear();
        entitiesByPk.clear();
        entitiesByType.clear();
    }

    @Override
    public Iterator<Entity> iterator() {
        return toEntityList().iterator();
    }

    public Collection<Entity> safe() {
        return new ArrayList<>(toEntityList());
    }

    private List<Entity> toEntityList() {
        List<Entity> list = new ArrayList<>(entitiesByUuid.size());
        for (EntityInfo ei: entitiesByUuid.values()) {
            list.add( ei.getEntity() );
        }
        return list;
    }

    public int size() {
        return entitiesByUuid.size();
    }


}
