package scott.sort.api.exception.query;

import scott.sort.server.jdbc.persister.exception.SortPersistException;

public class QueryConnectionRequiredException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    public QueryConnectionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryConnectionRequiredException(String message) {
        super(message);
    }


}
