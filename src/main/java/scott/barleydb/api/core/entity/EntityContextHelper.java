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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;

public class EntityContextHelper {

    public interface Predicate {
        boolean matches(Entity entity);
    }

    @SafeVarargs
    public static Collection<Entity> flatten(Collection<Entity> ...colColEntities) {
        LinkedHashSet<Entity> result = new LinkedHashSet<>();
        for (Collection<Entity>  col: colColEntities) {
          result.addAll(col);
        }
        return result;
    }

    /**
     * Navigates the object graph for each entity in the collection of entities.  returns the entities which match the predicate.
     * @param entities the collection of entities to navigate
     * @param predicate he predicate
     * @return
     */
    public static LinkedHashSet<Entity> findEntites(Collection<Entity> entities, Predicate predicate) {
        LinkedHashSet<Entity> matches = new LinkedHashSet<>();
        HashSet<Entity> checked = new HashSet<>();
        for (Entity entity: entities) {
            findEntites(matches, checked, entity, predicate);
        }
        return matches;
    }

    /**
     * Finds all entities by navigating the object graph from entity which match the given predicate.<br/>
     *
     * @param matches
     * @param checked
     * @param entity
     * @param predicate
     * @return
     */
    public static LinkedHashSet<Entity> findEntites(LinkedHashSet<Entity> matches, HashSet<Entity> checked, Entity entity, Predicate predicate) {
        if (!checked.add( entity )) {
            return matches;
        }
        if (predicate.matches( entity )) {
            matches.add(entity);
        }
        for (RefNode refNode: entity.getChildren(RefNode.class)) {
            Entity e = refNode.getReference();
            if (e != null) {
                findEntites(matches, checked, e, predicate);
            }
        }
        for (ToManyNode toManyNode: entity.getChildren(ToManyNode.class)) {
            for (Entity e: toManyNode.getList()) {
                if (e != null) {
                    findEntites(matches, checked, e, predicate);
                }
            }
        }
        return matches;
    }

    public static int countLoaded(Collection<Entity> entities) {
        int count = 0;
        for (Entity e: entities) {
            if (e.isLoaded()) {
                count++;
            }
        }
        return count;
    }

    public static int countNotLoaded(Collection<Entity> entities) {
        int count = 0;
        for (Entity e: entities) {
            if (e.isFetchRequired()) {
                count++;
            }
        }
        return count;
    }

    public static int countNew(Collection<Entity> entities) {
        int count = 0;
        for (Entity e: entities) {
            if (e.isClearlyNotInDatabase()) {
                count++;
            }
        }
        return count;
    }

    public static Collection<Entity> toParents(Collection<RefNode> fkReferences) {
        Collection<Entity> list = new LinkedList<Entity>();
        for (RefNode refNode : fkReferences) {
            list.add(refNode.getParent());
        }
        return list;
    }

    /**
     * Adds the set of entities assuming that the set will respect the fetched status of
     * tomany relations, ie if the set contains an entitiy with fetched == true for a ToManyNode
     * then the many entities are also in the list
     * @param entities
     * @param newContext
     */
    public static List<Entity> addEntities(Iterable<Entity> entities, EntityContext newContext) {
        List<Entity> copiedEntities = new LinkedList<Entity>();
        for (Entity entity : entities) {
            //don't add entities which are not yet loaded
            if (entity.getKey().getValue() != null && entity.isFetchRequired()) {
                continue;
            }
            Entity e = newContext.getEntityByUuidOrKey(entity.getUuid(), entity.getEntityType(), entity.getKey().getValue(), false);
            if (e == null) {
                e = new Entity(newContext, entity);
                newContext.add(e);
            }
            e.setEntityState(entity.getEntityState());
            e.copyValueNodesToMe(entity);
            copiedEntities.add(e);
        }
        return copiedEntities;
    }

    /**
     * Applies the changes to the new context
     * @param entities
     * @param newContext
     */
    public static List<Entity> applyChanges(List<Entity> entities, EntityContext newContext, EntityFilter filter) {
        List<Entity> newEntities = new LinkedList<Entity>();
        for (Entity entity : entities) {
            if (!filter.includesEntity(entity)) {
                continue;
            }
            Entity e = newContext.getEntityByUuidOrKey(entity.getUuid(), entity.getEntityType(), entity.getKey().getValue(), true);
            e.copyValueNodesToMe(entity);
            e.getConstraints().set( entity.getConstraints() );
            e.setEntityState(entity.getEntityState());
            newEntities.add(e);
        }
        return newEntities;
    }

