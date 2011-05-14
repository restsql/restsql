/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Logs request for troubleshooting applications. The implementation logs requests to access, error and trace logs.
 * 
 * @author Mark Sawers
 */
public interface RequestLogger {
	/**
	 * Adds sql statement.
	 * 
	 * @param sql sql
	 */
	public void addSql(final String sql);

	/**
	 * Logs exceptional response.
	 */
	public void log(final int responseCode, final Exception exception);

	/**
	 * Logs normal response.
	 */
	public void log(final String responseBody);
}
