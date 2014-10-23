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

public class ClosingStatementException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public ClosingStatementException() {
        super();
    }

    public ClosingStatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ClosingStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClosingStatementException(String message) {
        super(message);
    }

    public ClosingStatementException(Throwable cause) {
        super(cause);
    }

}
