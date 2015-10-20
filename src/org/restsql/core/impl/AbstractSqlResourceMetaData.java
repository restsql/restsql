/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.io.StringWriter;
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.Request;
import org.restsql.core.Request.Type;
import org.restsql.core.SqlBuilder;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;
import org.restsql.core.TableMetaData.TableRole;
import org.restsql.core.sqlresource.Documentation;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.SqlResourceDefinitionUtils;
import org.restsql.core.sqlresource.Table;

/**
 * Represents meta data for sql resource. Queries database for table and column meta data and primary and foreign keys.
 * 
 * @author Mark Sawers
 */
@XmlRootElement(name = "sqlResourceMetaData", namespace = "http://restsql.org/schema")
@XmlType(name = "SqlResourceMetaData", namespace = "http://restsql.org/schema", propOrder = { "resName",
		"hierarchical", "multipleDatabases", "tables", "parentTableName", "childTableName", "joinTableName",
		"parentPlusExtTableNames", "childPlusExtTableNames", "joinTableNames", "allReadColumnNames",
		"parentReadColumnNames", "childReadColumnNames", "documentation" })
public abstract class AbstractSqlResourceMetaData implements SqlResourceMetaData {
	private static final int DEFAULT_NUMBER_DATABASES = 5;
	private static final int DEFAULT_NUMBER_TABLES = 10;

	@XmlElementWrapper(name = "allReadColumns", required = true)
	@XmlElement(name = "column", required = true)
	private List<String> allReadColumnNames;

	@XmlTransient
	private List<ColumnMetaData> allReadColumns;

	@XmlElementWrapper(name = "childPlusExtTables", required = true)
	@XmlElement(name = "table")
	private List<String> childPlusExtTableNames;

	@XmlTransient
	private List<TableMetaData> childPlusExtTables;

	@XmlElementWrapper(name = "childReadColumns", required = true)
	@XmlElement(name = "column")
	private List<String> childReadColumnNames;

	@XmlTransient
	private List<ColumnMetaData> childReadColumns;

	@XmlTransient
	private TableMetaData childTable;

	@XmlElement(name = "childTable")
	private String childTableName;

	@XmlTransient
	private SqlResourceDefinition definition;

	@XmlElement(name = "documentation", required = true)
	private Documentation documentation;

	@XmlTransient
	private boolean extendedMetadataIsBuilt;

	@XmlAttribute
	private boolean hierarchical;

	@XmlTransient
	private List<TableMetaData> joinList;

	@XmlTransient
	private TableMetaData joinTable;

	@XmlElement(name = "joinTable")
	private String joinTableName;

	@XmlElementWrapper(name = "joinTables")
	@XmlElement(name = "table")
	private List<String> joinTableNames;

	@XmlAttribute
	private boolean multipleDatabases;

	@XmlElementWrapper(name = "parentPlusExtTables", required = true)
	@XmlElement(name = "table", required = true)
	private List<String> parentPlusExtTableNames;

	@XmlTransient
	private List<TableMetaData> parentPlusExtTables;

	@XmlElementWrapper(name = "parentReadColumns", required = true)
	@XmlElement(name = "column", required = true)
	private List<String> parentReadColumnNames;

	@XmlTransient
	private List<ColumnMetaData> parentReadColumns;

	@XmlTransient
	private TableMetaData parentTable;

	@XmlElement(name = "parentTable", required = true)
	private String parentTableName;

	@XmlAttribute(required = true)
	private String resName;

	/** Map<database.table, TableMetaData> */
	@XmlTransient
	private Map<String, TableMetaData> tableMap;

	@XmlElementWrapper(name = "tables", required = true)
	@XmlElement(name = "table", type = TableMetaDataImpl.class, required = true)
	private List<TableMetaData> tables;

	// Public methods to retrieve metadata

	@Override
	public List<ColumnMetaData> getAllReadColumns() {
		return allReadColumns;
	}

	@Override
	public TableMetaData getChild() {
		return childTable;
	}

	@Override
	public List<TableMetaData> getChildPlusExtTables() {
		return childPlusExtTables;
	}

	@Override
	public List<ColumnMetaData> getChildReadColumns() {
		return childReadColumns;
	}

	@Override
	public TableMetaData getJoin() {
		return joinTable;
	}

