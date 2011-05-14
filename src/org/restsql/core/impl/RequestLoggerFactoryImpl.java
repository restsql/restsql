/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.restsql.core.RequestLogger;
import org.restsql.core.Factory.RequestLoggerFactory;

public class RequestLoggerFactoryImpl implements RequestLoggerFactory {

	@Override
	public RequestLogger getRequestLogger(HttpServletRequest request) {
		return new RequestLoggerImpl(request);
	}

	@Override
	public RequestLogger getRequestLogger(HttpServletRequest request,
			MultivaluedMap<String, String> formParams) {
		return new RequestLoggerImpl(request, formParams);
	}

	@Override
	public RequestLogger getRequestLogger(HttpServletRequest request, String requestBody) {
		return new RequestLoggerImpl(request, requestBody);
	}

	@Override
	public RequestLogger getRequestLogger(String client, String method, String uri) {
		return new RequestLoggerImpl(client, method, uri);
	}

}
