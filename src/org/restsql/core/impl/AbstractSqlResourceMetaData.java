/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;
import org.restsql.core.TableMetaData.TableRole;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Represents meta data for sql resource. Queries database for table and column meta data and primary and foreign keys.
 * 
 * @author Mark Sawers
 */
public abstract class AbstractSqlResourceMetaData implements SqlResourceMetaData {
	private static final int DEFAULT_NUMBER_DATABASES = 5;
	private static final int DEFAULT_NUMBER_TABLES = 10;

	private List<ColumnMetaData> allReadColumns, parentReadColumns, childReadColumns;
	private TableMetaData childTable, parentTable, joinTable;
	private SqlResourceDefinition definition;
	private boolean hierarchical;
	private List<TableMetaData> joinList;
	private boolean multipleDatabases;
	/** Map<database.table, TableMetaData> */
	private Map<String, TableMetaData> tableMap;
	private List<TableMetaData> tables, childPlusExtTables, parentPlusExtTables;

	// Public methods to retrieve metadata

	public List<ColumnMetaData> getAllReadColumns() {
		return allReadColumns;
	}

	public TableMetaData getChild() {
		return childTable;
	}

	public List<TableMetaData> getChildPlusExtTables() {
		return childPlusExtTables;
	}

	public List<ColumnMetaData> getChildReadColumns() {
		return childReadColumns;
	}

	public TableMetaData getJoin() {
		return joinTable;
	}

	public List<TableMetaData> getJoinList() {
		return joinList;
	}

	public int getNumberTables() {
		return tables.size();
	}

	public TableMetaData getParent() {
		return parentTable;
	}

	public List<TableMetaData> getParentPlusExtTables() {
		return parentPlusExtTables;
	}

	public List<ColumnMetaData> getParentReadColumns() {
		return parentReadColumns;
	}

	public Map<String, TableMetaData> getTableMap() {
		return tableMap;
	}

	public List<TableMetaData> getTables() {
		return tables;
	}

	public boolean hasJoinTable() {
		return joinTable != null;
	}

	public boolean hasMultipleDatabases() {
		return multipleDatabases;
	}

	public boolean isHierarchical() {
		return hierarchical;
	}

