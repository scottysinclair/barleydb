package scott.sort.api.exception;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.config.NodeDefinition;
import scott.sort.api.exception.query.SortQueryException;

public class InvalidNodeDefinitionException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    private final NodeDefinition nodeDefinition;

    public InvalidNodeDefinitionException(NodeDefinition nodeDefinition, String message) {
        super(message);
        this.nodeDefinition = nodeDefinition;
    }

    public NodeDefinition getNodeDefinition() {
        return nodeDefinition;
    }

}
