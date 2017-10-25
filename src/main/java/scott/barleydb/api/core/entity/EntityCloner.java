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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.exception.BarleyDBRuntimeException;
import scott.barleydb.api.query.QJoin;
import scott.barleydb.api.query.QueryObject;

public class EntityCloner {

  private static final Logger LOG = LoggerFactory.getLogger(EntityCloner.class);
  private Map<Entity, Entity> cloneLookup = new HashMap<Entity, Entity>();

  public <T extends ProxyController> T clone(T model, QueryObject<T> cloneGraph) {
    Entity src = model.getEntity();
    EntityContext ctx = src.getEntityContext();
    Entity dest = clone(src, cloneGraph, "");
    T destModel = ctx.getProxy(dest);
    return destModel;
  }

  public Entity clone(Entity src, QueryObject<?> cloneGraph, String logPrefix) {
    Entity dest = cloneLookup.get(src);
    if (dest != null) {
      return dest;
    }
    EntityContext ctx = src.getEntityContext();
    dest = ctx.newEntity(src.getEntityType(), null, EntityConstraint.mustNotExistInDatabase());
    LOG.debug("{}Cloning entity {} to {}", logPrefix, src, dest.getUuidFirst7());
    cloneLookup.put(src, dest);
    copyValues(src, dest, logPrefix);
    Set<RefNode> clonedRefs = new LinkedHashSet<>();
    if (cloneGraph != null) {
      for (QJoin join: cloneGraph.getJoins()) {
        String property = join.getFkeyProperty();
        Node child = src.getChild(property);
        if (child instanceof RefNode) {
           RefNode refNode = (RefNode)child;
           clonedRefs.add(refNode);
           cloneRefNodeRelation(refNode, dest, join.getTo(), logPrefix);
        }
        else if (child instanceof ToManyNode) {
          cloneToManyNodeRelation((ToManyNode)child, dest, join.getTo(), logPrefix);
        }
        else {
          throw new BarleyDBRuntimeException("Join property is not on a RefNode or ToManyNode");
        }
      }
    }

    /*
     * give subclasses the chance to handle RefNode cloning.
     */
    for (RefNode refNode: src.getChildren(RefNode.class)) {
      if (clonedRefs.contains(refNode)) {
        //skip the refs which we cloned above
        continue;
      }
      if (forceClone(refNode)) {
        clonedRefs.add(refNode);
        cloneRefNodeRelation(refNode, dest, null, logPrefix);
      }
    }


    /*
     * all FK references which are not in the clone graph must be copied across
     * (the original reference is kept - "a cloned syntax still points to the same creation user")
     */
    for (RefNode refNode: src.getChildren(RefNode.class)) {
      if (clonedRefs.contains(refNode)) {
        //skip the refs which we cloned above
        continue;
      }
      RefNode destNode = dest.getChild(refNode.getName(), RefNode.class);
      if (refNode.isLoaded()) {
        Entity refEntity = refNode.getReference();
        //if refEntity has been cloned then we need to use the cloned version
        Entity clonedRef = cloneLookup.get(refEntity);
        if (clonedRef != null) {
          refEntity = clonedRef;
        }
        destNode.setReference( refEntity );
        if (refEntity == null) {
          LOG.debug("{}Set FK to '{}' -> null", logPrefix, refNode.getName());
        }
        else if (clonedRef != null){
          LOG.debug("{}Copy FK to cloned '{}' -> {}", logPrefix, refNode.getName(), refEntity.getUuidFirst7());
        }
        else {
          LOG.debug("{}Copy FK to original '{}' -> {}", logPrefix, refNode.getName(), refEntity.getKey().getValue());
        }
      }
      else {
        destNode.setLoaded(false);
      }
    }

    /*
     * if a ToMany node is not being cloned, then it doesn't make sense from the data point of view
     * our clone 'dest' cannot have refer to the same N-side entites as they actually need refer back,
     * which is only possible if they are cloned.
     *
     * Rather than fail fast here, lets allow it to pass through.
     * Best not to force our opinion on the application layer - perhaps there is a valid usecase
     */

    return dest;
  }

  /**
   * sub classes can override to specify behaviour
   * @param refNode
   * @return
   */
  protected boolean forceClone(RefNode refNode) {
    return false;
  }

  private void copyValues(Entity src, Entity dest, String logPrefix) {
    for (ValueNode srcNode: src.getChildren(ValueNode.class)) {
      if (src.getKey() == srcNode) {
        continue;
      }
      if (srcNode.getNodeType().isOptimisticLock()) {
        continue;
      }
      ValueNode destNode = dest.getChild(srcNode.getName(), ValueNode.class);
      destNode.setValue(srcNode.getValueNoFetch());
      LOG.debug("{}Copy value '{}' -> {}",  logPrefix, srcNode.getName(), srcNode.getValue());
    }
  }

  private void cloneToManyNodeRelation(ToManyNode child, Entity dest, QueryObject<?> restOfGraph, String logPrefix) {
    ToManyNode destNode = dest.getChild(child.getName(), ToManyNode.class);
    for (Entity entity: child.getList()) {
      if (excludedFromCloning(child, entity)) {
        LOG.debug("{}{} is excluded from cloning", logPrefix, entity);
        continue;
      }
      if (LOG.isDebugEnabled() && !cloneLookup.containsKey(entity)) {
        LOG.debug("{}Cloning 1:N relation '{}' ({})", logPrefix, child.getName(), entity);
      }
      destNode.add( clone(entity, restOfGraph, logPrefix + "  ") );
    }
  }

  /**
   * can be overriden for custom behaviour
   * @param child
   * @param entity
   * @return
   */
  protected boolean excludedFromCloning(ToManyNode child, Entity entity) {
    return false;
  }

  private void cloneRefNodeRelation(RefNode child, Entity dest, QueryObject<?> restOfGraph, String logPrefix) {
    RefNode destNode = dest.getChild(child.getName(), RefNode.class);
    Entity srcRefE = child.getReference();
    if (srcRefE != null) {
      if (LOG.isDebugEnabled() && !cloneLookup.containsKey(srcRefE)) {
        LOG.debug("{}Cloning N:1 relation '{}' -> {}", logPrefix, child.getName(), srcRefE);
      }
      destNode.setReference( clone(srcRefE, restOfGraph, logPrefix + "  "));
    }
    else {
      LOG.debug("{}Set N:1 relation '{}' to null", logPrefix, child.getName());
    }
  }

}
