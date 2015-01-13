package scott.sort.server.jdbc.persist;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

import scott.sort.api.config.EntityType;
import scott.sort.api.core.Environment;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.execution.persist.SortPersistException;
import scott.sort.api.query.QueryObject;

public class QuickHackSequenceGenerator implements SequenceGenerator {
    private Environment env;
    private String namespace;
    private Map<EntityType, Long> values = new HashMap<>();

    public QuickHackSequenceGenerator(Environment env, String namespace) {
        this.env = env;
        this.namespace = namespace;
    }

    @Override
    public synchronized Object getNextKey(EntityType entityType) throws SortPersistException {
        Long value = values.get(entityType);
        if (value == null) {
            try {
                QueryObject<?> qo = env.getDefinitions(namespace).getQuery(entityType);
                //a new entity context will consume a new connection
                EntityContext entityContext = new EntityContext(env, namespace);
                entityContext.performQuery(qo);
                Long highest = 0l;
                for (Entity e : entityContext.getEntitiesByType(entityType)) {
                    Long key = (Long) e.getKey().getValue();
                    if (key > highest) {
                        highest = key;
                    }
                }
                value = highest;
            } catch (Exception x) {
                throw new SortPersistException("Could not get next key for " + entityType, x);
            }
        }
        values.put(entityType, ++value);
        return value;
    }

}
