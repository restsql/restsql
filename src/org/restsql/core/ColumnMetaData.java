/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import org.restsql.core.TableMetaData.TableRole;

/**
 * Encapsulates column (or field) metadata of an SQL Resource.
 * 
 * @author Mark Sawers
 * @see SqlResource
 * @see TableMetaData
 */
public interface ColumnMetaData extends Comparable<ColumnMetaData> {

	/**
	 * Compares another column based on the column number of the select clause in the SQL Resource definition query.
	 * Implements  Comparable interface.
	 */
	public int compareTo(ColumnMetaData column);

	/**
	 * Returns column label, a string identified in double quotes after columns in the select clause in the SQL Resource
	 * definition query.
	 */
	public String getColumnLabel();

	/** Returns column name, as it is known by the database. */
	public String getColumnName();

	/** Returns column number in the select clause in the SQL Resource definition query. */
	public int getColumnNumber();

	/**
	 * Returns column type from java.sql.Types.
	 * 
	 * @return java.sql.Types constant
	 * @see java.sql.Types
	 */
	public int getColumnType();

	/** Returns column type name as given by the database JDBC driver. */
	public String getColumnTypeName();

	/** Returns database name. */
	public String getDatabaseName();

	/**
	 * Returns fully qualified column name in database-specific form for use in SQL statements. MySQL uses the form
	 * <code>database.table.column</code>, for example <code>sakila.film.film_id</code>. PostgreSQL uses the form
	 * <code>database.schema.table.column</code>, for example <code>sakila.public.film.film_id</code>.
	 */
	public String getQualifiedColumnName();

	/**
	 * Returns fully qualified table name in database-specific form for use in SQL statements. MySQL uses the form
	 * <code>database.table</code>, for example <code>sakila.film</code>. PostgreSQL uses the form
	 * <code>database.schema.table</code>, for example <code>sakila.public.film</code>.
	 */
	public String getQualifiedTableName();

	/** Returns sequence name associated with column or null if none. For MySQL, returns table name. */
	public String getSequenceName();

	/** Returns table name. */
	public String getTableName();

	/** Returns role of table in the SQL Resource. */
	public TableRole getTableRole();

	/** Returns true if column is a character string or date, time or timestamp. */
	public boolean isCharOrDateTimeType();

	/**
	 * Returns true for foreign key columns not declared in the SQL Resource query but added by the framework. These are
	 * required for writes to child extensions, parent extensions and and one-to-many child tables.
	 */
	public boolean isNonqueriedForeignKey();

	/** Returns true if the column is a primary key. */
	public boolean isPrimaryKey();

	/** Returns true if the column is read-only, for example derived from SQL function or a database view. */
	public boolean isReadOnly();

	/**
	 * Returns true if column is associated with a sequence.
	 * 
	 * @see #getSequenceName()
	 */
	public boolean isSequence();
}