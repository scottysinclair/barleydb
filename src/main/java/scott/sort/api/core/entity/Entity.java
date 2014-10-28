package scott.sort.api.core.entity;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeDefinition;

public class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(Entity.class);

    private EntityContext entityContext;
    private EntityType entityType;
    private TreeMap<String, Node> children;
    private EntityState entityState;
    private UUID uuid;

    /**
     * Copy constructor, a new version of the entity in a different context with the same uuid
     * @param context
     * @param toCopy
     */
    public Entity(EntityContext context, Entity toCopy) {
        this(context, toCopy.getEntityType(), toCopy.getKey().getValue(), toCopy.getUuid());
    }

    public Entity(EntityContext context, EntityType entityType) {
        this(context, entityType, null, UUID.randomUUID());
    }

    public Entity(EntityContext context, EntityType entityType, Object key) {
        this(context, entityType, key, UUID.randomUUID());
    }

    public Entity(EntityContext context, EntityType entityType, Object key, UUID uuid) {
        this.entityContext = context;
        this.entityType = entityType;
        this.children = new TreeMap<String, Node>();
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

    /**
     * Sets the nodes state to unloaded
     * Means only the key has a value and everything else
     * must be fetched
     */
    public void unload() {
        for (Node node : getChildren()) {
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
        for (Node node : getChildren()) {
            if (node instanceof RefNode) {
                ((RefNode) node).clear();
            }
            else if (node instanceof ToManyNode) {
                ((ToManyNode) node).clear();
            }
        }
    }

    public void refresh() {
        for (Node node : getChildren()) {
            if (node instanceof RefNode) {
                ((RefNode) node).refresh();
            }
            else if (node instanceof ToManyNode) {
                ((ToManyNode) node).refresh();
            }
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public boolean isOfType(EntityType entityType) {
        return this.entityType.equals(entityType);
    }

    public boolean hasKey(final Object key) {
        return key != null && key.equals(getKey().getValue());
    }

    public final ValueNode getKey() {
        return getChild(entityType.getKeyNodeName(), ValueNode.class);
    }

    public void setValueNode(String name, Object value) {
        getChild(name, ValueNode.class).setValue(value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T getChild(String name, Class<T> type) {
        return (T) children.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T getChild(String name, Class<T> type, boolean mustExist) {
        T child = (T) children.get(name);
        if (child == null && mustExist) {
            throw new IllegalStateException("Node '" + name + "' must exist in entity " + entityType.getInterfaceName());
        }
        return child;
    }

    @SuppressWarnings("unchecked")
    public <N extends Node> List<N> getChildren(Class<N> type) {
        List<N> result = new LinkedList<N>();
        for (Node child : getChildren()) {
            if (type.isAssignableFrom(child.getClass())) {
                result.add((N) child);
            }
        }
        return result;
    }

    public void downcast(EntityType newEntityType) {
        LOG.debug("Downcasting entity {} to type {}", this, newEntityType);
        List<Node> toAdd = new LinkedList<Node>();
        for (Iterator<Node> i = children.values().iterator(); i.hasNext();) {
            Node existing = i.next();
            if (existing instanceof ValueNode) {
                NodeDefinition ndNew = newEntityType.getNode(existing.getName(), true);
                if (ndNew.isForeignKey()) {
                    i.remove();
                    //tricky: creating a refnode with this entity and the newEntityType
                    //which is NOT YET associated with this entity
                    //this is unusual but required for example for abstract syntaxes
                    //which refer to concrete structures from other tables
                    LOG.debug("Converting ValueNode to RefNode for {}", ndNew.getName());
                    EntityType refNodeType = entityContext.getDefinitions().getEntityTypeMatchingInterface(ndNew.getRelationInterfaceName(), true);
                    RefNode refNode = new RefNode(this, ndNew.getName(), refNodeType);
                    refNode.setEntityKey(((ValueNode) existing).getValue());
                    toAdd.add(refNode);
                }
            }
        }
        for (Node n : toAdd) {
            children.put(n.getName(), n);
        }
        this.entityType = newEntityType;
        for (Node newNode : initNodes()) {
            if (newNode instanceof RefNode) {
                throw new IllegalStateException("new child ref node");
            }
            if (newNode instanceof ValueNode) {
                ((ValueNode) newNode).setValueNoEvent(NotLoaded.VALUE);
            }
        }
    }

    /**
     * Copies the values nodes from the input entity to this object
     * @param from
     */
    public void copyValueNodesToMe(Entity from) {
        for (ValueNode fromChild : from.getChildren(ValueNode.class)) {
            ValueNode toChild = getChild(fromChild.getName(), ValueNode.class);
            toChild.setValue(fromChild.getValue());
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
        Comparable<Object> myValue = (Comparable<Object>) getOptimisticLockValue();
        if (myValue == null) {
            throw new IllegalStateException("Invalid optimistic lock comparison, null optimistic lock value.");
        }
        return myValue.compareTo(other.getOptimisticLockValue());
    }

    public ValueNode getOptimisticLock() {
        for (Node child : children.values()) {
            if (child.getNodeDefinition().isOptimisticLock()) {
                return (ValueNode) child;
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
        for (Node child : children.values()) {
            Element el = child.toXml(doc);
            element.appendChild(el);
        }
        return element;
    }

    private List<Node> initNodes() {
        List<Node> newNodes = new LinkedList<Node>();
        for (NodeDefinition nd : entityType.getNodeDefinitions()) {
            if (!children.containsKey(nd.getName())) {
                Node node = newChild(nd);
                newNodes.add(node);
                children.put(nd.getName(), node);
            }
        }
        return newNodes;
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
        LOG.trace("Serializing entity {}", this);
        oos.writeObject(entityContext);
        oos.writeObject(entityState);
        oos.writeUTF(entityType.getInterfaceName());
        oos.writeObject(uuid);
        oos.writeObject(getKey().getValue());
        oos.writeObject(children);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        /*
         * Get the basic data
         */
        entityContext = (EntityContext)ois.readObject();
        entityState = (EntityState)ois.readObject();
        entityType = entityContext.getDefinitions().getEntityTypeMatchingInterface( ois.readUTF(), true);
        uuid = (UUID)ois.readObject();
        /*
         * Initialize nodes with no values so that the entity is in a better state to be added to the context
         */
        children = new TreeMap<String, Node>();
        initNodes();
        /*
         * Set the primary key
         */
        getKey().setValueNoEvent( ois.readObject() );
        /*
         * Add the entity to the context (UUID and PK lookup will work).
         */
        entityContext.add(this);
        /*
         * Copy the node values across
         */
        EntityContextState state = entityContext.getEntityContextState();
        try {
            entityContext.beginLoading();
            for (Node child: ((Map<String, Node>)ois.readObject()).values()) {
                if (child instanceof ValueNode) {
                    ValueNode fromStream = (ValueNode)child;
                    ValueNode ours = getChild(fromStream.getName(), ValueNode.class, true);
                    /*
                     * If a PK then this would set the PK to the existing value which it has
                     * no event for FK references is required.
                     */
                    ours.copyFrom(fromStream);
                }
                else if (child instanceof RefNode) {
                    RefNode fromStream = (RefNode)child;
                    RefNode ours = getChild(fromStream.getName(), RefNode.class, true);
                    ours.copyFrom(fromStream);
                }
                else if (child instanceof ToManyNode) {
                    ToManyNode fromStream = (ToManyNode)child;
                    ToManyNode ours = getChild(fromStream.getName(), ToManyNode.class, true);
                    ours.copyFrom(fromStream);
                }
            }
            //trace at end once object is constructed
            LOG.trace("Deserializing entity {}", this);
        }
        finally {
            entityContext.setEntityContextState(state);
        }
    }

    @Override
    public String toString() {
        if (getKey().getValue() != null) {
            return getEntityType().getInterfaceShortName() + " [" + getKey().getName() + "=" + getKey().getValue() + "]";
        }
        else {
            return getEntityType().getInterfaceShortName() + " [uuid=" + getUuid().toString().substring(0, 7) + "..]";
        }
    }

}
