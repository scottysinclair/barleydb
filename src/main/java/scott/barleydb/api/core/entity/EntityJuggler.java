package scott.barleydb.api.core.entity;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import scott.barleydb.api.exception.BarleyDBRuntimeException;

public class EntityJuggler {

  private boolean uuidsMatchAcrossContexts = true;
  private boolean overwriteOptimisticLocks = true;
  private Set<Entity> startedImport = new HashSet<>();

  public EntityJuggler(boolean uuidsMatchAcrossContexts, boolean overwriteOptimisticLocks) {
    this.uuidsMatchAcrossContexts = uuidsMatchAcrossContexts;
    this.overwriteOptimisticLocks = overwriteOptimisticLocks;
  }

  /**
   * State from entities is applied to the matching entities in the dest context.
   * Entities which are missing from dest are created.
   * @param entities
   * @param dest
   */
  public List<Entity> importEntities(Collection<Entity> entities, EntityContext dest) {
    return importOrApplyChanges(entities, dest, true);
  }

  /**
   * State from entities is applied to the matching entities in the dest context
   * @param entities
   * @param dest
   */
  public List<Entity> applyChanges(Collection<Entity> entities, EntityContext dest) {
    return importOrApplyChanges(entities, dest, false);
  }

  @SuppressWarnings("unused")
  private Entity importOrApplyChanges(Entity entity, EntityContext dest, boolean importEntities) {
    importOrApplyChanges(Arrays.asList(entity), dest, importEntities);
    return findMatchingEntity(dest, entity);
  }

  private List<Entity> importOrApplyChanges(Collection<Entity> entities, EntityContext dest, boolean importEntities) {
    List<Entity> result = new LinkedList<>();
    Set<Entity> skip = new HashSet<>();

    Collection<Entity> fullImportCollection = expandToIncludeReferences(entities);

    for (Entity e : fullImportCollection) {
      Entity eDest = findMatchingEntity(dest, e);
      if (!startedImport.add(e)) {
        //eDest is already being imported, we need to prevent stack overflow
        skip.add(e);
        result.add(eDest);
        continue;
      }
      if (eDest == null && importEntities) {
        eDest = newMatchingEntity(dest, e);
      }
      result.add(eDest);
      copyMetaData(e, eDest);
      eDest.copyValueNodesToMe(e, overwriteOptimisticLocks);
    }
    for (Entity e: fullImportCollection) {
        if (skip.contains(e)) {
          continue;
        }
        Entity eDest = findMatchingEntity(dest, e);
        copyRefNodes(e, eDest);
    }
    for (Entity e: fullImportCollection) {
      if (skip.contains(e)) {
        continue;
      }
      Entity eDest = findMatchingEntity(dest, e);
      copyToManyRefs(e, eDest);
    }
    return result;
  }


  private Collection<Entity> expandToIncludeReferences(Collection<Entity> entities) {
    Set<Entity> result = new LinkedHashSet<>(entities);
    for (Entity e : entities) {
      /*
       * include entities which we want to import from direct FK references
       */
      for (RefNode refNode: e.getChildren(RefNode.class)) {
        if (!refNode.isLoaded()) {
          //refNode is not loaded so we have nothing to add.
          continue;
        }
        Entity refEntity = refNode.getReference();
        if (refEntity != null && importRefNode(refNode)) {
          result.add(refEntity);
        }
      }
      /*
       * also include entities which we want to import from 1:N relations.
       */
      for (ToManyNode toManyNode: e.getChildren(ToManyNode.class)) {
        if (!toManyNode.isFetched()) {
          //toMany node is not fetched, we have nothing to add.
          continue;
        }
        for (Entity nEntity: toManyNode.getList()) {
          if (importToManyNode(toManyNode, nEntity)) {
            // we import the reference - so even if destRefEntity already exists -
             result.add(nEntity);
          }
        }
      }
    }
    return result;
  }

