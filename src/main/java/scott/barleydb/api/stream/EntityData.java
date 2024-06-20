package scott.barleydb.api.stream;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
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
import java.util.LinkedHashMap;
import java.util.UUID;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.entity.context.EntityId;

public class EntityData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String namespace;
    private String entityType;
    /*
     * the Value Nodes and RefNodes
     */
    private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
    private EntityConstraint constraints;
    private EntityState entityState;
    private UUID uuid;
    
    public String getNamespace() {
        return namespace;
    }
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    public String getEntityType() {
        return entityType;
    }
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    public LinkedHashMap<String, Object> getData() {
        return data;
    }
    public EntityConstraint getConstraints() {
        return constraints;
    }
    public void setConstraints(EntityConstraint constraints) {
        this.constraints = constraints;
    }
    public EntityState getEntityState() {
        return entityState;
    }
    public void setEntityState(EntityState entityState) {
        this.entityState = entityState;
    }
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public EntityType getEntityType(EntityContext ctx, Boolean mustExist) {
        return ctx.getDefinitions().getEntityTypeMatchingInterface(entityType, mustExist);
    }

    public Object getKey(EntityType entityType) {
        return data.get( entityType.getKeyNodeName() );
    }

    public EntityId getEntityId(EntityContext ctx) {
        EntityType entityType = getEntityType(ctx, true);
        return new EntityId(entityType, getUuid());
    }

    @Override
    public String toString() {
        return "EntityData [namespace=" + namespace + ", entityType=" + entityType + ", data=" + data + ", constraints="
                + constraints + ", entityState=" + entityState + ", uuid=" + uuid + "]";
    }
}
