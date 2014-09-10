package com.smartstream.morf.server.jdbc.persister.exception;

import com.smartstream.morf.api.config.*;

public class EntityMissingException extends PersistException {

    private static final long serialVersionUID = 1L;

    private final EntityType entityType;

    private final Object key;

    public EntityMissingException(EntityType entityType, Object key) {
        super("Entity of type '" + entityType.getInterfaceShortName() + "' with key '" + key + "' is missing from the database", null);
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
