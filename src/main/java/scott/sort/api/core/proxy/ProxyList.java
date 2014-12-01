package scott.sort.api.core.proxy;

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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;

public class ProxyList<T> extends AbstractList<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EntityContext entityContext;
    private final List<Entity> entities;

    public ProxyList(EntityContext entityContext, Collection<Entity> entities) {
        this.entityContext = entityContext;
        this.entities = new LinkedList<>(entities);
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
