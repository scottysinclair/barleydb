package scott.barleydb.api.core.entity;

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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.RefNode;

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
     * The actual reference which this node returns.
     */
    private Entity reference;

    private boolean loaded = true;


    public RefNode(Entity parent, String name, EntityType entityType) {
        super(parent, name);
        this.entityType = entityType;
    }

    public Object getEntityKey() {
        return reference != null ? reference.getKey().getValue() : null;
    }

    private void checkFetched() {
        if (getParent().getEntityState() == EntityState.LOADING) {
            return;
        }
        getParent().fetchIfRequiredAndAllowed();
        //perhaps the entity was fetched, but this column was lazy
        if (!loaded) {
            //will only fetch if not in internal mode
            //we assume this fetch will fix the value
            getEntityContext().fetch(getParent(), false, true, true, getName());
        }
        if (!loaded) {
            LOG.warn("RefNode entity key still not loaded for entity {}", getParent());
        }
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public EntityType getEntityType() {
        return entityType;
    }

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
        checkFetched();

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
         * set the reference to the new entity
         */
        this.reference = entity;
        if (reference != null) {
            getEntityContext().addReference(this, reference);
        }
    }

    /**
     * gets the entity refered to by this ref
     * which by definition must exist if we have an entityKey
     */
    public Entity getReference() {
        return getReference(true);
    }

    public Entity getReference(boolean checkFetch) {
        if (checkFetch) {
            checkFetched();
        }
        return reference;
    }

    @Override
    public Entity getParent() {
        return (Entity) super.getParent();
    }

    @Override
    public Element toXml(Document doc) {
        Element element = doc.createElement(getName());
        Entity ref = getReference(false);
        if (!loaded) {
            element.setAttribute("loaded", "false");
        }
        if (ref != null) {
            if (ref.getKey().getValue() != null) {
                element.setAttribute("key", String.valueOf( ref.getKey().getValue() ));
            }
            else if (ref.getUuid() != null) {
                element.setAttribute("uuid", ref.getUuid().toString());
            }
        }
        return element;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing reference to {}", this);
        oos.writeUTF(entityType.getInterfaceName());
        oos.writeBoolean(loaded);
        oos.writeObject(reference);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        String interfaceName = ois.readUTF();
        entityType = getParent().getEntityContext().getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
        loaded = ois.readBoolean();
        reference = (Entity)ois.readObject();
        //trace at end once object is constructed
        LOG.trace("Deserialized reference to {}", this);
    }

    public void copyFrom(RefNode other) {
        this.reference = other.reference;
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
