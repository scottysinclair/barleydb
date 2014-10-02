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