    public interface EntityFilter {
        public boolean includesEntity(Entity entity);
    }

    /**
     * Copies the ref states from entityContext to newContext
     * @param entityContext
     * @param newContext
     */
    public static void copyRefStates(EntityContext entityContext, EntityContext newContext, Iterable<Entity> newEntities, EntityFilter entityFilter) {
        //we need to iterate without concurrent modification errors, when ref ids are set

        //we need to process all refs first as the tomany refs depend on the refs
        for (Entity entity : newEntities) {
            Entity orig = entityContext.getEntityByUuidOrKey(entity.getUuid(), entity.getEntityType(), entity.getKey().getValue(), true);
            for (RefNode refNode : entity.getChildren(RefNode.class)) {
                RefNode origRefNode = orig.getChild(refNode.getName(), RefNode.class);
                if (!refNode.getEntityType().equals(origRefNode.getEntityType())) {
                    throw new IllegalStateException("CopyRefStatesFailed: entity " + origRefNode.getParent() + " has ref " + origRefNode.getName() + " with the wrong entity type: " + origRefNode.getEntityType());
                }

                refNode.setEntityKey(origRefNode.getEntityKey());
                /*
                 * If we have an actual entity on the reference then set it on refNode if it is included.
                 */
                Entity origRefE = origRefNode.getReference();
                if (origRefE != null && entityFilter.includesEntity(origRefE)) {
                    Entity e = newContext.getEntityByUuidOrKey(origRefE.getUuid(), origRefE.getEntityType(), origRefE.getKey().getValue(), true);
                    refNode.setReference(e);
                }
            }
        }
        for (Entity entity : newEntities) {
            Entity orig = entityContext.getEntityByUuidOrKey(entity.getUuid(), entity.getEntityType(), entity.getKey().getValue(), true);
            for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
                ToManyNode origToManyNode = orig.getChild(toManyNode.getName(), ToManyNode.class);
                /*
                                LOG.debug(" ------------------------------------------- ");
                                LOG.debug("copyRefState: fetched  orig: " + origToManyNode.toString() + " " + origToManyNode.isFetched());
                                LOG.debug("copyRefState: entities orig: " + origToManyNode.toString() + " " + origToManyNode.getList());
                                LOG.debug("copyRefState: removed orig: " + origToManyNode.toString() + " " + origToManyNode.getRemovedEntities());
                                LOG.debug("copyRefState: fetched  dest: " + toManyNode.toString() + " " + toManyNode.isFetched());
                                LOG.debug("copyRefState: entities dest: " + toManyNode.toString() + " " + toManyNode.getList());
                                LOG.debug("copyRefState: removed dest: " + toManyNode.toString() + " " + toManyNode.getRemovedEntities());
                */

                /*
                 * If the original tomany node is fetched then our one will be too.
                 *
                 * If toManyNode is already fetched then should leave it, it
                 * should not be set to false
                 */
                if (!toManyNode.isFetched()) {
                    toManyNode.setFetched(origToManyNode.isFetched());
                }

                /*
                 * Add any new entities to the destination ToManyRef
                 */
                Set<Entity> processed = new HashSet<>();
                if (containsIncludedNewRefs(origToManyNode, entityFilter)) {
                    for (Entity e : origToManyNode.getNewEntities()) {
                        if (!processed.add(e)) {
                            continue;
                        }
                        Entity e2 = newContext.getEntityByUuidOrKey(e.getUuid(), e.getEntityType(), e.getKey().getValue(), true);
                        if (!toManyNode.contains( e2 )) {
                            toManyNode.add(e2);
                        }
                    }
                }

                //looks up the entities in  it's own context
                toManyNode.refresh();
            }
        }
    }

    private static boolean containsIncludedNewRefs(ToManyNode toManyNode, EntityFilter entityFilter) {
        int countIncludes = 0, countExcludes = 0;
        for (Entity e : toManyNode.getNewEntities()) {
            if (entityFilter.includesEntity(e)) {
                countIncludes++;
            }
            else {
                countExcludes++;
            }
        }
        if (countExcludes > 0 && countIncludes > 0) {
            throw new IllegalStateException("A ToMany nodes references must all be included or all excluded '" + toManyNode + "'");
        }
        return countIncludes > 0;
    }

}
