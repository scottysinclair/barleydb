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

public class AddBatchException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public AddBatchException() {
        super();
    }

    public AddBatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AddBatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public AddBatchException(String message) {
        super(message);
    }

    public AddBatchException(Throwable cause) {
        super(cause);
    }

}
