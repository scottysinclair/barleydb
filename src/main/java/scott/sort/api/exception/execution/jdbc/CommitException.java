package scott.sort.api.exception.execution.jdbc;

public class CommitException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public CommitException() {
        super();
    }

    public CommitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CommitException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommitException(String message) {
        super(message);
    }

    public CommitException(Throwable cause) {
        super(cause);
    }

}
