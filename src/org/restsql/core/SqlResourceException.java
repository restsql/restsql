/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Represents a restSQL error, typically wrapping an <code>SQLException</code> and adding an SQL String. Calling
 * <code>getMessage()</code> and <code>toString()</code> will include the SQL in the output. The class can also be used
 * to wrap other exceptions. It is also the superclass for other restSQL errors allowing the framework to manage a
 * single exception in method signatures.
 * 
 * @author Mark Sawers
 */
public class SqlResourceException extends Exception {
	private static final long serialVersionUID = 1L;

	private String sql;

	public SqlResourceException(String message) {
		super(message);
	}

	public SqlResourceException(Throwable cause) {
		super(cause);
	}

	public SqlResourceException(Throwable cause, String sql) {
		super(cause);
		this.sql = sql;
	}

	public SqlResourceException(String message, String sql) {
		super(message);
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

	public String getMessage() {
		String message;
		if (getCause() != null) {
			message = getCause().getMessage();
		} else {
			message = super.getMessage();
		}
		if (sql != null) {
			message += " :: " + sql;
		}
		return message;
	}
}
