package scott.barleydb.api.core.entity;

import java.io.Serializable;
import java.lang.ref.WeakReference;

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

import java.util.*;
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
    private final WeakHashMap<Entity, DependencyTree> batchFetchEntities;
    private final Set<Set<WeakReference<Entity>>> batchGroups = new HashSet<>();
    
    
    public FetchHelper(EntityContext ctx) {
        this.ctx = ctx;
        this.batchFetchEntities = new WeakHashMap<>();
    }

    public void batchFetchDescendants(Entity entity) {
        batchFetchEntities.put(entity, null);
    }

    public void batchFetchDescendants(Collection<Entity> entities) {
    	for (Entity entity: entities) {
    		batchFetchEntities.put(entity, null);
    	}
        batchGroups.add(toSetOfWeakRefs(entities));
    }

    private Set<WeakReference<Entity>> toSetOfWeakRefs(Collection<Entity> entities) {
    	Set<WeakReference<Entity>> result = new LinkedHashSet<>();
    	for (Entity e: entities) {
    		result.add(new WeakReference<Entity>(e));
    	}
		return result;
	}

	public void fetchEntity(Entity entity, boolean force, boolean fetchInternal, boolean evenIfLoaded, String singlePropertyName) {
        if (!force && entity.getEntityContext().isInternal()) {
            return;
        }
        if (!evenIfLoaded && entity.getEntityState() == EntityState.LOADED) {
            return;
        }
        if (attemptBatchFetch(entity, fetchInternal)) {
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
        qo.where(pk.equal(entity.getKeyValue()));

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

    public void fetchEntities(Set<Entity> entities, boolean fetchInternal) {
        EntityContext ctx = entities.iterator().next().getEntityContext();
        Entity firstEntity = entities.iterator().next();
        LOG.debug("Batch Fetching {} entities of type {}" , entities.size(), firstEntity.getEntityType().getInterfaceShortName());

        QueryObject<Object> qo = ctx.getQuery(firstEntity.getEntityType(), fetchInternal);
        if (qo == null) {
            qo = new QueryObject<Object>(firstEntity.getEntityType().getInterfaceName());
        }

        final QProperty<Object> pk = new QProperty<Object>(qo, firstEntity.getEntityType().getKeyNodeName());
        final Collection<Set<Object>> entityPkValues = toEntityKeyValues(entities, 1000);
        for (Set<Object> pkValues : entityPkValues) {
          qo.or(pk.in(pkValues));
        }

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

    private Collection<Set<Object>> toEntityKeyValues(Set<Entity> entities, int maxSize) {
        Set<Object> oneBigGroup = entities.stream().map(e -> e.getKeyValue()).collect(Collectors.toSet());
        if (oneBigGroup.size() <= maxSize) {
            return Collections.singletonList(oneBigGroup);
        }
        Collection result = new LinkedList();
        Set<Object> group = new LinkedHashSet<>();
        for (Object o: oneBigGroup) {
            group.add(o);
            if (group.size() >= maxSize) {
                result.add(group);
                group = new LinkedHashSet<>();
            }
        }
        if (group.size() > 0) {
            result.add(group);
        }
        return result;
    }

    private Set<Object> toEntityKeyValuesTmn(Collection<ToManyNode> toManyNodes) {
        return toManyNodes.stream().map(t -> t.getParent().getKeyValue()).collect(Collectors.toSet());
    }

    private boolean attemptBatchFetch(Entity entity, boolean fetchInternal) {
    	EntityPath shortest = findShortestPath(entity);
        if (shortest == null) {
            return false;
        }
        LOG.debug("Found shortest path from ROOT to entity {}", shortest.getPathAsString());
        Set<Entity> tofetch = findAllEntitiesWithSamePath(shortest);
        fetchEntities(tofetch, fetchInternal);
        return true;
    }
    
	private Set<Entity> defererence(Set<WeakReference<Entity>> set) {
		Set<Entity> result = new LinkedHashSet<>();
		for (WeakReference<Entity> ref: set) {
			Entity e = ref.get();
			if (e != null) {
				result.add(e);
			}
		}
		return result;
	}
	
	private boolean containsEntity(Set<WeakReference<Entity>> set, Entity entity) {
		for (WeakReference<Entity> e: set) {
			if (e.get() == entity) {
				return true;
			}
		}
		return false;
	}

	public EntityPath findShortestPath(Entity entity) {
        Set<EntityPath> paths = calculatePathsFromBatchFetchEntities(entity);
        if (LOG.isDebugEnabled()) {
	        LOG.debug("Found {} paths from ROOT to entity {}", paths.size(), entity);
	        for (EntityPath ep: paths) {
		        LOG.debug("Found path from ROOT to entity {}", ep.getPathAsString());
	        }
        }
        return findShortestPath(paths);
    }

    private Set<EntityPath> calculatePathsFromBatchFetchEntities(Entity entity) {
        Set<EntityPath> result = new HashSet<>();
        for (Entity batchRoot: new HashSet<>(batchFetchEntities.keySet())) {
            EntityPath path = findPath(batchRoot, entity);
            if (path != null) {
                result.add(path);
            }
        }
        return result;
    }

    private EntityPath findPath(Entity batchRoot, Entity entity) {
        DependencyTree tree  = batchFetchEntities.get(batchRoot);
        if (tree == null) {
            tree = new DependencyTree();
            tree.build(Collections.singleton(new EntityDependencyTreeNode(batchRoot)), false);
        }
        else {
            for (EntityDependencyTreeNode node: tree.<EntityDependencyTreeNode>getNodes()) {
               node.requireRebuildIfNotAllDependenciesFetched();
            }
            tree.build(Collections.emptyList(), false);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("yuml: " + tree.generateYumlString(Collections.emptySet()));
        }
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
        workingSet.addAll(asBatchGroup(path.getEntity()));
        
        EntityPath p = path;
        while(p != null) {
            Set<Entity> nextWorkingSet = new HashSet<>();
            for (Entity e: workingSet) {
                Node node = e.getChild(p.getNode().getName());
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
        result.addAll(workingSet);
        return result;
    }


    private Collection<Entity> asBatchGroup(Entity entity) {
    	if (!batchFetchEntities.containsKey(entity)) {
    		return Collections.emptySet();
    	}
    	for (Set<WeakReference<Entity>> set: batchGroups) {
    		if (containsEntity(set, entity)) {
    			return defererence(set);
    		}
    	}
		return Collections.singleton(entity);
	}


	public final class EntityPath {
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
        public String getPathAsString() {
        	String me = entity.toString() + "." + getNodeAsString();
        	if (next != null) {
        		return me + "." + next.getRestOfPathAsString();
        	}
        	return me;
        }

        private String getRestOfPathAsString() {
        	String me = getNodeAsString();
        	if (next != null) {
        		return me + "." + next.getRestOfPathAsString();
        	}
        	return me;
        }
        
        private String getNodeAsString() {
        	if (node instanceof RefNode) {
        		RefNode rn = (RefNode)node;
        		return rn.getName() + "(" + (rn.getReference() != null ? rn.getReference().toString() : "null") + ")";
        	}
        	else if (node instanceof ToManyNode) {
        		ToManyNode tmn = (ToManyNode)node;
        		return tmn.getName() + "[]";
        	}
        	else if (node instanceof ValueNode) {
        		return node.getName();
        	}
        	throw new IllegalArgumentException("unkndown node type " + node.getClass());
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

       if (attemptBatchFetch(toManyNode, fetchInternal)) {
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
           final Object primaryKeyOfOneSide = toManyNode.getParent().getKeyValue();
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
           fromQo.where(fromPk.equal(toManyNode.getParent().getKeyValue()));

           try {
               ctx.performQuery(fromQo);
               toManyNode.setFetched(true);
               toManyNode.refresh();
           } catch (Exception x) {
               throw new IllegalStateException("Error performing fetch", x);
           }
       }
   }

   private void fetchToManys(Collection<ToManyNode> toFetch, boolean fetchInternal) {
       ToManyNode firstToMany = toFetch.iterator().next();
       LOG.debug("BATCH FETCHING TOMANYS: " + toFetch.size() + " " + firstToMany.getNodeType().getEntityType().getTableName() + "." + firstToMany.getNodeType().getName());
       final NodeType toManyDef = firstToMany.getNodeType();

       //get the name of the node/property which we need to filter on to get the correct entities back on the many side
       final String foreignNodeName = toManyDef.getForeignNodeName();
       if (foreignNodeName != null) {
           QueryObject<Object> qo = ctx.getQuery(firstToMany.getEntityType(), fetchInternal);
           if (qo == null) {
               qo = new QueryObject<Object>(firstToMany.getEntityType().getInterfaceName());
           }

           final QProperty<Object> manyFk = new QProperty<Object>(qo, foreignNodeName);
           final Set<Object> primaryKeysOfOneSide = toEntityKeyValuesTmn(toFetch);
           qo.where(manyFk.in(primaryKeysOfOneSide));
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
               NodeType datatypeNodeType = firstToMany.getEntityType().getNodeType(toManyDef.getJoinProperty(), true);
               EntityType datatype = ctx.getDefinitions().getEntityTypeMatchingInterface(datatypeNodeType.getRelationInterfaceName(), true);
               QueryObject<Object> qdatatype = ctx.getQuery(datatype, fetchInternal);
               if (qdatatype == null) {
                  qdatatype = new QueryObject<Object>(datatype.getInterfaceName());
               }
               qo.addLeftOuterJoin(qdatatype, datatypeNodeType.getName());
           }

           try {
               ctx.performQuery(qo);
               for (ToManyNode toManyNode: toFetch) {
                   toManyNode.setFetched(true);
                   toManyNode.refresh();
               }
           } catch (Exception x) {
               throw new IllegalStateException("Error performing fetch", x);
           }
       } else {
           /*
            * get the query for loading the entity which has the relation we want to fetch
            * ie the template query for when we want to fetch the datatypes.
            */
           final QueryObject<Object> fromQo = ctx.getDefinitions().getQuery(firstToMany.getParent().getEntityType());
           /*
            * gets the query for loading the entity which the relation fulfills
            * ie the datatype query which the template wants to load
            */
           final QueryObject<Object> toQo = ctx.getDefinitions().getQuery(firstToMany.getEntityType());
           /*
            * add the join from template to query
            * the query generator knows to include the join table
            */
           fromQo.addLeftOuterJoin(toQo, firstToMany.getName());
           /*
            * constrain from query to only return data for the entity we are fetching for
            * ie constrain the template query by the id of the template we are fetching for
            */
           final QProperty<Object> fromPk = new QProperty<Object>(fromQo, firstToMany.getParent().getKey().getName());
           final Set<Object> primaryKeysOfOneSide = toEntityKeyValuesTmn(toFetch);
           fromQo.where(fromPk.in(primaryKeysOfOneSide));

           try {
               ctx.performQuery(fromQo);
               for (ToManyNode toManyNode: toFetch) {
                   toManyNode.setFetched(true);
                   toManyNode.refresh();
               }
           } catch (Exception x) {
               throw new IllegalStateException("Error performing fetch", x);
           }
       }
   }


    private boolean attemptBatchFetch(ToManyNode toManyNode, boolean fetchInternal) {
    	Set<Entity> entites = new LinkedHashSet<>();
    	Collection<Entity> batchGroup = asBatchGroup(toManyNode.getParent());
    	if (!batchGroup.isEmpty()) {
    		/*
    		 * ToManyNode parent is a batch fetch entity which is part of a batch group.
    		 */
    		entites.addAll(batchGroup);
    	}
    	else {
	    	Set<EntityPath> paths = calculatePathsFromBatchFetchEntities(toManyNode.getParent());
	        EntityPath shortest = findShortestPath(paths);
	        if (shortest == null) {
	        	return false;
	        }
	        entites.addAll(findAllEntitiesWithSamePath(shortest));
    	}
        Set<ToManyNode> toFetch = entites.stream()
                .map(e -> e.getChild(toManyNode.getName(), ToManyNode.class))
                .collect(Collectors.toSet());
        //TODO:use database specific limit
        for (Collection<ToManyNode> set : batchesOf(toFetch, 1000)) {
          fetchToManys(set, fetchInternal);
        }
        return true;
    }

    private <T> Collection<Collection<T>> batchesOf(Set<T> set, int maxSize) {
       if (set.size() < maxSize) {
           return Collections.singletonList(set);
       }
       else {
           return batchesOf(new ArrayList<>(set), maxSize);
       }
    }
    private <T> Collection<Collection<T>> batchesOf(List<T> list, int maxSize) {
        Collection<Collection<T>> result = new LinkedList<>();
        int from  = 0;
        int to = from + maxSize;
        while(from < list.size()) {
            if (list.size() >= to) {
                result.add(list.subList(from, to));
            }
            else {
                result.add(list.subList(from, list.size()));
            }
            from += maxSize;
            to += maxSize;
        }
        return result;
    }


}
