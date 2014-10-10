package scott.sort.api.exception;

public class SortException extends Exception {

    private static final long serialVersionUID = 1L;

    public SortException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public SortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortException(String message) {
        super(message);
    }

    public SortException(Throwable cause) {
        super(cause);
    }
}
