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

    @Override
    public String getShortDescription() {
        return entity.toString();
    }

    @Override
    public void buildDependencies(Collection<DependencyTreeNode> nodes) {
        for (RefNode refNode: entity.getChildren(RefNode.class)) {
            Entity e = refNode.getReference(false);
            if (e != null) {
                EntityDependencyTreeNode etn = new EntityDependencyTreeNode(e);
                dependencies.add(new Dependency(this, etn, refNode));
                nodes.add(etn);
            }
        }
        for (ToManyNode toManyNode: entity.getChildren(ToManyNode.class)) {
            for (Entity e: toManyNode.getList()) {
                EntityDependencyTreeNode etn = new EntityDependencyTreeNode(e);
                dependencies.add(new Dependency(this, etn, toManyNode));
                nodes.add(etn);
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

}
