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

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.RefNode;
import static scott.sort.api.core.entity.EntityContextHelper.toParents;

public final class EntityInfo implements Serializable {
    private static final long serialVersionUID = 1L;

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
    }

    public void removeAssociation(RefNode refNode) {
        fkReferences.remove(refNode);
    }

    public Set<RefNode> getFkReferences() {
        return fkReferences;
    }

    @Override
    public String toString() {
        return "EntityInfo [entity=" + entity + ", fkReferences=" + toParents(fkReferences) + "]";
    }

}
