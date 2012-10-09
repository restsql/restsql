/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Contains HTTP request attributes. Always used by the service, and optionally by Java API clients.
 * 
 * @author Mark Sawers
 */
public interface HttpRequestAttributes {
	public static final String DEFAULT_MEDIA_TYPE = "application/xml";

	/** Returns client IP or hostname. */
	public String getClient();

	/** Returns HTTP method. */
	public String getMethod();

	/** Returns request body, if any. */
	public String getRequestBody();

	/** Returns request media type. */
	public String getRequestMediaType();

	/** Returns response media type. */
	public String getResponseMediaType();

	/** Returns URI. */
	public String getUri();

	/**
	 * Sets attributes of an HTTP request.
	 * 
	 * @param client IP or host name
	 * @param method HTTP method
	 * @param uri request URI
	 * @param requestBody request body
	 * @param requestMediaType request body format, use internet media type e.g. application/xml
	 * @param responseMediaType request body format, use internet media type e.g. application/xml
	 */
	public void setAttributes(final String client, final String method, final String uri,
			final String requestBody, final String requestMediaType, final String responseMediaType);

	/**
	 * Sets response media type.
	 * 
	 * @param responseMediaType request body format, use internet media type e.g. application/xml
	 */
	public void setResponseMediaType(final String responseMediaType);
}
