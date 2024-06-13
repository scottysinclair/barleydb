package scott.barleydb.api.core.entity.context;

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

import scott.barleydb.api.core.entity.Entity;

class EntityPkKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String interfaceName;

    private final Object key;

    public EntityPkKey(Entity entity) {
        this.interfaceName = entity.getEntityType().getInterfaceName();
        this.key = entity.getKeyValue();
    }

    public EntityPkKey(String interfaceName, Object key) {
        this.interfaceName = interfaceName;
        this.key = key;
    }

    public EntityPkKey(EntityInfo entityInfo) {
        this.interfaceName = entityInfo.getEntityType().getInterfaceName();
        this.key = entityInfo.getPrimaryKey();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        EntityPkKey other = (EntityPkKey) obj;
        if (interfaceName == null) {
            if (other.interfaceName != null) return false;
        } else if (!interfaceName.equals(other.interfaceName)) return false;
        if (key == null) {
            if (other.key != null) return false;
        } else if (!key.equals(other.key)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "EntityPkKey [interfaceName=" + interfaceName + ", key=" + key + "]";
    }
}
