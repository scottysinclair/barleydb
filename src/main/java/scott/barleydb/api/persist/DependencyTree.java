package scott.barleydb.api.persist;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.EntityJuggler;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.dependency.diagram.DependencyDiagram;
import scott.barleydb.api.dependency.diagram.Link;
import scott.barleydb.api.dependency.diagram.LinkType;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.server.jdbc.query.QueryResult;

/**
 * Generates an operations dependency tree so that the order of entity operations can be calculated.<br/>
 * <br/>
 * The DependencyTree takes into account the different types of relations between entities and will also calculate entites which have to be deleted
 * due to orphan removal.
 * @author scott.sinclair
 *
 */
public class DependencyTree implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DependencyTree.class);

    /**
     * the final calculated dependency order
     */
    private final List<Node> dependencyOrder = new LinkedList<>();

    /**
     * all of the operation Nodes in the dependency tree
     */
    private final Map<Entity, Node> nodes = new LinkedHashMap<>();

    /**
     * the entity context containing the entities on which operations will be performed.
     */
    private final EntityContext ctx;

    /**
     * the 'delete entity context' used to load and therefore verify the existance of entities which will require removal.
     */
    private final EntityContext dctx;

    /**
     * a map of orphan checks starting from an Entity which has an update or delete operation on it.
     */
    private final Map<Entity, OrphanCheck> orphanChecks = new HashMap<>();

    /**
     * if true the dependency order will try and ensure that entities of the same type will lie contiguously with one another.
     */
    private final boolean tryAndOrderInBatches;

    public DependencyTree(EntityContext refCtx, boolean tryAndOrderInBatches) {
        this.ctx = refCtx;
        this.dctx = refCtx.newEntityContextSharingTransaction();
        this.tryAndOrderInBatches = tryAndOrderInBatches;
    }

    /**
     * generates a dependency diagram in the system temporary directory
     */
    public void generateDiagram() {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + "/dep-tree-" + System.currentTimeMillis() + ".jpeg");
            generateDiagram(file);
            LOG.warn("GENERATED DIAGRAM AT " + file.getPath());
        }
        catch(Exception x) {
            LOG.warn("Could not generate diagram!", x);
        }
    }

    /**
     * Generates a dependency diagram at the given location.
     * @param diagram
     * @throws IOException
     */
    public void generateDiagram(File diagram) throws IOException {
        createDiagram().generate(diagram);
    }

    public String generateDiagramYumlString() {
        return createDiagram().toYumlString();
    }

    private DependencyDiagram createDiagram() {
        DependencyDiagram diag = new DependencyDiagram();
        Link firstLink = null;
        for (Node node: nodes.values()) {
            for (Node depNode: node.dependency) {
                LinkType linkType = LinkType.DEPENDENCY;
                if (node.operation.opType == OperationType.NONE) {
                    linkType = LinkType.DEPENDENCY_DASHED;
                }
                Link l = diag.link(genNodeDiagramName(node), genNodeDiagramName(depNode), "", linkType);
                if (firstLink == null) {
                    firstLink = l;
                }
            }
        }
        return diag;
    }

    private String genNodeDiagramName(Node node) {
        String entityName = node.operation.entity.toString();
        return entityName + "|" + node.operation.opType;

    }

    public String dumpCurrentState() {
        StringBuilder sb = new StringBuilder();
        sb.append("DEPENDENCY ORDER\n");
        for (Node node : dependencyOrder) {
            sb.append(node.operation);
            sb.append('\n');
        }
        Collection<Node> missing = new ArrayList<>(nodes.values());
        missing.removeAll(dependencyOrder);
        if (!missing.isEmpty()) {
            sb.append("MISSING FROM DEPENDENCY ORDER\n");
            for (Node node : missing) {
                sb.append(node.operation);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     *
     * @return a copy of the ordered list of operations
     */
    public List<Operation> getOrder() {
        List<Operation> result = new ArrayList<>(dependencyOrder.size());
        for (Node node : dependencyOrder) {
            result.add(node.operation);
        }
        return result;
    }

    /**
     * Builds the dependency tree based on the collection of operations to perform.
     *
     * @param operations
     * @throws SortServiceProviderException
     * @throws BarleyDBQueryException
     */
    public void build(Collection<Operation> operations) throws SortServiceProviderException, BarleyDBQueryException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("---------------------------------------------------------------------------------------");
            LOG.debug(" STARTING DEPENDENCY TREE BUILD ");
            LOG.debug("---------------------------------------------------------------------------------------");
        }

        LOG.debug(" adding initial set of operations handed over to us...");

        for (Operation operation : operations) {
            createOrGetNode(operation.entity, operation.opType);
        }

        boolean allDependenciesBuilt;
        boolean allOrphanChecksBuilt;
        do {
            /*
             * building node dependencies can cause new nodes to be added so do
             * a small loop until no more nodes are added
             */
            int numberOfNodes;
            do {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "---------------------------------------------------------------------------------------");
                    LOG.debug("- processing node dependencies, current pending checks {}.",
                            printNodesPendingDependencyChecks(nodes));
                }
                numberOfNodes = nodes.size();
                for (Node node : new ArrayList<>(nodes.values())) {
                    if (!node.isBuiltDependencies()) {
                        /*
                         * building dependencies can cause new dependencies to be found.
                         */
                        node.buildDependencies();
                    }
                }
            } while (numberOfNodes < nodes.size());

            LOG.debug("---------------------------------------------------------------------------------------");
            LOG.debug("- FINISHED processing node dependencies current set contains {}.", print(nodes.values()));
            LOG.debug("- We have built nodes for every single entity operation in memory");
            LOG.debug(
                    "- We need to now query the database to include delete operations records which were not loaded.");
            LOG.debug("---------------------------------------------------------------------------------------");

            LOG.debug("- Checking if any of the {} operations in our dependency tree require orphan checks",
                    nodes.size());
            for (Node node : nodes.values()) {
                if (!node.isBuiltOrphanChecks()) {
                    node.buildOprhanChecks();
                }
            }

            while (pendingOrphanChecks()) {
                LOG.debug("- Processing pending orphan checks...");

                /*
                 * query for the data as efficiently as possible
                 */
                performOrphanChecks();
                /*
                 * integrate into the dependency tree
                 */
                integrateNewDeleteOperationsIntoDependencyTree();
            }

            allDependenciesBuilt = allNodesHaveDependenciesBuilt();
            allOrphanChecksBuilt = allNodesHaveOrphanChecksBuilt();

            if (allDependenciesBuilt) {
                LOG.debug("All dependency checks built");
            } else {
                LOG.debug("Dependency checks still pending...");
            }
            if (allOrphanChecksBuilt) {
                LOG.debug("All orphan checks built");
            } else {
                LOG.debug("Orphan checks still pending...");
            }
        } while (!allDependenciesBuilt || !allOrphanChecksBuilt);

        LOG.debug("FINALLY calculating dependency order...");
        calculateDependencyOrder();
    }

    private boolean allNodesHaveDependenciesBuilt() {
        for (Node node : nodes.values()) {
            if (!node.isBuiltDependencies()) {
                return false;
            }
        }
        return true;
    }

    private boolean allNodesHaveOrphanChecksBuilt() {
        for (Node node : nodes.values()) {
            if (!node.isBuiltOrphanChecks()) {
                return false;
            }
        }
        return true;
    }

    private String print(Collection<Node> nodes) {
        if (nodes.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[\n");
        for (Node node : nodes) {
            sb.append(node);
            sb.append(",\n");
        }
        sb.setLength(sb.length() - 2);
        sb.append("\n]");
        return sb.toString();
    }

    private String printNodesPendingDependencyChecks(Map<Entity, Node> nodes) {
        StringBuilder sb = new StringBuilder("[\n");
        for (Node node : nodes.values()) {
            if (!node.isBuiltDependencies()) {
                sb.append(node);
                sb.append(",\n");
            }
        }
        sb.setLength(sb.length() - 2);
        sb.append("\n]");
        return sb.toString();
    }

    /**
     * Populates the dependencyOrder list by processing the dependency nodes.
     */
    private void calculateDependencyOrder() {
//           generateDiagram();
        if (LOG.isDebugEnabled()) {
          logDebugSummaryReport();
        }
        Collection<Node> readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(false);
        int count = 0;
        while (count < 1000 && dependencyOrder.size() < countNodeRequiredInDependencyOrder(true)) {
            if (tryAndOrderInBatches) {
                //for batch ordering we need to recalculate the ready nodes each time we add to dependencyOrder.
                Node next = null;
                if (!dependencyOrder.isEmpty()) {
                    next = findMatchingNode(readyNodes, dependencyOrder.get( dependencyOrder.size() - 1));
                }
                if (next == null) {
                    next = readyNodes.iterator().next();
                }
                dependencyOrder.add( next );
                next.notifyAddedToDependencyOrder();
                LOG.debug("added single node to dependecy order {}", next.toString());
            }
            else {
                dependencyOrder.addAll(readyNodes);
                for (Node n: readyNodes) {
                    n.notifyAddedToDependencyOrder();
                }
                LOG.debug("added following nodes to dependecy order {}", print(readyNodes));
            }
            readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(false);
            if (readyNodes.isEmpty()) {
                count++;
            }
            else {
                count = 0;
            }
        }
        if (count == 1000) {
            LOG.error("Could not calculate dependencies:\n{}", generateDiagramYumlString());
            throw new IllegalStateException("Infinite loop, calculating dependency order");
        }
        /*
         * now process deletes
         */
        readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(true);
        count = 0;
        while (count < 100 && dependencyOrder.size() < countNodeRequiredInDependencyOrder(false)) {
            if (readyNodes.isEmpty()) {
                LOG.error(dumpCurrentState());
//                generateDiagram();
                throw new IllegalStateException("Could not calculate the dependency order.");
            }
            if (tryAndOrderInBatches) {
                //for batch ordering we need to recalculate the ready nodes each time we add to dependencyOrder.
                Node next = null;
                if (!dependencyOrder.isEmpty()) {
                    next = findMatchingNode(readyNodes, dependencyOrder.get( dependencyOrder.size() - 1));
                }
                if (next == null) {
                    next = readyNodes.iterator().next();
                }
                dependencyOrder.add( next );
                next.notifyAddedToDependencyOrder();
                LOG.debug("added single node to dependecy order {}", next.toString());
            }
            else {
                dependencyOrder.addAll(readyNodes);
                for (Node n: readyNodes) {
                    n.notifyAddedToDependencyOrder();
                }
                LOG.debug("added following nodes to dependecy order {}", print(readyNodes));
            }
            readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies(true);
            if (readyNodes.isEmpty()) {
                count++;
            }
            else {
                count = 0;
            }
        }
        if (count == 100) {
            throw new IllegalStateException("Infinite loop, calculating dependency order");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(dumpCurrentState());
        }
    }

    private void logDebugSummaryReport() {
      if (!LOG.isDebugEnabled()) {
        return;
      }
      LOG.debug("Number of Nodes: {}", nodes.size());
      int countDeps = 0;
      for (Node node: nodes.values()) {
        countDeps += node.dependency.size();
      }
      LOG.debug("Number of Dependencies: {}", countDeps);
    }

    /**
     * finds a node with the same entity type.
     * @param readyNodes
     * @param node
     * @return
     */
    private Node findMatchingNode(Collection<Node> readyNodes, Node node) {
        for (Node n: readyNodes) {
            if (n.operation.entity.getEntityType() == node.operation.entity.getEntityType()) {
                return n;
            }
        }
        return null;
    }

    private int countNodeRequiredInDependencyOrder(boolean excludeDeleteOperations) {
        int count = 0;
        for (Node node: nodes.values()) {
            if (excludeDeleteOperations && node.operation.opType == OperationType.DELETE) {
                continue;
            }
            if (node.operation.opType == OperationType.NONE) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * finds nodes which have themselves not been processed, but their
     * dependencies have been.
     *
     * @param deletesMode
     * @return
     */
    private Collection<Node> getUnprocessedNodesWithNoUnprocessedDependencies(boolean deletesMode) {
        LOG.trace("getUnprocessedNodesWithNoUnprocessedDependencies - {}", deletesMode ? "true" : "false");
        Collection<Node> result = new LinkedList<>();
        for (Node node : nodes.values()) {
            if (node.isProcessed()) {
                continue;
            }
            if (deletesMode && !node.operation.isDelete()) {
                continue;
            }
            if (!deletesMode && node.operation.isDelete()) {
                continue;
            }
            if (node.operation.opType == OperationType.NONE) {
                continue;
            }
            if (LOG.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("DEPENDENCY REPORT\n");
                sb.append("  ");
                sb.append(node + " has following dependecies:\n");
                for (Node dep : node.dependency) {
                    sb.append("    ");
                    sb.append(dep);
                    if (dep.isProcessed()) {
                        sb.append(" which is processed\n");
                    }
                    else {
                            sb.append(" which is NOT processed\n");
                    }
                }
                LOG.trace(sb.toString());
            }
            if (!node.hasUnprocessedDependecies(deletesMode)) {
                LOG.trace("Node {} is ready for processing", node);
                result.add(node);
            }
        }
        return result;
    }

    private Node getReffedDependencyNode(RefNode refNode) {
        Entity dependentEntity = refNode.getReference();
        return dependentEntity != null ? nodes.get(dependentEntity) : null;
    }

    /**
     * creates the node for the given entity and operation
     *
     * @param entity
     * @param opType
     * @return the Node or null if it an operation already exists for the entity
     */
    private Node createOrGetNode(Entity entity, OperationType opType) {
        return createOrGetNode(entity, opType, true);
    }

    /**
     * creates the node for the given entity and operation
     *
     * @param entity
     * @param opType
     * @param orphanCheck
     *            if an orphan check is required.
     * @return the Node or null if it an operation already exists for the entity
     */
    private Node createOrGetNode(Entity entity, OperationType opType, boolean orphanCheck) {
        Node node = nodes.get(entity);
        if (node == null) {
            node = new Node(new Operation(entity, opType), orphanCheck);
            nodes.put(entity, node);
        } else {
            if (node.operation.isNone()) {
                node.operation.updateOpType(opType);
            }
        }
        LOG.debug("Added node {} to dependency tree", node);
        return node;
    }

    private class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Operation operation;
        private final Set<Node> dependency = new LinkedHashSet<>();

        private boolean builtDependencies = false;
        private boolean builtOrphanChecks = false;

        private boolean inDependencyOrder;

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
            LOG.debug("Building orphan checks for {}", this);
            builtOrphanChecks = true;
            if (operation.entity.getKey().getValue() == null) {
                /*
                 * this entity has no PK, it cannot exist in the DB, therefore
                 * nothing to do
                 */
                return;
            }
            if (operation.isNone()) {
                /*
                 * there is no operation being performed on this entity so it
                 * cannot cause any of it's owned stuff to be deleted.
                 */
                return;
            }

            /*
             * we need to check what should be deleted from the database based
             * on the set of operations. ie entities which are owned by RefNodes
             * but no longer pointed to need to be deleted, the same goes for to
             * many nodes.
             */
            LOG.debug("Analysing orphan checks for {}", this);
            /*
             * go through our FK refs and build dependencies TO them
             */
            for (RefNode ref : operation.entity.getChildren(RefNode.class)) {
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
             * If we own the N side of the relationship and an entity was
             * removed from the List of N then that entity should be deleted.
             *
             * If we are being deleted and we own the N side then all are
             * deleted.
             *
             * IE we always have to load the original N when we own the relation
             */
            /*
             * go through our FK refs and build dependencies FROM them
             */
            for (ToManyNode toManyNode : operation.entity.getChildren(ToManyNode.class)) {
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
            return;
        }

        public void buildDependencies() {
            if (builtDependencies) {
                return;
            }
            builtDependencies = true;
            LOG.debug("--------------------------");
            LOG.debug("- Building dependencies for {}", this);
            /*
             * first process insert or update logic
             */
            if (!operation.isDelete() && !operation.isNone()) {
                /*
                 * go through our FK refs and build dependencies TO them
                 */
                for (RefNode ref : operation.entity.getChildren(RefNode.class)) {
                    /*
                     * look to see if the reference points to anything
                     */
                    Entity entity = ref.getReference(false);
                    if (entity != null) {
                        /*
                         * check if we already have a dependency node for the
                         * reff'd entity.
                         */
                        Node dependentNode = getReffedDependencyNode(ref);
                        if (dependentNode == null) {
                            /*
                             * true We need to add a new node to our dependency
                             * tree.
                             */

                            /*
                             * if the entity is already in the DB and we own it
                             * and we have it loaded into memory then we should
                             * perform an update
                             */
                            if (entity.isUnclearIfInDatabase()) {
                                throw new IllegalStateException(
                                        "Entities should be clearly defined at this point: " + entity);
                            }
                            if (entity.isClearlyInDatabase() && !entity.isFetchRequired() && ref.getNodeType().isOwns()) {
                                dependentNode = createOrGetNode(entity, OperationType.UPDATE);
                            }
                            else if (entity.isClearlyInDatabase() && !entity.isFetchRequired() && ref.getNodeType().isDependsOn()){
                                dependentNode = createOrGetNode(entity, OperationType.DEPENDS);
                            }
                            /*
                             * if the entity it not in the database then it has
                             * to be inserted otherwise we cannot set the FK
                             */
                            else if (entity.isClearlyNotInDatabase()) {
                                dependentNode = createOrGetNode(entity, OperationType.INSERT);
                            } else {
                                /*
                                 * the dependency is there but there is nothing
                                 * to.do.
                                 */
                                dependentNode = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        if (operation.opType != OperationType.NONE && operation.opType != OperationType.DEPENDS) {
                            dependency.add(dependentNode);
                            LOG.debug("Added dependency from {} to {}", this, dependentNode);
                        }
                        else {
                            LOG.debug("No dependencies created from {} to {} for OPTYPE {}", this, dependentNode, dependentNode.operation.opType);
                       }
                    }
                }

                /*
                 * go through our to many refs and build dependencies FROM them
                 */
                for (ToManyNode toManyNode : operation.entity.getChildren(ToManyNode.class)) {
                    if (toManyNode.getList().isEmpty() && !toManyNode.isFetched()) {
                        /*
                         * if the many relation was never fetched, and it
                         * contains nothing, so skip.
                         */
                        continue;
                    }
                    for (Entity entity : toManyNode.getList()) {
                        /*
                         * check if we already have a dependency node for the
                         * reff'd entity.
                         */
                        Node dependentNode = getDependencyNode(entity);
                        if (dependentNode == null) {
                            /*
                             * if the entity is already in the DB and we own it
                             * and we have it loaded into memory then we should
                             * perform an update
                             */
                            if (entity.isClearlyInDatabase() && toManyNode.getNodeType().isOwns()
                                    && !entity.isFetchRequired()) {
                                dependentNode = createOrGetNode(entity, OperationType.UPDATE);
                            }
                            /*
                             * if the entity it not in the database then it has
                             * to be inserted otherwise we cannot set the FK
                             */
                            else if (entity.isClearlyNotInDatabase()) {
                                dependentNode = createOrGetNode(entity, OperationType.INSERT);
                            } else if (toManyNode.getNodeType().isDependsOn()) {
                                dependentNode = createOrGetNode(entity, OperationType.DEPENDS);
                            } else {
                                /*
                                 * the dependency is there but there is nothing
                                 * to.do.
                                 */
                                dependentNode = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        /*
                         * the direction of the FK dependency is reversed for
                         * ToManyNodes
                         */
                        if (dependentNode.operation.opType != OperationType.NONE && dependentNode.operation.opType != OperationType.DEPENDS) {
                            dependentNode.dependency.add(this);
                            LOG.debug("Added dependency from {} to {}", dependentNode, this);
                        }
                        else {
                            LOG.debug("No dependencies created from {} to {} for OPTYPE {}", dependentNode, this, dependentNode.operation.opType);
                        }
                    }
                }
            } else {
                /*
                 * delete operations are handles somewhere else
                 */
            }
            return;
        }

        /**
         * this can only be called when integrating new delete operations into
         * the contetx which have been directly pulled from the database.
         *
         */
        public void buildDeleteDependencies() {
            if (builtDependencies) {
                return;
            }
            builtDependencies = true;
            LOG.debug("Building delete dependencies for {}", this);
            /*
             * only care about deletes for this methos
             */
            if (operation.isDelete()) {
                /*
                 * go through our to many refs and build dependencies FROM them
                 */
                for (ToManyNode toManyNode : operation.entity.getChildren(ToManyNode.class)) {
                    if (toManyNode.getList().isEmpty() && !toManyNode.isFetched()) {
                        /*
                         * if the many relation was never fetched, and it
                         * contains nothing, so skip.
                         */
                        continue;
                    }
                    for (Entity entity : toManyNode.getList()) {
                        /*
                         * check if we already have a dependency node for the
                         * reff'd entity.
                         */
                        Node dependentNode = getDependencyNode(entity);
                        if (dependentNode == null) {
                            /*
                             * We need to add a new node to our dependency tree.
                             * We must have had a recursive data structure, this
                             * is the only time it happens with delete logic...
                             */

                            /*
                             * if we own the reffed entity then the reffed
                             * entity must be deleted.
                             *
                             * in any case - there is a clear dependency that we
                             * must be deleted before the reffed entity, so we
                             * depend on it.
                             */
                            if (toManyNode.getNodeType().isOwns()) {
                                dependentNode = createOrGetNode(entity, OperationType.DELETE);
                            } else {
                                dependentNode = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        /*
                         * express the dependency, things which refer to us must
                         * be deleted before us.
                         */
                        if (operation.opType != OperationType.NONE && operation.opType != OperationType.DEPENDS) {
                            dependency.add(dependentNode);
                            LOG.debug("Added dependency from {} to {}", this, dependentNode);
                        }
                        else {
                            LOG.debug("No dependencies created from {} to {} for OPTYPE {}", this, dependentNode, dependentNode.operation.opType);
                       }
                    }
                }

                /*
                 * go through our FK refs and build dependencies FROM them TO us
                 */
                for (RefNode ref : operation.entity.getChildren(RefNode.class)) {
                    /*
                     * look to see if the reference points to anything
                     */
                    Entity entity = ref.getReference(false);
                    if (entity != null) {
                        /*
                         * check if we already have a dependency node for the
                         * reff'd entity.
                         */
                        Node dependentNode = getReffedDependencyNode(ref);
                        if (dependentNode == null) {
                            /*
                             * We need to add a new node to our dependency tree.
                             * We must have had a recursive data structure, this
                             * is the only time it happens with delete logic...
                             */

                            /*
                             * If a syntax has a FK reference to a structure
                             * then we can express a delete dependency that the
                             * structure depends on us if it wants to be
                             * deleted.
                             */

                            if (ref.getNodeType().isOwns()) {
                                dependentNode = createOrGetNode(entity, OperationType.DELETE);
                            } else {
                                dependentNode = createOrGetNode(entity, OperationType.NONE);
                            }
                        }
                        /*
                         * express the dependency, we must be deleted before our
                         * ref
                         *
                         * If something is referring to me then I cannot be deleted, so express a dependency
                         * regardless of the operation
                         */
                        dependentNode.dependency.add(this);
                        LOG.debug("Added dependency from {} to {}", dependentNode, this);
                    }
                }
            } else {

                /*
                 * non-delete operations are handles somewhere else
                 */
            }
            return;
        }

        public void notifyAddedToDependencyOrder() {
            inDependencyOrder = true;
        }

        public boolean isProcessed() {
            return inDependencyOrder;
        }

        public boolean hasUnprocessedDependecies(boolean deletesMode) {
            Set<Node> filtered = filterOutOperations( dependency, OperationType.NONE );
            if (!deletesMode) {
                filtered = filterOutOperations( filtered, OperationType.DELETE );
            }
            if (filtered.isEmpty()) {
                return false;
            }
            for (Node n: filtered) {
                if (!n.isProcessed()) {
                    return true;
                }
            }
            return false;
        }

        private Set<Node> filterOutOperations(Set<Node> nodes,OperationType type) {
            Set<Node> result = new HashSet<>();
            for (Node node: nodes) {
                if (node.operation.opType != type) {
                    result.add( node );
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return "Node [" + operation + "]";
        }
    }

    public class OrphanCheck implements Serializable {
        private static final long serialVersionUID = 1L;

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

        @Override
        public String toString() {
            return "OrphanCheck [entity=" + entity + ", result=" + result + ", checkWasPerformed=" + checkWasPerformed
                    + "]";
        }
    }

    private boolean pendingOrphanChecks() {
        for (OrphanCheck oc : orphanChecks.values()) {
            if (!oc.checkWasPerformed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs all pending orphan checks.
     *
     * @throws SortServiceProviderException
     * @throws BarleyDBQueryException
     */
    private void performOrphanChecks() throws SortServiceProviderException, BarleyDBQueryException {
        LOG.debug("-------------------------------------------------------------");
        LOG.debug("START Executing queries for all pending orphan checks");
        LOG.debug("-------------------------------------------------------------");
        QueryBatcher qbatcher = new QueryBatcher();

        Map<EntityId, OrphanCheck> lookup = new HashMap<>();

        for (OrphanCheck orphCheck : new ArrayList<>(orphanChecks.values())) {
            if (orphCheck.checkWasPerformed()) {
                // LOG.debug("Already performed orphan check for {}",
                // orphCheck.entity);
                continue;
            }
            LOG.debug("- Creating query to load all data of type {} for orphan checking",
                    orphCheck.entity.getEntityType().getInterfaceName());
            LOG.debug("-------------------------------------------------------------");
            /*
             * at the top level of this loop we just need 1 orphCheck of each
             * EntityType.
             */
            EntityId eid = new EntityId(orphCheck.entity.getEntityType(), orphCheck.entity.getKey().getValue());
            /*
             * create a query for the entity type, which includes all orphan
             * checks of the same type
             */
            QueryObject<Object> query = createQueryForReferencesToDelete(orphCheck.entity);
            orphCheck.setCheckWasPerformed(true);
            lookup.put(eid, orphCheck);
            int logNumberOfQueryConditions = 1;

            //include any other entities of the same type which we need to load (from other orphan checks)
            for (OrphanCheck sub : orphanChecks.values()) {
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
                EntityId eidSub = new EntityId(sub.entity.getEntityType(), sub.entity.getKey().getValue());
                QProperty<Object> keyProp = new QProperty<>(query, sub.entity.getKey().getName());
                query.or(keyProp.equal(sub.entity.getKey().getValue()));
                logNumberOfQueryConditions++;

                sub.setCheckWasPerformed(true);
                lookup.put(eidSub, sub);
            }

            LOG.debug("- Query of type {} created which performs {} checks.", query.getTypeName(),
                    logNumberOfQueryConditions);
            LOG.debug("-------------------------------------------------------------");
            qbatcher.addQuery(query);
        }

        /*
         * perform the queries and hold the data for later integration
         */
        if (!qbatcher.getQueries().isEmpty()) {
            LOG.debug("- Executing all prepared queries");
            LOG.debug("-------------------------------------------------------------");
            dctx.performQueries(qbatcher);
            for (QueryResult<?> result : qbatcher.getResults()) {
                for (Entity entityResult : result.getEntityList()) {
                    OrphanCheck check = lookup
                            .get(new EntityId(entityResult.getEntityType(), entityResult.getKey().getValue()));
                    LOG.debug("Setting orphan check result {}", entityResult);
                    check.setResult(entityResult);
                }
            }
        } else {
            LOG.debug("- No queries were pepared, orphan checks is completed.");
        }
        LOG.debug("-------------------------------------------------------------");
        LOG.debug("END Executing queries for all pending orphan checks");
        LOG.debug("-------------------------------------------------------------");

    }

    /**
     * analyses the entities which were loaded as part of the delete orphan
     * checking and integrates them into the main ctx and sets the delete
     * operation
     *
     */
    private void integrateNewDeleteOperationsIntoDependencyTree() {
        LOG.debug("-------------------------------------------------------------");
        LOG.debug("START Checking if we need to integrate new delete operations into the depdendecy tree.");

        for (OrphanCheck oc : orphanChecks.values()) {
            if (oc.result == null) {
                continue;
            }
            LOG.debug("-------------------------------------------------------------");
            LOG.debug("- Processing orphan check {}", oc.entity);

            Node dependencyNode = getDependencyNode(oc.entity);
            if (dependencyNode == null) {
                throw new IllegalStateException(
                        "Could not find dependency node for an orphan check entity: " + oc.entity);
            }
            /*
             * create delete operations for the orphan checks and the data it
             * owns (data which it eagerly loaded in our case).
             */
            for (RefNode refNode : oc.result.getChildren(RefNode.class)) {
                if (!refNode.getNodeType().isOwns()) {
                    continue;
                }
                Entity reffedEntity = refNode.getReference(false);
                if (reffedEntity == null) {
                    continue;
                }
                if (dependencyNode.operation.isInsert()) {
                    // there is nothing to delete if the entity referring to us
                    // is not yet in the DB
                    continue;
                }
                if (dependencyNode.operation.isDelete()) {
                    LOG.debug("- Adding delete operation for reffed entity {} because the owner is being deleted",
                            reffedEntity);
                    LOG.debug("-------------------------------------------------------------");
                    copyIntoContextAndCreateDeleteNodes(reffedEntity);
                    continue;
                }
                if (dependencyNode.operation.isUpdate()) {
                    if (isOrphaningReference(dependencyNode.operation, refNode.getNodeType(), oc.result)) {
                        LOG.debug("- Adding delete operation for reffed entity {} because the owner orphaned it",
                                reffedEntity);
                        LOG.debug("-------------------------------------------------------------");
                        copyIntoContextAndCreateDeleteNodes(reffedEntity);
                        continue;
                    } else {
                        LOG.debug("No orphan for {} reference to {}", oc.entity, refNode.getName());
                        continue;
                    }
                }
                throw new IllegalStateException("Unexpected state reached when processing orphan check " + oc
                        + " for integrating into the dependency tree");
            }

            /*
             * create delete operations for the orphan checks and the data it
             * owns (data which it eagerly loaded in our case).
             */
            for (ToManyNode refNode : oc.result.getChildren(ToManyNode.class)) {
                if (!refNode.getNodeType().isOwns()) {
                    continue;
                }
                if (!refNode.isFetched()) {
                    continue;
                }
                for (Entity reffedEntity : refNode.getList()) {
                    if (reffedEntity == null) {
                        continue;
                    }
                    if (dependencyNode.operation.isInsert()) {
                        // there is nothing to delete if the entity referring to
                        // us is not yet in the DB
                        continue;
                    }
                    if (dependencyNode.operation.isUpdate()) {
                        if (wasRemovedFromList(oc.entity, refNode.getName(), reffedEntity)) {
                            LOG.debug("Adding delete operation entity {} which was removde from list", reffedEntity);
                            copyIntoContextAndCreateDeleteNodes(reffedEntity);
                        } else {
                            LOG.debug("entity {} was not removed from list", reffedEntity);
                        }
                        continue;
                    }
                    /*
                     * if we are being deleted, then the fulll N side is also
                     * being deleted.
                     */
                    if (dependencyNode.operation.isDelete()) {
                        LOG.debug("Adding delete operation for reffed entity {} because the owner is being deleted",
                                reffedEntity);
                        copyIntoContextAndCreateDeleteNodes(refNode, reffedEntity);
                        continue;
                    }
                    throw new IllegalStateException("Unexpected state reached when processing orphan check " + oc
                            + " for integrating into the dependency tree");
                }
            }

            for (Node node : nodes.values()) {
                node.buildDependencies();
            }
        }

        LOG.debug("-------------------------------------------------------------");
        LOG.debug("END Checking if we need to integrate new delete operations into the depdendecy tree.");
        LOG.debug("-------------------------------------------------------------");
    }

    /**
     * Checks if an entity on the N side of 1:N was actually removed. We can do
     * this because we can compare the data from the client programmer to the
     * data in the database obtained from the orphan check.
     *
     * @param entity
     * @param name
     * @param reffedEntity
     * @return
     */
    private boolean wasRemovedFromList(Entity entity, String name, Entity reffedEntity) {
        if (entity.isNotLoaded()) {
            /*
             * the list cannot have something removed if the entity owning the
             * list was never loaded.
             */
            return false;
        }
        ToManyNode toManyNode = entity.getChild(name, ToManyNode.class);
        if (!toManyNode.isFetched()) {
            /*
             * the list cannot have something removed if the list was never
             * fetched.
             */
            return false;
        }
        for (Entity e : toManyNode.getList()) {
            if (e.getKey().getValue() != null
                    && Objects.equals(e.getKey().getValue(), reffedEntity.getKey().getValue())) {
                // we found the entity, so it wasn't removed
                return false;
            }
        }
        // only leaves 1 possibility
        return true;
    }

    private void copyIntoContextAndCreateDeleteNodes(Entity entity) {
        copyIntoContextAndCreateDeleteNodes(null, entity);
    }

    /**
     * puts the entity into the main context creates delete operations for the
     * whole entity tree of ownership
     *
     * @param entity
     */
    private void copyIntoContextAndCreateDeleteNodes(ToManyNode toManyNode, Entity entity) {
        LOG.debug("Copying {} and all children into main ctx and create delete operations for them all.", entity);

        // watch out for ownership over join tables (N:M)
        final Collection<Entity> toCopy;
        if (toManyNode != null && toManyNode.getNodeType().getJoinProperty() != null) {
            toCopy = findOwnedEntites(new LinkedHashSet<Entity>(), new HashSet<Entity>(), entity,
                    toManyNode.getNodeType().getJoinProperty());
        } else {
            toCopy = findOwnedEntites(new LinkedHashSet<Entity>(), new HashSet<Entity>(), entity, null);
        }

        /*
         * make sure we don't overwrite any optimistic locks, just incase the Entity which we are copying to our CTX is already there
         * and out of date.
         *
         * --we are only copying ref entities of owning relationships
         * -- other ref entites have entity state NOT_lOADED
         */
        EntityJuggler juggler = new EntityJuggler(false, false) {
          @Override
          protected boolean importRefNode(RefNode refNode) {
            return toCopy.contains(refNode.getReference());
          }
          @Override
          protected boolean importToManyNode(ToManyNode toMany, Entity entity) {
            return toCopy.contains(entity);
          }
        };

        List<Entity> copied = juggler.importEntities(toCopy, ctx);
//        List<Entity> copied = EntityContextHelper.addEntities(toCopy, ctx, true, false);
//        EntityContextHelper.copyRefStates(dctx, ctx, copied, new EntityContextHelper.EntityFilter() {
//            @Override
//            public boolean includesEntity(Entity entity) {
//                //we only copy ref states for entites which have been copied into the ctx.
//                //need to make sure any references which we can't load are lasy
//                return toCopy.contains(entity);
//              //return true;
//            }
//        });

        List<Node> nodes = new LinkedList<>();
        for (Entity e : copied) {
            LOG.debug("Adding delete operation for entity {}", e);
            /*
             * as this entity ori...
             */
            Entity eDctx = getEntityInCollection(toCopy, e.getEntityType(), e.getKey().getValue());
            if (eDctx == null) {
                throw new IllegalStateException("Cannot find entity " + e + " in dCtx");
            }
            boolean orphanChecksAreRequired = entityHasOwningRelationsWhichAreLazy( eDctx );
            nodes.add(createOrGetNode(e, OperationType.DELETE, orphanChecksAreRequired));
        }
        for (Node node : nodes) {
            node.buildDeleteDependencies();
        }
    }

    private Entity getEntityInCollection(Collection<Entity> col, EntityType entityType, Object key) {
        for (Entity e: col) {
            if (e.getEntityType() == entityType && e.getKey().getValue() != null && e.getKey().getValue().equals(key)) {
                return e;
            }
        }
        return null;
    }

    /**
     * returns true iff the entity has a tomanynode with an isowning relationship and the it is not loaded
     *
     * @param entityWhichWasLoadedForOrphanCheck
     * @return
     */
    private boolean entityHasOwningRelationsWhichAreLazy(Entity entityWhichWasLoadedForOrphanCheck) {
        for (ToManyNode toManyNode: entityWhichWasLoadedForOrphanCheck.getChildren(ToManyNode.class)) {
            if (toManyNode.getNodeType().isOwns() && !toManyNode.isFetched()) {
                return true;
            }
        }
        for (RefNode refNode: entityWhichWasLoadedForOrphanCheck.getChildren(RefNode.class)) {
            if (refNode.getNodeType().isOwns() &&
                    refNode.getReference(false) != null &&
                    refNode.getReference().isNotLoaded()) {

                /*
                 * we refer to an entity which we own, but the entity was not loaded
                 */
                return true;
            }
        }
        return false;
    }

    /**
     * Finds all owned entities in the entity graph (no-loading).
     *
     * @param matches
     * @param checked
     * @param entity
     * @param joinProperty
     *            is set iff the Entity is a jointable entity which is owned by
     *            the ToManyNode which linked to the join table..
     * @return
     */
    public static LinkedHashSet<Entity> findOwnedEntites(LinkedHashSet<Entity> matches, HashSet<Entity> checked,
            Entity entity, String joinProperty) {
        if (!checked.add(entity)) {
            return matches;
        }
        matches.add(entity);
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            // either we own the ref or we are a joinProperty from a ToMany node
            // which must own the ref
            if (!refNode.getNodeType().isOwns()) {
                if (!refNode.getName().equals(joinProperty)) {
                    continue;
                } else {
                    LOG.debug("Finding owned entities across N:M relation for {}", entity);
                }
            }
            Entity e = refNode.getReference(false);
            if (e != null) {
                findOwnedEntites(matches, checked, e, null);
            }
        }
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            if (!toManyNode.getNodeType().isOwns()) {
                continue;
            }
            /*
             * if we have a N:M jointable relationship and our side is an owning
             * relation then we need to reach over and include the other side of
             * the join table ie templates owning the list of business types via
             * the TemplateBusinessType entity
             */
            for (Entity e : toManyNode.getList()) {
                if (e != null) {
                    findOwnedEntites(matches, checked, e, toManyNode.getNodeType().getJoinProperty());
                }
            }
        }
        return matches;
    }

    /**
     * checks if a given operation would cause the entity to be orhaned
     *
     * @param operation
     * @param potentalOrphan
     * @return
     */
    private boolean isOrphaningReference(Operation operation, NodeType nodeType, Entity potentalOrphan) {
        Object newFkRefValue = operation.entity.getChild(nodeType.getName(), RefNode.class).getEntityKey();
        Object origFkRefValue = potentalOrphan.getChild(nodeType.getName(), RefNode.class).getEntityKey();
        return origFkRefValue != null && !origFkRefValue.equals(newFkRefValue);
    }

    private QueryObject<Object> createQueryForReferencesToDelete(Entity entity) {
        QueryObject<Object> query = dctx.getUnitQuery(entity.getEntityType(), true);

        // filter on PK
        final QProperty<Object> keyProp = new QProperty<Object>(query, entity.getKey().getName());
        query.where(keyProp.equal(entity.getKey().getValue()));
        LOG.trace("Added property {} to query {}", keyProp.getName(), query.getTypeName());

        /*
         * add joins to all owning relations
         */
        addJoinsForAllOwningRefs(entity.getEntityType(), query, new HashSet<NodeType>(), null);

        return query;
    }

    private void addJoinsForAllOwningRefs(EntityType entityType, QueryObject<Object> query,
            Set<NodeType> alreadyProcessed, String ownedByJoinProperty) {
        /*
         * add joins to all owning Refs
         */
        for (NodeType nodeType : entityType.getNodeTypes()) {
            if (!nodeType.isOwns()) {
                if (ownedByJoinProperty == null) {
                    continue;
                } else if (!nodeType.getName().equals(ownedByJoinProperty)) {
                    continue;
                } else {
                    LOG.debug("Querying over join table for {}.{} " + entityType.getInterfaceShortName(),
                            nodeType.getName());
                }
            }
            /*
             * at this point either the node type isowns or it matches the join
             * property which is owned.
             */
            /*
             * add left outer joins for owning relationships
             */
            if (alreadyProcessed.add(nodeType)) {
                QueryObject<Object> to = new QueryObject<>(nodeType.getRelationInterfaceName());
                query.addLeftOuterJoin(to, nodeType.getName());
                LOG.trace("Added left outer join from {} with property {} to {}", query.getTypeName(),
                        nodeType.getName(), nodeType.getRelationInterfaceName());

                EntityType reffedEntityType = entityType.getDefinitions()
                        .getEntityTypeMatchingInterface(nodeType.getRelationInterfaceName(), true);
                addJoinsForAllOwningRefs(reffedEntityType, to, alreadyProcessed, nodeType.getJoinProperty());
            }
        }
    }

    public Node getDependencyNode(Entity entity) {
        return nodes.get(entity);
    }

    public void addOrphanCheck(Entity entity) {
        orphanChecks.put(entity, new OrphanCheck(entity));
        LOG.debug("Adding orphan check  for {}", entity);
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

class EntityId implements Serializable {

    private static final long serialVersionUID = 1L;

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
