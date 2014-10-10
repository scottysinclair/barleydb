package scott.sort.api.exception.query;

import scott.sort.api.exception.SortException;

public class SortQueryException extends SortException {

    private static final long serialVersionUID = 1L;

    public SortQueryException() {
        super();
    }

    public SortQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortQueryException(String message) {
        super(message);
    }

    public SortQueryException(Throwable cause) {
        super(cause);
    }


}
