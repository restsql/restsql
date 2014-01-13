/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;
import java.util.Map;

import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Represents an SQL Resource, a queryable and updatable database "view".
 * 
 * @author Mark Sawers
 */
public interface SqlResource {
	/**
	 * Returns SQL resource information defined by the user, including query, validated attributes and trigger.
	 * 
	 * @return definition
	 */
	public SqlResourceDefinition getDefinition();

	/**
	 * Returns SQL resource name.
	 * 
	 * @return SQL resource name
	 */
	public String getName();

	/**
	 * Returns meta data for SQL resource.
	 * 
	 * @return SQL rsource meta data
	 */
	public SqlResourceMetaData getMetaData();

	/**
	 * Returns triggers classes.
	 * 
	 * @return list of trigger classes
	 */
	public List<Trigger> getTriggers();

	/**
	 * Executes query returning results as an object collection.
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if a database access error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	public List<Map<String, Object>> read(Request request) throws SqlResourceException;

	/**
	 * Executes query returning results as a string.
	 * 
	 * @param request Request object
	 * @param mediaType response format, use internet media type e.g. application/xml
	 * @throws SqlResourceException if a database access error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	public String read(final Request request, final String mediaType) throws SqlResourceException;

	/**
	 * Executes database write (insert, update or delete).
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if the request is invalid or a database access error or trigger exception occurs
	 * @return write response
	 */
	public WriteResponse write(final Request request) throws SqlResourceException;
}