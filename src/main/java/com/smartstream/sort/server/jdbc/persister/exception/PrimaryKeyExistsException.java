package com.smartstream.sort.server.jdbc.persister.exception;

import com.smartstream.sort.api.config.EntityType;

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
