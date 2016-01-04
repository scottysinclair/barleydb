package scott.sort.api.exception.execution.persist;

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
