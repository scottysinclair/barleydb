package com.smartstream.morf.api.core.proxy;

import java.io.Serializable;
import java.util.List;

import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.ProxyController;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ToManyNode;

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
            return entity.equals(((ProxyController)obj).getEntity());
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
            return (T)entity.getEntityContext().getProxy(en);
        }
    }

    protected final void setToRefNode(RefNode node, Object object) {
        ProxyController entityModel = (ProxyController)object;
        if (entityModel == null) {
            node.setEntityKey(null);
            node.setReference(null);
        }
        else {
            Entity en = entityModel.getEntity();
            node.setEntityKey( en.getKey().getValue() );
            node.setReference(en);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T> List<T> getListProxy(ToManyNode node) {
        return (List<T>)new ToManyProxy<Object>(node);
    }

    protected final void setList(ToManyNode node, List<?> list) {
        //todo:set current entities to removed if not in the new list
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
