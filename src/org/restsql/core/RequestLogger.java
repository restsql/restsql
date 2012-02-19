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
	 * Sets request attributes. Used by the service or Java API clients.
	 */
	public void setRequestAttributes(String client, String method, String uri);

	/**
	 * Sets request attributes. Used by the service or Java API clients.
	 * 
	 * @param client IP or host name
	 * @param method HTTP method
	 * @param uri request URI
	 * @param requestBody request body, e.g. XML or JSON
	 */
	public void setRequestAttributes(String client, String method, String uri, String requestBody);
}
