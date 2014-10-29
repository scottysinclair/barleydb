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
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.EntityType;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import static scott.sort.api.core.entity.EntityContextHelper.toParents;

public final class EntityInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EntityInfo.class);

    private final EntityType entityType;

    private final WeakReference<Entity> entityRef;

    /*
     * FK references to this entityRef
     */
    private final Set<WeakReference<RefNode>> fkReferences = new HashSet<>();

    private String entityInfo;

    public EntityInfo(Entity entity) {
        this.entityRef = new WeakReference<Entity>(entity);
        this.entityType = entity.getEntityType();
        this.entityInfo = entity.toString();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * Gets the entityRef, can return null if the entityRef is garbage collected.
     * @return
     */
    public Entity getEntity(boolean mustExist) {
        Entity entity = entityRef.get();
        if (mustExist && entity == null) {
            throw new IllegalStateException("Entity " + entityInfo  + " has been garbage collected, but  must exist");
        }
        if (entity == null) {
            System.out.println("I WAS COLLECTED");
        }
        return entity;
    }

    public void addAssociation(RefNode refNode) {
        if (fkReferences.add(new WeakReference<>(refNode))) {
            LOG.trace("Added association from {} to {}", entityRef.get(), refNode.getParent());
        }
    }

    public void removeAssociation(RefNode refNode) {
        if (fkReferences.remove(refNode)) {
            LOG.trace("Removed association from {} to {}", entityRef.get(), refNode.getParent());
        }
    }

    public Set<RefNode> getFkReferences() {
        Set<RefNode> result = new HashSet<RefNode>();
        for (Iterator<WeakReference<RefNode>> i = fkReferences.iterator(); i.hasNext();) {
            RefNode refNode = i.next().get();
            if (refNode != null) {
                result.add( refNode );
            }
            else {
                i.remove();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "EntityInfo [entityRef=" + entityRef.get() + ", fkReferences=" + toParents(getFkReferences()) + "]";
    }

}
