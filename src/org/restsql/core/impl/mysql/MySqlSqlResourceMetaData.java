/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.restsql.core.impl.AbstractSqlResourceMetaData;
import org.restsql.core.impl.ColumnMetaDataImpl;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Implements SqlResourceMetaData for MySQL.
 * 
 * @author Mark Sawers
 */
public class MySqlSqlResourceMetaData extends AbstractSqlResourceMetaData {
	private static final String SQL_COLUMNS_QUERY = "select column_name, data_type, extra from information_schema.columns where table_schema = ? and table_name = ?";
	private static final String SQL_PK_QUERY = "select column_name from information_schema.table_constraints tc, information_schema.key_column_usage kcu"
			+ " where tc.constraint_schema= ? and tc.table_name = ?"
			+ " and tc.constraint_type = 'PRIMARY KEY'"
			+ " and tc.constraint_schema = kcu.constraint_schema and tc.table_name = kcu.table_name"
			+ " and tc.constraint_name = kcu.constraint_name";

	/**
	 * Retrieves sql for querying columns. Hook method for buildInvisibleForeignKeys() and buildJoinTableMetadata()
	 * allows database-specific overrides.
	 */
	@Override
	protected String getSqlColumnsQuery() {
		return SQL_COLUMNS_QUERY;
	}

	/**
	 * Retrieves sql for querying primary keys. Hook method for buildPrimaryKeys allows database-specific overrides.
	 */
	@Override
	protected String getSqlPkQuery() {
		return SQL_PK_QUERY;
	}

	/** Retrieves database-specific table name used in SQL statements. */
	@Override
	protected String getQualifiedTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return resultSetMetaData.getCatalogName(colNumber) + "." + resultSetMetaData.getTableName(colNumber);
	}

	/** Retrieves database-specific table name used in SQL statements. Used to build join table meta data. */
	@Override
	protected String getQualifiedTableName(Connection connection, String databaseName, String tableName)
			throws SQLException {
		return databaseName + "." + tableName;
	}

	/**
	 * Sets sequence metadata for a column with the columns query result set. The extra column value will contain the
	 * string auto_increment if this is a sequence.
	 * 
	 * @throws SQLException when a database error occurs
	 */
	@Override
	protected void setSequenceMetaData(ColumnMetaDataImpl column, ResultSet resultSet) throws SQLException {
		final String extra = resultSet.getString(3);
		if (extra != null && extra.equals("auto_increment")) {
			column.setSequence(true);
			column.setSequenceName(column.getTableName());
		}
	}
}
