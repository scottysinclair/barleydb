package scott.sort.api.exception.execution.jdbc;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public class AquireConnectionException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public AquireConnectionException() {
        super();
    }

    public AquireConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AquireConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AquireConnectionException(String message) {
        super(message);
    }

    public AquireConnectionException(Throwable cause) {
        super(cause);
    }

}
