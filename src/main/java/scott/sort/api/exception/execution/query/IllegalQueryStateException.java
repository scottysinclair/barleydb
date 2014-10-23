package scott.sort.api.exception.execution.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


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
