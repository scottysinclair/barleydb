package scott.sort.server.jdbc.persist.audit;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.core.entity.Node;

public class Change {
    public final Node node;
    public final Object oldValue;
    public final Object newValue;

    public Change(Node node, Object oldValue, Object newValue) {
        this.node = node;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return "Change [node=" + node + ", oldValue=" + oldValue + ", newValue="
                + newValue + "]";
    }

}