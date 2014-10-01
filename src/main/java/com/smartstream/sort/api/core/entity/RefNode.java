package com.smartstream.sort.api.core.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.smartstream.sort.api.config.EntityType;

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
    private transient EntityType entityType;

    /**
     * The key of the saved/loaded entity we are associated to.
     */
    private Object entityKey;

    /**
     * Holds the saved / loaded entity key which was removed from this association.
     */
    private Object removedEntityKey;

    /**
     * The actual reference which this node returns.
     */
    private Entity reference;

    /**
     * The reference which was set by the user
     */
    private Entity updatedReference;

    public RefNode(Entity parent, String name, EntityType entityType) {
        super(parent.getEntityContext(), parent, name);
        this.entityType = entityType;
    }

    public void setEntityKey(Object newEntityKey) {
        if (Objects.equals(entityKey, newEntityKey)) {
            return;
        }
        if (entityKey != null) {
            getEntityContext().removeReference(this, getReference());
        }

        if (getEntityContext().isUser()) {
            /*
             * If we are in user mode then we track the previous entityKey
             * so that we know what has been changed
             */
            if (removedEntityKey == null) {
                removedEntityKey = entityKey;
            } else if (removedEntityKey.equals(newEntityKey)) {
                //reversing the removal of the orignal key
                removedEntityKey = null;
            }
        }
        entityKey = newEntityKey;
        if (entityKey != null) {
            //get or create the corresponding entity in the context
            reference = getEntityContext().getOrCreate(entityType, entityKey);
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
        reference = updatedReference = null;
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

    public void setReference(Entity entity) {
        getParent().checkFetched(); //TODO:why do we check if the parent is fetched here?
        Entity origReference = getReference();
        if (origReference == entity) {
            return;
        }
        final Object origKey = origReference != null ? origReference.getKey().getValue() : null;
        final Object newKey = entity != null ? entity.getKey().getValue() : null;
        if (origKey != null && origKey == newKey) {
            return;
        }
        if (newKey != null && newKey.equals(removedEntityKey)) {
            /*
             * The reference is being set back to it's original one
             * usually the key is set before the reference so this logic would not be reached
             */
            if (origReference != null) {
                getEntityContext().removeReference(this, origReference);
            }
            entityKey = removedEntityKey;
            removedEntityKey = null;
            if (entity != null) {
                getEntityContext().addReference(this, entity);
            }
            updatedReference = null;
            return;
        }
        /*
         * the reference is really being updated
         */
        if (origReference != null) {
            getEntityContext().removeReference(this, origReference);
        }
        this.updatedReference = entity;
        if (updatedReference != null) {
            getEntityContext().addReference(this, updatedReference);
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
        if (updatedReference != null) {
            return updatedReference;
        } else if (reference != null) {
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
            if (ref == updatedReference) {
                element.setAttribute("updated", "true");
            }
            if (ref.getUuid() != null) {
                element.setAttribute("uuid", ref.getUuid().toString());
            }
        }
        return element;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeUTF(entityType.getInterfaceName());
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        String interfaceName = ois.readUTF();
        entityType = getEntityContext().getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
    }

    @Override
    public String toString() {
        Entity e = getReference();
        return String.valueOf(e);
    }

}