	@Override
	public List<TableMetaData> getJoinList() {
		return joinList;
	}

	@Override
	public int getNumberTables() {
		return tables.size();
	}

	@Override
	public TableMetaData getParent() {
		return parentTable;
	}

	@Override
	public List<TableMetaData> getParentPlusExtTables() {
		return parentPlusExtTables;
	}

	@Override
	public List<ColumnMetaData> getParentReadColumns() {
		return parentReadColumns;
	}

	@Override
	public Map<String, TableMetaData> getTableMap() {
		return tableMap;
	}

	@Override
	public List<TableMetaData> getTables() {
		return tables;
	}

	/**
	 * Determines the tables to use for write, possibly substituting the parent+, child+ or join table for query tables.
	 */
	@Override
	public List<TableMetaData> getWriteTables(final Type requestType, final boolean doParent) {
		List<TableMetaData> tables;
		if (isHierarchical()) {
			if (!doParent) { // child write
				if (hasJoinTable() && requestType != Type.UPDATE) {
					// Substitute join table for child if many to many hierarchical
					tables = getJoinList();
				} else {
					tables = getChildPlusExtTables();
				}
			} else { // parent write
				tables = getParentPlusExtTables();
			}
		} else {
			// Use all query tables
			tables = getTables();
		}
		return tables;
	}

	@Override
	public boolean hasJoinTable() {
		return joinTable != null;
	}

	@Override
	public boolean hasMultipleDatabases() {
		return multipleDatabases;
	}

