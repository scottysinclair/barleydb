package scott.sort.api.exception.execution.jdbc;

import scott.sort.api.exception.execution.SortServiceProviderException;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

/**
 * A rollback was attempted on a connection in autocommit mode.
 *
 * The JDBC spec says that an SQLException is thrown in this case. So we check first before rolling back
 * and throw a specific exception
 *
 * @author scott
 *
 */
public class RollbackWithoutTransactionException extends SortServiceProviderException {

    private static final long serialVersionUID = 1L;

    public RollbackWithoutTransactionException() {
        super();
    }

    public RollbackWithoutTransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RollbackWithoutTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbackWithoutTransactionException(String message) {
        super(message);
    }

    public RollbackWithoutTransactionException(Throwable cause) {
        super(cause);
    }

}