  private void copyRefNodes(Entity e, Entity eDest) {
    EntityContext destCtx = eDest.getEntityContext();
    for (RefNode refNode: e.getChildren(RefNode.class)) {
      if (!refNode.isLoaded()) {
        //refNode is not loaded so we have nothing to copy over to rDest, simplest to treat this as a noop.
        continue;
      }
      RefNode rDest = eDest.getChild(refNode.getName(), RefNode.class);
      rDest.setLoaded(true);

      Entity refEntity = refNode.getReference();
      if (refEntity == null) {
        rDest.setReference(null);
      } else {
        Entity destRefEntity = findMatchingEntity(destCtx, refEntity);
        // check we we import otherwise set it as NOT_LOADED ref
        if (startedImport.contains(refEntity)) {
          Objects.requireNonNull(destRefEntity, "must exist as import has started");
          rDest.setReference(destRefEntity);
        } else {
          // we don't import, so just set the equivalent reference so the
          // foreign key is correct
          if (destRefEntity == null) {
            destRefEntity = createNotLoadedEquivalent(refEntity, destCtx);
          }
          rDest.setReference(destRefEntity);
        }
      }
    }
  }

  private void copyToManyRefs(Entity e, Entity eDest) {
    EntityContext destCtx = eDest.getEntityContext();
    for (ToManyNode toManyNode: e.getChildren(ToManyNode.class)) {
      ToManyNode toManyDest = eDest.getChild(toManyNode.getName(), ToManyNode.class);
      if (!toManyNode.isFetched()) {
        //toMany node is not fetched, we have no information on it to copy over - this is a noop.
        continue;
      }
      toManyDest.setFetched(true);
      //setup the relation importing and applying changes on the N side.
      for (Entity nEntity: toManyNode.getList()) {
        Entity destNEntity = findMatchingEntity(destCtx, nEntity);

        if (startedImport.contains(nEntity)) {
           Objects.requireNonNull(destNEntity, "must exist as it is being imported");
        }
        else {
          if (destNEntity == null) {
            destNEntity = createNotLoadedEquivalent(nEntity, destCtx);
          }
        }
        toManyDest.addIfAbsent(destNEntity);
      }
    }
  }

  private void copyMetaData(Entity e, Entity eDest) {
    eDest.setEntityState( e.getEntityState() );
    eDest.getConstraints().set( e.getConstraints() );
  }


  private Entity createNotLoadedEquivalent(Entity refEntity, EntityContext destCtx) {
    if (refEntity.getKey().getValue() == null) {
      throw new BarleyDBRuntimeException("Cannot setup a equivalent NON-LOADED reference, entity has no PK");
    }
    UUID uuid = uuidsMatchAcrossContexts ? refEntity.getUuid() : UUID.randomUUID();
    Entity destE = destCtx.newEntity(refEntity.getEntityType(), refEntity.getKey().getValue(), refEntity.getConstraints(), uuid);
    destE.setEntityState(EntityState.NOTLOADED);
    return destE;
  }

  private Entity newMatchingEntity(EntityContext dest, Entity e) {
    Entity destE;
    if (uuidsMatchAcrossContexts) {
      destE = dest.newEntity(e.getEntityType(), e.getKey().getValue(), e.getConstraints(), e.getUuid());
    }
    else if (e.getKey().getValue() == null) {
      throw new BarleyDBRuntimeException("Cannot create natching entity with null PK, UUIDs do not match across contexts (in this case)");
    }
    else {
      destE = dest.newEntity(e.getEntityType(), e.getKey().getValue(), e.getConstraints());
    }
    destE.setEntityState(e.getEntityState());
    return destE;
  }

  private Entity findMatchingEntity(EntityContext dest, Entity e) {
    if (uuidsMatchAcrossContexts) {
      return dest.getEntityByUuid(e.getUuid(), false);
    }
    if (e.getKey().getValue() == null) {
      throw new BarleyDBRuntimeException("Cannot find Entity with null PK, UUIDs do not match across contexts (in this case)");
    }
    return dest.getEntity(e.getEntityType(), e.getKey().getValue(), false);
  }

  protected boolean importRefNode(RefNode refNode){
    return true;
  }

  protected boolean importToManyNode(ToManyNode toMany, Entity entity){
    return true;
  }

}
