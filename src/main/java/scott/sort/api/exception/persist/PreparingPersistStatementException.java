package scott.sort.api.exception.persist;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.server.jdbc.persister.exception.SortPersistException;

/**
 * An exception occurred preparing statements either from the JDBC driver
 * or in the sort logic.
 *
 * @author scott
 *
 */
public class PreparingPersistStatementException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    public PreparingPersistStatementException() {
    }

    public PreparingPersistStatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PreparingPersistStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreparingPersistStatementException(String message) {
        super(message);
    }

    public PreparingPersistStatementException(Throwable cause) {
        super(cause);
    }


}
