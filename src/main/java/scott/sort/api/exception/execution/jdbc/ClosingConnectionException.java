package scott.sort.api.exception.execution.jdbc;

public class ClosingConnectionException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public ClosingConnectionException() {
        super();
    }

    public ClosingConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ClosingConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClosingConnectionException(String message) {
        super(message);
    }

    public ClosingConnectionException(Throwable cause) {
        super(cause);
    }

}
