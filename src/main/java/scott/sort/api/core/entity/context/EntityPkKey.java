package scott.sort.api.core.entity.context;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.Serializable;

import scott.sort.api.core.entity.Entity;

class EntityPkKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String interfaceName;

    private final Object key;

    public EntityPkKey(Entity entity) {
        this.interfaceName = entity.getEntityType().getInterfaceName();
        this.key = entity.getKey().getValue();
    }

    public EntityPkKey(String interfaceName, Object key) {
        this.interfaceName = interfaceName;
        this.key = key;
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
