/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;

/**
 * Creates {@link HttpRequestAttributes} and request body strings from form params.
 * 
 * @author Mark Sawers
 */
public class HttpRequestHelper {

	/**
	 * Creates attributes helper object from http request with a request body.
	 * 
	 * @param httpRequest servlet request
	 * @param requestBody request body
	 * @param requestMediaType request body format, use internet media type e.g. application/xml
	 * @param responseMediaType request body format, use internet media type e.g. application/xml
	 */
	public static HttpRequestAttributes getHttpRequestAttributes(final HttpServletRequest httpRequest,
			final String requestBody, final String requestMediaType, final String responseMediaType) {
		String client, method, uri;
		client = httpRequest.getRemoteAddr();
		method = httpRequest.getMethod();
		if (httpRequest.getQueryString() == null) {
			uri = httpRequest.getRequestURI();
		} else {
			uri = httpRequest.getRequestURI() + "?" + httpRequest.getQueryString();
		}

		return Factory.getHttpRequestAttributes(client, method, uri, requestBody, requestMediaType,
				responseMediaType);
	}

	/**
	 * Converts form params into string. Jersey does not provide the string form of the request body when it generates a
	 * form param map.
	 */
	public static String getRequestBodyFromFormParams(final MultivaluedMap<String, String> formParams) {
		final StringBuilder string = new StringBuilder(300);
		for (final String key : formParams.keySet()) {
			if (string.length() > 0) {
				string.append('&');
			}
			string.append(key);
			string.append("=");
			string.append(formParams.get(key).get(0));
		}
		return string.toString();
	}
}