package scott.barleydb.server.jdbc.persist.audit;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import scott.barleydb.api.core.entity.Entity;

public class AuditInformation {
    final Map<AuditKey, AuditRecord> recordsLookup = new HashMap<>();
    final List<AuditRecord> records = new LinkedList<>();

    public void add(List<AuditRecord> someRecords) {
        records.addAll(someRecords);
        for (AuditRecord auditRecord : someRecords) {
            recordsLookup.put(new AuditKey(auditRecord), auditRecord);
        }
    }

    public boolean contains(Entity entity) {
        return recordsLookup.containsKey(new AuditKey(entity));
    }

    public AuditRecord getOrCreateRecord(Entity entity) {
        AuditKey key = new AuditKey(entity);
        AuditRecord record = recordsLookup.get(key);
        if (record == null) {
            record = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
            recordsLookup.put(key, record);
            records.add(record);
        }
        return record;
    }

    public List<AuditRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    @Override
    public String toString() {
        return "AuditInformation [records=" + records + "]";
    }
}
