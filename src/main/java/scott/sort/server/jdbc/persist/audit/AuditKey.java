package scott.sort.server.jdbc.persist.audit;

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
import scott.sort.api.core.entity.Entity;

class AuditKey {
    private final EntityType entityType;
    private final Object entityKey;

    public AuditKey(Entity entity) {
        this.entityType = entity.getEntityType();
        this.entityKey = entity.getKey().getValue();
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