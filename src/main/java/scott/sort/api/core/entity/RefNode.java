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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.sort.api.config.EntityType;

/**
 * A Node which refers to an EntityNode by it's EntityType and key *
 *
 * if a RefNode has a key
 * then the entity corresponding to that key will exist in the context
 * in a loaded or non-loaded state
 * @author scott
 *
 */
public final class RefNode extends Node {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(RefNode.class);

    /**
     * The type of entity we refer to
     */
    private EntityType entityType;

    /**
     * The key of the saved/loaded entity we are associated to.
     */
    private Object entityKey;

    /**
     * Holds the existing (in database) entity key which was removed by changing the reference.
     */
    private Object removedEntityKey;

    /**
     * The actual reference which this node returns.
     */
    private Entity reference;


    public RefNode(Entity parent, String name, EntityType entityType) {
        super(parent, name);
        this.entityType = entityType;
    }

    /**
     * Called when setting the entity to null or to an entity which has a key
     * @param newEntityKey
     */
    public void setEntityKey(Object newEntityKey) {
        if (Objects.equals(entityKey, newEntityKey)) {
            /*
             * Same key, do nothing
             */
            return;
        }
        /*
         * We were referring to an entity already so we need to remove this
         * reference from the entity context tracking.
         */
        if (entityKey != null) {
            getEntityContext().removeReference(this, getReference());
        }

        if (getEntityContext().isUser()) {
            /*
             * If we are in user mode then the model is being manipulated by
             * the user code and so we track the previous entityKey
             * so that we know what has been changed
             */
            if (removedEntityKey == null) {
                /*
                 * Once the removedEntityKey is set we never have to set it again
                 * because we are tracking the original entity which was removed from this reference.
                 *
                 * TODO: we have to make sure that removedEntityKey can only point to
                 * the original database entity
                 */
                removedEntityKey = entityKey;
            } else if (removedEntityKey.equals(newEntityKey)) {
                /*
                 * the removedEntityKey was restored by this operation.
                 */
                removedEntityKey = null;
            }
        }

        /*
         * Set the new key
         */
        entityKey = newEntityKey;
        if (entityKey != null) {
            /*
             * Create the entity in the context if it is not there yet.
             */
            reference = getEntityContext().getOrCreate(entityType, entityKey);
            /*
             * add the tracking to the entity context.
             */
            getEntityContext().addReference(this, reference);
        }
    }

    public boolean refersTo(EntityType entityType, Object key) {
        return entityType.equals(this.entityType) && entityKey != null && entityKey.equals(key);
    }

    public void setRemovedEntityKey(Object removedEntityKey) {
        this.removedEntityKey = removedEntityKey;
    }

    public Object getRemovedEntityKey() {
        return this.removedEntityKey;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Object getEntityKey() {
        return entityKey;
    }

    /**
     * clears any state just leaving the
     * reference key and reference, more like a reset really
     */
    public void clear() {
        reference = null;
        removedEntityKey = null;
        if (entityKey != null) {
            //get or create the corresponding entity in the context
            reference = getEntityContext().getOrCreate(entityType, entityKey);
        }
    }

    /**
     * Refresh the entity reference.
     */
    public void refresh() {}

    /**
     * The reference is being set, it can be either an entity which
     * exists in the database or a new entity
     * @param entity
     */
    public void setReference(Entity entity) {
        /*
         * Force ourselves to get fetched so that we will have the correct
         * initial state before changing the reference, so that removedEntityKey can be set.
         */
        getParent().checkFetched();

        /*
         * If the the entity matches our current reference then do nothing.
         */
        Entity origReference = getReference();
        if (origReference == entity) {
            return;
        }

        /*
         * If we have a current reference then stop tracking it in the context.
         */
        if (origReference != null) {
            getEntityContext().removeReference(this, origReference);
        }


        /*
         * Get the key of our original reference.
         */
        final Object origKey = origReference != null ? origReference.getKey().getValue() : null;
        /*
         * Get the key of our new reference.
         */
        final Object newKey = entity != null ? entity.getKey().getValue() : null;
        if (origKey != null && origKey == newKey) {
            return;
        }
        /*
         * Check if the reference is being set back to it's original one from the database.
         */
        if (newKey != null && newKey.equals(removedEntityKey)) {
            entityKey = removedEntityKey;
            removedEntityKey = null;
        }
        /*
         * set the reference to the new entity
         */
        this.reference = entity;
        if (reference != null) {
            getEntityContext().addReference(this, reference);
        }
    }

    @Override
    public void handleEvent(NodeEvent event) {
        if (event.getType() == NodeEvent.Type.KEYSET) {
            final Entity reference = getReference();
            if (reference != null && reference.getKey() == event.getSource()) {
                this.entityKey = ((ValueNode) event.getSource()).getValue();
                LOG.debug(getName() + " FK set to " + this.entityKey + " for " + getParent().getEntityType().getInterfaceShortName() + " with key " + getParent().getKey() + " and uuid " + getParent().getUuid());
            }
            else {
                LOG.debug("WHY WAS I CALLED");
            }
        }
    }

    /**
     * gets the entity refered to by this ref
     * which by definition must exist if we have an entityKey
     */
    public Entity getReference() {
        if (reference != null) {
            return reference;
        } else if (entityKey != null) {
            return reference = getEntityContext().getEntity(entityType, entityKey, true);
        }
        return null;
    }

    @Override
    public Entity getParent() {
        return (Entity) super.getParent();
    }

    @Override
    public Element toXml(Document doc) {
        Element element = doc.createElement(getName());
        element.setAttribute("key", String.valueOf(entityKey));
        Entity ref = getReference();
        if (ref != null) {
            if (removedEntityKey != null) {
                element.setAttribute("updated", "true");
            }
            if (ref.getUuid() != null) {
                element.setAttribute("uuid", ref.getUuid().toString());
            }
        }
        return element;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing reference to {}", this);
        oos.writeObject(entityKey);
        oos.writeUTF(entityType.getInterfaceName());
        oos.writeObject(reference);
        oos.writeObject(removedEntityKey);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        entityKey = ois.readObject();
        String interfaceName = ois.readUTF();
        entityType = getParent().getEntityContext().getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
        reference = (Entity)ois.readObject();
        removedEntityKey = ois.readObject();
        //trace at end once object is constructed
        LOG.trace("Deserialized reference to {}", this);
    }

    public void copyFrom(RefNode other) {
        this.entityKey = other.entityKey;
        this.reference = other.reference;
        this.removedEntityKey = other.removedEntityKey;
        if (this.reference  != null) {
            getEntityContext().addReference(this, this.reference);
        }
    }

    @Override
    public String toString() {
        Entity e = getReference();
        return String.valueOf(e);
    }

}
