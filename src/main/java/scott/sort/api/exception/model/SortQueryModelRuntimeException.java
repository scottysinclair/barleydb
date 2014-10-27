package scott.sort.api.exception.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.exception.SortRuntimeException;

public class SortQueryModelRuntimeException extends SortRuntimeException {

    private static final long serialVersionUID = 1L;

    public SortQueryModelRuntimeException() {
        super();
    }

    public SortQueryModelRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortQueryModelRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortQueryModelRuntimeException(String message) {
        super(message);
    }

    public SortQueryModelRuntimeException(Throwable cause) {
        super(cause);
    }
}
