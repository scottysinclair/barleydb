package scott.barleydb.api.core.entity;

import java.io.Serializable;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.dependency.Dependency;
import scott.barleydb.api.dependency.DependencyTree;
import scott.barleydb.api.dependency.EntityDependencyTreeNode;
import scott.barleydb.api.exception.constraint.EntityMustExistInDBException;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;

public class FetchHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FetchHelper.class);

    private final EntityContext ctx;


    /**
     * A WeakHashMap linking the entity to the entity info.
     *
     * The WeakHashMap does not prevent the entity key from being collected.
     *
     */
    private final WeakHashMap<Entity, Object> batchFetchEntities;

    public FetchHelper(EntityContext ctx) {
        this.ctx = ctx;
        this.batchFetchEntities = new WeakHashMap<>();
    }

    public void batchFetch(Entity entity) {
        batchFetchEntities.put(entity, null);
    }

    public void fetchEntity(Entity entity, boolean force, boolean fetchInternal, boolean evenIfLoaded, String singlePropertyName) {
        if (!force && entity.getEntityContext().isInternal()) {
            return;
        }
        if (!evenIfLoaded && entity.getEntityState() == EntityState.LOADED) {
            return;
        }
        if (attemptBatchFetch(entity)) {
            return;
        }
        /*
         * else normal fetch
         */
        LOG.debug("Fetching {}" , entity);
        QueryObject<Object> qo = ctx.getQuery(entity.getEntityType(), fetchInternal);
        if (qo == null) {
            qo = new QueryObject<Object>(entity.getEntityType().getInterfaceName());
        }

        final QProperty<Object> pk = new QProperty<Object>(qo, entity.getEntityType().getKeyNodeName());
        qo.where(pk.equal(entity.getKey().getValue()));

        try {
            if (singlePropertyName != null) {
                QProperty<?> property =  qo.getMandatoryQProperty( singlePropertyName );
                qo.select(property);
            }
            QueryResult<Object> result = ctx.performQuery(qo);
            /*
             * Some cleanup required.
             * the executer will set all loading entities to LOADED
             * but we set the state to LOADING ourselves
             * so check the result, and manually set the entity state
             */
            if (result.getList().isEmpty()) {
                if (entity.getConstraints().isMustExistInDatabase()) {
                    throw new EntityMustExistInDBException(entity);
                }
                else {
                    entity.setEntityState(EntityState.NOT_IN_DB);
                }
            }
            else  {
                entity.setEntityState(EntityState.LOADED);
            }
        } catch (Exception x) {
            throw new IllegalStateException("Error performing fetch", x);
        }
    }

    public void fetchEntities(Set<Entity> entities, boolean force, boolean fetchInternal, boolean evenIfLoaded) {
        EntityContext ctx = entities.iterator().next().getEntityContext();
        LOG.debug("Fetching {}" , entities);
        Entity firstEntity = entities.iterator().next();

        QueryObject<Object> qo = ctx.getQuery(firstEntity.getEntityType(), fetchInternal);
        if (qo == null) {
            qo = new QueryObject<Object>(firstEntity.getEntityType().getInterfaceName());
        }

        final QProperty<Object> pk = new QProperty<Object>(qo, firstEntity.getEntityType().getKeyNodeName());
        final Set<Object> entityPkValues = toEntityKeyValues(entities);
        qo.where(pk.in(entityPkValues));

        try {
            QueryResult<Object> result = ctx.performQuery(qo);
            /*
             * Some cleanup required.
             * the executer will set all loading entities to LOADED
             * but we set the state to LOADING ourselves
             * so check the result, and manually set the entity state
             */
            for (Entity e: entities) {
                if (!result.getEntityList().contains(e)) {
                    if (e.getConstraints().isMustExistInDatabase()) {
                        throw new EntityMustExistInDBException(e);
                    }
                    else {
                        e.setEntityState(EntityState.NOT_IN_DB);
                    }
                }
                else  {
                    e.setEntityState(EntityState.LOADED);
                }
            }
        } catch (Exception x) {
            throw new IllegalStateException("Error performing fetch", x);
        }
    }

    private Set<Object> toEntityKeyValues(Set<Entity> entities) {
        return entities.stream().map(e -> e.getKey().getValue()).collect(Collectors.toSet());
    }

    private boolean attemptBatchFetch(Entity entity) {
        Set<EntityPath> paths = calculatePathsFromBatchFetchEntities(entity);
        EntityPath shortest = findShortestPath(paths);
        if (shortest == null) {
            return false;
        }
        Set<Entity> tofetch = findAllEntitiesWithSamePath(shortest);
        return true;
    }

    private Set<EntityPath> calculatePathsFromBatchFetchEntities(Entity entity) {
        Set<EntityPath> result = new HashSet<>();
        for (Entity batchRoot: new HashSet<>(batchFetchEntities.keySet())) {
            //GC safe
            if (batchFetchEntities.containsKey(batchRoot)) {
                EntityPath path = findPath(batchRoot, entity);
                if (path != null) {
                    result.add(path);
                }
            }
        }
        return result;
    }

    private EntityPath findPath(Entity batchRoot, Entity entity) {
        DependencyTree tree = new DependencyTree();
        tree.build(Collections.singleton(new EntityDependencyTreeNode(batchRoot)));
        /*
         * the tree did not discover entity from batchRoot, there is no link
         */
        if (tree.getNodeFor(entity) == null) {
            return null;
        }
        List<Dependency> path = tree.findShortestPath(batchRoot, entity);
        return toEntityPath(path);
    }

    private EntityPath toEntityPath(List<Dependency> path) {
        Collections.reverse(path);
        EntityPath ep = null;
        for(Dependency dep: path) {
            ep = new EntityPath((Entity)dep.getFrom().getThing(), (Node)dep.getThing(), ep);
        }
        return ep;
    }


    private EntityPath findShortestPath(Set<EntityPath> paths) {
        return paths.stream()
         .reduce((a, b) -> a.getSize() < b.getSize() ? a : b)
         .orElse(null);
    }

    private Set<Entity> findAllEntitiesWithSamePath(EntityPath path) {
        Set<Entity> result = new HashSet<>();
        Set<Entity> workingSet = new HashSet<>();
        workingSet.add(path.getEntity());
        EntityPath p = path;
        while(p.getNext() != null) {
            Set<Entity> nextWorkingSet = new HashSet<>();
            for (Entity e: workingSet) {
                Node node = e.getChild(path.getNode().getName());
                if (node instanceof RefNode) {
                    Entity nextE = ((RefNode)node).getReference();
                    if (nextE != null) {
                        nextWorkingSet.add(nextE);
                    }
                }
                else {
                    nextWorkingSet.addAll(((ToManyNode)node).getList());
                }
              }
            workingSet.clear();
            workingSet.addAll(nextWorkingSet);
            p = p.getNext();
        }
        return result;
    }


    final class EntityPath {
        private final  Entity entity;
        private final Node node;
        private final EntityPath next;
        public EntityPath(Entity entity, Node node, EntityPath next) {
            this.entity = entity;
            this.node = node;
            this.next = next;
        }
        public int getSize() {
            return next == null ? 1: next.getSize() + 1;
        }
        public Entity getEntity() {
            return entity;
        }
        public EntityPath getNext() {
            return next;
        }
        public Node getNode() {
            return node;
        }
    }

    /**
    *
    * @param toManyNode
    * @param override if true we fetch even if in internal mode.
    * @param fetchInternal
    */
   public void fetch(ToManyNode toManyNode, boolean override, boolean fetchInternal) {
       if (!override && toManyNode.getEntityContext().isInternal()) {
           return;
       }

       final NodeType toManyDef = toManyNode.getNodeType();

       //get the name of the node/property which we need to filter on to get the correct entities back on the many side
       final String foreignNodeName = toManyDef.getForeignNodeName();
       if (foreignNodeName != null) {
           QueryObject<Object> qo = ctx.getQuery(toManyNode.getEntityType(), fetchInternal);
           if (qo == null) {
               qo = new QueryObject<Object>(toManyNode.getEntityType().getInterfaceName());
           }

           final QProperty<Object> manyFk = new QProperty<Object>(qo, foreignNodeName);
           final Object primaryKeyOfOneSide = toManyNode.getParent().getKey().getValue();
           qo.where(manyFk.equal(primaryKeyOfOneSide));
           /*
            * If a user call is causing a fetch to a "join entity"
            * then join from the join entity to the referenced entity.
            *
            * So if the ToManyNode joins from Template to TemplateDatatype and has joinProperty "datatype"
            * then we will make the QTemplateBusinessType outer join to QBusinessType so that QBusinessType is automatically pulled in
            *
            * Template.datatypes == the ToManyNode
            * TemplateDatatype.datatype == the RefNode on the "join entity"
            *
            * the if below uses these nouns for better clarification
            *
            */
           if (!fetchInternal && toManyDef.getJoinProperty() != null) {
               NodeType datatypeNodeType = toManyNode.getEntityType().getNodeType(toManyDef.getJoinProperty(), true);
               EntityType datatype = ctx.getDefinitions().getEntityTypeMatchingInterface(datatypeNodeType.getRelationInterfaceName(), true);
               QueryObject<Object> qdatatype = ctx.getQuery(datatype, fetchInternal);
               if (qdatatype == null) {
                  qdatatype = new QueryObject<Object>(datatype.getInterfaceName());
               }
               qo.addLeftOuterJoin(qdatatype, datatypeNodeType.getName());
           }

           try {
               ctx.performQuery(qo);
               toManyNode.setFetched(true);
               toManyNode.refresh();
           } catch (Exception x) {
               throw new IllegalStateException("Error performing fetch", x);
           }
       } else {
           /*
            * get the query for loading the entity which has the relation we want to fetch
            * ie the template query for when we want to fetch the datatypes.
            */
           final QueryObject<Object> fromQo = ctx.getDefinitions().getQuery(toManyNode.getParent().getEntityType());
           /*
            * gets the query for loading the entity which the relation fulfills
            * ie the datatype query which the template wants to load
            */
           final QueryObject<Object> toQo = ctx.getDefinitions().getQuery(toManyNode.getEntityType());
           /*
            * add the join from template to query
            * the query generator knows to include the join table
            */
           fromQo.addLeftOuterJoin(toQo, toManyNode.getName());
           /*
            * constrain from query to only return data for the entity we are fetching for
            * ie constrain the template query by the id of the template we are fetching for
            */
           final QProperty<Object> fromPk = new QProperty<Object>(fromQo, toManyNode.getParent().getKey().getName());
           fromQo.where(fromPk.equal(toManyNode.getParent().getKey().getValue()));

           try {
               ctx.performQuery(fromQo);
               toManyNode.setFetched(true);
               toManyNode.refresh();
           } catch (Exception x) {
               throw new IllegalStateException("Error performing fetch", x);
           }
       }
   }


}
