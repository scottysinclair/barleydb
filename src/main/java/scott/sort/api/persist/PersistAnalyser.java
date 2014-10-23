package scott.sort.api.persist;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import java.io.Serializable;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;
import scott.sort.api.core.entity.EntityContextHelper;
import scott.sort.api.core.entity.ProxyController;
import scott.sort.api.core.entity.RefNode;
import scott.sort.api.core.entity.ToManyNode;
import scott.sort.api.exception.execution.persist.IllegalPersistStateException;
import scott.sort.server.jdbc.persist.OperationGroup;

public class PersistAnalyser implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PersistAnalyser.class);

    private final OperationGroup createGroup;

    private final OperationGroup updateGroup;

    private final OperationGroup deleteGroup;

    private final OperationGroup dependsOnGroup;

    private final OperationGroup allGroups[];

    private final EntityContext entityContext;

    private final Set<Entity> analysing = new HashSet<>();

    public PersistAnalyser(EntityContext entityContext) {
        this(entityContext, new OperationGroup(), new OperationGroup(), new OperationGroup(), new OperationGroup());
    }

    private PersistAnalyser(EntityContext entityContext, OperationGroup createGroup, OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) {
        this.entityContext = entityContext;
        this.createGroup = createGroup;
        this.updateGroup = updateGroup;
        this.deleteGroup = deleteGroup;
        this.dependsOnGroup = dependsOnGroup;
        this.allGroups = new OperationGroup[] { createGroup, updateGroup, deleteGroup, dependsOnGroup };
    }

    public EntityContext getEntityContext() {
        return entityContext;
    }

    /**
     * Clones the entity context and all the entities
     * @return
     */
    public PersistAnalyser deepCopy() {
        EntityContext newContext = entityContext.newEntityContextSharingTransaction();
        newContext.beginSaving();
        PersistAnalyser copyAnalyser = new PersistAnalyser(newContext);

        copyEntityValues(createGroup, copyAnalyser.createGroup, newContext);
        copyEntityValues(updateGroup, copyAnalyser.updateGroup, newContext);
        copyEntityValues(deleteGroup, copyAnalyser.deleteGroup, newContext);
        copyEntityValues(dependsOnGroup, copyAnalyser.dependsOnGroup, newContext);
        EntityContextHelper.copyRefStates(entityContext, newContext, newContext.getEntitiesSafeIterable(), new EntityContextHelper.EntityFilter() {
            @Override
            public boolean includesEntity(Entity entity) {
                return containsEntity(entity);
            }
        });
//    	LOG.debug("Printing copied context");
//    	LOG.debug(newContext.printXml());
        return copyAnalyser;
    }

    private boolean containsEntity(Entity entity) {
        for (OperationGroup group : allGroups) {
            for (Entity e : group.getEntities()) {
                if (e == entity) {
                    return true;
                }
            }
        }
        return false;
    }

    public PersistAnalyser optimizedCopy() {
        return new PersistAnalyser(
                entityContext,
                createGroup.optimizedForInsertCopy(),
                updateGroup.optimizedForUpdateCopy(),
                deleteGroup.optimizedForDeleteCopy(),
                dependsOnGroup);
    }

    public OperationGroup getCreateGroup() {
        return createGroup;
    }

    public OperationGroup getUpdateGroup() {
        return updateGroup;
    }

    public OperationGroup getDeleteGroup() {
        return deleteGroup;
    }

    public OperationGroup getDependsOnGroup() {
        return dependsOnGroup;
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nCreate Group ----------------\n");
        reportGroup(sb, createGroup);
        sb.append("\nUpdate Group ----------------\n");
        reportGroup(sb, updateGroup);
        sb.append("\nDelete Group ----------------\n");
        reportGroup(sb, deleteGroup);
        sb.append("\nDepends On Group ----------------\n");
        reportGroup(sb, dependsOnGroup);
        return sb.toString();
    }

    private void reportGroup(StringBuilder sb, OperationGroup group) {
        for (Entity entity : group.getEntities()) {
            sb.append(entity.toString());
            sb.append('\n');
        }
    }

    public void analyse(PersistRequest persistRequest) throws IllegalPersistStateException {
        try {
            for (Object toSave : persistRequest.getToSave()) {
                final Entity entity = ((ProxyController) toSave).getEntity();
                /*
                 * top level toSave entities get analysed by themselves
                 * so if they have been analysed already then clear that.
                 */
                removeAnalysis(entity);

                if (entity.getEntityContext() != entityContext) {
                    throw new IllegalPersistStateException("Cannot persist entity from a different context");
                }
                if (entity.getKey().getValue() == null) {
                    analyseCreate(entity);
                } else {
                    analyseUpdate(entity);
                }
            }
            for (Object toDelete : persistRequest.getToDelete()) {
                final Entity entity = ((ProxyController) toDelete).getEntity();
                removeAnalysis(entity);
                if (entity.getEntityContext() != entityContext) {
                    throw new IllegalPersistStateException("Cannot persist entity from a different context");
                }
                analyseDelete(entity);
            }
        } finally {
            analysing.clear();
        }
    }

    private void removeAnalysis(Entity entity) {
        if (analysing.remove(entity)) {
            for (OperationGroup og : allGroups) {
                og.getEntities().remove(entity);
            }

        }

    }

    private void analyseCreate(Entity entity) {
        if (!analysing.add(entity)) {
            return;
        }
        LOG.debug("analysing " + entity + " for create");
        /*
         * I can only be created after any of my fk refs are created.
         */
        analyseRefNodes(entity, true);

        /*
         * Schedule ourselves for creation
         */
        createGroup.add(entity);

        /*
         * Look at the to many relations, they must require creation
         * as our pk doesn't exist yet
         */
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            for (Entity refEntity : toManyNode.getList()) {
                analyseCreate(refEntity);
            }
        }
    }

    private void analyseUpdate(Entity entity) {
        if (!analysing.add(entity)) {
            return;
        }
        LOG.debug("analysing " + entity + " for update");
        /*
         * I may now refer to different fks so they should be processed before my update.
         */
        analyseRefNodes(entity, true);
        /*
         * Schedule ourselves for update
         */
        updateGroup.add(entity);
        /*
         * if we own any refs which were removed then we have to delete the
         * entities which the refs pointed at
         */
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            final Object entityKey = checkForRemovedReference(refNode);
            if (entityKey != null) {
                Entity removedEntity = entity.getEntityContext().getOrLoad(refNode.getEntityType(), entityKey);
                analyseDelete(removedEntity);
            }
        }
        /*
         * Look at the to many relations, some items may have been added, updated or deleted
         */
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            if (!toManyNode.isFetched()) {
                /*
                 * if the many relation was never fetched, then nothing was changed, skip.
                 */
                continue;
            }
            for (Entity refEntity : toManyNode.getList()) {
                if (refEntity.getKey().getValue() == null) {
                    analyseCreate(refEntity);
                }
                else if (toManyNode.getNodeDefinition().isOwns()) {
                    analyseUpdate(refEntity);
                }
            }
            for (Entity refEntity : toManyNode.getRemovedEntities()) {
                analyseDelete(refEntity);
            }
        }
    }

    private void analyseDelete(Entity entity) {
        if (!analysing.add(entity)) {
            return;
        }
        LOG.debug("analysing " + entity + " for delete");
        /*
         * Look at the to many relations, some items may have been added, updated or deleted
         */
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            /*
             * we only delete the many side if we own it.
             */
            if (!toManyNode.getNodeDefinition().isOwns()) {
                //TODO: we should also check if the entities in the many side
                //are part of the same delete request, of so we should analyse them too
                continue;
            }
            /*
             *  fetch the relation if required so that we can process the many nodes.
             */
            if (!toManyNode.isFetched()) {
                //load the relation if required, so we can delete the items
                LOG.debug("Fetching 1:N relation as part of delete analysis");
                entityContext.fetchSingle(toManyNode, true);
            }
            /*
             *  schedule any many entities with non-null keys for deletion.
             */
            for (Entity refEntity : toManyNode.getList()) {
                if (refEntity.getKey().getValue() != null) {
                    analyseDelete(refEntity);
                }
            }
            for (Entity refEntity : toManyNode.getRemovedEntities()) {
                analyseDelete(refEntity);
            }
        }
        /*
         * Schedule ourselves for creation
         */
        LOG.debug("adding " + entity + " for delete");
        deleteGroup.add(entity);

        /*
         * Then delete any refs which we own
         */
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            final Entity refEntity = refNode.getReference();
            /*
             * An empty ref, nothing to analyse
             */
            if (refEntity == null) {
                continue;
            }
            if (refNode.getNodeDefinition().isOwns()) {
                /*
                 * If the ref is used then schedule the ref'd entity for deletion
                 */
                if (refEntity.getKey().getValue() != null) {
                    if (!refEntity.isLoaded()) {
                        entityContext.fetchSingle(refEntity, true);
                    }
                    analyseDelete(refEntity);
                }
                /*
                 * If the ref has a removed reference, then this needs to be deleted also.
                 */
                final Object entityKey = checkForRemovedReference(refNode);
                if (entityKey != null) {
                    Entity removedEntity = entity.getEntityContext().getOrLoad(refNode.getEntityType(), entityKey);
                    analyseDelete(removedEntity);
                }
            }
        }
    }

    private void analyseDependsOn(Entity entity) {
        if (!analysing.add(entity)) {
            return;
        }

        LOG.debug("analysing " + entity + " for depends on");
        /*
         * I may myself depend on other entities which should also be up-to-date
         */
        analyseRefNodes(entity, false);

        dependsOnGroup.add(entity);

        /*
         * Look at the to many relations, we also need to make sure that they are fresh if we depend on them
         */
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            if (!toManyNode.isFetched()) {
                /*
                 * if the many relation was never fetched, it is not used and has no impact to freshness.
                 */
                continue;
            }
            if (!toManyNode.getNodeDefinition().dependsOrOwns()) {
                /*
                 * We only need to look at tomany refs if we depend-on or own them
                 */
                continue;
            }
            for (Entity refEntity : toManyNode.getList()) {
                analyseDependsOn(refEntity);
            }
            for (Entity refEntity : toManyNode.getRemovedEntities()) {
                analyseDependsOn(refEntity);
            }
        }
    }

    private Object checkForRemovedReference(RefNode refNode) {
        return refNode.getNodeDefinition().isOwns() ? refNode.getRemovedEntityKey() : null;
    }

    private void analyseRefNodes(Entity entity, boolean updateOwnedRefs) {
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            final Entity refEntity = refNode.getReference();
            /*
             * An empty ref, nothing to analyse
             */
            if (refEntity == null) {
                continue;
            }
            if (refEntity.getKey().getValue() == null) {
                /*
                 * we are refering to an entity which is not yet created, process it first
                 */
                analyseCreate(refEntity);
            } else if (updateOwnedRefs && refNode.getNodeDefinition().isOwns() && refEntity.isLoaded()) {
                /*
                 * the entity aleady exists in the db, but we own it so we are also going to perform an update.
                 * as long as it was loaded
                 */
                analyseUpdate(refEntity);
            }
            else if (refNode.getNodeDefinition().dependsOrOwns() && refEntity.isLoaded()) {
                /*
                 * We don't own, but logically depend on this reference to be considered valid
                 * since the entity was loaded, we need to analyse this dependency incase the version
                 * is out of date.
                 * ie we don't want to allow saving of a syntax based on an out-of-date structure.
                 *
                 */
                analyseDependsOn(refEntity);
            }
        }
    }

    private void copyEntityValues(OperationGroup fromGroup, OperationGroup toGroup, EntityContext newContext) {
        for (Entity entity : fromGroup.getEntities()) {
            copyInto(entity, newContext, toGroup);
        }
    }

    private void copyInto(Entity entity, EntityContext newContext, OperationGroup toGroup) {
        Entity copy = new Entity(newContext, entity);
        newContext.add(copy);
        copy.setEntityState(entity.getEntityState());
        copy.copyValueNodesToMe(entity);
        toGroup.add(copy);
    }

    public void applyChanges(EntityContext otherContext) {
        LOG.debug("APPLYING CHANGES -------------------------");
        OperationGroup changed = createGroup.mergedCopy(updateGroup);
        List<Entity> otherEntities = EntityContextHelper.applyChanges(changed.getEntities(), otherContext);
        EntityContextHelper.copyRefStates(entityContext, otherContext, otherEntities, new EntityContextHelper.EntityFilter() {
            @Override
            public boolean includesEntity(Entity entity) {
                return true; // everything gets copied back
            }
        });
        EntityContextHelper.removeEntities(deleteGroup.getEntities(), otherContext, true);
    }

}
