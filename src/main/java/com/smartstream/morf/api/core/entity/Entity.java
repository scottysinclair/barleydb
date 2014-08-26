package com.smartstream.morf.api.core.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedList;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.smartstream.morf.api.config.EntityType;
import com.smartstream.morf.api.config.NodeDefinition;

public class Entity implements Serializable {
	private static final long serialVersionUID = 1L;

	private final EntityContext entityContext;
    private transient EntityType entityType;
    private TreeMap<String,Node> children;
    private EntityState entityState;
    private String uuid;

    /**
     * Copy constructor, a new version of the entity in a different context with the same uuid
     * @param context
     * @param toCopy
     */
    public Entity(EntityContext context, Entity toCopy) {
        this(context, toCopy.getEntityType(), toCopy.getKey().getValue(), toCopy.getUuid());
    }

    public Entity(EntityContext context, EntityType entityType) {
        this(context, entityType, null, UUID.randomUUID().toString());
    }

    public Entity(EntityContext context, EntityType entityType, Object key) {
        this(context, entityType, key, UUID.randomUUID().toString());
    }

    public Entity(EntityContext context, EntityType entityType, Object key, String uuid) {
    	this.entityContext = context;
        this.entityType = entityType;
        this.children = new TreeMap<String,Node>();
        entityState = EntityState.NOTLOADED;
        initNodes();
        if (key != null) {
            getKey().setValueNoEvent(key);
        }
        this.uuid = uuid;
    }

    public boolean isLoaded() {
        return entityState == EntityState.LOADED;
    }
    public boolean isNotLoaded() {
        return entityState == EntityState.NOTLOADED;
    }

    public void setDeleted() {
        this.entityState = EntityState.DELETED;
    }

    public final boolean isDeleted() {
        return entityState == EntityState.DELETED;
    }

    /**
     * Sets the nodes state to unloaded
     * Means only the key has a value and everything else
     * must be fetched
     */
    public void unload() {
    	for (Node node: getChildren()) {
    		if (node != getKey()) {
    			if (node instanceof ValueNode) {
    				((ValueNode) node).setValueNoEvent(NotLoaded.VALUE);
    			}
    			else if (node instanceof RefNode) {
    				((RefNode) node).setEntityKey(null);
    			}
    			/* even if a syntax is unloaded, all of it's mappings can
    			 * still be there in the context
    			 *
    			else if (node instanceof ToManyNode) {
    				((ToManyNode) node).setFetched(false);
    			}*/
    		}
    	}
    	setEntityState(EntityState.NOTLOADED);
    	clear();
    }

    /**
     * Clears any changes to RefNodes and ToManyNodes
     */
    public void clear() {
		for (Node node: getChildren()) {
			if (node instanceof RefNode) {
				((RefNode)node).clear();
			}
			else if (node instanceof ToManyNode) {
				((ToManyNode)node).clear();
			}
		}
    }

    public void refresh() {
		for (Node node: getChildren()) {
			if (node instanceof RefNode) {
				((RefNode)node).refresh();
			}
			else if (node instanceof ToManyNode) {
				((ToManyNode)node).refresh();
			}
		}
    }

