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

import scott.barleydb.api.dependency.Dependency;
import scott.barleydb.api.dependency.DependencyTree;
import scott.barleydb.api.dependency.DependencyTreeNode;
import scott.barleydb.api.specification.EntitySpec;
import scott.barleydb.api.specification.NodeSpec;

public class CleanStatementOrder {

    public static List<EntitySpec> order(Collection<EntitySpec> specs) {
        DependencyTree deptree = new DependencyTree();

        deptree.build( toNodes(specs), true );

        List<EntitySpec> result = toSpecs( deptree.getDependencyOrder() );
        //deptree.generateDiagramStepbyStep();
        return result;

    }

    private static Collection<DependencyTreeNode> toNodes(Collection<EntitySpec> specs) {
        return specs.stream().map( toDependencyNode() ).collect(Collectors.<DependencyTreeNode>toList());
    }

    private static List<EntitySpec> toSpecs(Collection<DependencyTreeNode> specs) {
        return specs.stream().map( toSpecs() ).collect(Collectors.<EntitySpec>toList());
    }

    private static Function<DependencyTreeNode, EntitySpec> toSpecs() {
        return new Function<DependencyTreeNode, EntitySpec>() {
            @Override
            public EntitySpec apply(DependencyTreeNode node) {
                return ((EntitySpecDependencyNode)node).getThing();
            }
        };
    }

    private static Function<EntitySpec, DependencyTreeNode> toDependencyNode() {
        return new Function<EntitySpec, DependencyTreeNode>() {
            @Override
            public DependencyTreeNode apply(EntitySpec spec) {
                return new EntitySpecDependencyNode(spec);
            }
        };
    }

    private static boolean fkRelationExistsFromTo(EntitySpec specA, EntitySpec specB) {
        for (NodeSpec nodeSpec: specA.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null && nodeSpec.getRelationSpec().isForeignKeyRelation()) {
//                System.out.println(specA.getTableName() + " " + nodeSpec.getColumnName() + " ==>> " + specB.getTableName());
                if (nodeSpec.getRelationSpec().getEntitySpec().getTableName().equals(specB.getTableName())) {
                    return true;
                }
            }
        }
        return false;
    }


    private static class EntitySpecDependencyNode extends DependencyTreeNode {

        private boolean builtDependencies = false;
        private final EntitySpec spec;

        private Collection<Dependency> dependencies = new LinkedHashSet<>();

        public EntitySpecDependencyNode(EntitySpec spec) {
            this.spec = spec;
        }

        @Override
        public String getShortDescription() {
            return spec.getClassName();
        }

        public EntitySpec getThing() {
            return spec;
        }

        @Override
        public boolean hasBuiltDependencies() {
            return builtDependencies;
        }

        @Override
        public String getDiagramName() {
            return spec.getTableName();
        }

        @Override
        public void buildDependencies(DependencyTree tree) {
            if (builtDependencies) {
                return;
            }
            builtDependencies = true;
            for (DependencyTreeNode node: tree.getNodes()) {
                if (node == this) {
                    continue;
                }
                if (!(node instanceof EntitySpecDependencyNode)) {
                    continue;
                }
                EntitySpec otherSpec = ((EntitySpecDependencyNode)node).getThing();
                if (fkRelationExistsFromTo(otherSpec, spec)) {
                    //otherspec has a FK relation to us, so it must be cleaned first.
                    //ie we are dependent on it (we can't be cleaned until they are)
                    dependencies.add( new Dependency(this, node, null) );
                }
            }

        }

        @Override
        public Collection<Dependency> getDependencies() {
            return dependencies;
        }

    }

}
