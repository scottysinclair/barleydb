package com.smartstream.sort.api.core.entity.context;

import java.util.HashSet;
import java.util.Set;

import static com.smartstream.sort.api.core.entity.EntityContextHelper.toParents;

import com.smartstream.sort.api.core.entity.Entity;
import com.smartstream.sort.api.core.entity.RefNode;

public final class EntityInfo {
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
