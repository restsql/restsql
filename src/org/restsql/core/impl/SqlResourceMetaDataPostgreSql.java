/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.postgresql.PGResultSetMetaData;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.SqlResourceDefinitionUtils;

/**
 * Implements SqlResourceMetaData for PostgreSQL.
 * 
 * @author Mark Sawers
 */
public class SqlResourceMetaDataPostgreSql extends AbstractSqlResourceMetaData {
	private static final String SQL_COLUMNS_QUERY = "select column_name, data_type from information_schema.columns where table_catalog = ? and table_name = ?";
	private static final String SQL_PK_QUERY = "select column_name from information_schema.table_constraints tc, information_schema.key_column_usage kcu"
			+ " where tc.constraint_catalog = ? and tc.table_name = ?"
			+ " and tc.constraint_type = 'PRIMARY KEY'"
			+ " and tc.constraint_schema = kcu.constraint_schema and tc.table_name = kcu.table_name"
			+ " and tc.constraint_name = kcu.constraint_name";
	private static final String SQL_TABLE_SCHEMA_QUERY = "select table_schema from information_schema.tables where table_catalog = ? and table_name = ?";

	/**
	 * Retrieves database name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	@Override
	protected String getColumnDatabaseName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return SqlResourceDefinitionUtils.getDefaultDatabase(definition);
	}

	/**
	 * Retrieves actual column name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	@Override
	protected String getColumnName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return ((PGResultSetMetaData) resultSetMetaData).getBaseColumnName(colNumber);
	}

	/**
	 * Retrieves table name from result set meta data. Hook method for buildTablesAndColumns() allows database-specific
	 * overrides.
	 */
	@Override
	protected String getColumnTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return ((PGResultSetMetaData) resultSetMetaData).getBaseTableName(colNumber);
	}

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
		final PGResultSetMetaData pgMetaData = (PGResultSetMetaData) resultSetMetaData;
		return SqlResourceDefinitionUtils.getDefaultDatabase(definition) + "."
				+ pgMetaData.getBaseSchemaName(colNumber) + "." + pgMetaData.getBaseTableName(colNumber);
	}

	/** Retrieves database-specific table name used in SQL statements. Used to build join table meta data. */
	@Override
	protected String getQualifiedTableName(Connection connection, String databaseName, String tableName)
			throws SQLException {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			statement = connection.prepareStatement(SQL_TABLE_SCHEMA_QUERY);
			String schemaName = "unknown";
			statement.setString(1, databaseName);
			statement.setString(2, tableName);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				schemaName = resultSet.getString(1);
			}
			return databaseName + "." + schemaName + "." + tableName;
		} catch (final SQLException exception) {
			if (resultSet != null) {
				resultSet.close();
			}
			if (statement != null) {
				statement.close();
			}
			throw exception;
		}
	}
}
