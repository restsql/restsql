/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Encapsulates column (or field) metadata of an SQL Resource.
 * 
 * @author Mark Sawers
 * @see SqlResource
 * @see TableMetaData
 */
public interface ColumnMetaData {

	/** Returns database name. */
	public String getDatabaseName();

	/**
	 * Returns column label, a string identified in double quotes after columns in the select clause in the SQL Resource
	 * definition query.
	 */
	public String getColumnLabel();

	/** Returns column name, as it is known by the database. */
	public String getColumnName();

	/** Returns column number in the in the select clause in the SQL Resource definition query. */
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

	/**
	 * Returns fully qualified table name in database-specific form for use in SQL statements. MySQL uses the form
	 * <code>database.table</code>, for example <code>sakila.film</code>. PostgreSQL uses the form
	 * <code>database.schema.table</code>, for example <code>sakila.public.film</code>.
	 */
	public String getQualifiedTableName();

	/** Returns table name. */
	public String getTableName();

	/** Returns true if column is a character string, for example char or varchar. */
	public boolean isCharType();

	/** Returns true if column is a date, time or timestamp. */
	public boolean isDateTimeType();

	/**
	 * Returns true for foreign key columns not declared in the SQL Resource query but added by the framework. These are
	 * required for writes to child extensions, parent extensions and and one-to-many child tables.
	 */
	public boolean isNonqueriedForeignKey();

	/** Returns true if the column is a primary key. */
	public boolean isPrimaryKey();

}