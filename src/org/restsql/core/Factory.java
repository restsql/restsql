/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.restsql.core.Request.Type;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Creates implementations and also is a facade for other framework factories. Customize the framework with new
 * factories that use custom API implementation classes. Use restsql properties to specify the implementation class
 * names. Note: Factories are always singletons and always produce new objects on request.
 * 
 * @author Mark Sawers
 */
public class Factory extends AbstractFactory {

	/** Creates request for child row with blank params. Configurable implementation class. */
	public static Request getChildRequest(final Request parentRequest) {
		final RequestFactory requestFactory = (RequestFactory) getInstance(Config.KEY_REQUEST_FACTORY,
				Config.DEFAULT_REQUEST_FACTORY);
		return requestFactory.getChildRequest(parentRequest);
	}

	/** Creates new column meta data object. Configurable implementation class. */
	public static ColumnMetaData getColumnMetaData() {
		return (ColumnMetaData)newInstance(Config.KEY_COLUMN_METADATA, Config.DEFAULT_COLUMN_METADATA);
	}

	/** Returns connection. */
	public static Connection getConnection(final String defaultDatabase) throws SQLException {
		return getConnectionFactory().getConnection(defaultDatabase);
	}

	/** Return connection factory. Useful for destroying it on app unload. Configurable implementation class. */
	public static ConnectionFactory getConnectionFactory() {
		return (ConnectionFactory) getInstance(Config.KEY_CONNECTION_FACTORY,
				Config.DEFAULT_CONNECTION_FACTORY);
	}

	/**
	 * Creates HTTP request attributes. Configurable implementation class.
	 * 
	 * @param client IP or host name
	 * @param method HTTP method
	 * @param uri request URI
	 * @param requestBody request body, e.g. XML or JSON
	 */
	public static HttpRequestAttributes getHttpRequestAttributes(final String client, final String method,
			final String uri, final String requestBody, final String requestContentType,
			final String responseContentType) {
		HttpRequestAttributes attributes = (HttpRequestAttributes) newInstance(
				Config.KEY_HTTP_REQUEST_ATTRIBUTES, Config.DEFAULT_HTTP_REQUEST_ATTRIBUTES);
		attributes.setAttributes(client, method, uri, requestBody, requestContentType, responseContentType);
		return attributes;
	}

	/**
	 * Returns request object with pre-parsed data from the URI. Used by Java API clients.
	 */
	public static Request getRequest(final Request.Type type, final String sqlResource,
			final List<RequestValue> resIds, final List<RequestValue> params,
			final List<List<RequestValue>> childrenParams, final RequestLogger requestLogger)
			throws InvalidRequestException {
		return getRequest(null, type, sqlResource, resIds, params, childrenParams, requestLogger);
	}

	/**
	 * Returns request object with pre-parsed data from the URI. Used by service and Java API clients. Configurable
	 * implementation class.
	 */
	public static Request getRequest(final HttpRequestAttributes httpAttributes, final Request.Type type,
			final String sqlResource, final List<RequestValue> resIds, final List<RequestValue> params,
			final List<List<RequestValue>> childrenParams, final RequestLogger requestLogger)
			throws InvalidRequestException {
		final RequestFactory requestFactory = (RequestFactory) getInstance(Config.KEY_REQUEST_FACTORY,
				Config.DEFAULT_REQUEST_FACTORY);
		return requestFactory.getRequest(httpAttributes, type, sqlResource, resIds, params, childrenParams,
				requestLogger);
	}

	/**
	 * Builds request from URI. Assumes pattern
	 * <code>res/{resourceName}/{resId1}/{resId2}?{param1}={value1}&amp;{param2}={value2}</code>. Used by the test harness,
	 * Java API clients and perhaps a straight servlet implementation. Configurable implementation class.
	 */
	public static Request getRequest(final HttpRequestAttributes httpAttributes)
			throws InvalidRequestException, SqlResourceFactoryException, SqlResourceException {
		final RequestFactory requestFactory = (RequestFactory) getInstance(Config.KEY_REQUEST_FACTORY,
				Config.DEFAULT_REQUEST_FACTORY);
		return requestFactory.getRequest(httpAttributes);
	}

	/** Returns request logger. Configurable implementation class. */
	public static RequestLogger getRequestLogger() {
		return (RequestLogger) newInstance(Config.KEY_REQUEST_LOGGER, Config.DEFAULT_REQUEST_LOGGER);
	}

	/**
	 * Returns request deserializer. Configurable implementation class.
	 * 
	 * @throws SqlResourceException if deserializer not found for media type
	 */
	public static RequestDeserializer getRequestDeserializer(final String mediaType)
			throws SqlResourceException {
		final RequestDeserializerFactory rdFactory = (RequestDeserializerFactory) getInstance(
				Config.KEY_REQUEST_DESERIALIZER_FACTORY, Config.DEFAULT_REQUEST_DESERIALIZER_FACTORY);
		return rdFactory.getRequestDeserializer(mediaType);
	}

