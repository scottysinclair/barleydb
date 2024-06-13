package scott.barleydb.api.persist;

import static scott.barleydb.api.core.entity.EntityContextHelper.findEntites;

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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextHelper;
import scott.barleydb.api.core.entity.EntityContextHelper.Predicate;
import scott.barleydb.api.core.entity.EntityState;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.EntityMissingException;
import scott.barleydb.api.exception.execution.persist.IllegalPersistStateException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.server.jdbc.persist.DatabaseDataSet;
import scott.barleydb.server.jdbc.persist.OperationGroup;

/**
 * Analyses a PersistRequest and figures out the operations which must be performed.
 *
 * @See {@link PersistRequest}
 * @author scott.sinclair
 *
 */
public class PersistAnalyser implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PersistAnalyser.class);

    private final OperationGroup createGroup;

    private final OperationGroup updateGroup;

    private final OperationGroup deleteGroup;

    private final OperationGroup dependsOnGroup;

    private final OperationGroup allGroups[];

    private final EntityContext entityContext;

    private final EntityContext analyserContext;

    private final DependencyTree dependencyTree;

    private final Set<Entity> analysing = new HashSet<>();

    public PersistAnalyser(EntityContext entityContext) {
        this(entityContext, new OperationGroup(), new OperationGroup(), new OperationGroup(), new OperationGroup());
    }

    private PersistAnalyser(EntityContext entityContext, OperationGroup createGroup, OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) {
        this.entityContext = entityContext;
        this.analyserContext = entityContext.newEntityContextSharingTransaction();
        this.analyserContext.setAllowGarbageCollection(false);
        this.createGroup = createGroup;
        this.updateGroup = updateGroup;
        this.deleteGroup = deleteGroup;
        this.dependsOnGroup = dependsOnGroup;
        this.allGroups = new OperationGroup[] { createGroup, updateGroup, deleteGroup, dependsOnGroup };
        this.dependencyTree = new DependencyTree(entityContext, analyserContext, true);
    }

    public EntityContext getEntityContext() {
        return entityContext;
    }

    public EntityContext getAnalyserContext() {
        return analyserContext;
    }

    /**
     * Clones the entity context and all the entities
     * @return
     */
    public PersistAnalyser deepCopy() {
        LOG.debug("Performing a deep copy of the PersistAnalyser");
        EntityContext newContext = entityContext.newEntityContextSharingTransaction();
        //TOD:bug fix we switch to internal mode but never switch back
        newContext.switchToInternalMode();
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

    /**
     * Analyzes the persistRequest
     *
     * @param persistRequest
     * @throws IllegalPersistStateException
     * @throws EntityMissingException
     * @throws SortServiceProviderException
     */
    public void analyse(PersistRequest persistRequest) throws SortPersistException, EntityMissingException {
        try {

//            setCorrectStateForEntitiesWhichMayOrMayNotBeInTheDatabase( persistRequest.getOperations() );

            try {
                dependencyTree.build( persistRequest.getOperations() );
            } catch (SortServiceProviderException | BarleyDBQueryException x) {
                throw new SortPersistException("Error building dependency tree", x);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("yuml: " + dependencyTree.generateDiagramYumlString());
            }

            /*
             * process the inserts, updates and depends, they have the same order
             */
            for (Operation operation: dependencyTree.getOrder()) {
                switch(operation.opType) {
                    case INSERT: {
                        createGroup.add(operation.entity);
                        break;
                    }
                    case UPDATE: {
                        updateGroup.add(operation.entity);
                        break;
                    }
                    case SAVE: {
                        if (operation.entity.isClearlyNotInDatabase()) {
                            createGroup.add( operation.entity );
                        }
                        else if (operation.entity.isClearlyInDatabase()) {
                            updateGroup.add( operation.entity );
                        }
                        else {
                            throw new IllegalStateException("It must be clear by now if the entity exists or not: " + operation.entity);
                        }
                        break;
                    }
                    case DELETE: {
                        break;
                    }
                    case DEPENDS: {
                        dependsOnGroup.add(operation.entity);
                        break;
                    }
                    case NONE: {
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unsupported operation type: " + operation.opType);
                }
            }

            /*
             * process the deletes, their order is reversed
             */
            List<Operation> order = dependencyTree.getOrder();
            for (Operation operation: order) {
                switch(operation.opType) {
                    case INSERT:
                    case UPDATE:
                    case SAVE:
                    case DEPENDS:
                    case NONE: break;
                    case DELETE: {
                        deleteGroup.add(operation.entity);
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unsupported operation type: " + operation.opType);
                }
            }

        } finally {
            analysing.clear();
        }

        for (Entity entity: createGroup.getEntities()) {
          if (entity.getConstraints().isMustExistInDatabase()) {
            throw new SortPersistException("Entity " + entity + " is in the create group but is constrained to exist in the database");
          }
        }
    }

    private final void setCorrectStateForEntitiesWhichMayOrMayNotBeInTheDatabase(Collection<Operation> operations) throws SortPersistException {
        LOG.debug("Setting the correct entity state for entities which may or may not be in the database.");

        Collection<Entity> entities = new LinkedList<>();
        for (Operation op: operations) {
            entities.add( op.entity );
        }

        LinkedHashSet<Entity> matches = findEntites(entities, new Predicate() {
                    @Override
                    public boolean matches(Entity entity) {
                        return entity.isUnclearIfInDatabase();
                    }
                });

        if (matches.isEmpty()) {
            return;
        }
        DatabaseDataSet dds = new DatabaseDataSet(entityContext, true);
        try {
            dds.loadEntities(matches);
        } catch (SortServiceProviderException | BarleyDBQueryException x) {
            throw new SortPersistException("Error checking which entities are in the database", x);
        }
        for (Entity eToSave: matches) {
            if (dds.getEntity(eToSave.getEntityType(), eToSave.getKeyValue()) != null) {
                LOG.debug("Found entity {} in the database.", eToSave);
                eToSave.setEntityState(EntityState.LOADED);
            }
            else {
                eToSave.setEntityState(EntityState.NOT_IN_DB);
            }
        }
    }

    private void copyEntityValues(OperationGroup fromGroup, OperationGroup toGroup, EntityContext newContext) {
        for (Entity entity : fromGroup.getEntities()) {
            copyInto(entity, newContext, toGroup);
        }
    }

    private void copyInto(Entity entity, EntityContext newContext, OperationGroup toGroup) {
        toGroup.add( newContext.copyInto( entity ) );
    }

    public void applyChanges(final EntityContext otherContext) {
        LOG.debug("applying changes to other entity context");
        OperationGroup changed = createGroup.mergedCopy(updateGroup).mergedCopy(deleteGroup);

        EntityContextHelper.EntityFilter filter = new EntityContextHelper.EntityFilter() {
            @Override
            public boolean includesEntity(Entity entity) {
             // everything gets copied back apart from entities loaded during analysis.
                return otherContextHasEntity(entity);
            }

            private boolean otherContextHasEntity(Entity entity) {
                Entity e2 = otherContext.getEntityByUuid(entity.getUuid(), false);
                if (e2 != null) {
                    return true;
                }
                Object key = entity.getKeyValue();
                return key != null && otherContext.getEntity(entity.getEntityType(), key, false) != null;
            }
        };

        /*
         * We have to allow for the fact that not all the entities which were part of our analysis should be copied back
         * for example entities which were deleted server side, but never part of the client context
         */
        List<Entity> otherEntities = EntityContextHelper.applyChanges(changed.getEntities(), otherContext, filter);
        EntityContextHelper.copyRefStates(entityContext, otherContext, otherEntities, filter);
    }

}
