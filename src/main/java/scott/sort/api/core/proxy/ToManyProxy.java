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
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.Definitions;
import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeType;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityState;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;

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
                Entity joinEntity = new Entity(e.getEntityContext(), EntityState.NEW, joinEntityType);
                e.getEntityContext().add(joinEntity);
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
        if (toManyNode.getParent().getEntityState() != EntityState.LOADED) {
            /*
             * our entity is not loaded so we don't fetch
             */
            return;
        }
        /*
         * ok, we got here.. if we have not yet been fetched then try it
         */
        if (!toManyNode.isFetched()) {
            toManyNode.getEntityContext().fetch(toManyNode);
        }
    }
}