package com.smartstream.sort.server.jdbc.persister.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.smartstream.sort.api.core.entity.Entity;

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