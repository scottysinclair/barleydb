package scott.sort.server.jdbc.persister;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.*;

import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.exception.execution.persist.IllegalPersistStateException;

public class PersistRequest {

    private final List<Object> toSave = new LinkedList<>();

    private final List<Object> toDelete = new LinkedList<>();

    public PersistRequest save(Object object) {
        toSave.add(object);
        return this;
    }

    public PersistRequest delete(Object object) {
        toDelete.add(object);
        return this;
    }

    public Collection<Object> getToSave() {
        return toSave;
    }

    public Collection<Object> getToDelete() {
        return toDelete;
    }

    public boolean isEmpty() {
        return toSave.isEmpty() && toDelete.isEmpty();
    }

    public EntityContext getEntityContext() throws IllegalPersistStateException {
        if (!toSave.isEmpty()) {
            return getEntityContext(toSave.get(0));
        }
        else if (!toDelete.isEmpty()) {
            return getEntityContext(toDelete.get(0));
        }
        throw new IllegalPersistStateException("PersistRequest has no objects to save or delete");
    }

    private EntityContext getEntityContext(Object object) {
        return ((ProxyController) object).getEntity().getEntityContext();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PersistRequest: [");
        sb.append("toSave: ");
        sb.append(toSave);
        sb.append(" toDelete: ");
        sb.append(toDelete);
        sb.append("]");
        return sb.toString();
    }

}