    public String getUuid() {
        return uuid;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public boolean isOfType(EntityType entityType) {
        return this.entityType.equals(entityType);
    }

    public boolean hasKey(final Object key) {
        return key != null && key.equals( getKey().getValue() );
    }

    public final ValueNode getKey() {
        return getChild( entityType.getKeyNodeName(), ValueNode.class );
    }

    public void setValueNode(String name, Object value) {
        getChild(name, ValueNode.class).setValue(value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T getChild(String name, Class<T> type) {
        return (T)children.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T getChild(String name, Class<T> type, boolean mustExist) {
        T child =  (T)children.get(name);
        if (child == null && mustExist) {
            throw new IllegalStateException("Node '" + name + "' must exist in entity " + entityType.getInterfaceName());
        }
        return child;
    }

    @SuppressWarnings("unchecked")
    public <N extends Node> List<N> getChildren(Class<N> type) {
        List<N> result = new LinkedList<N>();
        for (Node child: getChildren()) {
            if (type.isAssignableFrom(child.getClass())) {
                result.add((N)child);
            }
        }
        return result;
    }

    /**
     * Copies the values nodes from the input entity to this object
     * @param from
     */
    public void copyValueNodesToMe(Entity from) {
		for (ValueNode fromChild: from.getChildren(ValueNode.class)) {
			ValueNode toChild = getChild(fromChild.getName(), ValueNode.class);
			toChild.setValue( fromChild.getValue() );
		}
    }


    public Iterable<Node> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    public EntityState getEntityState() {
        return entityState;
    }

    public void setEntityState(EntityState entityState) {
        this.entityState = entityState;
    }

    public int compareOptimisticLocks(Entity other) {
        if (other.getEntityType() != entityType) {
            throw new IllegalStateException("Invalid optimistic lock comparison, different entity types.");
        }
        @SuppressWarnings("unchecked")
        Comparable<Object> myValue = (Comparable<Object>)getOptimisticLockValue();
        if (myValue == null) {
            throw new IllegalStateException("Invalid optimistic lock comparison, null optimistic lock value.");
        }
        return myValue.compareTo(other.getOptimisticLockValue());
    }

    public ValueNode getOptimisticLock() {
      for (Node child: children.values()) {
        if (child.getNodeDefinition().isOptimisticLock()) {
            return (ValueNode)child;
        }
      }
      return null;
    }

    private Object getOptimisticLockValue() {
        ValueNode node = getOptimisticLock();
        return node != null ? node.getValue() : null;
    }


    public Element toXml(Document doc) {
        Element element = doc.createElement(entityType.getInterfaceName());
        element.setAttribute("state", entityState.name());
        if (getUuid() != null) {
            element.setAttribute("creationId", getUuid().toString());
        }
        for (Node child: children.values()) {
            Element el = child.toXml(doc);
            element.appendChild(el);
        }
        return element;
    }

    private void initNodes() {
        for (NodeDefinition nd: entityType.getNodeDefinitions()) {
            if (!children.containsKey( nd.getName() )) {
                children.put(nd.getName(), newChild(nd));
            }
        }
    }

    public void checkFetched() {
        if (entityState == EntityState.NOTLOADED && getKey().getValue() != null) {
            entityContext.fetch(this);
        }
    }

    private Node newChild(NodeDefinition nd) {
        if (nd.getRelationInterfaceName() != null && nd.getColumnName() != null) {
            //1:1 relationship
            return new RefNode(this, nd.getName(), entityType.getDefinitions().getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true));
        } else if (nd.getColumnName() != null) {
            //a value
            return new ValueNode(this, nd.getName());
        } else if (nd.getRelationInterfaceName() != null && nd.getForeignNodeName() != null) {
            //1:N relationship
            return new ToManyNode(this, nd.getName(), entityType.getDefinitions().getEntityTypeMatchingInterface(nd.getRelationInterfaceName(), true));
        } else {
            throw new IllegalStateException("Illegal node definition '" + nd + "'");
        }
    }

    public EntityContext getEntityContext() {
    	return entityContext;
    }

    public void handleEvent(NodeEvent event) {
    	entityContext.handleEvent(event);
    }

    public String getName() {
    	return getEntityType().getInterfaceShortName() + "." + getKey();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
	    oos.defaultWriteObject();
	    oos.writeUTF(entityType.getInterfaceName());
	}

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
	    ois.defaultReadObject();
	    String interfaceName = ois.readUTF();
	    entityType = entityContext.getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
	}


    @Override
    public String toString() {
        if (getKey().getValue() != null) {
            return getEntityType().getInterfaceShortName() + " [" + getKey().getName() + "=" + getKey().getValue() + "]";
        }
        else {
            return getEntityType().getInterfaceShortName() + " [uuid=" + getUuid().substring(0,  7) + "..]";
        }
    }


}
