package scott.sort.api.persist;

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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.exception.execution.persist.IllegalPersistStateException;
import scott.sort.api.exception.model.ProxyRequiredException;

public class PersistRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Entity> toInsert = new LinkedList<>();
    
    private final List<Entity> toSave = new LinkedList<>();

    private final List<Entity> toDelete = new LinkedList<>();

    public PersistRequest save(Object object) {
        toSave.add( verifyArg(object, "save") );
        return this;
    }

    public PersistRequest insert(Object object) {
        toInsert.add( verifyArg(object, "insert") );
        return this;
    }

    public PersistRequest delete(Object object) {
        toDelete.add( verifyArg(object, "delete") );
        return this;
    }

    public Collection<Entity> getToSave() {
        return toSave;
    }

    public List<Entity> getToInsert() {
		return toInsert;
	}

	public Collection<Entity> getToDelete() {
        return toDelete;
    }

    public boolean isEmpty() {
        return toSave.isEmpty() && toDelete.isEmpty();
    }

    public EntityContext getEntityContext() throws IllegalPersistStateException {
        if (!toSave.isEmpty()) {
            return toSave.get(0).getEntityContext();
        }
        else if (!toDelete.isEmpty()) {
            return toDelete.get(0).getEntityContext();
        }
        else if (!toInsert.isEmpty()) {
            return toInsert.get(0).getEntityContext();
        }
        throw new IllegalPersistStateException("PersistRequest has no objects to save or delete");
    }

    private Entity verifyArg(Object object, String operation) {
        if (object == null) {
            throw new NullPointerException("Cannot " + operation + "a null reference");
        }
        if (object instanceof Entity) {
            return (Entity)object;
        }
        if (!(object instanceof ProxyController)) {
            throw new ProxyRequiredException("Object to save must be a proxy type, " + object.getClass() + " is not allowed.");
        }
        return ((ProxyController)object).getEntity();
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
