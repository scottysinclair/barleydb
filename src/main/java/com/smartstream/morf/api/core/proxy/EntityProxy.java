package com.smartstream.morf.api.core.proxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.Node;
import com.smartstream.morf.api.core.entity.ProxyController;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ToManyNode;
import com.smartstream.morf.api.core.entity.ValueNode;

public class EntityProxy implements InvocationHandler, Serializable {

	private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public static <K> K generateProxy(ClassLoader cl, Entity entity) throws ClassNotFoundException {
		return (K)Proxy.newProxyInstance(cl, new Class[]{ProxyController.class, Class.forName(entity.getEntityType().getInterfaceName(), true, cl)}, new EntityProxy(entity));
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
				return entity.equals(((ProxyController)args[0]).getEntity());
			}
			else {
				return entity.equals(args[0]);
			}
		}
		else if (methodName.equals("setEntity")) {
			this.entity = (Entity)args[0];
			return null;
		}
		else if (methodName.equals("getEntity")) {
			return entity;
		}

		if (entity.isDeleted()) {
		    throw new IllegalStateException("Entity is deleted from node context.");
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
			throw new IllegalStateException("No such property " + nodeName);
		}
		else if (node instanceof ValueNode) {
			if (set) {
				((ValueNode)node).setValue(args[0]);
				return null;
			}
			else {
				return ((ValueNode)node).getValue();
			}
		}
		else if (node instanceof RefNode) {
			if (set) {
				ProxyController entityModel = (ProxyController)args[0];
				if (entityModel == null) {
					((RefNode)node).setEntityKey(null);
				  ((RefNode)node).setReference(null);
				}
				else {
					Entity en = entityModel.getEntity();
					((RefNode)node).setEntityKey( en.getKey().getValue() );
					((RefNode)node).setReference(en);
				}
				return null;
			}
			else {
				final Entity en = ((RefNode)node).getReference();
				if (en == null) {
					return null;
				}
				else {
					return entity.getEntityContext().getProxy(en);
				}
			}
		}
		else if (node instanceof ToManyNode){
			if (set) {
				ToManyNode tmnode = ((ToManyNode)node);
		        //todo:set current entities to removed if not in the new list
				tmnode.getList().clear();
				List<?> list = (List<?>)args[0];
				/*
				 *We ignore any null elements in the received list
				 */
				if (list != null) {
					for (Object o: list) {
						if (o != null) {
		                    if (o instanceof ProxyController) {
		                        ProxyController em = (ProxyController)o;
		                        tmnode.add(em.getEntity());
		                    }
		                    else if (o instanceof Entity) {
		                        tmnode.add((Entity)o);
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
