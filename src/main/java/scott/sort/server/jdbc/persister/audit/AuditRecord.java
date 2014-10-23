package scott.sort.server.jdbc.persister.audit;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import scott.sort.api.config.EntityType;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.Node;
import scott.sort.api.core.entity.ValueNode;
import scott.sort.api.exception.execution.persist.IllegalPersistStateException;

public final class AuditRecord {
    private final EntityType entityType;
    private final Object entityKey;
    private final List<Change> changes = new LinkedList<>();
    private final Set<Node> nodesChanged;

    public AuditRecord(EntityType entityType, Object entityKey) {
        this.entityType = entityType;
        this.entityKey = entityKey;
        this.nodesChanged = new HashSet<>();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Object getEntityKey() {
        return entityKey;
    }

    public void addChange(Node node, Object oldValue, Object newValue) throws IllegalPersistStateException {
        if (nodesChanged.add(node)) {
            changes.add(new Change(node, oldValue, newValue));
        }
        else {
            throw new IllegalPersistStateException("Already consumed change for node " + node);
        }
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public Iterable<Change> changes() {
        return Collections.unmodifiableList(changes);
    }

    /**
     * Sets the optimistic lock change, assumes the node is not there yet
     * @param entity
     * @param newOptimisticLock
     */
    public void setOptimisticLock(Entity entity, Long newOptimisticLock) {
        ValueNode olNode = entity.getOptimisticLock();
        changes.add(new Change(olNode, olNode.getValue(), newOptimisticLock));
    }

    @Override
    public String toString() {
        return "AuditRecord [entityType=" + entityType + ", entityKey=" + entityKey
                + ", changes=" + changes + "]";
    }

}