package scott.sort.server.jdbc.persister.exception;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.config.EntityType;

public class PrimaryKeyExistsException extends PersistException {

    private static final long serialVersionUID = 1L;

    private final EntityType entityType;

    private final Object key;

    public PrimaryKeyExistsException(EntityType entityType, Object key) {
        super("Entity of type '" + entityType.getInterfaceShortName() + " with pk " + key + " already exists", null);
        this.entityType = entityType;
        this.key = key;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Object getKey() {
        return key;
    }

}