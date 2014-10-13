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

public class QueryConnectionRequiredException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public QueryConnectionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryConnectionRequiredException(String message) {
        super(message);
    }


}
