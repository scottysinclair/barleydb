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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.sort.api.config.NodeDefinition;

/**
 * TODO: Problems using this exception should be detected on startup and so this exception
 * should be moved to scott.sort.api.exception.model and an IllegalQueryStateException should be used instead.
 * @author scott
 *
 */
public class InvalidNodeDefinitionException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(InvalidNodeDefinitionException.class);

    private NodeDefinition nodeDefinition;

    public InvalidNodeDefinitionException(NodeDefinition nodeDefinition, String message) {
        super(message);
        this.nodeDefinition = nodeDefinition;
    }

    public NodeDefinition getNodeDefinition() {
        return nodeDefinition;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing InvalidNodeDefinitionException {}", this);
        nodeDefinition.write(oos);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        nodeDefinition = NodeDefinition.read(ois);
        LOG.trace("Deserialized InvalidNodeDefinitionException {}", this);
    }


}
