package scott.sort.api.exception;

public class DatabaseAccessError extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public DatabaseAccessError() {
        super();
    }

    public DatabaseAccessError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DatabaseAccessError(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseAccessError(String message) {
        super(message);
    }

    public DatabaseAccessError(Throwable cause) {
        super(cause);
    }

}
