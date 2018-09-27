package scott.barleydb.api.dependency;

/**
 * @author scott
 *
 */
public class Dependency {
    private final DependencyTreeNode from;
    private final DependencyTreeNode to;
    private final Object thing;
    public Dependency(DependencyTreeNode from, DependencyTreeNode to, Object thing) {
        this.from = from;
        this.to = to;
        this.thing = thing;
    }
    public DependencyTreeNode getFrom() {
        return from;
    }
    public DependencyTreeNode getTo() {
        return to;
    }
    public Object getThing() {
        return thing;
    }
}
