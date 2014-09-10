package com.smartstream.morf.api.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.query.QueryObject;

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
            if (map.containsKey(getKey(qo))) {
                register("user", qo);
            } else {
                register(null, qo);
            }
        }
    }

    private void register(String name, QueryObject<?> qo) {
        map.put(getKey(qo), qo);
    }

    private String getKey(QueryObject<? extends Object> qo) {
        return qo.getTypeName();
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

    @SuppressWarnings("unchecked")
    private <T> QueryObject<T> clone(QueryObject<T> queryObject) {
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
