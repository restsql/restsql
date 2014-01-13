/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Converts read/write results to a string, e.g. XML document or JSON objects.
 * 
 * @author Mark Sawers
 */
public interface ResponseSerializer {

	/** Returns supported media type. */
	public String getSupportedMediaType();

	/**
	 * Converts flat select results to a string.
	 * 
	 * @param sqlResource SQL resource
	 * @param resultSet results
	 * @return string
	 */
	public String serializeReadFlat(final SqlResource sqlResource, final ResultSet resultSet)
			throws SQLException;

	/**
	 * Converts hierarchical select results to a string.
	 * 
	 * @param sqlResource SQL resource
	 * @param results results
	 * @return string
	 */
	public String serializeReadHierarchical(final SqlResource sqlResource,
			final List<Map<String, Object>> results);

	/**
	 * Converts write response to a string.
	 * @param sqlResource SQL resource
	 * @param response response
	 */
	public String serializeWrite(SqlResource sqlResource, final WriteResponse response);	
}