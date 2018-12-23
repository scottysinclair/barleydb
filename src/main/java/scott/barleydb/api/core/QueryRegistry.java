package scott.barleydb.api.core;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.core.QueryRegistry;

/**
 * A registry of queries by type
 * The queries which are returned are copies
 */
public class QueryRegistry implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private Map<String, QueryObject<?>> map = new HashMap<String, QueryObject<?>>();

    public QueryRegistry() {}

    public QueryRegistry(QueryRegistry qr) {
        map.putAll(qr.map);
    }

    public void addAll(QueryRegistry qr) {
        map.putAll(qr.map);
    }

    /**
     * The query objects in the map are considered immutable
     * since we always return cloned copies and we only allow them to be replaced.
     */
    @Override
    public QueryRegistry clone() {
        try {
            QueryRegistry qr = (QueryRegistry) super.clone();
            qr.map = new HashMap<String, QueryObject<?>>();
            qr.map.putAll(map);
            return qr;
        } catch (CloneNotSupportedException x) {
            throw new IllegalStateException("Clone must be supported", x);
        }
    }

    public void register(QueryObject<?>... qos) {
        for (QueryObject<?> qo : qos) {
            String key = getKey(qo);
            map.put(key, clone(qo));
        }
    }

    public QueryObject<Object> getQuery(EntityType entityType) {
        return getQuery(entityType.getInterfaceName());
    }

    @SuppressWarnings("unchecked")
    public QueryObject<Object> getQuery(String interfaceName) {
        return clone((QueryObject<Object>) map.get(interfaceName));
    }

    @SuppressWarnings("unchecked")
    public <T> QueryObject<T> getQuery(Class<T> interfaceType) {
        return clone((QueryObject<T>) map.get(interfaceType.getName()));
    }

    private String getKey(QueryObject<? extends Object> qo) {
        return qo.getTypeName();
    }

    @SuppressWarnings("unchecked")
    public static <T> QueryObject<T> clone(QueryObject<T> queryObject) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bo);
            os.writeObject(queryObject);
            ObjectInputStream oi = new ObjectInputStream(
                    new ByteArrayInputStream(bo.toByteArray()));
            return (QueryObject<T>) oi.readObject();
        } catch (Exception x) {
            throw new IllegalStateException("Error cloning query", x);
        }
    }

}
