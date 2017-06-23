package scott.barleydb.build.specification.ddlgen;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import scott.barleydb.api.dependency.DependencyTree;
import scott.barleydb.api.dependency.DependencyTreeNode;
import scott.barleydb.api.specification.DefinitionsSpec;

public class CreateScriptOrder {

    public static List<DefinitionsSpec> order(Collection<DefinitionsSpec> specs) {
        DependencyTree deptree = new DependencyTree();

        deptree.build( toNodes(specs) );

        List<DefinitionsSpec> result = toSpecs( deptree.getDependencyOrder() );
        //deptree.generateDiagramStepbyStep();
        //deptree.generateDiagram();
        return result;

    }

    private static Collection<DependencyTreeNode> toNodes(Collection<DefinitionsSpec> specs) {
        return specs.stream().map( toDependencyNode() ).collect(Collectors.<DependencyTreeNode>toList());
    }


    private static List<DefinitionsSpec> toSpecs(Collection<DependencyTreeNode> specs) {
        return specs.stream().map( toSpecs() ).collect(Collectors.<DefinitionsSpec>toList());
    }

    private static Function<DefinitionsSpec, DependencyTreeNode> toDependencyNode() {
        return new Function<DefinitionsSpec, DependencyTreeNode>() {
            @Override
            public DependencyTreeNode apply(DefinitionsSpec spec) {
                return new DefinitionsSpecDependencyNode(spec);
            }
        };
    }


    private static Function<DependencyTreeNode, DefinitionsSpec> toSpecs() {
        return new Function<DependencyTreeNode, DefinitionsSpec>() {
            @Override
            public DefinitionsSpec apply(DependencyTreeNode node) {
                return ((DefinitionsSpecDependencyNode)node).getThing();
            }
        };
    }

    private static boolean importsSpec(DefinitionsSpec spec, DefinitionsSpec otherSpec) {
        for (String specNamespace: spec.getImports()) {
            if (otherSpec.getNamespace().equals(specNamespace)) {
                return true;
            }
        }
        return false;
    }


    private static class DefinitionsSpecDependencyNode extends DependencyTreeNode {

        private boolean builtDependencies = false;
        private final DefinitionsSpec spec;

        private Collection<DependencyTreeNode> dependencies = new LinkedHashSet<>();

        public DefinitionsSpecDependencyNode(DefinitionsSpec spec) {
            this.spec = spec;
        }

        @Override
        public String getShortDescription() {
            return spec.getNamespace();
        }

        public DefinitionsSpec getThing() {
            return spec;
        }

        @Override
        public boolean hasBuiltDependencies() {
            return builtDependencies;
        }

        @Override
        public String getDiagramName() {
            return spec.getNamespace();
        }

        @Override
        public void buildDependencies(Collection<DependencyTreeNode> nodes) {
            if (builtDependencies) {
                return;
            }
            builtDependencies = true;
            for (DependencyTreeNode node: nodes) {
                if (node == this) {
                    continue;
                }
                if (!(node instanceof DefinitionsSpecDependencyNode)) {
                    continue;
                }
                DefinitionsSpec otherSpec = ((DefinitionsSpecDependencyNode)node).getThing();
                if (importsSpec(spec, otherSpec)) {
                    //we import other spec
                    //ie we are dependent on it (we can't be created until they are)
                    dependencies.add( node );
                }
            }
        }

        @Override
        public Collection<DependencyTreeNode> getDependencies() {
            return dependencies;
        }

    }

}
