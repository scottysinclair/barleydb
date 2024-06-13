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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.stream.QueryEntityDataInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;

/**
 * Contains information on how this node refers to many entities.
 * To look up matching entities in the node context we need
 * the entity type which we are referring to and the primary key of the entity which we belong to.
 * For example for the syntax.mappings ToManyNode this translates to the Mappings entityType and the syntax id.
 *
 * The ToMany node also tracks which entities have been deleted from the list and which new entities have been added to the list.
 *
 *
 */
public class ToManyNode extends Node {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ToManyNode.class);

    /**
     * The entity type that we refer to
     */
    private EntityType entityType;

    /**
     * tracks all entities that we reference currently
     */
    private List<Entity> entities;
    /**
     * only refers to new entities
     */
    private List<Entity> newEntities;

    private boolean fetched;

    public ToManyNode(Entity parent, String name, EntityType entityType) {
        super(parent, name);
        this.entityType = entityType;
        this.entities = new LinkedList<Entity>();
        this.newEntities = new LinkedList<Entity>();
    }

    @Override
    public Element toXml(Document doc) {
        final Element element = doc.createElement(getName());
        element.setAttribute("fetched", String.valueOf(fetched));
        for (final Entity en : entities) {
            Element el = doc.createElement("ref");
            if (en.getKeyValue() != null) {
                el.setAttribute("key", en.getKeyValue().toString());
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

    public void unloadAndClear() {
        entities.clear();
        newEntities.clear();
        fetched = false;
    }

    public void setFetched(boolean fetched) {
        LOG.debug(getParent() + "." + getName() + "=" + this + " fetched == " + fetched);
        this.fetched = fetched;
    }

    public void refresh() {
        if (!isFetched()) {
            return;
        }

        /*
         * remove entities from the newEntities list which are no longer new.
         */
        for (Iterator<Entity> i = newEntities.iterator(); i.hasNext();) {
            Entity e = i.next();
            if (!e.isClearlyNotInDatabase()) {
                LOG.trace("ToManyNode {} has new entity {} which is now saved, removing from newEntities list", this, e);
                i.remove();
            }
        }

        /*
         * do the refresh
         */
        if (getParent().getKeyValue() != null) {
            List<Entity> result = Collections.emptyList();
            if (getNodeType().getForeignNodeName() != null) {
                result = getEntityContext().getEntitiesWithReferenceKey(
                        entityType,
                        getNodeType().getForeignNodeName(),
                        getParent().getEntityType(),
                        getParent().getKeyValue());

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
                    //the list of entities must have a consistent order
                    //we sort on the sort column or the PK if not specified.
                    List<String> sortNodeNames = new LinkedList<>();
                    if (getNodeType().getSortNode() != null) {
                        sortNodeNames.add(getNodeType().getSortNode());
                    }
                    if (sortNodeNames.isEmpty()) {
                        sortNodeNames = entityType.getKeyNodeNames();
                    }
                    MyComparator comparator = null;
                    Collections.reverse(sortNodeNames);
                   for (String sortNodeName : sortNodeNames) {
                      comparator = new MyComparator(sortNodeName, comparator);
                   }
                   Collections.sort(entities, comparator);
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


    public boolean contains(Entity entity) {
        return entities.contains(entity);
    }

    /**
     * We are adding an entity to this list.
     *
     * @param index
     * @param entity
     */
    public void add(int index, Entity entity) {
        if (entities.contains(entity)) {
            throw new IllegalStateException("ToMany relation already contains '" + entity + "'");
        }
        if (entity.getEntityType() != entityType) {
            throw new IllegalStateException("Cannot add " + entity.getEntityType() + " to " + getParent() + "." + getName());
        }
        if (entity.isClearlyNotInDatabase()) {
            newEntities.add(entity);
        }
        entities.add(index, entity);
    }

    public void addIfAbsent(Entity e) {
      if (!entities.contains(e)) {
        add(e);
      }
    }

    public void add(Entity entity) {
        if (entities.contains(entity)) {
            throw new IllegalStateException("ToMany relation already contains '" + entity + "'");
        }
        if (entity.getEntityType() != entityType) {
            throw new IllegalStateException("Cannot add " + entity.getEntityType() + " to " + getParent() + "." + getName());
        }
        if (entity.isClearlyNotInDatabase()) {
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
        }
        return entity;
    }

    public List<Entity> getNewEntities() {
        return newEntities;
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

    /**
     *
     * @return a QueryEntityInputStream to stream the the data from the N relation.
     * @throws SortServiceProviderException
     * @throws BarleyDBQueryException
     */
    public QueryEntityInputStream stream() throws SortServiceProviderException, BarleyDBQueryException {
        QueryObject<Object> query = getEntityContext().getUnitQuery(entityType);
        return stream(query);
    }

    /**
    *
    * @return a QueryEntityInputStream to stream the the data from the N relation.
    * @throws SortServiceProviderException
    * @throws BarleyDBQueryException
    */
    public QueryEntityInputStream stream(QueryObject<?> query) throws SortServiceProviderException, BarleyDBQueryException {
        EntityContext ctx = getEntityContext();

        final String foreignNodeName = getNodeType().getForeignNodeName();
        if (foreignNodeName != null) {
            final QProperty<Object> manyFk = new QProperty<Object>(query, foreignNodeName);
            final Object primaryKeyOfOneSide = getParent().getKeyValue();
            query.where(manyFk.equal(primaryKeyOfOneSide));
        }
        else {
            throw new IllegalStateException("streaming over join tables directly from a tomany node is not yet supported, stream via the entity context instead.");
        }
        QueryEntityDataInputStream in  = ctx.streamQueryEntityData(query, null);
        return new QueryEntityInputStream(in, ctx, false);
    }

    public void copyFrom(ToManyNode other) {
        this.fetched = other.fetched;
        this.entities = new LinkedList<Entity>(other.entities);
        this.newEntities = new LinkedList<Entity>(other.newEntities);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing many references {}", this);
        oos.writeUTF(entityType.getInterfaceName());
        oos.writeBoolean(fetched);
        oos.writeObject(entities);
        oos.writeObject(newEntities);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        String interfaceName = ois.readUTF();
        entityType = getEntityContext().getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
        fetched = ois.readBoolean();
        entities = (List<Entity>)ois.readObject();
        newEntities = (List<Entity>)ois.readObject();
        //trace at end once object is constructed
        LOG.trace("Deserialized many references {}", this);
   }

    /**
     * Provides standard sorting for the list in the to many relation.
     * @author scott
     *
     */
    private final class MyComparator implements Comparator<Entity> {
        private final String sortNodeName;

        private final MyComparator next;

        public MyComparator(String sortNodeName, MyComparator next) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Comparing entities for {} according to {}", getNodeType().getShortId(), sortNodeName);
            }
            this.sortNodeName = sortNodeName;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compare(Entity o1, Entity o2) {
            Object value1 = getValue(o1, sortNodeName);
            Object value2 = getValue(o2, sortNodeName);
            if (value1 != null) {
                if (value2 == null) return 1;
                else {
                    int result = ((Comparable<Object>) value1).compareTo(value2);
                    if (result == 0) {
                        if (next != null) {
                            return next.compare(o1, o2);
                        }
                        else {
                            return 0;
                        }
                    }
                    else return result;
                }
            }
            else if (value2 == null) {
                if (next != null) {
                    return next.compare(o1, o2);
                }
                else {
                    return 0;
                }
            }
            else return -1;
        }

        private Object getValue(Entity entity, String sortNodeName) {
            Node node = entity.getChild(sortNodeName, Node.class);
            if (node instanceof ValueNode) {
                return ((ValueNode) node).getValue();
            }
            else if (node instanceof RefNode) {
                return ((RefNode) node).getEntityKey();
            }
            else {
                return null;
            }
        }
    }
}

