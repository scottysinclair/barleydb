package scott.sort.api.exception.query;

import scott.sort.api.exception.SortException;

public class SortQueryModelException extends SortException {

    private static final long serialVersionUID = 1L;

    public SortQueryModelException() {
        super();
    }

    public SortQueryModelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortQueryModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortQueryModelException(String message) {
        super(message);
    }

    public SortQueryModelException(Throwable cause) {
        super(cause);
    }
}
