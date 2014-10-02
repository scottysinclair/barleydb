package scott.sort.api.core.entity;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

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
