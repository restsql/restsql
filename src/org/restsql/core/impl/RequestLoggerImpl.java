/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.Request;

/**
 * Logs request for troubleshooting applications. The implementation logs requests to access, error and trace logs.
 * 
 * @author Mark Sawers
 */
public class RequestLoggerImpl implements org.restsql.core.RequestLogger {
	private static final Log accessLogger = LogFactory.getLog(Config.NAME_LOGGER_ACCESS);
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	private static final Log errorLogger = LogFactory.getLog(Config.NAME_LOGGER_ERROR);
	private static final Log traceLogger = LogFactory.getLog(Config.NAME_LOGGER_TRACE);

	private List<String> sqls;
	private final GregorianCalendar startTime;
	private HttpRequestAttributes httpAttributes;

	public RequestLoggerImpl() {
		startTime = new GregorianCalendar();
		httpAttributes = Factory.getHttpRequestAttributes("?", "?", "?", null, null, null);
	}

	/**
	 * Adds a SQL statement generated during request processing. Used by the framework.
	 */
	@Override
	public void addSql(final String sql) {
		if (sqls == null) {
			sqls = new ArrayList<String>(httpAttributes.getRequestBody() == null ? 1 : 16);
		}
		sqls.add(sql);
	}

	/**
	 * Returns list of SQL statements generated during request processing. Intended for Java API clients.
	 */
	@Override
	public List<String> getSql() {
		return sqls;
	}

	/**
	 * Logs exceptional response without an exception. Used by the service or Java API client.
	 */
	@Override
	public void log(final int responseCode) {
		log(responseCode, null, null);
	}

	/**
	 * Logs exceptional response with an exception. Used by the service or Java API client.
	 */
	@Override
	public void log(final int responseCode, final Exception exception) {
		log(responseCode, null, exception);
	}

	/**
	 * Logs normal response. Used by the service or Java API client.
	 */
	@Override
	public void log(final String responseBody) {
		log(200, responseBody, null);
	}

	/**
	 * Sets attributes of an HTTP request. Used by service when request is unauthorized prior to restSQL {@link Request}
	 * creation.
	 */
	public void setHttpRequestAttributes(final HttpRequestAttributes httpAttributes) {
		this.httpAttributes = httpAttributes;
	}

	/**
	 * Sets request. Used by {@link Request} implementation.
	 */
	@Override
	public void setRequest(final Request request) {
		setHttpRequestAttributes(request.getHttpRequestAttributes());
	}

	// Private utils

	private String getAccess(final int responseCode) {
		final StringBuffer string = new StringBuffer(300);

		// Client
		string.append(httpAttributes.getClient());

		// Timestamp
		string.append(' ');
		string.append(DATE_FORMAT.format(startTime.getTime()));

		// Method
		string.append(' ');
		string.append(httpAttributes.getMethod());

		// URI
		string.append(' ');
		string.append(httpAttributes.getUri());

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
		if (httpAttributes.getRequestBody() != null) {
			logger.info("   request:");
			logger.info(httpAttributes.getRequestBody());
		}
		if (sqls != null && sqls.size() > 0) {
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