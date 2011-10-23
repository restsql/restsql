/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;
import java.util.Map;

import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Represents an SQL Resource, an queryable and updatable database "view".
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
	 * Returns meta data for child table of hierarchical resource.
	 * 
	 * @return table meta data or null if flat resource
	 */
	public TableMetaData getChildTable();

	/**
	 * Returns meta data for join table of many-to-many hierarchical resource.
	 * 
	 * @return table meta data or null if flat resource or one-to-many hierarchical
	 */
	public TableMetaData getJoinTable();

	/**
	 * Returns meta data for parent table.
	 * 
	 * @return table meta data
	 */
	public TableMetaData getParentTable();

	/**
	 * Returns meta data for all tables and columns. Includes parent, parent extensions, child and child extensions, but
	 * not the join table.
	 * 
	 * @return map of table meta data by qualified table name, i.e. database.table
	 */
	public Map<String, TableMetaData> getTables();

	/**
	 * Returns triggers classes.
	 * 
	 * @return list of trigger classes
	 */
	public List<Trigger> getTriggers();

	/**
	 * Returns true if resource has a parent-child structure.
	 * 
	 * @return true if resource has a parent-child structure
	 */
	public boolean isHierarchical();

	/**
	 * Executes query returning results as object collection.
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if a database access error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	public List<Map<String, Object>> readAsCollection(Request request)
			throws SqlResourceException;

	/**
	 * Executes query returning results as an XML string.
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if a database access error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	public String readAsXml(Request request) throws SqlResourceException;

	/**
	 * Executes insert, update or delete.
	 * 
	 * @param request Request object
	 * @return number of rows updated
	 * @throws SqlResourceException if a database access error occurs
	 */
	public int write(Request request) throws SqlResourceException;
}