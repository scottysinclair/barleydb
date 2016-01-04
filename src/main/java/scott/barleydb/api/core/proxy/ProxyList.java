package scott.barleydb.api.core.proxy;

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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;

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
