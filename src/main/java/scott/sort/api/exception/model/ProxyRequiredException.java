package scott.sort.api.exception.model;

import scott.sort.api.exception.SortRuntimeException;

public class ProxyRequiredException extends SortRuntimeException {

    private static final long serialVersionUID = 1L;

    public ProxyRequiredException() {
        super();
    }

    public ProxyRequiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProxyRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyRequiredException(String message) {
        super(message);
    }

    public ProxyRequiredException(Throwable cause) {
        super(cause);
    }

}
