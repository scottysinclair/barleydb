package scott.barleydb.api.dependency;

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
