package scott.sort.api.exception.execution.query;

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

/**
 * TODO: Problems using this exception should be detected on startup and so this exception
 * should be moved to scott.sort.api.exception.model and an IllegalQueryStateException should be used instead.
 * @author scott
 *
 */
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
