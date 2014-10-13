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

public class RollbackException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public RollbackException() {
        super();
    }

    public RollbackException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RollbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbackException(String message) {
        super(message);
    }

    public RollbackException(Throwable cause) {
        super(cause);
    }

}
