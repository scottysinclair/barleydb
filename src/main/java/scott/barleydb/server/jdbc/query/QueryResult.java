package scott.barleydb.server.jdbc.query;

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
import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextHelper;
import scott.barleydb.api.core.entity.EntityContextState;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleydb.server.jdbc.query.QueryResultList;

/**
 * A result of a query
 * @author scott
 *
 * @param <T> the top level java type returned by the list
 */
public class QueryResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(QueryResult.class);

    private final EntityContext entityContext;
//    private final Class<T> resultType;
    private final List<Entity> entities = new LinkedList<Entity>();
    private final QueryResultList<T> result;

    public QueryResult(EntityContext entityContext) {
        this.entityContext = entityContext;
        this.result = new QueryResultList<T>(entityContext, entities);
    }
    
    /**
     * Gets the result list
     * @return
     */
    public List<T> getList() {
        return result;
    }

    /**
     * Provides access to the list of entities directly.
     * The entities in the list correspond to the proxies which are returned
     * by {@link #getList()}
     * @return
     */
    public List<Entity> getEntityList() {
        return entities;
    }

    public T getSingleResult() {
        if (result.size() > 1) {
            throw new IllegalStateException("Too much data returned");
        }
        return result.size() == 1 ? result.get(0) : null;
    }

    public EntityContext getEntityContext() {
        return entityContext;
    }

    public void addEntities(List<Entity> entities) {
        result.addEntities(entities);
    }

    public QueryResult<T> copyResultTo(EntityContext newEntityContext) {
        if (entityContext == newEntityContext) {
            return this;
        }
        LOG.debug("=== COPYING DATA ACROSS =======");
        EntityContextState ecs1 = entityContext.switchToInternalMode();
        EntityContextState ecs2 = newEntityContext.switchToInternalMode();
        try {
            QueryResult<T> result = new QueryResult<T>(newEntityContext);
            List<Entity> copied = EntityContextHelper.addEntities(entityContext.getEntities(), newEntityContext, true, true);
            EntityContextHelper.copyRefStates(entityContext, newEntityContext, copied, new EntityContextHelper.EntityFilter() {
                @Override
                public boolean includesEntity(Entity entity) {
                    return true;
                }
            });
            for (Entity e : entities) {
                result.entities.add(newEntityContext.getEntityByUuidOrKey(e.getUuid(), e.getEntityType(), e.getKeyValue(), true));
            }

            return result;
        } finally {
            newEntityContext.switchToMode(ecs2);
            entityContext.switchToMode(ecs1);
        }
    }
}

class QueryResultList<T> extends AbstractList<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EntityContext entityContext;
    private final List<Entity> entities;

    public QueryResultList(EntityContext entityContext, List<Entity> entities) {
        this.entityContext = entityContext;
        this.entities = entities;
    }

    public void addEntities(List<Entity> entities) {
        this.entities.addAll(entities);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        Entity en = entities.get(index);
        if (en == null) {
            return null;
        }
        return (T) entityContext.getProxy(en);
    }

    @Override
    public int size() {
        return entities.size();
    }

}
