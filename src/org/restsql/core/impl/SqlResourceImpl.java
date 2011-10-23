/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.SqlBuilder;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;
import org.restsql.core.Trigger;
import org.restsql.core.Request.Type;
import org.restsql.core.SqlBuilder.SqlStruct;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Represents a SQL Resource, an queryable and updatable database "view". Loads metadata on creation and caches it.
 * 
 * @author Mark Sawers
 */
public class SqlResourceImpl implements SqlResource {
	private final SqlResourceDefinition definition;
	private final SqlResourceMetaData metaData;
	private final SqlBuilder sqlBuilder;
	private final List<Trigger> triggers;

	public SqlResourceImpl(final SqlResourceDefinition definition, final SqlResourceMetaData metaData,
			final SqlBuilder sqlBuilder, final List<Trigger> triggers) throws SqlResourceException {
		this.definition = definition;
		cleanSql();
		this.metaData = metaData;
		this.sqlBuilder = sqlBuilder;
		this.triggers = triggers;
	}

	@Override
	public TableMetaData getChildTable() {
		return metaData.getChild();
	}

	public SqlResourceDefinition getDefinition() {
		return definition;
	}

	public TableMetaData getJoinTable() {
		return metaData.getJoin();
	}

	public String getName() {
		return definition.getName();
	}

	@Override
	public TableMetaData getParentTable() {
		return metaData.getParent();
	}

	public Map<String, TableMetaData> getTables() {
		return metaData.getTableMap();
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public boolean isHierarchical() {
		return metaData.isHierarchical();
	}

	/**
	 * Executes query.
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if a database access error occurs
	 * @throws TriggerException if a trigger error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	public List<Map<String, Object>> readAsCollection(final Request request) throws SqlResourceException {
		TriggerManager.executeTriggers(getName(), request, true);

		final List<Map<String, Object>> results;
		Connection connection = null;
		String sql = null;
		try {
			connection = Factory.getConnection(definition.getDefaultDatabase());
			final Statement statement = connection.createStatement();
			sql = sqlBuilder.buildSelectSql(metaData, definition.getQuery().getValue(), request
					.getResourceIdentifiers(), request.getParameters());
			Config.logger.debug(sql);
			request.getLogger().addSql(sql);
			final ResultSet resultSet = statement.executeQuery(sql);
			if (metaData.isHierarchical()) {
				results = buildReadResultsHierachicalCollection(resultSet);
			} else {
				results = buildReadResultsFlatCollection(resultSet);
			}
			resultSet.close();
			statement.close();
		} catch (final SQLException exception) {
			throw new SqlResourceException(exception, sql);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException ignored) {
				}
			}
		}

		TriggerManager.executeTriggers(getName(), request, false);
		return results;
	}

	/**
	 * Executes query.
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if the request is invalid or a database access error or trigger exception occurs
	 * @return xml string
	 */
	public String readAsXml(final Request request) throws SqlResourceException {
		TriggerManager.executeTriggers(getName(), request, true);

		String results;
		Connection connection = null;
		String sql = null;
		try {
			connection = Factory.getConnection(definition.getDefaultDatabase());
			final Statement statement = connection.createStatement();
			sql = sqlBuilder.buildSelectSql(metaData, definition.getQuery().getValue(), request
					.getResourceIdentifiers(), request.getParameters());
			Config.logger.debug(sql);
			request.getLogger().addSql(sql);
			final ResultSet resultSet = statement.executeQuery(sql);
			if (metaData.isHierarchical()) {
				results = buildReadResultsHierachicalXml(resultSet);
			} else {
				results = buildReadResultsFlatXml(resultSet);
			}
			resultSet.close();
			statement.close();
		} catch (final SQLException exception) {
			throw new SqlResourceException(exception, sql);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException ignored) {
				}
			}
		}

