package com.smartstream.sort.api.core.proxy;

import java.io.Serializable;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.sort.api.config.Definitions;
import com.smartstream.sort.api.config.EntityType;
import com.smartstream.sort.api.config.NodeDefinition;
import com.smartstream.sort.api.core.entity.Entity;
import com.smartstream.sort.api.core.entity.EntityState;
import com.smartstream.sort.api.core.entity.ProxyController;
import com.smartstream.sort.api.core.entity.RefNode;
import com.smartstream.sort.api.core.entity.ToManyNode;

public class ToManyProxy<R> extends AbstractList<R> implements Serializable {
    private static final long serialVersionUID = 1L;

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
        String joinProperty = toManyNode.getNodeDefinition().getJoinProperty();
        if (joinProperty != null) {
            en = en.getChild(joinProperty, RefNode.class, true).getReference();
        }
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

        NodeDefinition nd = toManyNode.getNodeDefinition();
        Definitions defs = e.getEntityContext().getDefinitions();
        String joinProperty = nd.getJoinProperty();
        if (joinProperty != null) {
            EntityType joinEntityType = defs.getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true);
            String typePastJoin = joinEntityType.getNode(joinProperty, true).getRelationInterfaceName();
            if (e.getEntityType().getInterfaceName().equals(typePastJoin)) {
                Entity joinEntity = new Entity(e.getEntityContext(), joinEntityType);
                e.getEntityContext().add(joinEntity);
                NodeDefinition nodeTypeReferringBack = joinEntityType.getNodeWithRelationTo(toManyNode.getParent().getEntityType().getInterfaceName());
                joinEntity.getChild(nodeTypeReferringBack.getName(), RefNode.class, true).setReference(toManyNode.getParent());
                NodeDefinition nodeTypeReferringForward = joinEntityType.getNodeWithRelationTo(e.getEntityType().getInterfaceName());
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
        if (toManyNode.getParent().getKey().getValue() == null) {
            /*
             * the syntax has no id, so how can we load it's mappings
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