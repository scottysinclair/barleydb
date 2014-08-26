package com.smartstream.morf.server.jdbc.persister.exception;


public class PersistException extends Exception {

	private static final long serialVersionUID = 1L;

	public PersistException(String message) {
        super(message);
    }

    public PersistException(String message, Throwable cause) {
        super(message, cause);
    }
}
