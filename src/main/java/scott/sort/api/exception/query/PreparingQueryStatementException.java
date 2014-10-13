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



/**
 * An exception occurred preparing statements either from the JDBC driver
 * or in the sort logic.
 *
 * @author scott
 *
 */
public class PreparingQueryStatementException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public PreparingQueryStatementException() {
    }

    public PreparingQueryStatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PreparingQueryStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreparingQueryStatementException(String message) {
        super(message);
    }

    public PreparingQueryStatementException(Throwable cause) {
        super(cause);
    }


}