	/** Populates metadata using definition. */
	public void setDefinition(final SqlResourceDefinition definition) throws SqlResourceException {
		this.definition = definition;
		Connection connection = null;
		String sql = null;
		try {
			connection = Factory.getConnection(definition.getDefaultDatabase());
			final Statement statement = connection.createStatement();
			sql = getSqlMainQuery(definition);
			if (Config.logger.isDebugEnabled()) {
				Config.logger.debug("Loading meta data for " + definition.getName() + " - " + sql);
			}
			final ResultSet resultSet = statement.executeQuery(sql);
			resultSet.next();
			buildTablesAndColumns(resultSet);
			resultSet.close();
			statement.close();
			buildPrimaryKeys(connection);
			buildInvisibleForeignKeys(connection);
			buildJoinTableMetadata(connection);
		} catch (final SQLException exception) {
			throw new SqlResourceException(exception, sql);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final SQLException ignored) {
				}
			}
		}
		hierarchical = definition.getChild() != null;
	}

	// Protected methods for database-specific implementation

	/**
	 * Retrieves database name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	protected String getColumnDatabaseName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return resultSetMetaData.getCatalogName(colNumber);
	}

	/**
	 * Retrieves actual column name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	protected String getColumnName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return resultSetMetaData.getColumnName(colNumber);
	}

	/**
	 * Retrieves table name from result set meta data. Hook method for buildTablesAndColumns() allows database-specific
	 * overrides.
	 */
	protected String getColumnTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return resultSetMetaData.getTableName(colNumber);
	}

	/** Retrieves database-specific table name used in SQL statements. */
	protected abstract String getQualifiedTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException;

	/** Retrieves database-specific table name used in SQL statements. Used to build join table meta data. */
	protected abstract String getQualifiedTableName(Connection connection, String databaseName, String tableName)
			throws SQLException;

	/**
	 * Retrieves sql for querying columns. Hook method for buildInvisibleForeignKeys() and buildJoinTableMetadata()
	 * allows database-specific overrides.
	 */
	protected abstract String getSqlColumnsQuery();

	/**
	 * Retrieves sql for the main query based on the definition. Optimized to retrieve only one row. Hook method for
	 * constructor allows database-specific overrides.
	 */
	protected String getSqlMainQuery(final SqlResourceDefinition definition) {
		return definition.getQuery().getValue() + " LIMIT 1 OFFSET 0";
	}

	// Private utils

	/**
	 * Retrieves sql for querying primary keys. Hook method for buildPrimaryKeys allows database-specific overrides.
	 */
	protected abstract String getSqlPkQuery();

	private void buildInvisibleForeignKeys(final Connection connection) throws SQLException {
		final PreparedStatement statement = connection.prepareStatement(getSqlColumnsQuery());
		ResultSet resultSet = null;
		try {
			for (final TableMetaData table : tables) {
				if (!table.isParent()) {
					statement.setString(1, table.getDatabaseName());
					statement.setString(2, table.getTableName());
					resultSet = statement.executeQuery();
					while (resultSet.next()) {
						final String columnName = resultSet.getString(1);
						if (!table.getColumns().containsKey(columnName)) {
							TableMetaData mainTable;
							switch (table.getTableRole()) {
								case ChildExtension:
									mainTable = childTable;
									break;
								default: // Child, ParentExtension, Unknown
									mainTable = parentTable;
							}
							// Look for a pk on the main table with the same name
							for (final ColumnMetaData pk : mainTable.getPrimaryKeys()) {
								if (columnName.equals(pk.getColumnName())) {
									final ColumnMetaDataImpl fkColumn = new ColumnMetaDataImpl(table
											.getDatabaseName(), table.getQualifiedTableName(), table
											.getTableName(), columnName, resultSet.getString(2));
									((TableMetaDataImpl) table).addColumn(fkColumn);
								}
							}
						}
					}
				}
			}
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

	private void buildJoinTableMetadata(final Connection connection) throws SQLException {
		final String possiblyQualifiedTableName = definition.getJoin();
		if (possiblyQualifiedTableName != null && joinTable == null) {
			// Determine table and database name
			String tableName, databaseName;
			final int dotIndex = possiblyQualifiedTableName.indexOf('.');
			if (dotIndex > 0) {
				tableName = possiblyQualifiedTableName.substring(0, dotIndex);
				databaseName = possiblyQualifiedTableName.substring(dotIndex + 1);
			} else {
				tableName = possiblyQualifiedTableName;
				databaseName = definition.getDefaultDatabase();
			}

			final String qualifiedTableName = getQualifiedTableName(connection, databaseName, tableName);

			// Create table and add to special lists
			joinTable = new TableMetaDataImpl(tableName, qualifiedTableName, databaseName, TableRole.Join);
			tableMap.put(joinTable.getQualifiedTableName(), joinTable);
			tables.add(joinTable);
			joinList = new ArrayList<TableMetaData>(1);
			joinList.add(joinTable);

			// Execute metadata query and populate metadata structure
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			try {
				statement = connection.prepareStatement(getSqlColumnsQuery());
				statement.setString(1, databaseName);
				statement.setString(2, tableName);
				resultSet = statement.executeQuery();
				while (resultSet.next()) {
					final ColumnMetaDataImpl column = new ColumnMetaDataImpl(databaseName,
							qualifiedTableName, tableName, resultSet.getString(1), resultSet.getString(2));
					((TableMetaDataImpl) joinTable).addColumn(column);
				}
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

	/**
	 * Builds list of primary key column labels.
	 * 
	 * @param Connection connection
	 * @throws SqlResourceException if a database access error occurs
	 */
	private void buildPrimaryKeys(final Connection connection) throws SQLException {
		final PreparedStatement statement = connection.prepareStatement(getSqlPkQuery());
		ResultSet resultSet = null;
		try {
			for (final TableMetaData table : tables) {
				statement.setString(1, table.getDatabaseName());
				statement.setString(2, table.getTableName());
				resultSet = statement.executeQuery();
				while (resultSet.next()) {
					final String columnName = resultSet.getString(1);
					for (final ColumnMetaData column : table.getColumns().values()) {
						if (columnName.equals(column.getColumnName())) {
							((ColumnMetaDataImpl) column).setPrimaryKey(true);
							((TableMetaDataImpl) table).addPrimaryKey(column);
						}
					}
				}
			}
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

	/**
	 * Builds table and column meta data.
	 * 
	 * @param resultSet resultSet
	 * @throws SQLException if a database access error occurs
	 */
	private void buildTablesAndColumns(final ResultSet resultSet) throws SQLException {
		final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
		final int columnCount = resultSetMetaData.getColumnCount();

		allReadColumns = new ArrayList<ColumnMetaData>(columnCount);
		parentReadColumns = new ArrayList<ColumnMetaData>(columnCount);
		childReadColumns = new ArrayList<ColumnMetaData>(columnCount);
		tableMap = new HashMap<String, TableMetaData>(DEFAULT_NUMBER_TABLES);
		tables = new ArrayList<TableMetaData>(DEFAULT_NUMBER_TABLES);
		childPlusExtTables = new ArrayList<TableMetaData>(DEFAULT_NUMBER_TABLES);
		parentPlusExtTables = new ArrayList<TableMetaData>(DEFAULT_NUMBER_TABLES);
		final HashSet<String> databases = new HashSet<String>(DEFAULT_NUMBER_DATABASES);

		for (int colNumber = 1; colNumber <= columnCount; colNumber++) {
			final String databaseName = getColumnDatabaseName(definition, resultSetMetaData, colNumber);
			databases.add(databaseName);
			final String qualifiedTableName = getQualifiedTableName(definition, resultSetMetaData, colNumber);
			final String tableName = getColumnTableName(definition, resultSetMetaData, colNumber);
			final ColumnMetaDataImpl column = new ColumnMetaDataImpl(colNumber, databaseName,
					qualifiedTableName, tableName, getColumnName(definition, resultSetMetaData, colNumber),
					resultSetMetaData.getColumnLabel(colNumber), resultSetMetaData
							.getColumnTypeName(colNumber), resultSetMetaData.getColumnType(colNumber));

			TableMetaDataImpl table = (TableMetaDataImpl) tableMap.get(column.getQualifiedTableName());
			if (table == null) {
				// Create table object and add to special references
				final TableRole tableRole = getTableRole(column);
				table = new TableMetaDataImpl(tableName, qualifiedTableName, databaseName, tableRole);
				tableMap.put(column.getQualifiedTableName(), table);
				tables.add(table);

				switch (tableRole) {
					case Parent:
						parentTable = table;
						if (definition.getParentAlias() != null) {
							table.setTableAlias(definition.getParentAlias());
						}
						// fall through
					case ParentExtension:
						parentPlusExtTables.add(table);
						break;
					case Child:
						childTable = table;
						if (definition.getChildAlias() != null) {
							table.setTableAlias(definition.getChildAlias());
						}
						// fall through
					case ChildExtension:
						childPlusExtTables.add(table);
						break;
					case Join: // unlikely to be in the select columns, but just in case
						joinTable = table;
						joinList = new ArrayList<TableMetaData>(1);
						joinList.add(joinTable);
						break;
					default: // Unknown
				}
			}
			table.addColumn(column);

			// Add column to special column lists
			allReadColumns.add(column);
			switch (table.getTableRole()) {
				case Parent:
				case ParentExtension:
					parentReadColumns.add(column);
					break;
				case Child:
				case ChildExtension:
					childReadColumns.add(column);
					break;
			}
		}

		multipleDatabases = databases.size() > 1;
	}

	private TableRole getTableRole(final ColumnMetaData column) {
		if (queryTableMatches(column, definition.getParent())) {
			return TableRole.Parent;
		} else if (queryTableMatches(column, definition.getChild())) {
			return TableRole.Child;
		} else if (queryTableMatches(column, definition.getJoin())) {
			return TableRole.Join;
		} else if (queryTableMatches(column, definition.getParentExt())) {
			return TableRole.ParentExtension;
		} else if (queryTableMatches(column, definition.getChildExt())) {
			return TableRole.ChildExtension;
		} else {
			return TableRole.Unknown;
		}
	}

	private boolean queryTableMatches(final ColumnMetaData column, final String queryTable) {
		if (queryTable != null) {
			if (queryTable.indexOf(".") > 0) {
				return column.getQualifiedTableName().equals(queryTable);
			} else {
				return column.getTableName().equals(queryTable);
			}
		} else {
			return false;
		}
	}
}
