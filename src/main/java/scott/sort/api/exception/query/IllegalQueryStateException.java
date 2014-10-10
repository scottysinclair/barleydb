package scott.sort.api.exception.query;


public class IllegalQueryStateException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public IllegalQueryStateException() {
        super();
    }

    public IllegalQueryStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public IllegalQueryStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalQueryStateException(String message) {
        super(message);
    }

    public IllegalQueryStateException(Throwable cause) {
        super(cause);
    }

}
