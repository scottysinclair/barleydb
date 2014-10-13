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


public class DowncastEntityException extends IllegalQueryStateException {

    private static final long serialVersionUID = 1L;

    public DowncastEntityException() {
        super();
    }

    public DowncastEntityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DowncastEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public DowncastEntityException(String message) {
        super(message);
    }

    public DowncastEntityException(Throwable cause) {
        super(cause);
    }


}
