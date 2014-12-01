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

import scott.sort.api.exception.execution.persist.PreparingPersistStatementException;

public class TypeConverterNotFoundException extends PreparingPersistStatementException {

	private static final long serialVersionUID = 1L;

	public TypeConverterNotFoundException() {
		super();
	}

	public TypeConverterNotFoundException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TypeConverterNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public TypeConverterNotFoundException(String message) {
		super(message);
	}

	public TypeConverterNotFoundException(Throwable cause) {
		super(cause);
	}
}
