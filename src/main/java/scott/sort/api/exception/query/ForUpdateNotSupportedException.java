package scott.sort.api.exception.query;

public class ForUpdateNotSupportedException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public ForUpdateNotSupportedException() {
        super();
    }

    public ForUpdateNotSupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ForUpdateNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForUpdateNotSupportedException(String message) {
        super(message);
    }

    public ForUpdateNotSupportedException(Throwable cause) {
        super(cause);
    }


}
