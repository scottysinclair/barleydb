package scott.sort.api.exception.execution.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


public class ResultDataConversionException extends SortQueryException {

    private static final long serialVersionUID = 1L;

    public ResultDataConversionException() {
        super();
    }

    public ResultDataConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ResultDataConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultDataConversionException(String message) {
        super(message);
    }

    public ResultDataConversionException(Throwable cause) {
        super(cause);
    }


}
