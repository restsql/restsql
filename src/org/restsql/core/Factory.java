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
 * Creates implementations and also is a facade for other framework factories. Customize the framework with new
 * factories that use custom API implementation classes. Use restsql properties to specify the implementation class
 * names.
 * 
 * @author Mark Sawers
 */
public class Factory extends AbstractFactory {

	/** Returns connection. */
	public static Connection getConnection(final String defaultDatabase) throws SQLException {
		return getConnectionFactory().getConnection(defaultDatabase);
	}

	/** Return connection factory. Useful for destroying it on app unload. */
	public static ConnectionFactory getConnectionFactory() {
		return (ConnectionFactory) getInstance(Config.KEY_CONNECTION_FACTORY,
				Config.DEFAULT_CONNECTION_FACTORY);
	}

	/** Returns request object. */
	public static Request getRequest(final Request.Type type, final String sqlResource,
			final List<NameValuePair> resIds, final List<NameValuePair> params,
			final List<List<NameValuePair>> childrenParams, final RequestLogger requestLogger)
			throws InvalidRequestException {
		final RequestFactory requestFactory = (RequestFactory) getInstance(Config.KEY_REQUEST_FACTORY,
				Config.DEFAULT_REQUEST_FACTORY);
		return requestFactory.getRequest(type, sqlResource, resIds, params, childrenParams, requestLogger);
	}

	/** Returns request object. */
	public static Request getRequest(final String client, final String method, final String uri)
			throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException {
		final RequestFactory requestFactory = (RequestFactory) getInstance(Config.KEY_REQUEST_FACTORY,
				Config.DEFAULT_REQUEST_FACTORY);
		return requestFactory.getRequest(client, method, uri);
	}

	/** Creates request for child record with blank params. */
	public static Request getRequestForChild(final Type type, final String sqlResource,
			final List<NameValuePair> resIds, final RequestLogger requestLogger) {
		final RequestFactory requestFactory = (RequestFactory) getInstance(Config.KEY_REQUEST_FACTORY,
				Config.DEFAULT_REQUEST_FACTORY);
		return requestFactory.getRequestForChild(type, sqlResource, resIds, requestLogger);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final HttpServletRequest request) {
		final RequestLoggerFactory requestLoggerFactory = (RequestLoggerFactory) getInstance(
				Config.KEY_REQUEST_LOGGER_FACTORY, Config.DEFAULT_REQUEST_LOGGER_FACTORY);
		return requestLoggerFactory.getRequestLogger(request);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final HttpServletRequest request,
			final MultivaluedMap<String, String> formParams) {
		final RequestLoggerFactory requestLoggerFactory = (RequestLoggerFactory) getInstance(
				Config.KEY_REQUEST_LOGGER_FACTORY, Config.DEFAULT_REQUEST_LOGGER_FACTORY);
		return requestLoggerFactory.getRequestLogger(request, formParams);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final HttpServletRequest request, final String requestBody) {
		final RequestLoggerFactory requestLoggerFactory = (RequestLoggerFactory) getInstance(
				Config.KEY_REQUEST_LOGGER_FACTORY, Config.DEFAULT_REQUEST_LOGGER_FACTORY);
		return requestLoggerFactory.getRequestLogger(request, requestBody);
	}

	/** Returns request logger. */
	public static RequestLogger getRequestLogger(final String client, final String method, final String uri) {
		final RequestLoggerFactory requestLoggerFactory = (RequestLoggerFactory) getInstance(
				Config.KEY_REQUEST_LOGGER_FACTORY, Config.DEFAULT_REQUEST_LOGGER_FACTORY);
		return requestLoggerFactory.getRequestLogger(client, method, uri);
	}

	/** Creates SqlBuilder instance. */
	public static SqlBuilder getSqlBuilder() {
		return (SqlBuilder) newInstance(Config.KEY_SQL_BUILDER, Config.DEFAULT_SQL_BUILDER);
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
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		return sqlResourceFactory.getSqlResource(resName);
	}

	/**
	 * Returns definition content as input stream.
	 */
	public static InputStream getSqlResourceDefinition(final String resName)
			throws SqlResourceFactoryException {
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		return sqlResourceFactory.getSqlResourceDefinition(resName);
	}

	/**
	 * Returns meta data object for definition.
	 * 
	 * @throws SqlResourceException if a database access error occurs
	 */
	public static SqlResourceMetaData getSqlResourceMetaData(final String resName,
			final SqlResourceDefinition definition) throws SqlResourceException {
		final SqlResourceMetaData sqlResourceMetaData = (SqlResourceMetaData) newInstance(
				Config.KEY_SQL_RESOURCE_METADATA, Config.DEFAULT_SQL_RESOURCE_METADATA);
		sqlResourceMetaData.setDefinition(resName, definition);
		return sqlResourceMetaData;
	}

	/**
	 * Returns available SQL Resource names.
	 */
	public static List<String> getSqlResourceNames() {
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		return sqlResourceFactory.getSqlResourceNames();
	}

	// Factory Interfaces

	/** Creates JDBC connection objects. */
	public interface ConnectionFactory {
		public Connection getConnection(String defaultDatabase) throws SQLException;

		public void destroy() throws SQLException;
	}

	/** Creates request objects. */
	public interface RequestFactory {
		public Request getRequest(final String client, final String method, final String uri)
				throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException;

		public Request getRequest(Type type, String sqlResource, List<NameValuePair> resIds,
				List<NameValuePair> params, List<List<NameValuePair>> childrenParams,
				RequestLogger requestLogger) throws InvalidRequestException;

		public Request getRequestForChild(Type type, String sqlResource, List<NameValuePair> resIds,
				RequestLogger requestLogger);
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