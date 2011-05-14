/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.restsql.core.Request.Type;

/**
 * Facade for framework factories. Customize the framework with new factories that use custom API implementation
 * classes. Use System properties to specify the new factories. Property names are in {@link Config} in the form
 * <code>KEY_xxx_FACTORY</code>.
 * 
 * @author Mark Sawers
 */
public class Factory {
	private static ConnectionFactory connectionFactory;
	private static RequestFactory requestFactory;
	private static RequestLoggerFactory requestLoggerFactory;
	private static SqlResourceFactory sqlResourceFactory;

	/** Returns connection. */
	public static Connection getConnection(String defaultDatabase) throws SQLException {
		return getConnectionFactory().getConnection(defaultDatabase);
	}

	/** Returns request object. */
	public static Request getRequest(final Request.Type type, final String sqlResource,
			final List<NameValuePair> resIds, final List<NameValuePair> params,
			final List<List<NameValuePair>> childrenParams, RequestLogger requestLogger)
			throws InvalidRequestException {
		return getRequestFactory().getRequest(type, sqlResource, resIds, params, childrenParams,
				requestLogger);
	}

	/** Returns request object. */
	public static Request getRequest(final String client, final String method, final String uri)
			throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException {
		return getRequestFactory().getRequest(client, method, uri);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final HttpServletRequest request) {
		return getRequestLoggerFactory().getRequestLogger(request);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final HttpServletRequest request,
			final MultivaluedMap<String, String> formParams) {
		return getRequestLoggerFactory().getRequestLogger(request, formParams);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final HttpServletRequest request, final String requestBody) {
		return getRequestLoggerFactory().getRequestLogger(request, requestBody);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final String client, final String method, final String uri) {
		return getRequestLoggerFactory().getRequestLogger(client, method, uri);
	}

	/**
	 * Returns SQL Resource for named resource.
	 * 
	 * @param resName resource name
	 * @return SQLResource object
	 * @throws SqlResourceFactoryException if the definition could not be marshalled
	 * @throws SqlResourceException if a database error occurs while collecting metadata
	 */
	public static SqlResource getSqlResource(final String resName) throws SqlResourceFactoryException,
			SqlResourceException {
		return getSqlResourceFactory().getSqlResource(resName);
	}

	/**
	 * Returns definition content as input stream.
	 */
	public static InputStream getSqlResourceDefinition(String resName) throws SqlResourceFactoryException {
		return getSqlResourceFactory().getSqlResourceDefinition(resName);
	}

	/**
	 * Returns available SQL Resource names.
	 */
	public static List<String> getSqlResourceNames() {
		return getSqlResourceFactory().getSqlResourceNames();
	}

	// Private utils

	private static ConnectionFactory getConnectionFactory() {
		if (connectionFactory == null) {
			final String className = Config.properties.getProperty(Config.KEY_CONNECTION_FACTORY,
					Config.DEFAULT_CONNECTION_FACTORY);
			try {
				connectionFactory = (ConnectionFactory) Class.forName(className).newInstance();
			} catch (final Exception exception) {
				throw new RuntimeException("Error loading ConnectionFactory implementation " + className,
						exception);
			}
		}
		return connectionFactory;
	}

	private static RequestFactory getRequestFactory() {
		if (requestFactory == null) {
			final String className = Config.properties.getProperty(Config.KEY_REQUEST_FACTORY,
					Config.DEFAULT_REQUEST_FACTORY);
			try {
				requestFactory = (RequestFactory) Class.forName(className).newInstance();
			} catch (final Exception exception) {
				throw new RuntimeException("Error loading RequestFactory implementation " + className,
						exception);
			}
		}
		return requestFactory;
	}

	private static RequestLoggerFactory getRequestLoggerFactory() {
		if (requestLoggerFactory == null) {
			final String className = Config.properties.getProperty(Config.KEY_REQUEST_LOGGER_FACTORY,
					Config.DEFAULT_REQUEST_LOGGER_FACTORY);
			try {
				requestLoggerFactory = (RequestLoggerFactory) Class.forName(className).newInstance();
			} catch (final Exception exception) {
				throw new RuntimeException("Error loading RequestLoggerFactory implementation " + className,
						exception);
			}
		}
		return requestLoggerFactory;
	}

	private static SqlResourceFactory getSqlResourceFactory() {
		if (sqlResourceFactory == null) {
			final String className = Config.properties.getProperty(Config.KEY_SQL_RESOURCE_FACTORY,
					Config.DEFAULT_SQL_RESOURCE_FACTORY);
			try {
				sqlResourceFactory = (SqlResourceFactory) Class.forName(className).newInstance();
			} catch (final Exception exception) {
				throw new RuntimeException("Error loading SqlResourceFactory implementation " + className,
						exception);
			}
		}
		return sqlResourceFactory;
	}

	// Factory Interfaces

	/** Creates JDBC connection objects. */
	public interface ConnectionFactory {
		public Connection getConnection(String defaultDatabase) throws SQLException;
	}

	/** Creates request objects. */
	public interface RequestFactory {
		public Request getRequest(final String client, final String method, final String uri)
				throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException;

		public Request getRequest(Type type, String sqlResource, List<NameValuePair> resIds,
				List<NameValuePair> params, List<List<NameValuePair>> childrenParams,
				RequestLogger requestLogger) throws InvalidRequestException;
	}

	/** Creates request loggers. */
	public interface RequestLoggerFactory {
		public RequestLogger getRequestLogger(final HttpServletRequest request);

		public RequestLogger getRequestLogger(final HttpServletRequest request,
				final MultivaluedMap<String, String> formParams);

		public RequestLogger getRequestLogger(final HttpServletRequest request, final String requestBody);

		public RequestLogger getRequestLogger(final String client, final String method, final String uri);
	}

	/** Creates SQL Resource objects. */
	public interface SqlResourceFactory {
		public SqlResource getSqlResource(final String resName) throws SqlResourceFactoryException,
				SqlResourceException;

		public List<String> getSqlResourceNames();

		public InputStream getSqlResourceDefinition(String resName) throws SqlResourceFactoryException;
	}

	/** Indicates an error in creating a SQL Resource object from a definition. */
	public static class SqlResourceFactoryException extends SqlResourceException {
		private static final long serialVersionUID = 1L;

		public SqlResourceFactoryException(final String message) {
			super(message);
		}

		public SqlResourceFactoryException(final Throwable cause) {
			super(cause);
		}
	}
}