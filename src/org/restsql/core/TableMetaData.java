/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlType;

/**
 * Encapsulates table information for an SQL Resource.
 * 
 * @author Mark Sawers
 * @see SqlResource
 * @see ColumnMetaData
 */
public interface TableMetaData {

	/** Adds normal column. */
	public void addColumn(final ColumnMetaData column);

	/** Adds primary key column. */
	public void addPrimaryKey(final ColumnMetaData column);

	/** Returns map of column meta data, keyed by the column label (the alias if provided in the query, otherwise the name). */
	public Map<String, ColumnMetaData> getColumns();

	/** Returns database name. */
	public String getDatabaseName();

	/** Returns ordered list of columns that are primary keys. */
	public List<ColumnMetaData> getPrimaryKeys();

	/**
	 * Returns fully qualified table name in database-specific form for use in SQL statements. MySQL uses the form
	 * <code>database.table</code>, for example <code>sakila.film</code>. PostgreSQL uses the form
	 * <code>database.schema.table</code>, for example <code>sakila.public.film</code>.
	 */
	public String getQualifiedTableName();

	/** Returns row alias. */
	public String getRowAlias();
	
	/** Returns row set alias. */
	public String getRowSetAlias();
	
	/**
	 * Returns row alias.
	 * 
     * @deprecated As of 0.8.11 use {@link #getRowAlias()}
	 */
	@Deprecated
	public String getTableAlias();

	/** Returns table name. */
	public String getTableName();

	/** Returns role of table in the SQL Resource. */
	public TableRole getTableRole();

	/** Returns true if the SQL Resource role is child. */
	public boolean isChild();

	/** Returns true if the SQL Resource role is parent. */
	public boolean isParent();

	/** Sets all the row and row set aliases. */
	public void setAliases(final String alias, final String rowAlias, final String rowSetAlias);

	/** Sets attributes. */
	public void setAttributes(final String tableName, final String qualifedTableName,
			final String databaseName, final TableRole tableRole);

	/**
	 * Sets table alias.
	 * 
     * @deprecated As of 0.8.11 use {@link #setAliases(String, String, String)}
	 */
	@Deprecated
	public void setTableAlias(final String tableAlias);
	
	/** Represents all of the roles a table may plan in a SQL Resource. */
	@XmlType(namespace = "http://restsql.org/schema")
	public enum TableRole {
		Child, ChildExtension, Join, Parent, ParentExtension, Unknown;
	}
}