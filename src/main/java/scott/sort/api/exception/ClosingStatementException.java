package scott.sort.api.exception;

public class ClosingStatementException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public ClosingStatementException() {
        super();
    }

    public ClosingStatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ClosingStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClosingStatementException(String message) {
        super(message);
    }

    public ClosingStatementException(Throwable cause) {
        super(cause);
    }

}
