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
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import static scott.sort.api.core.entity.EntityContextHelper.toParents;

public final class EntityInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EntityInfo.class);

    private final Entity entity;

    /*
     * FK references to this entity
     */
    private final Set<RefNode> fkReferences = new HashSet<>();

    public EntityInfo(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public void addAssociation(RefNode refNode) {
        fkReferences.add(refNode);
        LOG.trace("Added association from {} to {}", entity, refNode.getParent());
    }

    public void removeAssociation(RefNode refNode) {
        fkReferences.remove(refNode);
        LOG.trace("Removed association from {} to {}", entity, refNode.getParent());
    }

    public Set<RefNode> getFkReferences() {
        return fkReferences;
    }

    @Override
    public String toString() {
        return "EntityInfo [entity=" + entity + ", fkReferences=" + toParents(fkReferences) + "]";
    }

}
