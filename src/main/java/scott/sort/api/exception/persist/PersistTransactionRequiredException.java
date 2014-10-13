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

public class PersistTransactionRequiredException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    public PersistTransactionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistTransactionRequiredException(String message) {
        super(message);
    }


}
