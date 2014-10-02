package scott.sort.server.jdbc.queryexecution;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.query.QueryObject;

/**
 * Builds and maintains a set of entity loaders for a given projection, resultset and entitycontext
 *
 * @author scott
 *
 */
final class EntityLoaders implements Iterable<EntityLoader> {
    private final List<EntityLoader> entityLoadersList;

    public EntityLoaders(Projection projection, ResultSet resultSet, EntityContext entityContext) {
        this.entityLoadersList = build(projection, resultSet, entityContext);
    }

    public void clearRowCache() {
        for (EntityLoader entityLoader : entityLoadersList) {
            entityLoader.clearRowCache();
        }
    }

    public Entity getMostRecentlyLoaded(QueryObject<?> queryObject) {
        for (EntityLoader entityLoader : entityLoadersList) {
            if (entityLoader.getQueryObject() == queryObject) {
                List<Entity> list = entityLoader.getLoadedEntities();
                return list.isEmpty() ? null : list.get(list.size() - 1);
            }
        }
        throw new IllegalStateException("entityLoader for queryobject " + queryObject + " not exist");
    }

    public List<Entity> getEntitiesForQueryObject(QueryObject<?> queryObject) {
        for (EntityLoader entityLoader : entityLoadersList) {
            if (entityLoader.getQueryObject() == queryObject) {
                return entityLoader.getLoadedEntities();
            }
        }
        throw new IllegalStateException("entityLoader for queryobject " + queryObject + " not exist");
    }

    @Override
    public Iterator<EntityLoader> iterator() {
        return entityLoadersList.iterator();
    }

    private List<EntityLoader> build(Projection projection, ResultSet resultSet, EntityContext entityContext) {
        List<EntityLoader> loadable = new LinkedList<>();
        QueryObject<?> queryObject = null;
        for (ProjectionColumn column : projection.getColumns()) {
            if (queryObject == null || queryObject != column.getQueryObject()) {
                queryObject = column.getQueryObject();
                loadable.add(new EntityLoader(this, projection, queryObject, resultSet, entityContext));
            }
        }
        return loadable;
    }
}
