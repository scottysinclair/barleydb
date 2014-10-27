package scott.sort.api.exception;

import java.util.UUID;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public class SortException extends Exception {

    private static final long serialVersionUID = 1L;

    public SortException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public SortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortException(String message) {
        super(message);
    }

    public SortException(Throwable cause) {
        super(cause);
    }

    /**
     * Gets the corresponding entity in the given entity context.
     * @param entityContext
     * @param originalEntity
     * @return
     */
    protected Entity getCorrespondingEntity(EntityContext entityContext, Entity originalEntity) {
        UUID uuid = originalEntity.getUuid();
        Object key = originalEntity.getKey().getValue();
        Entity replacement = entityContext.getEntityByUuidOrKey(uuid, originalEntity.getEntityType(), key, false);
        return replacement != null ? replacement : originalEntity;
    }
}