	/** Populates metadata using definition. */
	@Override
	public void init(final String resName, final SqlResourceDefinition definition, final SqlBuilder sqlBuilder)
			throws SqlResourceException {
		this.resName = resName;
		this.definition = definition;
		Connection connection = null;
		String sql = null;
		SqlResourceDefinitionUtils.validate(definition);
		try {
			connection = Factory.getConnection(SqlResourceDefinitionUtils.getDefaultDatabase(definition));
			final Statement statement = connection.createStatement();
			sql = getSqlMainQuery(definition, sqlBuilder);
			if (Config.logger.isDebugEnabled()) {
				Config.logger.debug("Loading meta data for " + this.resName + " - " + sql);
			}
			final ResultSet resultSet = statement.executeQuery(sql);
			resultSet.next();
			buildTablesAndColumns(resultSet, connection);
			resultSet.close();
			statement.close();
			buildPrimaryKeys(connection);
			buildInvisibleForeignKeys(connection);
			buildJoinTableMetadata(connection);
			buildSequenceMetaData(connection);
			documentation = definition.getDocumentation();
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
		hierarchical = getChild() != null;
	}

	@Override
	public boolean isHierarchical() {
		return hierarchical;
	}

	/** Returns HTML representation. */
	@Override
	public String toHtml() {
		buildExtendedMetadata();
		try {
			final JAXBContext context = JAXBContext.newInstance(AbstractSqlResourceMetaData.class);
			final Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter writer = new StringWriter();
			writer.append("<?xml version=\"1.0\"?>");
			writer.append("<?xml-stylesheet type=\"text/xsl\" href=\"../../tools/Documentation.xsl\" ?>");
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
			marshaller.marshal(this, writer);
			return writer.toString();
		} catch (final JAXBException exception) {
			return exception.toString();
		}
	}

	/** Returns XML representation. */
	@Override
	public String toXml() {
		buildExtendedMetadata();
		try {
			final JAXBContext context = JAXBContext.newInstance(AbstractSqlResourceMetaData.class);
			final Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final StringWriter writer = new StringWriter();
			marshaller.marshal(this, writer);
			return writer.toString();
		} catch (final JAXBException exception) {
			return exception.toString();
		}
	}

	/**
	 * Retrieves database name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	protected String getColumnDatabaseName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return resultSetMetaData.getCatalogName(colNumber);
	}

	// Protected methods for database-specific implementation

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

	/** Retrieves label disambiguated for duplication. Used in a result set value retrieval. */
	protected abstract String getQualifiedColumnLabel(final String tableName,
			final String qualifiedTableName, final boolean readOnly, final String label);

	/**
	 * Returns fully qualified column name in database-specific form for use in SQL statements. MySQL uses the form
	 * <code>database.table.column</code>, for example <code>sakila.film.film_id</code>. PostgreSQL uses the form
	 * <code>database.schema.table.column</code>, for example <code>sakila.public.film.film_id</code>.
	 * 
	 * @param tableName table name
	 * @param qualifiedTableName qualified table name
	 * @param readOnly true if column is a function
	 * @param name base column name
	 * @return qualified name
	 */
	protected String getQualifiedColumnName(final String tableName, final String qualifiedTableName,
			final boolean readOnly, final String name) {
		final StringBuilder qualifiedName = new StringBuilder(100);
		if (hasMultipleDatabases()) {
			qualifiedName.append(qualifiedTableName);
		} else {
			qualifiedName.append(tableName);
		}
		qualifiedName.append('.');
		qualifiedName.append(name);
		return qualifiedName.toString();
	}

	/** Retrieves database-specific table name used in SQL statements. Used to build join table meta data. */
	protected abstract String getQualifiedTableName(Connection connection, String databaseName,
			String tableName) throws SQLException;

	/** Retrieves database-specific table name used in SQL statements. */
	protected abstract String getQualifiedTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException;

	/**
	 * Retrieves sql for querying columns. Hook method for buildInvisibleForeignKeys() and buildJoinTableMetadata()
	 * allows database-specific overrides.
	 */
	protected abstract String getSqlColumnsQuery();

	/**
	 * Retrieves sql for the main query based on the definition. Optimized to retrieve only one row using limit/offset.
	 * Hook method for constructor allows database-specific overrides, however the usual route for customization is
	 * through SqlBuilder.buildSelectSql().
	 * 
	 * @throws InvalidRequestException if main query is invalid
	 */
	protected String getSqlMainQuery(final SqlResourceDefinition definition, final SqlBuilder sqlBuilder)
			throws InvalidRequestException {
		final Request request = Factory.getRequest(Type.SELECT, resName, null, null, null, null);
		request.setSelectLimit(new Integer(1));
		request.setSelectOffset(new Integer(0));
		return sqlBuilder.buildSelectSql(this, definition.getQuery().getValue(), request).getStatement();
	}

	/**
	 * Retrieves sql for querying primary keys. Hook method for buildPrimaryKeys allows database-specific overrides.
	 */
	protected abstract String getSqlPkQuery();

	/**
	 * Return whether a column in the given result set is read-only. The default implementation just calls isReadOnly()
	 * on the result set, database specific implementations can override this behavior. Contributed by <a
	 * href="https://github.com/rhuitl">rhuitl</a>.
	 * 
	 * @param resultSetMetaData Result set metadata
	 * @param colNumber Column number (1..N)
	 * @throws SQLException if a database access error occurs
	 */
	protected boolean isColumnReadOnly(final ResultSetMetaData resultSetMetaData, final int colNumber)
			throws SQLException {
		return resultSetMetaData.isReadOnly(colNumber);
	}

	/**
	 * Returns true if db metadata, e.g. database/owner, table and column names are stored as upper case, and therefore
	 * lookups should be forced to upper. Database specific implementation can override this response.
	 */
	protected boolean isDbMetaDataUpperCase() {
		return false;
	}

	/**
	 * Sets sequence metadata for a column with the columns query result set.
	 * 
	 * @throws SQLException when a database error occurs
	 */
	protected abstract void setSequenceMetaData(ColumnMetaDataImpl column, ResultSet resultSet)
			throws SQLException;

	/** Build extended metadata for serialization if first time through. */
	private void buildExtendedMetadata() {
		if (!extendedMetadataIsBuilt) {
			parentTableName = getQualifiedTableName(parentTable);
			childTableName = getQualifiedTableName(childTable);
			joinTableName = getQualifiedTableName(joinTable);
			parentPlusExtTableNames = getQualifiedTableNames(parentPlusExtTables);
			childPlusExtTableNames = getQualifiedTableNames(childPlusExtTables);
			allReadColumnNames = getQualifiedColumnNames(allReadColumns);
			childReadColumnNames = getQualifiedColumnNames(childReadColumns);
			parentReadColumnNames = getQualifiedColumnNames(parentReadColumns);
			extendedMetadataIsBuilt = true;
		}
	}

	// Private methods

	private void buildInvisibleForeignKeys(final Connection connection) throws SQLException {
		final PreparedStatement statement = connection.prepareStatement(getSqlColumnsQuery());
		ResultSet resultSet = null;
		try {
			for (final TableMetaData table : tables) {
				if (!table.isParent()) {
					statement.setString(1, isDbMetaDataUpperCase() ? table.getDatabaseName().toUpperCase()
							: table.getDatabaseName());
					statement.setString(2, isDbMetaDataUpperCase() ? table.getTableName().toUpperCase()
							: table.getTableName());
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
									final ColumnMetaData fkColumn = Factory.getColumnMetaData();
									fkColumn.setAttributes(
											table.getDatabaseName(),
											table.getQualifiedTableName(),
											table.getTableName(),
											table.getTableRole(),
											columnName,
											getQualifiedColumnName(table.getTableName(),
													table.getQualifiedTableName(), false, columnName),
													pk.getColumnLabel(),
													getQualifiedColumnLabel(table.getTableName(),
															table.getQualifiedTableName(), false, pk.getColumnLabel()),
															resultSet.getString(2));
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
		// Join table could have been identified in buildTablesAndColumns(), but not always
		final Table joinDef = SqlResourceDefinitionUtils.getTable(definition, TableRole.Join);
		if (joinDef != null && joinTable == null) {
			// Determine table and database name
			String tableName, databaseName;
			final String possiblyQualifiedTableName = joinDef.getName();
			final int dotIndex = possiblyQualifiedTableName.indexOf('.');
			if (dotIndex > 0) {
				tableName = possiblyQualifiedTableName.substring(0, dotIndex);
				databaseName = possiblyQualifiedTableName.substring(dotIndex + 1);
			} else {
				tableName = possiblyQualifiedTableName;
				databaseName = SqlResourceDefinitionUtils.getDefaultDatabase(definition);
			}

			final String qualifiedTableName = getQualifiedTableName(connection, databaseName, tableName);

			// Create table and add to special lists
			joinTable = Factory.getTableMetaData();
			joinTable.setAttributes(tableName, qualifiedTableName, databaseName, TableRole.Join);
			tableMap.put(joinTable.getQualifiedTableName(), joinTable);
			tables.add(joinTable);
			joinList = new ArrayList<TableMetaData>(1);
			joinList.add(joinTable);

			// Execute metadata query and populate metadata structure
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			try {
				statement = connection.prepareStatement(getSqlColumnsQuery());
				statement.setString(1, isDbMetaDataUpperCase() ? databaseName.toUpperCase() : databaseName);
				statement.setString(2, isDbMetaDataUpperCase() ? tableName.toUpperCase() : tableName);
				resultSet = statement.executeQuery();
				while (resultSet.next()) {
					final String columnName = resultSet.getString(1);
					final ColumnMetaData column = Factory.getColumnMetaData();
					column.setAttributes(databaseName, qualifiedTableName, tableName, TableRole.Join,
							columnName,
							getQualifiedColumnName(tableName, qualifiedTableName, false, columnName),
							columnName,
							getQualifiedColumnLabel(tableName, qualifiedTableName, false, columnName),
							resultSet.getString(2));
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
				statement.setString(1, isDbMetaDataUpperCase() ? table.getDatabaseName().toUpperCase()
						: table.getDatabaseName());
				statement.setString(2,
						isDbMetaDataUpperCase() ? table.getTableName().toUpperCase() : table.getTableName());
				resultSet = statement.executeQuery();
				while (resultSet.next()) {
					final String columnName = resultSet.getString(1);
					for (final ColumnMetaData column : table.getColumns().values()) {
						if (columnName.equalsIgnoreCase(column.getColumnName())) { // ignore case accommodates a db like
							// Oracle
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
	 * Builds sequence metadata for all columns.
	 * 
	 * @param connection database connection
	 * @throws SQLException if a database access error occurs
	 */
	private void buildSequenceMetaData(final Connection connection) throws SQLException {

		final PreparedStatement statement = connection.prepareStatement(getSqlColumnsQuery());
		ResultSet resultSet = null;
		try {
			for (final TableMetaData table : tables) {
				statement.setString(1, isDbMetaDataUpperCase() ? table.getDatabaseName().toUpperCase()
						: table.getDatabaseName());
				statement.setString(2,
						isDbMetaDataUpperCase() ? table.getTableName().toUpperCase() : table.getTableName());
				resultSet = statement.executeQuery();
				while (resultSet.next()) {
					final String columnName = resultSet.getString(1);
					for (final ColumnMetaData column : table.getColumns().values()) {
						if (column.getColumnName().equalsIgnoreCase(columnName)) { // ignore case accommodates a db like
							// Oracle
							setSequenceMetaData((ColumnMetaDataImpl) column, resultSet);
							break;
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
	 * @param connection database connection - used to get qualified name for read-only columns
	 * @throws SQLException if a database access error occurs
	 * @throws SqlResourceException if definition is invalid
	 */
	@SuppressWarnings("fallthrough")
	private void buildTablesAndColumns(final ResultSet resultSet, final Connection connection)
			throws SQLException, SqlResourceException {
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
			final String databaseName, qualifiedTableName, tableName;
			final boolean readOnly = isColumnReadOnly(resultSetMetaData, colNumber);
			if (readOnly) {
				databaseName = SqlResourceDefinitionUtils.getDefaultDatabase(definition);
				tableName = SqlResourceDefinitionUtils.getTable(definition, TableRole.Parent).getName();
				qualifiedTableName = getQualifiedTableName(connection, databaseName, tableName);
			} else {
				databaseName = getColumnDatabaseName(definition, resultSetMetaData, colNumber);
				databases.add(databaseName);
				tableName = getColumnTableName(definition, resultSetMetaData, colNumber);
				qualifiedTableName = getQualifiedTableName(definition, resultSetMetaData, colNumber);
			}

			final String columnName = getColumnName(definition, resultSetMetaData, colNumber);
			final String columnLabel = resultSetMetaData.getColumnLabel(colNumber);
			final ColumnMetaData column = Factory.getColumnMetaData();
			column.setAttributes(colNumber, databaseName, qualifiedTableName, tableName, columnName,
					getQualifiedColumnName(tableName, qualifiedTableName, readOnly, columnName), columnLabel,
					getQualifiedColumnLabel(tableName, qualifiedTableName, readOnly, columnLabel),
					resultSetMetaData.getColumnTypeName(colNumber),
					resultSetMetaData.getColumnType(colNumber), readOnly);

			TableMetaData table = tableMap.get(column.getQualifiedTableName());
			if (table == null) {
				// Create table metadata object and add to special references
				final Table tableDef = SqlResourceDefinitionUtils.getTable(definition, column);
				if (tableDef == null) {
					throw new SqlResourceException("Definition requires table element for "
							+ column.getTableName() + ", referenced by column " + column.getColumnLabel());
				}
				table = Factory.getTableMetaData();
				table.setAttributes(tableName, qualifiedTableName, databaseName,
						TableRole.valueOf(tableDef.getRole()));
				tableMap.put(column.getQualifiedTableName(), table);
				tables.add(table);
				table.setAliases(tableDef.getAlias(), tableDef.getRowAlias(), tableDef.getRowSetAlias());

				switch (table.getTableRole()) {
					case Parent:
						parentTable = table;
						// fall through
					case ParentExtension:
						parentPlusExtTables.add(table);
						break;
					case Child:
						childTable = table;
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

			// Add column to the table
			table.addColumn(column);
			column.setTableRole(table.getTableRole());

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
				default: // Unknown
			}
		}

		// Determine number of databases
		multipleDatabases = databases.size() > 1;
	}

	private List<String> getQualifiedColumnNames(final List<ColumnMetaData> columns) {
		if (columns != null) {
			final List<String> names = new ArrayList<String>(columns.size());
			for (final ColumnMetaData column : columns) {
				names.add(column.getQualifiedColumnName());
			}
			return names;
		} else {
			return null;
		}
	}

	private String getQualifiedTableName(final TableMetaData table) {
		if (table != null) {
			return table.getQualifiedTableName();
		} else {
			return null;
		}
	}

	private List<String> getQualifiedTableNames(final List<TableMetaData> tables) {
		if (tables != null) {
			final List<String> names = new ArrayList<String>(tables.size());
			for (final TableMetaData table : tables) {
				names.add(table.getQualifiedTableName());
			}
			return names;
		} else {
			return null;
		}
	}
}