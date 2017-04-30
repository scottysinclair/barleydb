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

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.EntityData;
import scott.barleydb.server.jdbc.JdbcEntityContextServices;
import scott.barleydb.server.jdbc.converter.TypeConverter;

/**
 * Builds and maintains a set of entity loaders for a given projection, resultset and entitycontext
 *
 * @author scott
 *
 */
final class EntityLoaders implements Iterable<EntityLoader> {
    private final JdbcEntityContextServices entityContextServices;
    private final List<EntityLoader> entityLoadersList;
    private final Definitions definitions;
    private final LinkedHashMap<EntityKey, EntityData> loadedEntityData = new LinkedHashMap<>();

    public EntityLoaders(JdbcEntityContextServices entityContextServices, Definitions definitions, Projection projection, ResultSet resultSet) {
        this.entityContextServices = entityContextServices;
        this.definitions = definitions;
        this.entityLoadersList = build(projection, resultSet);
    }

    public Definitions getDefinitions() {
        return definitions;
    }

    public TypeConverter getTypeConverter(String typeConverterFqn) {
        return entityContextServices.getTypeConverter(typeConverterFqn);
    }

    public LinkedHashMap<EntityKey, EntityData> getLoadedEntityData() {
        return loadedEntityData;
    }

    public void clearRowCache() {
        for (EntityLoader entityLoader : entityLoadersList) {
            entityLoader.clearRowCache();
        }
    }

    @Override
    public Iterator<EntityLoader> iterator() {
        return entityLoadersList.iterator();
    }

    private List<EntityLoader> build(Projection projection, ResultSet resultSet) {
        List<EntityLoader> loadable = new LinkedList<>();
        QueryObject<?> queryObject = null;
        for (ProjectionColumn column : projection.getColumns()) {
            if (queryObject == null || queryObject != column.getQueryObject()) {
                queryObject = column.getQueryObject();
                loadable.add(new EntityLoader(this, projection, queryObject, resultSet));
            }
        }
        return loadable;
    }

    public Collection<EntityData> getEntityDataLoadedFor(QueryObject<?> queryObject) {
        for (EntityLoader loader: entityLoadersList) {
            if (loader.getQueryObject() == queryObject) {
                return loader.getLoadedEntityData().values();
            }
        }
        return Collections.emptyList();
    }
}

class EntityKey {
    private EntityType entityType;
    private Object entityKey;
    public EntityKey(EntityType entityType, Object entityKey) {
        this.entityType = entityType;
        this.entityKey = entityKey;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityKey == null) ? 0 : entityKey.hashCode());
        result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityKey other = (EntityKey) obj;
        if (entityKey == null) {
            if (other.entityKey != null)
                return false;
        } else if (!entityKey.equals(other.entityKey))
            return false;
        if (entityType == null) {
            if (other.entityType != null)
                return false;
        } else if (!entityType.equals(other.entityType))
            return false;
        return true;
    }

}
