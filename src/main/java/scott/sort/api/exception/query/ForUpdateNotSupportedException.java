package scott.sort.api.exception.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public class ForUpdateNotSupportedException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public ForUpdateNotSupportedException() {
        super();
    }

    public ForUpdateNotSupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ForUpdateNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForUpdateNotSupportedException(String message) {
        super(message);
    }

    public ForUpdateNotSupportedException(Throwable cause) {
        super(cause);
    }


}
