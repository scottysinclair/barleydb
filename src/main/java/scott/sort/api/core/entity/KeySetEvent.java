package scott.sort.api.core.entity;

public class KeySetEvent extends NodeEvent {

    private final Object originalKey;

    public KeySetEvent(Node source, Object originalKey) {
        super(source, NodeEvent.Type.KEYSET);
        this.originalKey = originalKey;
    }

    public Object getOriginalKey() {
        return originalKey;
    }

}
