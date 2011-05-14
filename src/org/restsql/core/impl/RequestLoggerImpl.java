/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restsql.core.Config;

/**
 * Logs requests for access, error and trace logs.
 * 
 * @author Mark Sawers
 */
public class RequestLoggerImpl implements org.restsql.core.RequestLogger {
	private static final Log accessLogger = LogFactory.getLog(Config.NAME_LOGGER_ACCESS);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	private static final Log errorLogger = LogFactory.getLog(Config.NAME_LOGGER_ERROR);
	private static final Log traceLogger = LogFactory.getLog(Config.NAME_LOGGER_TRACE);

	private String client;
	private String method;
	private String requestBody;
	private List<String> sqls;
	private GregorianCalendar startTime;
	private String uri;

	/**
	 * Creates logger for service.
	 * 
	 * @param request servlet request
	 */
	public RequestLoggerImpl(final HttpServletRequest request) {
		this(request, 1);
	}

	/**
	 * Creates logger for service with URL encoded form params.
	 * 
	 * @param request servlet request
	 * @param formParams URL encoded form params
	 */
	public RequestLoggerImpl(final HttpServletRequest request, final MultivaluedMap<String, String> formParams) {
		this(request);
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
	}

	/**
	 * Creates logger for service with a request body.
	 * 
	 * @param request servlet request
	 * @param requestBody an xml body
	 */
	public RequestLoggerImpl(final HttpServletRequest request, final String requestBody) {
		this(request, 16);
		this.requestBody = requestBody;
	}

	/**
	 * Creates logger for application using API.
	 * 
	 * @param client ip or host name
	 * @param method HTTP method
	 * @param uri request URI
	 */
	public RequestLoggerImpl(final String client, final String method, final String uri) {
		if (accessLogger.isInfoEnabled() || errorLogger.isInfoEnabled() || traceLogger.isInfoEnabled()) {
			startTime = new GregorianCalendar();
			this.client = client;
			this.method = method;
			this.uri = uri;
		}
		createSqlList(1);
	}

	private RequestLoggerImpl(final HttpServletRequest request, final int sqlListSize) {
		if (accessLogger.isInfoEnabled() || errorLogger.isInfoEnabled() || traceLogger.isInfoEnabled()) {
			startTime = new GregorianCalendar();
			client = request.getRemoteAddr();
			method = request.getMethod();
			if (request.getQueryString() == null) {
				uri = request.getRequestURI();
			} else {
				uri = request.getRequestURI() + "?" + request.getQueryString();
			}
		}
		createSqlList(sqlListSize);
	}

	/**
	 * Adds sql statement.
	 * 
	 * @param sql sql
	 */
	public void addSql(final String sql) {
		sqls.add(sql);
	}

	/**
	 * Logs exceptional response.
	 */
	public void log(final int responseCode, final Exception exception) {
		log(responseCode, null, exception);
	}

	/**
	 * Logs normal response.
	 */
	public void log(final String responseBody) {
		log(200, responseBody, null);
	}

	// Private utils

	private void createSqlList(final int sqlListSize) {
		if (errorLogger.isInfoEnabled() || traceLogger.isInfoEnabled()) {
			sqls = new ArrayList<String>(sqlListSize);
		}
	}

	private String getAccess(final int responseCode) {
		final StringBuffer string = new StringBuffer(300);

		// Client
		string.append(client);

		// Timestamp
		string.append(' ');
		string.append(DATE_FORMAT.format(startTime.getTime()));

		// Method
		string.append(' ');
		string.append(method);

		// URI
		string.append(' ');
		string.append(uri);

		// Response Code
		string.append(' ');
		string.append(String.valueOf(responseCode));

		// Elapsed time
		string.append(' ');
		string.append(String.valueOf(System.currentTimeMillis() - startTime.getTimeInMillis()));
		string.append("ms");

		return string.toString();
	}

	private String getBriefError(final int responseCode, final Exception exception) {
		final StringBuffer string = new StringBuffer(300);
		string.append("   ");
		string.append(String.valueOf(responseCode));
		string.append(": ");
		string.append(exception.getMessage());
		return string.toString();
	}

	private void log(final int responseCode, final String responseBody, final Exception exception) {
		if (accessLogger.isInfoEnabled() || errorLogger.isInfoEnabled() || traceLogger.isInfoEnabled()) {
			final String access = getAccess(responseCode);
			if (accessLogger.isInfoEnabled()) {
				accessLogger.info(access);
				if (responseCode != 200 && exception != null) {
					accessLogger.info(getBriefError(responseCode, exception));
				}
			}
			if (errorLogger.isInfoEnabled() && exception != null) {
				logComplete(errorLogger, access, responseBody, exception);
			}
			if (traceLogger.isInfoEnabled()) {
				logComplete(traceLogger, access, responseBody, exception);
			}
		}
	}

	private void logComplete(final Log logger, final String access, final String responseBody,
			final Exception exception) {
		logger.info(access);
		if (requestBody != null) {
			logger.info("   request:");
			logger.info(requestBody);
		}
		if (sqls.size() > 0) {
			logger.info("   sql:");
			for (final String sql : sqls) {
				logger.info(sql);
			}
		}
		logger.info("   response:");
		if (responseBody != null) {
			logger.info(responseBody);
		} else if (exception != null) { // should always be null at this point
			logger.info(exception.getMessage());
		}
		logger.info("---------------------");
	}
}