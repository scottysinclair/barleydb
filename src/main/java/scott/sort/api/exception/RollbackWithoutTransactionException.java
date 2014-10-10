package scott.sort.api.exception;

/**
 * A rollback was attempted on a connection in autocommit mode.
 *
 * The JDBC spec says that an SQLException is thrown in this case. So we check first before rolling back
 * and throw a specific exception
 *
 * @author scott
 *
 */
public class RollbackWithoutTransactionException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public RollbackWithoutTransactionException() {
        super();
    }

    public RollbackWithoutTransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RollbackWithoutTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbackWithoutTransactionException(String message) {
        super(message);
    }

    public RollbackWithoutTransactionException(Throwable cause) {
        super(cause);
    }

}
