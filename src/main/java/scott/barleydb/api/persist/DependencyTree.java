package scott.barleydb.api.persist;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.RefNode;

public class DependencyTree {

    /**
     * if true then the dependency direction match the requirements for deleting entities.
     */
    private boolean deletionOrder;

    private final Collection<Node> dependencyOrder = new LinkedList<>();
    private final Map<Entity, Node> nodes = new LinkedHashMap<>();

    public DependencyTree(boolean deletionOrder) {
        this.deletionOrder = deletionOrder;
    }

    public Iterable<Entity> getOrder() {
        return new Iterable<Entity>() {
            @Override
            public Iterator<Entity> iterator() {
                final Iterator<Node> it = dependencyOrder.iterator();
                return new Iterator<Entity>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Entity next() {
                        return it.next().entity;
                    }
                };
            }
        };
    }

    public void build(Collection<Entity> entities) {
        for (Entity entity: entities) {
            nodes.put(entity, new Node(entity) );
        }
        for (Node node: nodes.values()) {
            node.buildDependencies();
        }
        calculateDependecyOrder();
    }

    private void calculateDependecyOrder() {
        Collection<Node> readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies();
        while(dependencyOrder.size() < nodes.size()) {
            dependencyOrder.addAll( readyNodes );
            readyNodes = getUnprocessedNodesWithNoUnprocessedDependencies();
            if (readyNodes.isEmpty()) {
                throw new IllegalStateException("Could not calculate the dependency order.");
            }
        }
    }

    private Collection<Node> getUnprocessedNodesWithNoUnprocessedDependencies() {
        Collection<Node> result = new LinkedList<>();
        for (Node node: nodes.values()) {
            if (!node.isProcessed() && !node.hasUnprocessedDependecies()) {
                result.add( node );
            }
        }
        return result;
    }

    private Node getReffedDependencyNode(RefNode refNode) {
        Entity dependentEntity = refNode.getReference();
        return dependentEntity != null ? nodes.get( dependentEntity ) : null;
    }


    private class Node {
        private final Entity entity;
        private final List<Node> dependency = new LinkedList<Node>();

        public Node(Entity entity) {
            this.entity =  entity;
        }

        public void buildDependencies() {
            /*
             * a dependency is a FK reference
             */
            for (RefNode ref: entity.getChildren(RefNode.class)) {
                Node dependentNode = getReffedDependencyNode(ref);
                if (dependentNode!= null) {
                    if (!deletionOrder) {
                        dependency.add( dependentNode );
                    }
                    else {
                        dependentNode.dependency.add( this );
                    }
                }
            }
        }

        public boolean isProcessed() {
            return dependencyOrder.contains(this);
        }

        public boolean hasUnprocessedDependecies() {
            return containsAnyOf(dependency);
        }

        @Override
        public String toString() {
            return "Node [" + entity.getName() + "]";
        }

    }

    private boolean containsAnyOf(Collection<Node> nodes) {
        for (Node n: nodes) {
            if (dependencyOrder.contains(n)) {
                return true;
            }
        }
        return false;
    }

}

