/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates table information for an SQL Resource.
 * 
 * @author Mark Sawers
 * @see SqlResource
 * @see ColumnMetaData
 */
public interface TableMetaData {

	/** Returns map of column meta data, keyed by the column label. */
	public Map<String, ColumnMetaData> getColumns();

	/** Returns database name. */
	public String getDatabaseName();

	/** Returns ordered list of columns that are primary keys. */
	public List<ColumnMetaData> getPrimaryKeys();

	/**
	 * Returns table name qualified by database in the form <code>database.table</code>, for example
	 * <code>sakila.film</code>.
	 */
	public String getQualifiedTableName();

	/** Returns table name. */
	public String getTableName();

	/** Returns role of table in the SQL Resource. */
	public TableRole getTableRole();

	/** Returns true if the SQL Resource role is child. */
	public boolean isChild();

	/** Returns true if the SQL Resource role is parent. */
	public boolean isParent();

	/** Represents all of the roles a table may plan in a SQL Resource. */
	public enum TableRole {
		Child, ChildExtension, Join, Parent, ParentExtension, Unknown;
	}
}