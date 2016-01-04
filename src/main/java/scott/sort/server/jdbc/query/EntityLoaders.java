package scott.sort.server.jdbc.query;

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

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.exception.execution.query.IllegalQueryStateException;
import scott.sort.api.query.QueryObject;
import scott.sort.server.jdbc.JdbcEntityContextServices;
import scott.sort.server.jdbc.converter.TypeConverter;

/**
 * Builds and maintains a set of entity loaders for a given projection, resultset and entitycontext
 *
 * @author scott
 *
 */
final class EntityLoaders implements Iterable<EntityLoader> {
	private final JdbcEntityContextServices entityContextServices;
    private final List<EntityLoader> entityLoadersList;

    public EntityLoaders(JdbcEntityContextServices entityContextServices, Projection projection, ResultSet resultSet, EntityContext entityContext) {
    	this.entityContextServices = entityContextServices;
        this.entityLoadersList = build(projection, resultSet, entityContext);
    }
    
	public TypeConverter getTypeConverter(String typeConverterFqn) {
		return entityContextServices.getTypeConverter(typeConverterFqn);
	}

    public void clearRowCache() {
        for (EntityLoader entityLoader : entityLoadersList) {
            entityLoader.clearRowCache();
        }
    }

    public Entity getMostRecentlyLoaded(QueryObject<?> queryObject) throws IllegalQueryStateException {
        for (EntityLoader entityLoader : entityLoadersList) {
            if (entityLoader.getQueryObject() == queryObject) {
                List<Entity> list = entityLoader.getLoadedEntities();
                return list.isEmpty() ? null : list.get(list.size() - 1);
            }
        }
        throw new IllegalQueryStateException("entityLoader for queryobject " + queryObject + " not exist");
    }

    public List<Entity> getEntitiesForQueryObject(QueryObject<?> queryObject) throws IllegalQueryStateException {
        for (EntityLoader entityLoader : entityLoadersList) {
            if (entityLoader.getQueryObject() == queryObject) {
                return entityLoader.getLoadedEntities();
            }
        }
        throw new IllegalQueryStateException("entityLoader for queryobject " + queryObject + " not exist");
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
