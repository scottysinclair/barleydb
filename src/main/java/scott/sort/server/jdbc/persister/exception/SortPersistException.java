package scott.sort.server.jdbc.persister.exception;

import scott.sort.api.exception.SortException;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public class SortPersistException extends SortException {

    private static final long serialVersionUID = 1L;

    public SortPersistException(String message) {
        super(message);
    }

    public SortPersistException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortPersistException() {
        super();
    }

    public SortPersistException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortPersistException(Throwable cause) {
        super(cause);
    }

}
