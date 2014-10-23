package scott.sort.api.exception;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public class SortRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SortRuntimeException() {
        super();
    }

    public SortRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortRuntimeException(String message) {
        super(message);
    }

    public SortRuntimeException(Throwable cause) {
        super(cause);
    }
}