	/**
	 * Returns response serializer for media type. Configurable implementation class.
	 * 
	 * @throws SqlResourceException if serializer not found for media type
	 */
	public static ResponseSerializer getResponseSerializer(final String mediaType)
			throws SqlResourceException {
		final ResponseSerializerFactory rsFactory = (ResponseSerializerFactory) getInstance(
				Config.KEY_RESPONSE_SERIALIZER_FACTORY, Config.DEFAULT_RESPONSE_SERIALIZER_FACTORY);
		return rsFactory.getResponseSerializer(mediaType);
	}

	/** Returns singleton SequenceManager. Configurable implementation class. */
	public static SequenceManager getSequenceManager() {
		return (SequenceManager) getInstance(Config.KEY_SEQUENCE_MANAGER, Config.DEFAULT_SEQUENCE_MANAGER);
	}

	/** Returns existing singleton SqlBuilder. Configurable implementation. */
	public static SqlBuilder getSqlBuilder() {
		return (SqlBuilder) getInstance(Config.KEY_SQL_BUILDER, Config.DEFAULT_SQL_BUILDER);
	}

	/**
	 * Returns SQL Resource for named resource. Configurable implementation class.
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
	 * Returns definition content as input stream. Configurable implementation class.
	 */
	public static InputStream getSqlResourceDefinition(final String resName)
			throws SqlResourceFactoryException {
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		return sqlResourceFactory.getSqlResourceDefinition(resName);
	}

	/**
	 * Returns meta data object for definition. Configurable implementation class.
	 * @param sqlBuilder db-specific SQL builder
	 * 
	 * @throws SqlResourceException if a database access error occurs
	 */
	public static SqlResourceMetaData getSqlResourceMetaData(final String resName,
			final SqlResourceDefinition definition, SqlBuilder sqlBuilder) throws SqlResourceException {
		final SqlResourceMetaData sqlResourceMetaData = (SqlResourceMetaData) newInstance(
				Config.KEY_SQL_RESOURCE_METADATA, Config.DEFAULT_SQL_RESOURCE_METADATA);
		sqlResourceMetaData.init(resName, definition, sqlBuilder);
		return sqlResourceMetaData;
	}

	/** Returns SQL resources directory using configured SqlResourceFactory. */
	public static String getSqlResourcesDir() {
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		return sqlResourceFactory.getSqlResourcesDir();
	}

	/**
	 * Returns available SQL Resource names using configured SqlResourceFactory.
	 * 
	 * @throws SqlResourceFactoryException if the configured directory does not exist
	 */
	public static List<String> getSqlResourceNames() throws SqlResourceFactoryException {
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		return sqlResourceFactory.getSqlResourceNames();
	}
	
	/** Creates new table meta data object. Configurable implementation class. */
	public static TableMetaData getTableMetaData() {
		return (TableMetaData)newInstance(Config.KEY_TABLE_METADATA, Config.DEFAULT_TABLE_METADATA);
	}

	/**
	 * Reloads definition from the source using configured SqlResourceFactory. This operation is not thread safe and
	 * should be run in development mode only.
	 * 
	 * @param resName resource name
	 * @throws SqlResourceFactoryException if the definition could not be marshalled
	 * @throws SqlResourceException if a database error occurs while collecting metadata
	 */
	public static void reloadSqlResource(final String resName) throws SqlResourceFactoryException,
			SqlResourceException {
		final SqlResourceFactory sqlResourceFactory = (SqlResourceFactory) getInstance(
				Config.KEY_SQL_RESOURCE_FACTORY, Config.DEFAULT_SQL_RESOURCE_FACTORY);
		sqlResourceFactory.reloadSqlResource(resName);
	}

	// Factory Interfaces

	/** Creates JDBC connection objects. */
	public interface ConnectionFactory {
		public void destroy() throws SQLException;

		public Connection getConnection(String defaultDatabase) throws SQLException;
	}

	/** Creates Request objects. */
	public interface RequestFactory {
		public Request getChildRequest(final Request parentRequest);

		public Request getRequest(final HttpRequestAttributes httpAttributes) throws InvalidRequestException,
				SqlResourceFactoryException, SqlResourceException;

		public Request getRequest(final HttpRequestAttributes httpAttributes, final Type type,
				final String sqlResource, final List<RequestValue> resIds, final List<RequestValue> params,
				final List<List<RequestValue>> childrenParams, final RequestLogger requestLogger)
				throws InvalidRequestException;
	}

	/** Creates RequestDeserializer objects. */
	public interface RequestDeserializerFactory {
		public RequestDeserializer getRequestDeserializer(final String mediaType) throws SqlResourceException;
	}

	/** Creates ResponseSerializer objects. */
	public interface ResponseSerializerFactory {
		public ResponseSerializer getResponseSerializer(final String mediaType) throws SqlResourceException;
	}

	/** Creates SQLResource objects. */
	public interface SqlResourceFactory {
		public SqlResource getSqlResource(final String resName) throws SqlResourceFactoryException,
				SqlResourceException;

		public InputStream getSqlResourceDefinition(String resName) throws SqlResourceFactoryException;

		public List<String> getSqlResourceNames() throws SqlResourceFactoryException;

		public String getSqlResourcesDir();

		public void reloadSqlResource(String resName) throws SqlResourceFactoryException,
				SqlResourceException;
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