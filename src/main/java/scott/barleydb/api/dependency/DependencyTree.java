package scott.barleydb.api.dependency;

import java.io.File;
import java.io.IOException;

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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.util.CollectionUtil;
import scott.barleydb.api.dependency.diagram.DependencyDiagram;
import scott.barleydb.api.dependency.diagram.Link;
import scott.barleydb.api.dependency.diagram.LinkType;
import scott.barleydb.api.persist.OperationType;

/**
 * An abstract dependency tree
 * which can create a fixed dependency order between things.
 * @author scott
 *
 */
public class DependencyTree implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DependencyTree.class);

    private final Collection<DependencyTreeNode> dependencyOrder = new LinkedHashSet<>();
    private final Collection<DependencyTreeNode> nodes = new LinkedHashSet<>();

    public void build(Collection<DependencyTreeNode> nodes) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("---------------------------------------------------------------------------------------");
            LOG.debug(" STARTING DEPENDENCY TREE BUILD ");
            LOG.debug("---------------------------------------------------------------------------------------");
        }
        this.nodes.addAll(nodes);

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
                        printNodes(nodes, nodesPendingDependencyChecks()));
            }
            numberOfNodes = nodes.size();
            nodes.stream().forEach(
                    buildDependenciesIfRequired());
        } while (numberOfNodes < nodes.size());

        LOG.debug("FINALLY calculating dependency order...");
        calculateDependencyOrder();
    }

    public void generateDiagramStepbyStep() {
        try {
            for (int i=0; i<=dependencyOrder.size(); i++) {
                File file = new File(System.getProperty("java.io.tmpdir") + "/dep-tree_" + (i+1)+ ".jpeg");
                //exclude each dependency in real calculated order, to create step by step images
                generateDiagram(file, new HashSet<DependencyTreeNode>(new LinkedList<>(dependencyOrder).subList(0,  i)));
                LOG.warn("GENERATED DIAGRAM AT " + file.getPath());
            }
        }
        catch(Exception x) {
            LOG.warn("Could not generate diagram!", x);
        }
    }


    public void generateDiagram() {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + "/dep-tree.jpeg");
            generateDiagram(file, new HashSet<DependencyTreeNode>());
            LOG.warn("GENERATED DIAGRAM AT " + file.getPath());
        }
        catch(Exception x) {
            LOG.warn("Could not generate diagram!", x);
        }
    }

    public void generateDiagram(File diagram, Set<DependencyTreeNode> exclude) throws IOException {
        DependencyDiagram diag = new DependencyDiagram();
        Link firstLink = null;
        for (DependencyTreeNode node: nodes) {
            for (DependencyTreeNode depNode: node.getDependencies()) {
                if (exclude.contains(depNode)) {
                    continue;
                }
                LinkType linkType = LinkType.DEPENDENCY;
                Link l = diag.link(node.getDiagramName(), depNode.getDiagramName(), "", linkType);
                if (firstLink == null) {
                    firstLink = l;
                }
            }
        }
        diag.generate(diagram);
    }


    public Collection<DependencyTreeNode> getDependencyOrder() {
        return dependencyOrder;
    }

    /**
     * populates the dependencyOrder list by processing the dependency nodes.
     */
    private void calculateDependencyOrder() {
        Collection<DependencyTreeNode> readyNodes = getNodes( nodesReadyForOrdering() );
        int count = 0;
        while (count < 100 && dependencyOrder.size() < nodes.size()) {
            dependencyOrder.addAll(readyNodes);
            LOG.debug("added following nodes to dependency order {}", printNodes(readyNodes));
            readyNodes = getNodes( nodesReadyForOrdering() );
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
    }

    private String printNodes(Collection<DependencyTreeNode> nodes) {
        return printNodes(nodes, CollectionUtil.<DependencyTreeNode>truePredicate());
    }

    private Collection<DependencyTreeNode> getNodes(Predicate<DependencyTreeNode> predicate) {
        return nodes.stream()
                .filter(predicate)
                .collect(Collectors.<DependencyTreeNode>toList());
    }

    private String printNodes(Collection<DependencyTreeNode> nodes, Predicate<DependencyTreeNode>  predicate) {
        return nodes.stream()
            .filter(predicate)
            .map(toShortDescription())
            .collect(Collectors.joining("\n"));
    }

    private Predicate<DependencyTreeNode> nodesReadyForOrdering() {
        return new Predicate<DependencyTreeNode>() {
            @Override
            public boolean test(DependencyTreeNode node) {
                if (dependencyOrder.contains(node)) {
                    //already ordered
                    return false;
                }
                Collection<DependencyTreeNode> dependencies = node.getDependencies();
                return dependencies.isEmpty() || dependencyOrder.containsAll( node.getDependencies() );
            }
        };
    }


    private Consumer<DependencyTreeNode> buildDependenciesIfRequired() {
        return new Consumer<DependencyTreeNode>() {
            @Override
            public void accept(DependencyTreeNode node) {
                if (!node.hasBuiltDependencies()) {
                    node.buildDependencies(nodes);
                }
            }
        };
    }

    private Predicate<DependencyTreeNode> nodesPendingDependencyChecks() {
        return new Predicate<DependencyTreeNode>() {
            @Override
            public boolean test(DependencyTreeNode node) {
                return node.hasBuiltDependencies();
            }
        };
    }

    private Function<DependencyTreeNode, String> toShortDescription() {
        return new Function<DependencyTreeNode, String>() {
            @Override
            public String apply(DependencyTreeNode node) {
                return node.getShortDescription();
            }
        };
    }

}
