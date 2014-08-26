package com.smartstream.morf.server.jdbc.persister.exception;

import com.smartstream.morf.api.core.entity.Entity;

public class OptimisticLockMismatchException extends PersistException {

    private static final long serialVersionUID = 1L;

    private final Entity entity;

    private final Entity databaseEntity;

    public OptimisticLockMismatchException(Entity entity, Entity databaseEntity) {
        super("Optimistic locks don't match for entity '" + entity + "' and database entity '" + databaseEntity + "'", null);
        this.entity = entity;
        this.databaseEntity = databaseEntity;
    }

	public Entity getEntity() {
		return entity;
	}

	public Entity getDatabaseEntity() {
		return databaseEntity;
	}

}
