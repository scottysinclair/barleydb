package scott.barleydb.api.audit;

import java.io.Serializable;

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

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;

class AuditKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EntityType entityType;
    private final Object entityKey;

    public AuditKey(Entity entity) {
        this.entityType = entity.getEntityType();
        this.entityKey = entity.getKeyValue();
    }

    public AuditKey(AuditRecord auditRecord) {
        this.entityType = auditRecord.getEntityType();
        this.entityKey = auditRecord.getEntityKey();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((entityKey == null) ? 0 : entityKey.hashCode());
        result = prime * result
                + ((entityType == null) ? 0 : entityType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AuditKey other = (AuditKey) obj;
        if (entityKey == null) {
            if (other.entityKey != null) return false;
        } else if (!entityKey.equals(other.entityKey)) return false;
        if (entityType == null) {
            if (other.entityType != null) return false;
        } else if (!entityType.equals(other.entityType)) return false;
        return true;
    }
}
