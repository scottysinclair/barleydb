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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.ProxyController;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;

public class EntityProxy implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public static <K> K generateProxy(ClassLoader cl, Entity entity) throws ClassNotFoundException {
        return (K) Proxy.newProxyInstance(cl, new Class[] { ProxyController.class, Class.forName(entity.getEntityType().getInterfaceName(), true, cl) }, new EntityProxy(entity));
    }

    private Entity entity;

    public EntityProxy(Entity entity) {
        this.entity = entity;
    }

    public EntityType getEntityType() {
        return entity.getEntityType();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();
        if (methodName.equals("toString")) {
            return entity.toString();
        }
        else if (methodName.equals("hashCode")) {
            return entity.hashCode();
        }
        else if (methodName.equals("equals")) {
            if (args[0] == null) {
                return false;
            }
            if (args[0] instanceof ProxyController) {
                return entity.equals(((ProxyController) args[0]).getEntity());
            }
            else {
                return entity.equals(args[0]);
            }
        }
        else if (methodName.equals("setEntity")) {
            this.entity = (Entity) args[0];
            return null;
        }
        else if (methodName.equals("getEntity")) {
            return entity;
        }

        boolean set = methodName.startsWith("set");
        final String nodeName;
        if (set || methodName.startsWith("get")) {
            nodeName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        else if (methodName.startsWith("is")) {
            nodeName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        else {
            throw new IllegalStateException("Child node does not exist " + methodName);
        }

        Node node = entity.getChild(nodeName, Node.class);
        if (node == null) {
            throw new IllegalStateException("No such property '" + nodeName + "'");
        }
        else if (node instanceof ValueNode) {
            if (set) {
                ((ValueNode) node).setValue(args[0]);
                return null;
            }
            else {
                return ((ValueNode) node).getValue();
            }
        }
        else if (node instanceof RefNode) {
            if (set) {
                ProxyController entityModel = (ProxyController) args[0];
                if (entityModel == null) {
                    ((RefNode) node).setReference(null);
                }
                else {
                    Entity en = entityModel.getEntity();
                    ((RefNode) node).setReference(en);
                }
                return null;
            }
            else {
                final Entity en = ((RefNode) node).getReference();
                if (en == null) {
                    return null;
                }
                else {
                    return entity.getEntityContext().getProxy(en);
                }
            }
        }
        else if (node instanceof ToManyNode) {
            if (set) {
                ToManyNode tmnode = ((ToManyNode) node);
                tmnode.getList().clear();
                List<?> list = (List<?>) args[0];
                /*
                 *We ignore any null elements in the received list
                 */
                if (list != null) {
                    for (Object o : list) {
                        if (o != null) {
                            if (o instanceof ProxyController) {
                                ProxyController em = (ProxyController) o;
                                tmnode.add(em.getEntity());
                            }
                            else if (o instanceof Entity) {
                                tmnode.add((Entity) o);
                            }

                        }
                    }
                }
                return null;
            }
            else {
                //each node context must belong to a session
                //and each session must track it's proxies
                //rather than creating new ones each time.
                //we always resolve the tomany node based on it's owning entity
                return new ToManyProxy<Object>(entity.getChild(nodeName, ToManyNode.class));
            }
        }
        else {
            throw new Exception("woopsy1 " + node);
        }
    }

    ToManyNode resolveToMany(String child) {
        return entity.getChild(child, ToManyNode.class);
    }

    RefNode resolveRef(String child) {
        return entity.getChild(child, RefNode.class);
    }

    @Override
    public String toString() {
        return "EntityProxy [entity=" + entity + "]";
    }

}
