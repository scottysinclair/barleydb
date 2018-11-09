package scott.barleydb.api.dependency;

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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;

public class EntityDependencyTreeNode extends DependencyTreeNode {

    private Entity entity;
    private boolean builtDependencies;
    private boolean allDependenciesFetchedAndProcessed;
    private List<Dependency> dependencies = new LinkedList<>();

    public EntityDependencyTreeNode(Entity entity) {
        super();
        this.entity = entity;
    }

    @Override
    public Entity getThing() {
        return entity;
    }

    @Override
    public boolean hasBuiltDependencies() {
        return builtDependencies;
    }

    public boolean requireRebuildIfNotAllDependenciesFetched() {
        if (!allDependenciesFetchedAndProcessed) {
            builtDependencies = false;
            return true;
        }
        return false;
    }

    @Override
    public String getShortDescription() {
        return entity.toString();
    }

    @Override
    public void buildDependencies(DependencyTree tree) {
        allDependenciesFetchedAndProcessed = true;
        for (RefNode refNode: entity.getChildren(RefNode.class)) {
            Entity e = refNode.getReference(false);
            if (e != null) {
                if (e.isFetchRequired()) {
                    allDependenciesFetchedAndProcessed = false;
                }
                EntityDependencyTreeNode etn = new EntityDependencyTreeNode(e);
                dependencies.add(new Dependency(this, etn, refNode));
                tree.addNode(etn);
            }
        }
        for (ToManyNode toManyNode: entity.getChildren(ToManyNode.class)) {
            if (!toManyNode.isFetched()) {
                allDependenciesFetchedAndProcessed = false;
            }
            for (Entity e: toManyNode.getList()) {
                EntityDependencyTreeNode etn = new EntityDependencyTreeNode(e);
                dependencies.add(new Dependency(this, etn, toManyNode));
                tree.addNode(etn);
            }
        }
        builtDependencies = true;
    }

    @Override
    public Collection<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public String getDiagramName() {
        return entity.toString();
    }

    @Override
    public String toString() {
        return "EntityDependencyTreeNode[ " + entity.toString() + "]";
    }



}
