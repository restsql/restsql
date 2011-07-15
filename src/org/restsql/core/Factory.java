/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.restsql.core.Request.Type;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Creates implemenations and also is a facade for other framework factories. Customize the framework with new factories
 * that use custom API implementation classes. Use restsql properties to specify the implemenation class names.
 * 
 * @author Mark Sawers
 */
public class Factory {
	private static ConnectionFactory connectionFactory;
	private static RequestFactory requestFactory;
	private static RequestLoggerFactory requestLoggerFactory;
	private static String sqlBuilderClassName;
	private static SqlResourceFactory sqlResourceFactory;
	private static String sqlResourceMetaDataClassName;

	/** Returns connection. */
	public static Connection getConnection(final String defaultDatabase) throws SQLException {
		return getConnectionFactory().getConnection(defaultDatabase);
	}

	/** Returns request object. */
	public static Request getRequest(final Request.Type type, final String sqlResource,
			final List<NameValuePair> resIds, final List<NameValuePair> params,
			final List<List<NameValuePair>> childrenParams, final RequestLogger requestLogger)
			throws InvalidRequestException {
		return getRequestFactory().getRequest(type, sqlResource, resIds, params, childrenParams,
				requestLogger);
	}

	/** Returns request object. */
	public static Request getRequest(final String client, final String method, final String uri)
			throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException {
		return getRequestFactory().getRequest(client, method, uri);
	}

	/** Creates request for child record with blank params. */
	public static Request getRequestForChild(final Type type, final String sqlResource,
			final List<NameValuePair> resIds, final RequestLogger requestLogger) {
		return getRequestFactory().getRequestForChild(type, sqlResource, resIds, requestLogger);
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

	/** Creates SqlBuilder instance. */
	public static SqlBuilder getSqlBuilder() {
		if (sqlBuilderClassName == null) {
			sqlBuilderClassName = Config.properties.getProperty(Config.KEY_SQL_BUILDER,
					Config.DEFAULT_SQL_BUILDER);
		}
		try {
			return (SqlBuilder) Class.forName(sqlBuilderClassName).newInstance();
		} catch (final Exception exception) {
			throw new RuntimeException("Error loading SqlBuilder implementation " + sqlBuilderClassName,
					exception);
		}
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
	public static InputStream getSqlResourceDefinition(final String resName)
			throws SqlResourceFactoryException {
		return getSqlResourceFactory().getSqlResourceDefinition(resName);
	}

	// Private utils

	/**
	 * Returns meta data object for definition.
	 * 
	 * @throws SqlResourceException if a database access error occurs
	 */
	public static SqlResourceMetaData getSqlResourceMetaData(final SqlResourceDefinition definition)
			throws SqlResourceException {
		if (sqlResourceMetaDataClassName == null) {
			sqlResourceMetaDataClassName = Config.properties.getProperty(Config.KEY_SQL_RESOURCE_METADATA,
					Config.DEFAULT_SQL_RESOURCE_METADATA);
		}
		SqlResourceMetaData metaData = null;
		try {
			metaData = (SqlResourceMetaData) Class.forName(sqlResourceMetaDataClassName).newInstance();
		} catch (final Exception exception) {
			throw new RuntimeException("Error loading SqlResourceMetaData implementation "
					+ sqlResourceMetaDataClassName, exception);
		}
		metaData.setDefinition(definition);
		return metaData;
	}

	/**
	 * Returns available SQL Resource names.
	 */
	public static List<String> getSqlResourceNames() {
		return getSqlResourceFactory().getSqlResourceNames();
	}

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

	// Factory Interfaces

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

	/** Creates JDBC connection objects. */
	public interface ConnectionFactory {
		public Connection getConnection(String defaultDatabase) throws SQLException;
	}

	/** Creates request objects. */
	public interface RequestFactory {
		public Request getRequest(final String client, final String method, final String uri)
				throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException;

		public Request getRequestForChild(Type type, String sqlResource, List<NameValuePair> resIds,
				RequestLogger requestLogger);

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

		public InputStream getSqlResourceDefinition(String resName) throws SqlResourceFactoryException;

		public List<String> getSqlResourceNames();
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