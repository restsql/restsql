/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import org.restsql.core.HttpRequestAttributes;

/**
 * Simple http request attributes implementation.
 * 
 * @author Mark Sawers
 */
public class HttpRequestAttributesImpl implements HttpRequestAttributes {

	private String client;
	private String method;
	private String requestBody;
	private String requestMediaType, responseMediaType;
	private String uri;

	@Override
	public String getClient() {
		return client;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getRequestBody() {
		return requestBody;
	}

	@Override
	public String getRequestMediaType() {
		return requestMediaType;
	}

	@Override
	public String getResponseMediaType() {
		return responseMediaType;
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public void setAttributes(final String client, final String method, final String uri,
			final String requestBody, final String requestMediaType, final String responseMediaType) {
		this.client = client;
		this.requestMediaType = requestMediaType;
		this.responseMediaType = responseMediaType;
		this.method = method;
		this.uri = uri;
		this.requestBody = requestBody;
	}

	public void setClient(final String client) {
		this.client = client;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public void setRequestBody(final String requestBody) {
		this.requestBody = requestBody;
	}

	public void setRequestMediaType(final String requestMediaType) {
		this.requestMediaType = requestMediaType;
	}

	public void setResponseMediaType(final String responseMediaType) {
		this.responseMediaType = responseMediaType;
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

	@Override
	public String toString() {
		return client + " " + method + " " + uri;
	}
}
