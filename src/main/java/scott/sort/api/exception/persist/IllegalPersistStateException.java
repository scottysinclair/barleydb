package scott.sort.api.exception.persist;

import scott.sort.server.jdbc.persister.exception.SortPersistException;

public class IllegalPersistStateException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    public IllegalPersistStateException() {
        super();
    }

    public IllegalPersistStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public IllegalPersistStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPersistStateException(String message) {
        super(message);
    }

    public IllegalPersistStateException(Throwable cause) {
        super(cause);
    }
}
