package scott.sort.api.exception;

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
