package scott.sort.api.exception;

public class SetAutoCommitException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public SetAutoCommitException() {
        super();
    }

    public SetAutoCommitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SetAutoCommitException(String message, Throwable cause) {
        super(message, cause);
    }

    public SetAutoCommitException(String message) {
        super(message);
    }

    public SetAutoCommitException(Throwable cause) {
        super(cause);
    }


}
