package scott.barleydb.api.exception.execution.query;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.execution.query.InvalidNodeTypeException;
import scott.barleydb.api.exception.execution.query.BarleyDBQueryException;

/**
 * TODO: Problems using this exception should be detected on startup and so this exception
 * should be moved to scott.barleydb.api.exception.model and an IllegalQueryStateException should be used instead.
 * @author scott
 *
 */
public class InvalidNodeTypeException extends BarleyDBQueryException {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(InvalidNodeTypeException.class);

    private NodeType nodeType;

    public InvalidNodeTypeException(NodeType nodeType, String message) {
        super(message);
        this.nodeType = nodeType;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        LOG.trace("Serializing InvalidNodeTypeException {}", this);
        nodeType.write(oos);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        nodeType = NodeType.read(ois);
        LOG.trace("Deserialized InvalidNodeTypeException {}", this);
    }


}
