package scott.sort.api.core.entity;

public class NodeEvent {
    public static enum Type {
        KEYSET
    }

    private final Node source;
    private final Type type;

    public NodeEvent(Node source, Type type) {
        this.source = source;
        this.type = type;
    }

    public Node getSource() {
        return source;
    }

    public Type getType() {
        return type;
    }
}
