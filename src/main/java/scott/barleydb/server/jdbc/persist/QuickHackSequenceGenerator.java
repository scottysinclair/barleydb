package scott.barleydb.server.jdbc.persist;

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

import java.util.HashMap;
import java.util.Map;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.query.QueryObject;

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
