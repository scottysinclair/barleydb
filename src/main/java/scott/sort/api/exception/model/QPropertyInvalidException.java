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

public class QPropertyInvalidException extends SortQueryModelException {

    private static final long serialVersionUID = 1L;

    public QPropertyInvalidException() {
        super();
    }

    public QPropertyInvalidException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public QPropertyInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public QPropertyInvalidException(String message) {
        super(message);
    }

    public QPropertyInvalidException(Throwable cause) {
        super(cause);
    }
}
