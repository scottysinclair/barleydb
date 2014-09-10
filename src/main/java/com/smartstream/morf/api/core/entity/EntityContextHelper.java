package com.smartstream.morf.api.core.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EntityContextHelper {

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
            if (entity.getKey().getValue() != null && entity.isNotLoaded()) {
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

    public static List<Entity> removeEntities(List<Entity> entities, EntityContext newContext, boolean setDeleted) {
        List<Entity> toRemove = new LinkedList<>();
        for (Entity entity : entities) {
            Entity e = newContext.getEntityByUuidOrKey(entity.getUuid(), entity.getEntityType(), entity.getKey().getValue(), false);
            if (e != null) {
                toRemove.add(e);
                if (setDeleted) {
                    newContext.fireDeleted(e);
                    e.setDeleted();
                }
            }
        }
        newContext.remove(toRemove);
        return toRemove;
    }

    /**
     * Equivalent entities must exist in otherContext
     * @param entities
     * @param newContext
     */
    public static List<Entity> applyChanges(List<Entity> entities, EntityContext newContext) {
        List<Entity> newEntities = new LinkedList<Entity>();
        for (Entity entity : entities) {
            Entity e = newContext.getEntityByUuidOrKey(entity.getUuid(), entity.getEntityType(), entity.getKey().getValue(), true);
            e.copyValueNodesToMe(entity);
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
                refNode.setEntityKey(origRefNode.getEntityKey());
                refNode.setRemovedEntityKey(origRefNode.getRemovedEntityKey());
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
                        toManyNode.add(newContext.getEntityByUuidOrKey(e.getUuid(), e.getEntityType(), e.getKey().getValue(), true));
                    }
                }

                /*
                 * Add any remove entities to the destination ToManyRef
                 */
                if (containsIncludedRemovedRefs(origToManyNode, entityFilter)) {
                    for (Entity e : origToManyNode.getRemovedEntities()) {
                        if (!processed.add(e)) {
                            continue;
                        }
                        if (!entityFilter.includesEntity(e)) {
                            throw new IllegalStateException("Cannot copy over a removed entity in a ToMany node, the removed entity is not included in the copy");
                        }
                        toManyNode.getRemovedEntities().add(newContext.getEntityByUuidOrKey(e.getUuid(), e.getEntityType(), e.getKey().getValue(), true));
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

    private static boolean containsIncludedRemovedRefs(ToManyNode toManyNode, EntityFilter entityFilter) {
        int countIncludes = 0, countExcludes = 0;
        for (Entity e : toManyNode.getRemovedEntities()) {
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
