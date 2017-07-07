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
import java.util.List;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;

/**
 * Allows solutions to write their own proxies which would
 * perform better and be more debuggable than the JDK dynamic proxies.
 * @author scott
 *
 */
public abstract class AbstractCustomEntityProxy implements ProxyController, Serializable {

    private static final long serialVersionUID = 1L;
    protected final Entity entity;

    protected AbstractCustomEntityProxy(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof ProxyController) {
            return entity.equals(((ProxyController) obj).getEntity());
        }
        else {
            return entity.equals(obj);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T> T getFromRefNode(RefNode node) {
        final Entity en = node.getReference();
        if (en == null) {
            return null;
        }
        else {
            return (T) entity.getEntityContext().getProxy(en);
        }
    }

    protected final void setToRefNode(RefNode node, Object object) {
        ProxyController entityModel = (ProxyController) object;
        if (entityModel == null) {
            node.setReference(null);
        }
        else {
            Entity en = entityModel.getEntity();
            node.setReference(en);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T> List<T> getListProxy(ToManyNode node) {
        return (List<T>) new ToManyProxy<Object>(node);
    }

    protected final void setList(ToManyNode node, List<?> list) {
        node.getList().clear();
        /*
         *We ignore any null elements in the received list
         */
        if (list != null) {
            for (Object o : list) {
                if (o != null) {
                    if (o instanceof ProxyController) {
                        ProxyController em = (ProxyController) o;
                        node.add(em.getEntity());
                    }
                    else if (o instanceof Entity) {
                        node.add((Entity) o);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return entity.toString();
    }

}
