package scott.sort.api.exception.execution.persist;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;

public class OptimisticLockMismatchException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    private final Entity entity;

    private final Entity databaseEntity;

    public OptimisticLockMismatchException(Entity entity, Entity databaseEntity) {
        super("Optimistic locks don't match for entity '" + entity + "' and database entity '" + databaseEntity + "'", null);
        this.entity = entity;
        this.databaseEntity = databaseEntity;
    }

    /**
     * Tries to switch the entity to one in the new context and then throw a new exception.<br/>
     * Otherwise throws this exception.
     *
     * @param entityContext
     * @throws OptimisticLockMismatchException
     */
    public void switchEntitiesAndThrow(EntityContext entityContext) throws OptimisticLockMismatchException {
        Entity switched = getCorrespondingEntity(entityContext, entity);
        if (switched == null) {
            throw this;
        }
        else {
            OptimisticLockMismatchException x = new OptimisticLockMismatchException(switched, databaseEntity);
            x.setStackTrace(getStackTrace());
            throw x;
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public Entity getDatabaseEntity() {
        return databaseEntity;
    }

}
