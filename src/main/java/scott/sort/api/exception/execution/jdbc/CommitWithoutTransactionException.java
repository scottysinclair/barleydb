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

import scott.sort.api.exception.execution.SortServiceProviderException;

public class CommitWithoutTransactionException extends SortServiceProviderException {

    private static final long serialVersionUID = 1L;

    public CommitWithoutTransactionException() {
        super();
    }

    public CommitWithoutTransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CommitWithoutTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommitWithoutTransactionException(String message) {
        super(message);
    }

    public CommitWithoutTransactionException(Throwable cause) {
        super(cause);
    }

}