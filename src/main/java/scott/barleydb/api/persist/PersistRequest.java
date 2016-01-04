package scott.barleydb.api.persist;

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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.exception.execution.persist.IllegalPersistStateException;
import scott.barleydb.api.exception.model.ProxyRequiredException;

public class PersistRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Entity> toInsert = new LinkedList<>();
    
    private final List<Entity> toUpdate = new LinkedList<>();
    
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

    public PersistRequest update(Object object) {
        toUpdate.add( verifyArg(object, "update") );
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

	public List<Entity> getToUpdate() {
        return toUpdate;
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
        else if (!toUpdate.isEmpty()) {
            return toUpdate.get(0).getEntityContext();
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
