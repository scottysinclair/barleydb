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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.sort.api.config.EntityType;
import scott.sort.api.config.NodeType;

/**
 * Contains information on how this node refers to many entities.
 * To look up matching entities in the node context we need
 * the entity type which we are referring to and the primary key of the entity which we belong to.
 * For example for the syntax.mappings ToManyNode this translates to the Mappings entityType and the syntax id
 */
public class ToManyNode extends Node {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ToManyNode.class);

    /**
     * The entity type that we refer to
     */
    private EntityType entityType;

    /*
     * tracks all entities that we reference currently
     */
    private List<Entity> entities;
    /*
     * only refers to new entities
     */
    private List<Entity> newEntities;
    /*
     * tracks entities which have been removed
     */
    private List<Entity> removedEntities;
    private boolean fetched;

    public ToManyNode(Entity parent, String name, EntityType entityType) {
        super(parent, name);
        this.entityType = entityType;
        this.entities = new LinkedList<Entity>();
        this.newEntities = new LinkedList<Entity>();
        this.removedEntities = new LinkedList<Entity>();
    }

    @Override
    public Element toXml(Document doc) {
        final Element element = doc.createElement(getName());
        element.setAttribute("fetched", String.valueOf(fetched));
        for (final Entity en : entities) {
            Element el = doc.createElement("ref");
            if (en.getKey().getValue() != null) {
                el.setAttribute("key", en.getKey().getValue().toString());
            }
            else {
                el.setAttribute("uuid", en.getUuid().toString());
            }
            element.appendChild(el);
        }
        return element;
    }

    public boolean isFetched() {
        return fetched;
    }

    public void setFetched(boolean fetched) {
        LOG.debug(getParent() + "." + getName() + "=" + this + " fetched == " + fetched);
        this.fetched = fetched;
    }

    public void clear() {
        this.entities.clear();
        this.newEntities.clear();
        this.removedEntities.clear();
    }

    public void refresh() {
        if (!isFetched()) {
            return;
        }

        /*
         * if the new entities have keys then remove them from our newEntities list
         */
        for (Iterator<Entity> i = newEntities.iterator(); i.hasNext();) {
            if (i.next().getKey().getValue() != null) {
                i.remove();
            }
        }
        /*
         * if the removed entities have no keys then remove them from our removedEntities list
         */
        for (Iterator<Entity> i = removedEntities.iterator(); i.hasNext();) {
            Entity e = i.next();
            if (e.getKey().getValue() == null) {
                i.remove();
            }
        }
        /*
         * do the refresh
         */
        if (getParent().getKey().getValue() != null) {
            List<Entity> result = Collections.emptyList();
            if (getNodeType().getForeignNodeName() != null) {
                result = getEntityContext().getEntitiesWithReferenceKey(
                        entityType,
                        getNodeType().getForeignNodeName(),
                        getParent().getEntityType(),
                        getParent().getKey().getValue());

                result.removeAll(removedEntities);
            }

            /*
             * We only touch the entities list if something has changes, this prevents
             * needless concurrent modification exceptions.
             */
            List<Entity> refreshedEntities = new LinkedList<>(result);
            refreshedEntities.addAll(newEntities);
            if (entities.size() != refreshedEntities.size() || !entities.containsAll(refreshedEntities)) {
                entities.clear();
                entities.addAll(refreshedEntities);
                if (entities.size() > 0) {
                    //the list of entities must have a consistent natural order
                    //natural order means based on the PK
                    Collections.sort(entities, new Comparator<Entity>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public int compare(Entity o1, Entity o2) {
                            if (o1.getKey().getValue() != null) {
                                if (o2.getKey().getValue() == null) return 1;
                                else return ((Comparable<Object>) o1.getKey().getValue()).compareTo(o2.getKey().getValue());
                            }
                            else if (o2.getKey().getValue() == null) return 0;
                            else return -1;
                        }
                    });
                }
                if (result.isEmpty()) {
                    LOG.debug("no entities for " + getParent() + "." + getName() + "=" + this);
                }
                else {
                    LOG.debug("resolved " + result.size() + " entities for " + getParent() + "." + getName() + "=" + this);
                }
            }
        }
    }

    public void add(int index, Entity entity) {
        if (entity.getKey().getValue() == null) {
            newEntities.add(entity);
        }
        entities.add(index, entity);
    }

    public void add(Entity entity) {
        if (entities.contains(entity)) {
            throw new IllegalStateException("ToMany relation already contains '" + entity + "'");
        }
        if (entity.getEntityType() != entityType) {
            throw new IllegalStateException("Cannot add " + entity.getEntityType() + " to " + getParent() + "." + getName());
        }
        if (entity.getKey().getValue() == null) {
            newEntities.add(entity);
        }
        entities.add(entity);
    }

    public List<Entity> getList() {
        return entities;
    }

    public Entity remove(int index) {
        Entity entity = entities.remove(index);
        if (entity != null) {
            newEntities.remove(entity);
            if (entity.getKey().getValue() != null) {
                removedEntities.add(entity);
            }
        }
        return entity;
    }

    public List<Entity> getNewEntities() {
        return newEntities;
    }

    public List<Entity> getRemovedEntities() {
        return removedEntities;
    }

    @Override
    public String toString() {
        return getList().toString();
    }

    @Override
    public Entity getParent() {
        return (Entity) super.getParent();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public NodeType getNodeType() {
        return getParent().getEntityType().getNodeType(getName(), true);
    }

    public void copyFrom(ToManyNode other) {
        this.fetched = other.fetched;
        this.entities = new LinkedList<Entity>(other.entities);
        this.newEntities = new LinkedList<Entity>(other.newEntities);
        this.removedEntities = new LinkedList<Entity>(other.removedEntities);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing many references {}", this);
        oos.writeUTF(entityType.getInterfaceName());
        oos.writeBoolean(fetched);
        oos.writeObject(entities);
        oos.writeObject(newEntities);
        oos.writeObject(removedEntities);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        String interfaceName = ois.readUTF();
        entityType = getEntityContext().getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
        fetched = ois.readBoolean();
        entities = (List<Entity>)ois.readObject();
        newEntities = (List<Entity>)ois.readObject();
        removedEntities = (List<Entity>)ois.readObject();
        //trace at end once object is constructed
        LOG.trace("Deserialized many references {}", this);
   }

}
