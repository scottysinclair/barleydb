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

public class SetAutoCommitException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public SetAutoCommitException() {
        super();
    }

    public SetAutoCommitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SetAutoCommitException(String message, Throwable cause) {
        super(message, cause);
    }

    public SetAutoCommitException(String message) {
        super(message);
    }

    public SetAutoCommitException(Throwable cause) {
        super(cause);
    }


}