		TriggerManager.executeTriggers(getName(), request, false);
		return results;
	}

	/**
	 * Executes database write (insert, update or delete).
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if the request is invalid or a database access error or trigger exception occurs
	 * @return rows affected
	 */
	public int write(final Request request) throws SqlResourceException {
		TriggerManager.executeTriggers(getName(), request, true);

		int rowsAffected = 0;
		boolean doParent = true;
		Connection connection = null;

		try {
			connection = Factory.getConnection(definition.getDefaultDatabase());
			if (metaData.isHierarchical()) {
				final Request childRequest = Factory.getRequestForChild(request.getType(), getName(), request
						.getResourceIdentifiers(), request.getLogger());
				if (request.getChildrenParameters() != null) {
					// Delete, update or insert specified children
					for (final List<NameValuePair> childRowParams : request.getChildrenParameters()) {
						if (request.getType() == Type.INSERT) {
							// Need to add the parent pks, since inserts ignore the resIds
							childRowParams.addAll(request.getResourceIdentifiers());
						} // else deletes and updates use resIds
						childRequest.setParameters(childRowParams);
						rowsAffected += write(connection, childRequest, false);
					}
					// Don't touch the parent(s)
					doParent = false;
				} else if (request.getType() == Request.Type.DELETE) {
					// Delete all children and the parent(s)
					if (request.getResourceIdentifiers() == null) {
						childRequest.setParameters(request.getParameters());
					}
					rowsAffected += write(connection, childRequest, false);
				} // else just insert or update the parent(s)
			}

			if (doParent) {
				rowsAffected += write(connection, request, true);
			}

			TriggerManager.executeTriggers(getName(), request, false);
		} catch (SQLException exception) {
			throw new SqlResourceException(exception);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException ignored) {
				}
			}
		}
		return rowsAffected;
	}

	// Private utils

	/**
	 * For testability.
	 * 
	 * @return meta data
	 */
	SqlResourceMetaData getSqlResourceMetaData() {
		return metaData;
	}

	private List<Map<String, Object>> buildReadResultsFlatCollection(final ResultSet resultSet)
			throws SQLException {
		final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		while (resultSet.next()) {
			final Map<String, Object> row = new HashMap<String, Object>(metaData.getAllReadColumns().size());
			for (final ColumnMetaData column : metaData.getAllReadColumns()) {
				// Simple name, value pairs will do
				if (!column.isNonqueriedForeignKey()) {
					row.put(column.getColumnLabel(), getObjectByColumnNumber(column, resultSet));
				}
			}
			results.add(row);
		}
		return results;
	}

	// @todo figure out string sizing
	private String buildReadResultsFlatXml(final ResultSet resultSet) throws SQLException {
		final StringBuffer string = new StringBuffer(1000);
		Serializer.appendReadDocStart(string);
		while (resultSet.next()) {
			string.append("\n\t<");
			string.append(definition.getParent());
			for (final ColumnMetaData column : metaData.getAllReadColumns()) {
				// Simple name, value pairs will do
				if (!column.isNonqueriedForeignKey()) {
					Serializer.appendNameValuePair(string, column.getColumnLabel(), getObjectByColumnNumber(
							column, resultSet));
				}
			}
			string.append(" />");
		}
		Serializer.appendReadDocEnd(string);
		return string.toString();
	}

	private List<Map<String, Object>> buildReadResultsHierachicalCollection(final ResultSet resultSet)
			throws SQLException {
		final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		final List<Object> currentParentPkValues = new ArrayList<Object>(metaData.getParent()
				.getPrimaryKeys().size());
		boolean newParent = false;
		final int numberParentElementColumns = metaData.getParentReadColumns().size();
		final int numberChildElementColumns = metaData.getChildReadColumns().size();
		final String childRowElementName = metaData.getChild().getTableAlias() + "s";
		Map<String, Object> parentRow = null;
		List<Map<String, Object>> childRows = null;

		while (resultSet.next()) {
			// Assess state of parent
			if (currentParentPkValues.isEmpty()) {
				// First row
				newParent = true;
			} else {
				// Not the first row, check if parent differs from the last
				newParent = false;
				for (int i = 0; i < currentParentPkValues.size(); i++) {
					final ColumnMetaData column = metaData.getParent().getPrimaryKeys().get(i);
					if (!currentParentPkValues.get(i).equals(getObjectByColumnLabel(column, resultSet))) {
						newParent = true;
						break;
					}
				}
			}

			// Set current parent row pk values as well as in the parent row object
			if (newParent) {
				childRows = new ArrayList<Map<String, Object>>();
				parentRow = new HashMap<String, Object>(numberParentElementColumns);
				parentRow.put(childRowElementName, childRows);
				results.add(parentRow);
				currentParentPkValues.clear();

				for (final ColumnMetaData column : metaData.getParentReadColumns()) {
					final Object value = getObjectByColumnLabel(column, resultSet);
					if (column.isPrimaryKey()) {
						currentParentPkValues.add(value);
					}
					parentRow.put(column.getColumnLabel(), value);
				}
			}

			// Populate the child row object
			Map<String, Object> childRow = new HashMap<String, Object>(numberChildElementColumns);
			boolean nullPk = false;
			for (final ColumnMetaData column : metaData.getChildReadColumns()) {
				final Object value = getObjectByColumnLabel(column, resultSet);
				if (column.isPrimaryKey()) {
					nullPk = value == null;
				}
				childRow.put(column.getColumnLabel(), value);
			}
			if (nullPk) {
				childRow = null;
			} else {
				childRows.add(childRow);
			}

		}
		return results;
	}

	private String buildReadResultsHierachicalXml(final ResultSet resultSet) throws SQLException {
		final List<Map<String, Object>> results = buildReadResultsHierachicalCollection(resultSet);
		return Serializer.serializeRead(this, results);
	}

	/**
	 * Strips whitespace from sql in the definition.
	 */
	private void cleanSql() {
		String sql = definition.getQuery().getValue().replaceAll("\\n", "");
		sql = sql.replaceAll("\\r", "");
		sql = sql.replaceFirst("\\s+", "");
		sql = sql.replaceFirst("\\t+", " ");
		sql = sql.replaceFirst("\\t+$", "");
		sql = sql.replaceAll("\\t", " ");
		definition.getQuery().setValue(sql);
	}

	private Object getObjectByColumnLabel(final ColumnMetaData column, final ResultSet resultSet)
			throws SQLException {
		if (column.getColumnType() == Types.DATE && column.getColumnTypeName().equals("YEAR")) {
			return new Integer(resultSet.getInt(column.getColumnLabel()));
		} else {
			return resultSet.getObject(column.getColumnLabel());
		}
	}

	private Object getObjectByColumnNumber(final ColumnMetaData column, final ResultSet resultSet)
			throws SQLException {
		if (column.getColumnType() == Types.DATE && column.getColumnTypeName().equals("YEAR")) {
			return new Integer(resultSet.getInt(column.getColumnNumber()));
		} else {
			return resultSet.getObject(column.getColumnNumber());
		}
	}

	private int write(final Connection connection, final Request request, final boolean doParent)
			throws SqlResourceException {
		int rowsAffected = 0;
		final Map<String, SqlBuilder.SqlStruct> sqls = sqlBuilder.buildWriteSql(metaData, request, doParent);

		// Remove sql for main table
		final String mainTableName = doParent ? metaData.getParent().getQualifiedTableName() : metaData
				.getChild().getQualifiedTableName();
		final SqlBuilder.SqlStruct mainTableSqlStruct = sqls.remove(mainTableName);

		// Do the main table if insert
		if (request.getType() == Type.INSERT) {
			rowsAffected += write(connection, request, mainTableSqlStruct, true);
		}

		// Do extensions next
		for (final SqlBuilder.SqlStruct sqlStruct : sqls.values()) {
			rowsAffected += write(connection, request, sqlStruct, false);
		}

		// Do the main table if update or delete
		if (request.getType() != Type.INSERT) {
			rowsAffected += write(connection, request, mainTableSqlStruct, true);
		}

		return rowsAffected;
	}

	private int write(final Connection connection, Request request, final SqlStruct sqlStruct, boolean doMain)
			throws SqlResourceException {
		int rowsAffected = 0;
		if (sqlStruct != null) {
			if (!doMain && sqlStruct.isClauseEmpty()) {
				// do not execute update on extension, which would affect all rows
			} else {
				final String sql = sqlStruct.getSql();
				try {
					final Statement statement = connection.createStatement();
					Config.logger.debug(sql);
					request.getLogger().addSql(sql);
					rowsAffected = statement.executeUpdate(sql);
					statement.close();
				} catch (final SQLException exception) {
					throw new SqlResourceException(exception, sql);
				}
			}
		}
		return rowsAffected;
	}
}
