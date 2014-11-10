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

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import scott.sort.api.config.NodeType;

public abstract class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Entity parent;

    private final String name;

    public Node(final Entity parent, final String name) {
        this.parent = parent;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public EntityContext getEntityContext() {
        return parent.getEntityContext();
    }

    public Entity getParent() {
        return parent;
    }

    public void handleEvent(NodeEvent event) {
        if (parent != null) {
            parent.handleEvent(event);
        }
    }

    public NodeType getNodeType() {
        return parent.getEntityType().getNodeType(name, true);
    }

    public abstract Element toXml(Document doc);

}
