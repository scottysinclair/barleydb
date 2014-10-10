package scott.sort.api.exception.persist;

import scott.sort.server.jdbc.persister.exception.SortPersistException;

public class PersistTransactionRequiredException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    public PersistTransactionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistTransactionRequiredException(String message) {
        super(message);
    }


}
