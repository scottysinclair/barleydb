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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.EntityType;
import scott.sort.api.core.entity.Entity;

public class EntityMissingException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(Entity.class);

    private EntityType entityType;

    private Object key;

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

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing EntityMissingException {}", this);
        entityType.write(oos);
        oos.writeObject(key);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        entityType = EntityType.read(ois);
        key = ois.readObject();
        LOG.trace("Deserialized EntityMissingException {}", this);
    }

}
