/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;

/**
 * Logs request for troubleshooting applications. The implementation logs requests to access, error and trace logs.
 * 
 * @author Mark Sawers
 */
public interface RequestLogger {
	/**
	 * Adds a SQL statement generated during request processing. Used by the framework.
	 */
	public void addSql(final String sql);

	/**
	 * Returns list of SQL statements generated during request processing. Intended for Java API clients.
	 */
	public List<String> getSql();

	/**
	 * Logs exceptional response without an exception. Used by the service or Java API client.
	 */
	public void log(final int responseCode);

	/**
	 * Logs exceptional response with an exception. Used by the service or Java API client.
	 */
	public void log(final int responseCode, final Exception exception);

	/**
	 * Logs normal response. Used by the service or Java API client.
	 */
	public void log(final String responseBody);

	/**
	 * Sets request. Used by {@link Request} implementation.
	 */
	public void setRequest(Request request);

	/**
	 * Sets attributes of an HTTP request. Used by service when request is unauthorized prior to restSQL {@link Request}
	 * creation.
	 */
	public void setHttpRequestAttributes(final HttpRequestAttributes httpRequestAttributes);
}
