package scott.sort.server.jdbc.query;

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
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.EntityContextHelper;

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
    private final Class<T> resultType;
    private final List<Entity> entities = new LinkedList<Entity>();
    private final QueryResultList<T> result;

    public QueryResult(EntityContext entityContext, Class<T> resultType) {
        this.entityContext = entityContext;
        this.resultType = resultType;
        this.result = new QueryResultList<T>(entityContext, entities);
    }

    public List<T> getList() {
        return result;
    }

    public T getSingleResult() {
        if (result.size() > 1) {
            throw new IllegalStateException("Too much data returned");
        }
        return result.size() == 1 ? result.get(0) : null;
    }

    public Class<T> getResultType() {
        return resultType;
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
        entityContext.beginLoading();
        newEntityContext.beginLoading();
        try {
            QueryResult<T> result = new QueryResult<T>(newEntityContext, resultType);
            List<Entity> copied = EntityContextHelper.addEntities(entityContext.getEntities(), newEntityContext);
            EntityContextHelper.copyRefStates(entityContext, newEntityContext, copied, new EntityContextHelper.EntityFilter() {
                @Override
                public boolean includesEntity(Entity entity) {
                    return true;
                }
            });
            for (Entity e : entities) {
                result.entities.add(newEntityContext.getEntityByUuidOrKey(e.getUuid(), e.getEntityType(), e.getKey().getValue(), true));
            }

            return result;
        } finally {
            newEntityContext.endLoading();
            entityContext.endLoading();
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