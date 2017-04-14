package scott.barleydb.api.persist;

import java.util.ArrayList;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityContextHelper;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;

public class DependencyTree {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyTree.class);

    private final Collection<Node> dependencyOrder = new LinkedHashSet<>();
    private final Map<Entity, Node> nodes = new LinkedHashMap<>();

    private final EntityContext ctx;
    private final EntityContext dctx;
    private final Map<Entity, OrphanCheck> orphanChecks = new HashMap<>();

    public DependencyTree(EntityContext refCtx) {
        this.ctx = refCtx;
        this.dctx = refCtx.newEntityContext();
    }

    /**
     *
     * @return a copy of the ordered list of operations
     */
    public List<Operation> getOrder() {
        List<Operation> result = new ArrayList<>(dependencyOrder.size());
        for (Node node: dependencyOrder) {
            result.add( node.operation  );
        }
        return result;
    }

    public void build(Collection<Operation> operations) throws SortServiceProviderException, SortQueryException {
        for (Operation operation: operations) {
            createOrGetNode(operation.entity, operation.opType);
        }

        boolean somethingToDo;
        do {
            somethingToDo = false;
            /*
             * building node dependencies can cause new nodes to be added
             * so do a small loop until no more nodes are added
             */
            int numberOfNodes;
            do {
                numberOfNodes = nodes.size();
                LOG.debug("Processing node dependencies, current set contains {} items.", nodes.size());
                for (Node node: new ArrayList<>(nodes.values())) {
                    somethingToDo |= !node.isBuiltDependencies();
                    node.buildDependencies();
                }
            }
            while(numberOfNodes < nodes.size());

            /*
             * now that we have built Nodes for every single entity operation in memory
             * start querying to include delete operations for data which purely in the DB.
             */
            LOG.debug("Checking if any of the {} operations require orphan checks", nodes.size());
            int currentAmountOfOrphanChecks = orphanChecks.size();
            for (Node node: nodes.values()) {
                somethingToDo |= !node.isBuiltOrphanChecks();
                node.buildOprhanChecks();
            }
            if (LOG.isDebugEnabled()) {
                if (orphanChecks.size() > currentAmountOfOrphanChecks) {
                    LOG.debug("Added {} orphan checks", orphanChecks.size() - currentAmountOfOrphanChecks);
                }
                else {
                    LOG.debug("No new orphan checks added");
                }
            }

            numberOfNodes = nodes.size();
            while (pendingOrphanChecks()) {

                /*
                 * query for the data as efficiently as possible
                 */
                performOrphanChecks();
                /*
                 * integrate into the dependency tree
                 */
                integrateNewDeleteOperationsIntoDependencyTree();
            }
            somethingToDo |= nodes.size() > numberOfNodes;
        }
        while(somethingToDo);

        calculateDependencyOrder();
    }

    /**
     * populates the dependencyOrder list  by processing the dependency nodes.
     */
    private void calculateDependencyOrder() {
        Collection<Node> readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(false);
        while(!readyNodes.isEmpty()) {
            dependencyOrder.addAll( readyNodes );
            LOG.debug("Added {} Nodes to dependecy order (size={})", readyNodes.size(), dependencyOrder.size());
            readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(false);
        }
        /*
         * now process deletes
         */
        readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(true);
        while(dependencyOrder.size() < nodes.size()) {
            if (readyNodes.isEmpty()) {
                throw new IllegalStateException("Could not calculate the dependency order.");
            }
            dependencyOrder.addAll( readyNodes );
            LOG.debug("Added {} Nodes to deletes order (size={})", readyNodes.size(), dependencyOrder.size());
            readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(true);
        }
    }

    private Collection<Node> getUnprocessedNodesWithNoUnprocessedDependencies(boolean deletesMode) {
        Collection<Node> result = new LinkedList<>();
        for (Node node: nodes.values()) {
            if (deletesMode !=  node.operation.isDelete()) {
                continue;
            }
            if (!node.isProcessed() && !node.hasUnprocessedDependecies()) {
                LOG.trace("Node {} is ready for processing", node);
                result.add( node );
            }
        }
        return result;
    }

    private Node getReffedDependencyNode(RefNode refNode) {
        Entity dependentEntity = refNode.getReference();
        return dependentEntity != null ? nodes.get( dependentEntity ) : null;
    }


    /**
     * creates the node for the given entity and operation
     * @param entity
     * @param opType
     * @return the Node or null if it an operation already exists for the entity
     */
    private Node createOrGetNode(Entity entity, OperationType opType) {
        return createOrGetNode(entity, opType, true);
    }

    /**
     * creates the node for the given entity and operation
     * @param entity
     * @param opType
     * @param orphanCheck if an orphan check is required.
     * @return the Node or null if it an operation already exists for the entity
     */
    private Node createOrGetNode(Entity entity, OperationType opType, boolean orphanCheck) {
        Node node = nodes.get(entity);
        if (node == null) {
            node = new Node(new Operation(entity, opType), orphanCheck);
            nodes.put(entity, node);
        }
        return node;
    }


    private class Node {
        private final Operation operation;
        private final Set<Node> dependency = new LinkedHashSet<>();

        private boolean builtDependencies = false;
        private boolean builtOrphanChecks = false;

        public Node(Operation operation, boolean orphanChecksRequired) {
            this.operation = operation;
            if (!orphanChecksRequired) {
                builtOrphanChecks = true;
            }
        }

        public boolean isBuiltDependencies() {
            return builtDependencies;
        }

        public boolean isBuiltOrphanChecks() {
            return builtOrphanChecks;
        }

        public void buildOprhanChecks() {
            if (builtOrphanChecks) {
                return;
            }
            builtOrphanChecks = true;
            if (operation.entity.getKey().getValue() == null) {
                /*
                 * this entity has no PK, it cannot exist in the DB, therefore nothing to do
                 */
                return;
            }
            /*
             * we need to check what should be deleted from the database based on the set of operations.
             * ie entities which are owned by RefNodes but no longer pointed to need to be deleted, the same goes for to many nodes.
             */
            LOG.debug("Analysing orphan checks for {}", this);
            /*
             * go through our FK refs and build dependencies TO them
             */
            for (RefNode ref: operation.entity.getChildren(RefNode.class)) {
                /*
                 * it doesn't matter what the reference is currently pointing at
                 * we need to load from the DB and get the REAL reference.
                 *
                 * The only relevant point is that we own the reference.
                 */
                if (ref.getNodeType().isOwns()) {
                    addOrphanCheck(operation.entity);
                    return;
                }
            }

            /*
             * deleting the N side of a relation only makes sense if the 1 side is being deleted.
             * ie changing the syntax.id of a mapping to point somewhere else would be strange and, yes would remove the mapping from the syntax (strictly speaking)
             * but it is a non-sensical operation.
             */
            if (operation.isDelete()) {
                /*
                 * go through our FK refs and build dependencies FROM them
                 */
                for (ToManyNode toManyNode: operation.entity.getChildren(ToManyNode.class)) {
                    /*
                     * it doesn't matter what the reference is currently pointing at
                     * we need to load from the DB and get the REAL reference.
                     *
                     * The only relevant point is that we own the reference.
                     */
                    if (toManyNode.getNodeType().isOwns()) {
                        addOrphanCheck(operation.entity);
                        return;
                    }
                }
            }
            return;
        }

        public void buildDependencies() {
            if (builtDependencies) {
                return;
            }
            builtDependencies = true;
            LOG.debug("Building dependencies for {}", this);
            /*
             *  first process insert or update logic
             */
            if (!operation.isDelete()) {
                /*
                 * go through our FK refs and build dependencies TO them
                 */
                for (RefNode ref: operation.entity.getChildren(RefNode.class)) {
                    /*
                     * look to see if the reference points to anything
                     */
                    Entity entity = ref.getReference(false);
                    if (entity != null) {
                        /*
                         * check if we already have a dependency node for the reff'd entity.
                         */
                        Node dependentNode = getReffedDependencyNode(ref);
                        if (dependentNode == null) {
                            /*true
                             *  We need to add a new node to our dependency tree.
                             */

                            /*
                             * if the entity is already in the DB
                             * and we own it
                             * and we have it loaded into memory
                             * then we should perform an update
                             */
                            if (entity.isUnclearIfInDatabase()) {
                                throw new IllegalStateException("Entities should be clearly defined at this point: " + entity);
                            }
                            if (entity.isClearlyInDatabase() && ref.getNodeType().isOwns() && !entity.isFetchRequired()) {
                                dependentNode  = createOrGetNode(entity, OperationType.UPDATE);
                            }
                            /*
                             * if the entity it not in the database then it has to be inserted
                             * otherwise we cannot set the FK
                             */
                            else if (entity.isClearlyNotInDatabase()) {
                                dependentNode  = createOrGetNode(entity, OperationType.INSERT);
                            }
                            else if (ref.getNodeType().isDependsOn()) {
                                dependentNode  = createOrGetNode(entity, OperationType.DEPENDS);
                            }
                            else {
                                /*
                                 * the dependency is there but there is nothing to.do.
                                 */
                                dependentNode  = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        dependency.add( dependentNode );
                        LOG.debug("Added dependency from {} to {}", this, dependentNode);
                    }
                }

                /*
                 * go through our to many refs and build dependencies FROM them
                 */
                for (ToManyNode toManyNode: operation.entity.getChildren(ToManyNode.class)) {
                    if (toManyNode.getList().isEmpty() && !toManyNode.isFetched()) {
                        /*
                         * if the many relation was never fetched, and it contains nothing, so skip.
                         */
                        continue;
                    }
                    for (Entity entity : toManyNode.getList()) {
                        /*
                         * check if we already have a dependency node for the reff'd entity.
                         */
                        Node dependentNode = getDependencyNode(entity);
                        if (dependentNode == null) {
                            /*
                             * if the entity is already in the DB
                             * and we own it
                             * and we have it loaded into memory
                             * then we should perform an update
                             */
                            if (entity.isClearlyInDatabase() && toManyNode.getNodeType().isOwns() && !entity.isFetchRequired()) {
                                dependentNode  = createOrGetNode(entity, OperationType.UPDATE);
                            }
                            /*
                             * if the entity it not in the database then it has to be inserted
                             * otherwise we cannot set the FK
                             */
                            else if (entity.isClearlyNotInDatabase()) {
                                dependentNode  = createOrGetNode(entity, OperationType.INSERT);
                            }
                            else if (toManyNode.getNodeType().isDependsOn()) {
                                dependentNode  = createOrGetNode(entity, OperationType.DEPENDS);
                            }
                            else {
                                /*
                                 * the dependency is there but there is nothing to.do.
                                 */
                                dependentNode  = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        /*
                         * the direction of the FK dependency is reversed for ToManyNodes
                         */
                        dependentNode.dependency.add(this);
                        LOG.debug("Added dependency from {} to {}", this, dependentNode);

                    }
                }
            }
            else {        // TODO Auto-generated method stub

                /*
                 * delete operations are handles somewhere else
                 */
            }
            return;
        }

        /**
         * this can only be called when integrating new delete operations into the contetx
         * which have been directly pulled from the database.
         *
         */
        public void buildDeleteDependencies() {
            if (builtDependencies) {
                return;
            }
            builtDependencies = true;
            LOG.debug("Building delete dependencies for {}", this);
            /*
             *  only care about deletes for this methos
             */
            if (operation.isDelete()) {
                /*
                 * go through our to many refs and build dependencies FROM them
                 */
                for (ToManyNode toManyNode: operation.entity.getChildren(ToManyNode.class)) {
                    if (toManyNode.getList().isEmpty() && !toManyNode.isFetched()) {
                        /*
                         * if the many relation was never fetched, and it contains nothing, so skip.
                         */
                        continue;
                    }
                    for (Entity entity : toManyNode.getList()) {
                        /*
                         * check if we already have a dependency node for the reff'd entity.
                         */
                        Node dependentNode = getDependencyNode(entity);
                        if (dependentNode == null) {
                            /*
                             *  We need to add a new node to our dependency tree.
                             *  We must have had a recursive data structure, this is the only
                             *  time it happens with delete logic...
                             */


                            /*
                             * if we own the reffed entity then the reffed entity must be deleted.
                             *
                             * in any case - there is a clear dependency that we must be deleted before the
                             * reffed entity, so we depend on it.
                             */
                            if (toManyNode.getNodeType().isOwns()) {
                                dependentNode  = createOrGetNode(entity, OperationType.DELETE);
                            }
                            else {
                                dependentNode  = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        /*
                         * express the dependency, things which refer to us must be deleted before us.
                         */
                        dependency.add( dependentNode );
                        LOG.debug("Added dependency from {} to {}", this, dependentNode);
                    }
                }

                /*
                 * go through our FK refs and build dependencies FROM them TO us
                 */
                for (RefNode ref: operation.entity.getChildren(RefNode.class)) {
                    /*
                     * look to see if the reference points to anything
                     */
                    Entity entity = ref.getReference(false);
                    if (entity != null) {
                        /*
                         * check if we already have a dependency node for the reff'd entity.
                         */
                        Node dependentNode = getReffedDependencyNode(ref);
                        if (dependentNode == null) {
                            /*
                             *  We need to add a new node to our dependency tree.
                             *  We must have had a recursive data structure, this is the only
                             *  time it happens with delete logic...
                             */

                            /*
                             * If a syntax has a FK reference to a structure then we can express a delete
                             * dependency that the structure depends on us if it wants to be deleted.
                             */

                            if (ref.getNodeType().isOwns()) {
                                dependentNode  = createOrGetNode(entity, OperationType.DELETE);
                            }
                        }
                        /*
                         * express the dependency, we must be deleted before our ref
                         */
                        dependentNode.dependency.add( this );
                        LOG.debug("Added dependency from {} to {}", this, dependentNode);
                    }
                }
            }
            else {        // TODO Auto-generated method stub

                /*
                 * non-delete operations are handles somewhere else
                 */
            }
            return;
        }

        public boolean isProcessed() {
            return dependencyOrder.contains(this);
        }

        public boolean hasUnprocessedDependecies() {
            return !dependency.isEmpty() && !dependencyOrder.containsAll(dependency);
        }

        @Override
        public String toString() {
            return "Node [" + operation + "]";
        }
    }

    public class OrphanCheck {
        private final Entity entity;
        private Entity result;
        private boolean checkWasPerformed;

        public OrphanCheck(Entity entity) {
            this.entity = entity;
        }

        public boolean checkWasPerformed() {
            return checkWasPerformed;
        }

        public boolean isCheckWasPerformed() {
            return checkWasPerformed;
        }

        public void setCheckWasPerformed(boolean checkWasPerformed) {
            this.checkWasPerformed = checkWasPerformed;
        }

        public Entity getResult() {
            return result;
        }

        public void setResult(Entity result) {
            this.result = result;
        }
    }

    private boolean pendingOrphanChecks() {
        for (OrphanCheck oc: orphanChecks.values()) {
            if (oc.getResult() == null) {
                return true;
            }
        }
        return false;
    }

    private void performOrphanChecks() throws SortServiceProviderException, SortQueryException {
        LOG.debug("Peforming orphan checks.......");
        QueryBatcher qbatcher = new QueryBatcher();

        Map<EntityId, OrphanCheck> lookup = new HashMap<>();

        for (OrphanCheck orphCheck: new ArrayList<>(orphanChecks.values())) {
            if (orphCheck.checkWasPerformed()) {
                LOG.debug("Already performed orphan check for {}", orphCheck.entity);
                continue;
            }
            /*
             * at the top level of this loop we just need 1 orphCheck of each EntityType.
             */
            EntityId eid = new EntityId(orphCheck.entity.getEntityType(),  orphCheck.entity.getKey().getValue());
            /*
             * create a query for the entity type, which includes all orph checks of the same type
             */
            QueryObject<Object> query = createQueryForReferencesToDelete( orphCheck.entity );
            orphCheck.setCheckWasPerformed(true);
            lookup.put(eid, orphCheck);

            for (OrphanCheck sub: orphanChecks.values()) {
                if (sub == orphCheck) {
                    continue;
                }
                if (sub.checkWasPerformed()) {
                    LOG.debug("Already performed orphan check for {}", sub.entity);
                    continue;
                }
                if (sub.entity.getEntityType() != orphCheck.entity.getEntityType()) {
                    continue;
                }
                EntityId eidSub = new EntityId(sub.entity.getEntityType(),  sub.entity.getKey().getValue());
                QProperty<Object> keyProp = new QProperty<>(query, sub.entity.getKey().getName());
                query.or( keyProp.equal( sub.entity.getKey().getValue()));

                sub.setCheckWasPerformed(true);
                lookup.put(eidSub, sub);
            }

            qbatcher.addQuery(query);
        }

        /*
         * perform the queries and hold the data for later integration
         */
        if (!qbatcher.getQueries().isEmpty()) {
            LOG.debug("Performing query to load stuff to delete");
            dctx.performQueries( qbatcher );
            for (QueryResult<?> result: qbatcher.getResults()) {
                for (Entity entityResult: result.getEntityList()) {
                    OrphanCheck check = lookup.get(new EntityId(entityResult.getEntityType(), entityResult.getKey().getValue()));
                    LOG.debug("Set check result {}", entityResult);
                    check.setResult(entityResult);
                }
            }
        }
        else {
            LOG.debug("No queries were pepared, orphan checks is completed.");
        }
    }

    private void integrateNewDeleteOperationsIntoDependencyTree() {
        LOG.debug("Checking if we need to integrate new delete operations into the depdendecy tree.");

        for (OrphanCheck oc: orphanChecks.values()) {
            if (oc.result == null) {
                continue;
            }
            LOG.debug("Orphan Check has result {}", oc.result);
            Node dependencyNode = getDependencyNode(oc.entity);
            if (dependencyNode == null) {
                throw new IllegalStateException("Could not find dependency node for an orphan check entity: " + oc.entity);
            }
            /*
             * create delete operations for the orphan checks and the data it owns (data which it eagerly loaded in our case).
             */
            for (RefNode refNode: oc.result.getChildren(RefNode.class)) {
                if (!refNode.getNodeType().isOwns()) {
                    continue;
                }
                Entity reffedEntity = refNode.getReference(false);
                if (reffedEntity == null) {
                    continue;
                }
                if (dependencyNode.operation.isInsert()) {
                    //there is nothing to delete if the entity referring to us is not yet in the DB
                    continue;
                }
                if (dependencyNode.operation.isDelete()) {
                    LOG.debug("Adding delete operation for reffed entity {} because the owner is being deleted", reffedEntity);
                    copyIntoContextAndCreateDeleteNodes(reffedEntity);
                    continue;
                }
                if (dependencyNode.operation.isUpdate()) {
                    if (isOrphaningReference(dependencyNode.operation, refNode.getNodeType(), oc.result)) {
                        LOG.debug("Adding delete operation for reffed entity {} because the owner orphaned it", reffedEntity);
                        copyIntoContextAndCreateDeleteNodes(reffedEntity);
                        continue;
                    }
                    else {
                        LOG.debug("No orphan for {} reference to {}", oc.entity, refNode.getName());
                        continue;
                    }
                }
                throw new IllegalStateException("Unexpected state reached when processing orphan check " + oc + " for integrating into the dependency tree");
            }


            /*
             * create delete operations for the orphan checks and the data it owns (data which it eagerly loaded in our case).
             */
            for (ToManyNode refNode: oc.result.getChildren(ToManyNode.class)) {
                if (!refNode.getNodeType().isOwns()) {
                    continue;
                }
                if (!refNode.isFetched()) {
                    continue;
                }
                for (Entity reffedEntity: refNode.getList()) {
                    if (reffedEntity == null) {
                        continue;
                    }
                    if (dependencyNode.operation.isInsert()) {
                        //there is nothing to delete if the entity referring to us is not yet in the DB
                        continue;
                    }
                    /*
                     * we only ever delete the N side when the 1 side owns it and is being deleted.
                     */
                    if (dependencyNode.operation.isDelete()) {
                        LOG.debug("Adding delete operation for reffed entity {} because the owner is being deleted", reffedEntity);
                        copyIntoContextAndCreateDeleteNodes(reffedEntity);
                        continue;
                    }
                    throw new IllegalStateException("Unexpected state reached when processing orphan check " + oc + " for integrating into the dependency tree");
                }
            }


            for (Node node: nodes.values()) {
                node.buildDependencies();
            }
        }
    }


    /**
     * puts the entity into the main context creates delete operations for the whole entity tree of ownership
     * @param entity
     */
    private void copyIntoContextAndCreateDeleteNodes(Entity entity) {
        LOG.debug("Copying {} and all children into main ctx and create delete operations for them all.", entity);
        Collection<Entity> toCopy = EntityContextHelper.findAllEntites(entity);

        List<Entity> copied = EntityContextHelper.addEntities(toCopy, ctx, true);
        EntityContextHelper.copyRefStates(dctx, ctx, copied, new EntityContextHelper.EntityFilter() {
            @Override
            public boolean includesEntity(Entity entity) {
                return true;
            }
        });

        List<Node> nodes = new LinkedList<>();
        for (Entity e: copied) {
            LOG.debug("Adding delete operation for entity {}", entity);
            nodes.add( createOrGetNode(e, OperationType.DELETE, false) );
        }
        for (Node node: nodes) {
            node.buildDeleteDependencies();
        }
    }


    /**
     * checks if a given operation would cause the entity to be orhaned
     * @param operation
     * @param potentalOrphan
     * @return
     */
    private boolean isOrphaningReference(Operation operation, NodeType nodeType, Entity potentalOrphan) {
        Object newFkRefValue = operation.entity.getChild( nodeType.getName(), RefNode.class).getEntityKey();
        Object origFkRefValue = potentalOrphan.getChild( nodeType.getName(), RefNode.class).getEntityKey();
        return origFkRefValue != null && origFkRefValue.equals( newFkRefValue );
    }

    private QueryObject<Object> createQueryForReferencesToDelete(Entity entity) {
        LOG.debug("Creating query to load all data owned by {} into the delete context", entity);

        QueryObject<Object> query = dctx.getUnitQuery( entity.getEntityType() );

        //filter on PK
        final QProperty<Object> keyProp = new QProperty<Object>(query, entity.getKey().getName());
        query.where( keyProp.equal( entity.getKey().getValue() ));
        LOG.trace("Added property {} to query {}", keyProp.getName(), query.getTypeName());

        /*
         * add joins to all owning ToManyNodes
         */
        addJoinsForAllOwningRefs(entity.getEntityType(), query, new HashSet<NodeType>());

        return query;
    }

    private void addJoinsForAllOwningRefs(EntityType entityType, QueryObject<Object> query, Set<NodeType> alreadyProcessed) {
        /*
         * add joins to all owning Refs
         */
        for (NodeType nodeType: entityType.getNodeTypes()) {
            if (nodeType.isOwns()) {
                /*
                 * add left outer joins for owning relationships
                 */
                if (alreadyProcessed.add(nodeType)) {
                    QueryObject<Object> to = new QueryObject<>(nodeType.getRelationInterfaceName());
                    query.addLeftOuterJoin(to, nodeType.getName());
                    LOG.trace("Added left outer join from {} with property {} to {}", query.getTypeName(), nodeType.getName(), nodeType.getRelationInterfaceName());

                    EntityType reffedEntityType = entityType.getDefinitions().getEntityTypeMatchingInterface( nodeType.getRelationInterfaceName(), true);
                    addJoinsForAllOwningRefs(reffedEntityType, to, alreadyProcessed);
                }

            }
        }
    }

    public Node getDependencyNode(Entity entity) {
        return nodes.get(entity);
    }

    public void addOrphanCheck(Entity entity) {
        orphanChecks.put(entity, new OrphanCheck(entity));
        // TODO Auto-generated method stub
        LOG.debug("Adding orphan check  {}", entity);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dctx == null) ? 0 : dctx.hashCode());
        result = prime * result + ((dependencyOrder == null) ? 0 : dependencyOrder.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        result = prime * result + ((orphanChecks == null) ? 0 : orphanChecks.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DependencyTree other = (DependencyTree) obj;
        if (dctx == null) {
            if (other.dctx != null)
                return false;
        } else if (!dctx.equals(other.dctx))
            return false;
        if (dependencyOrder == null) {
            if (other.dependencyOrder != null)
                return false;
        } else if (!dependencyOrder.equals(other.dependencyOrder))
            return false;
        if (nodes == null) {
            if (other.nodes != null)
                return false;
        } else if (!nodes.equals(other.nodes))
            return false;
        if (orphanChecks == null) {
            if (other.orphanChecks != null)
                return false;
        } else if (!orphanChecks.equals(other.orphanChecks))
            return false;
        return true;
    }

}

class EntityId {
    private final EntityType et;
    private final Object keyValue;
    public EntityId(EntityType et, Object keyValue) {
        this.et = et;
        this.keyValue = keyValue;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((et == null) ? 0 : et.hashCode());
        result = prime * result + ((keyValue == null) ? 0 : keyValue.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityId other = (EntityId) obj;
        if (et == null) {
            if (other.et != null)
                return false;
        } else if (!et.equals(other.et))
            return false;
        if (keyValue == null) {
            if (other.keyValue != null)
                return false;
        } else if (!keyValue.equals(other.keyValue))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "EntityId [et=" + et + ", keyValue=" + keyValue + "]";
    }
}


