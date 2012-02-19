/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.RequestLogger;

/**
 * Creates request loggers, adapting a servlet request to the framework's configured RequestLogger.
 * 
 * @author Mark Sawers
 */
public class ServiceRequestLoggerFactory {
	private static final Log accessLogger = LogFactory.getLog(Config.NAME_LOGGER_ACCESS);
	private static final Log errorLogger = LogFactory.getLog(Config.NAME_LOGGER_ERROR);
	private static final Log traceLogger = LogFactory.getLog(Config.NAME_LOGGER_TRACE);

	/**
	 * Creates logger for service without a request body.
	 * 
	 * @param request servlet request
	 */
	public static RequestLogger getRequestLogger(final HttpServletRequest request) {
		return getRequestLogger(request, (String) null);
	}

	/**
	 * Creates logger for service with a request body.
	 * 
	 * @param request servlet request
	 * @param requestBody request body, e.g. in XML or JSON
	 */
	public static RequestLogger getRequestLogger(final HttpServletRequest request, final String requestBody) {
		final RequestLogger logger = Factory.getRequestLogger();
		if (accessLogger.isInfoEnabled() || errorLogger.isInfoEnabled() || traceLogger.isInfoEnabled()) {
			String client, method, uri;
			client = request.getRemoteAddr();
			method = request.getMethod();
			if (request.getQueryString() == null) {
				uri = request.getRequestURI();
			} else {
				uri = request.getRequestURI() + "?" + request.getQueryString();
			}
			logger.setRequestAttributes(client, method, uri, requestBody);
		}
		return logger;
	}

	/**
	 * Creates logger for service with URL encoded form params.
	 * 
	 * @param request servlet request
	 * @param formParams URL encoded form params
	 */
	public static RequestLogger getRequestLogger(final HttpServletRequest request,
			final MultivaluedMap<String, String> formParams) {
		String requestBody = null;
		if (errorLogger.isInfoEnabled() || traceLogger.isInfoEnabled()) {
			final StringBuffer string = new StringBuffer(300);
			for (final String key : formParams.keySet()) {
				if (string.length() > 0) {
					string.append('&');
				}
				string.append(key);
				string.append("=");
				string.append(formParams.get(key).get(0));
			}
			requestBody = string.toString();
		}
		return getRequestLogger(request, requestBody);
	}
}
