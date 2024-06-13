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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.query.QueryObject;

/**
 * Tracks information about an entity and also holds a weak reference to it.
 *
 * This class extends the WeakReference so that EntityInfo instances can be directly polled from the queue.
 *
 * @author scott
 *
 */
public final class EntityInfo extends WeakReference<Entity>  {

    private static final Logger LOG = LoggerFactory.getLogger(EntityInfo.class);

    private final EntityType entityType;

    /*
     * Tracking of FK references to the entity
     * The values in this HashMap should always be null, as it is used as a weak hash set.
     */
    private final WeakHashMap<RefNode,Object> fkReferences;

    private final UUID uuid;

    private Object primaryKey;

    private String primaryKeyName;
    
    /**
     * The query object responsible for loading the entity (if loaded from a query).
     */
    private QueryObject<?> fromQuery;
    
    public EntityInfo(Entity entity, ReferenceQueue<Entity> entityReferenceQueue, QueryObject<?> fromQuery) {
        super(entity, entityReferenceQueue);
        this.entityType = entity.getEntityType();
        this.fkReferences = new WeakHashMap<>();
        this.uuid = entity.getUuid();
        /*
         * PK may be null at this point, if so it gets set later
         */
        this.primaryKey = entity.getKeyValue();
        this.primaryKeyName  = entity.getKey().getName();
        this.fromQuery = fromQuery;
    }

    public Object getPrimaryKey() {
        return primaryKey;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setPrimaryKey(Object key) {
        this.primaryKey = key;
    }
    
    public QueryObject<?> getFromQuery() {
		return fromQuery;
	}

	public EntityType getEntityType() {
        return entityType;
    }

    public void clearCollectedRefs() {
        /*
         * The size method of the WeakHashMap expunges stale references.
         */
        fkReferences.size();
    }

    /**
     * Gets the entityRef, can return null if the entityRef is garbage collected.
     * @return
     */
    public Entity getEntity(boolean mustExist) {
        Entity entity = get();
        if (mustExist && entity == null) {
            throw new IllegalStateException("Entity " + this  + " has been garbage collected, but  must exist");
        }
        if (entity == null) {
            Entities.GC_LOG.info("Entity {} was collected but the stale refs were not yet removed.");
        }
        return entity;
    }

    public void addAssociation(RefNode refNode) {
        fkReferences.put(refNode, null);
        LOG.trace("Added association from {} to {}", get(), refNode.getParent());
    }

    public void removeAssociation(RefNode refNode) {
        fkReferences.remove(refNode);
        LOG.trace("Removed association from {} to {}", get(), refNode.getParent());
    }

    public Set<RefNode> getFkReferences() {
        return new HashSet<RefNode>(fkReferences.keySet());
    }

    @Override
    public String toString() {
        if (primaryKey != null) {
            return getEntityType().getInterfaceShortName() + " [" + primaryKeyName + "=" + primaryKey + "]";
        }
        else {
            return getEntityType().getInterfaceShortName() + " [uuid=" + getUuid().toString().substring(0, 7) + "..]";
        }
    }

	public void setFromQuery(QueryObject<?> query) {
		this.fromQuery = query;
	}

}

