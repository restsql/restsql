/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;

/**
 * Processes requests represented in some string form, e.g. XML or JSON.
 * 
 * @author Mark Sawers
 */
public interface RequestDeserializer {

	/** Executes write request. */
	public int execWrite(HttpRequestAttributes httpAttributes, final Request.Type requestType,
			final List<NameValuePair> resIds, final SqlResource sqlResource, final String requestBody,
			RequestLogger requestLogger) throws SqlResourceException;

	/** Returns supported media type. */
	public String getSupportedMediaType();
}
