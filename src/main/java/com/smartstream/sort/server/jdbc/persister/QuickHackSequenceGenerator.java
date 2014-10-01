package com.smartstream.sort.server.jdbc.persister;

import java.util.HashMap;
import java.util.Map;

import com.smartstream.sort.api.config.EntityType;
import com.smartstream.sort.api.core.Environment;
import com.smartstream.sort.api.core.entity.Entity;
import com.smartstream.sort.api.core.entity.EntityContext;
import com.smartstream.sort.api.query.QueryObject;

public class QuickHackSequenceGenerator implements SequenceGenerator {
    private Environment env;
    private String namespace;
    private Map<EntityType, Long> values = new HashMap<>();

    public QuickHackSequenceGenerator(Environment env, String namespace) {
        this.env = env;
        this.namespace = namespace;
    }

    @Override
    public synchronized Object getNextKey(EntityType entityType) {
        Long value = values.get(entityType);
        if (value == null) {
            try {
                QueryObject<?> qo = env.getDefinitions(namespace).getQuery(entityType);
                EntityContext entityContext = new EntityContext(env, namespace);
                entityContext.performQuery(qo);
                Long highest = 0l;
                for (Entity e : entityContext.getEntities()) {
                    Long key = (Long) e.getKey().getValue();
                    if (key > highest) {
                        highest = key;
                    }
                }
                value = highest;
            } catch (Exception x) {
                throw new IllegalStateException("Could not get next key for " + entityType, x);
            }
        }
        values.put(entityType, ++value);
        return value;
    }

}
