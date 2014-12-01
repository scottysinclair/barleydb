package scott.sort.api.exception.execution;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.exception.SortException;

public class TypeConversionException extends SortException {

	private static final long serialVersionUID = 1L;

	public TypeConversionException() {
		super();
	}

	public TypeConversionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TypeConversionException(String message, Throwable cause) {
		super(message, cause);
	}

	public TypeConversionException(String message) {
		super(message);
	}

	public TypeConversionException(Throwable cause) {
		super(cause);
	}

}
