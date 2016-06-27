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
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;

public class ToManyProxy<R> extends AbstractList<R> implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ToManyProxy.class);

    private final ToManyNode toManyNode;

    public ToManyProxy(ToManyNode toManyNode) {
        this.toManyNode = toManyNode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R get(int index) {
        //    LOG.debug("GET " + toManyNode.getParent() + "." + toManyNode.getName() + " = " + toManyNode);
        fetchIfNeeded();
        Entity en = toManyNode.getList().get(index);
        String joinProperty = toManyNode.getNodeType().getJoinProperty();
        if (joinProperty != null) {
            en = en.getChild(joinProperty, RefNode.class, true).getReference();
        }
        /*
         * TODO: detect if we have the correct kind of entity for the proxy
         */
        return (R) toManyNode.getEntityContext().getProxy(en);
    }

    @Override
    public void add(int index, R element) {
        fetchIfNeeded();
        ProxyController em = (ProxyController) element;
        Entity e = em.getEntity();
        if (e.getEntityType().equals(toManyNode.getEntityType())) {
            toManyNode.add(index, e);
            return;
        }

        NodeType nd = toManyNode.getNodeType();
        Definitions defs = e.getEntityContext().getDefinitions();
        String joinProperty = nd.getJoinProperty();
        if (joinProperty != null) {
            EntityType joinEntityType = defs.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            String typePastJoin = joinEntityType.getNodeType(joinProperty, true).getRelationInterfaceName();
            if (e.getEntityType().getInterfaceName().equals(typePastJoin)) {
                //an entity is being added to a n:m relation, the join table entity must therefore have state new.
                Entity joinEntity = e.getEntityContext().newEntity(joinEntityType, null, EntityConstraint.mustNotExistInDatabase());
                NodeType nodeTypeReferringBack = joinEntityType.getNodeTypeWithRelationTo(toManyNode.getParent().getEntityType().getInterfaceName());
                joinEntity.getChild(nodeTypeReferringBack.getName(), RefNode.class, true).setReference(toManyNode.getParent());
                NodeType nodeTypeReferringForward = joinEntityType.getNodeTypeWithRelationTo(e.getEntityType().getInterfaceName());
                joinEntity.getChild(nodeTypeReferringForward.getName(), RefNode.class, true).setReference(e);
                toManyNode.add(joinEntity);
                return;
            }
        }
        throw new IllegalStateException("Wrong type '" + e.getEntityType().getInterfaceShortName() + "', expected '" + toManyNode.getEntityType() + "'");

    }

    @SuppressWarnings("unchecked")
    @Override
    public R remove(int index) {
        fetchIfNeeded();
        Entity en = toManyNode.remove(index);
        return (R) toManyNode.getEntityContext().getProxy(en);
    }

    @Override
    public int size() {
        fetchIfNeeded();
        return toManyNode.getList().size();
    }

    @Override
    public Iterator<R> iterator() {
        fetchIfNeeded();
        return super.iterator();
    }

    private void fetchIfNeeded() {
        if (toManyNode.getParent().isNew()) {
            /*
             * the syntax is not in the database, so how can we load it's mappings
             */
            return;
        }
        if (toManyNode.getParent().getEntityState() == EntityState.LOADING) {
            /*
             * our entity is being loaded, so do not fetch
             */
            return;
        }
        /*
         * ok, we got here..
         * if we have not yet been fetched then try it
         */
        if (!toManyNode.isFetched()) {
            toManyNode.getEntityContext().fetch(toManyNode);
        }
    }
}
