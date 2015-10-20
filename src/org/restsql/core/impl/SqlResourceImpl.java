/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.restsql.core.BinaryObject;
import org.restsql.core.ColumnMetaData;
import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.Request;
import org.restsql.core.Request.Type;
import org.restsql.core.RequestValue;
import org.restsql.core.ResponseValue;
import org.restsql.core.SqlBuilder;
import org.restsql.core.SqlBuilder.SqlStruct;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;
import org.restsql.core.TableMetaData.TableRole;
import org.restsql.core.Trigger;
import org.restsql.core.WriteResponse;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.SqlResourceDefinitionUtils;

/**
 * Represents a SQL Resource, a queryable and updatable database "view". Loads metadata on creation and caches it.
 * 
 * @author Mark Sawers
 */
public class SqlResourceImpl implements SqlResource {

	public static String removeWhitespaceFromSql(String sql) {
		sql = sql.replaceAll("\\r", "");
		sql = sql.replaceFirst("^\\s+", "");
		sql = sql.replaceFirst("\\s+$", "");
		sql = sql.replaceFirst("\\t+", " ");
		sql = sql.replaceFirst("\\t+$", "");
		sql = sql.replaceAll("\\t", " ");
		return sql;
	}

	private final SqlResourceDefinition definition;

	private final SqlResourceMetaData metaData;
	private final String name;
	private final SqlBuilder sqlBuilder;
	private final List<Trigger> triggers;

	public SqlResourceImpl(final String name, final SqlResourceDefinition definition,
			final SqlResourceMetaData metaData, final SqlBuilder sqlBuilder, final List<Trigger> triggers)
			throws SqlResourceException {
		this.name = name;
		this.definition = definition;
		definition.getQuery().setValue(removeWhitespaceFromSql(definition.getQuery().getValue()));
		this.metaData = metaData;
		this.sqlBuilder = sqlBuilder;
		this.triggers = triggers;
	}

	@Override
	public SqlResourceDefinition getDefinition() {
		return definition;
	}

	@Override
	public SqlResourceMetaData getMetaData() {
		return metaData;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Trigger> getTriggers() {
		return triggers;
	}

	/**
	 * Executes query returning results as an object collection.
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if a database access error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> read(final Request request) throws SqlResourceException {
		return (List<Map<String, Object>>) execRead(request, null);
	}

	/**
	 * Executes query returning results as a string.
	 * 
	 * @param request Request object
	 * @param mediaType response format, use internet media type e.g. application/xml
	 * @throws SqlResourceException if a database access error occurs
	 * @return list of rows, where each row is a map of name-value pairs
	 */
	@Override
	public String read(final Request request, final String mediaType) throws SqlResourceException {
		return (String) execRead(request, mediaType);
	}

	/**
	 * Executes database write (insert, update or delete).
	 * 
	 * @param request Request object
	 * @throws SqlResourceException if the request is invalid or a database access error or trigger exception occurs
	 * @return write Response object
	 */
	@Override
	public WriteResponse write(final Request request) throws SqlResourceException {
		TriggerManager.executeTriggers(getName(), request, true);

		// Init response
		final WriteResponse response = new WriteResponse();
		int rowsAffected = 0;
		Set<ResponseValue> responseValues = null;
		if (request.getType() == Type.INSERT) {
			responseValues = new TreeSet<ResponseValue>();
		}

		boolean doParent = true;
		Connection connection = null;

		try {
			connection = Factory.getConnection(SqlResourceDefinitionUtils.getDefaultDatabase(definition));
			if (metaData.isHierarchical()) {
				final Request childRequest = Factory.getChildRequest(request);
				if (request.getChildrenParameters() != null) {

					// Set up response
					List<Set<ResponseValue>> childListResponseValues = null;
					Set<ResponseValue> childResponseValues = null;
					if (request.getType() == Type.INSERT) {
						childListResponseValues = new ArrayList<Set<ResponseValue>>(request
								.getChildrenParameters().size());
						responseValues.add(new ResponseValue(getChildRowsName(), childListResponseValues,
								Integer.MAX_VALUE));
						// Add parent params, since we won't be executing the write on the parent
						for (final TableMetaData table : metaData.getParentPlusExtTables()) {
							addRequestParamsToResponseValues(request, responseValues, table);
						}
					}

					// Delete, update or insert each specified child row
					for (final List<RequestValue> childRowParams : request.getChildrenParameters()) {
						if (request.getType() == Type.INSERT) {
							// Set up response value set
							childResponseValues = new TreeSet<ResponseValue>();
							childListResponseValues.add(childResponseValues);

							// Add the parent pks, since inserts ignore the resIds
							childRowParams.addAll(request.getResourceIdentifiers());
						} // else deletes and updates use resIds
						childRequest.setParameters(childRowParams);
						rowsAffected += execWrite(connection, childRequest, false, childResponseValues);
					}
					// Don't touch the parent(s)
					doParent = false;
				} else if (request.getType() == Request.Type.DELETE) {
					// Delete all children and the parent(s)
					if (request.getResourceIdentifiers() == null) {
						childRequest.setParameters(request.getParameters());
					}
					rowsAffected += execWrite(connection, childRequest, false, responseValues);
					// Now do the parent as well, doParent already equals true
				}
				// else just insert or update the parent (+ extensions)
			} // else insert, update or delete the parent (+ extensions)

			if (doParent) {
				rowsAffected += execWrite(connection, request, true, responseValues);
			}

			TriggerManager.executeTriggers(getName(), request, false);

			// Finalize response
			if (request.getType() == Type.INSERT) {
				response.addRow(responseValues);
			}
			response.addRowsAffected(rowsAffected);

		} catch (final SQLException exception) {
			throw new SqlResourceException(exception);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final SQLException ignored) {
				}
			}
		}
		return response;
	}

	// Private utils

	/**
	 * Converts the request params and resource IDs into response values and add to the result set.
	 * 
	 * @throws InvalidRequestException
	 */
	private void addRequestParamsToResponseValues(final Request request,
			final Set<ResponseValue> responseValues, final TableMetaData table)
			throws InvalidRequestException {
		if (request.getParameters() != null) {
			for (final RequestValue param : request.getParameters()) {
				final ColumnMetaData column = table.getColumns().get(param.getName());
				if (column != null && !column.isNonqueriedForeignKey()) {
					responseValues.add(new ResponseValue(param.getName(), param.getValue(), column
							.getColumnNumber()));
				}
			}
		}
		if (request.getResourceIdentifiers() != null) {
			for (final RequestValue param : request.getResourceIdentifiers()) {
				final ColumnMetaData column = table.getColumns().get(param.getName());
				if (column != null && !column.isNonqueriedForeignKey()) {
					column.normalizeValue(param); // this is called in the SQL Builder as well, but it's required here
													// for parent res ids
					responseValues.add(new ResponseValue(param.getName(), param.getValue(), column
							.getColumnNumber()));
				}
			}
		}
	}

	/** Creates collection from result set for flat resource. */
	private List<Map<String, Object>> buildReadResultsFlatCollection(final ResultSet resultSet)
			throws SQLException {
		final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		while (resultSet.next()) {
			final Map<String, Object> row = new HashMap<String, Object>(metaData.getAllReadColumns().size());
			for (final ColumnMetaData column : metaData.getAllReadColumns()) {
				// Simple name, value pairs will do
				if (!column.isNonqueriedForeignKey()) {
					row.put(column.getColumnLabel(), column.getResultByNumber(resultSet));
				}
			}
			results.add(row);
		}
		return results;
	}

	/** Creates collection from result set for hierarchical resource. */
	private List<Map<String, Object>> buildReadResultsHierachicalCollection(final ResultSet resultSet)
			throws SQLException {
		final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		final List<Object> currentParentPkValues = new ArrayList<Object>(metaData.getParent()
				.getPrimaryKeys().size());
		boolean newParent = false;
		final int numberParentElementColumns = metaData.getParentReadColumns().size();
		final int numberChildElementColumns = metaData.getChildReadColumns().size();
		final String childRowsName = getChildRowsName();
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
					if (!currentParentPkValues.get(i).equals(column.getResultByLabel(resultSet))) {
						newParent = true;
						break;
					}
				}
			}

			// Set current parent row pk values as well as in the parent row object
			if (newParent) {
				childRows = new ArrayList<Map<String, Object>>();
				parentRow = new HashMap<String, Object>(numberParentElementColumns);
				parentRow.put(childRowsName, childRows);
				results.add(parentRow);
				currentParentPkValues.clear();

				for (final ColumnMetaData column : metaData.getParentReadColumns()) {
					final Object value = column.getResultByLabel(resultSet);
					if (column.isPrimaryKey() && column.getTableRole() == TableRole.Parent) {
						currentParentPkValues.add(value);
					}
					parentRow.put(column.getColumnLabel(), value);
				}
			}

			// Populate the child row object
			Map<String, Object> childRow = new HashMap<String, Object>(numberChildElementColumns);
			boolean nullPk = false;
			for (final ColumnMetaData column : metaData.getChildReadColumns()) {
				final Object value = column.getResultByLabel(resultSet);
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

	private Object execRead(final Request request, final String contentType) throws SqlResourceException {
		request.extractParameters();
		TriggerManager.executeTriggers(getName(), request, true);

		final Object results;
		Connection connection = null;
		final SqlStruct sqlStruct = sqlBuilder.buildSelectSql(metaData, definition.getQuery().getValue(),
				request);
		try {
			connection = Factory.getConnection(SqlResourceDefinitionUtils.getDefaultDatabase(definition));
			final PreparedStatement statement = connection.prepareStatement(sqlStruct.getPreparedStatement());
			if (Config.logger.isDebugEnabled()) {
				Config.logger.debug("\n" + sqlStruct.getPreparedStatement() + "\n-----\n"
						+ sqlStruct.getStatement());
			}
			request.getLogger().addSql(sqlStruct.getStatement());
			for (int i = 0; i < sqlStruct.getPreparedValues().size(); i++) {
				statement.setObject(i + 1, sqlStruct.getPreparedValues().get(i));
			}
			final ResultSet resultSet = statement.executeQuery();
			if (metaData.isHierarchical()) {
				if (contentType != null) {
					results = Factory.getResponseSerializer(contentType).serializeReadHierarchical(this,
							buildReadResultsHierachicalCollection(resultSet));
				} else {
					results = buildReadResultsHierachicalCollection(resultSet);
				}
			} else {
				if (contentType != null) {
					results = Factory.getResponseSerializer(contentType).serializeReadFlat(this, resultSet);
				} else {
					results = buildReadResultsFlatCollection(resultSet);
				}
			}
			resultSet.close();
			statement.close();
		} catch (final SQLException exception) {
			throw new SqlResourceException(exception, sqlStruct.getStatement());
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final SQLException ignored) {
				}
			}
		}

		TriggerManager.executeTriggers(getName(), request, false);
		return results;
	}

	private int execWrite(final Connection connection, final Request request, final boolean doParent,
			final Set<ResponseValue> responseValues) throws SqlResourceException {
		final Map<String, SqlBuilder.SqlStruct> sqls = sqlBuilder.buildWriteSql(metaData, request, doParent);

		// Remove sql for main table
		final String mainTableName = doParent ? metaData.getParent().getQualifiedTableName() : metaData
				.getChild().getQualifiedTableName();
		final SqlBuilder.SqlStruct mainTableSqlStruct = sqls.remove(mainTableName);

		// Do the write operation(s)
		int rowsAffected = 0;

		// Do the main table if insert
		if (request.getType() == Type.INSERT) {
			rowsAffected += execWrite(connection, request, mainTableSqlStruct, true);
		}

		// Do extensions next
		for (final SqlBuilder.SqlStruct sqlStruct : sqls.values()) {
			rowsAffected += execWrite(connection, request, sqlStruct, false);
		}

		// Do the main table if update or delete
		if (request.getType() != Type.INSERT) {
			rowsAffected += execWrite(connection, request, mainTableSqlStruct, true);
		}

		// Build inserted results for the write response
		if (request.getType() == Type.INSERT) {
			List<TableMetaData> tables;
			if (doParent) {
				tables = metaData.getParentPlusExtTables();
			} else {
				tables = metaData.getChildPlusExtTables();
			}

			for (final TableMetaData table : tables) {
				addRequestParamsToResponseValues(request, responseValues, table);

				// Find columns missing from the request params that are sequences and request the current value
				for (final ColumnMetaData column : table.getColumns().values()) {
					if (!request.hasParameter(column.getColumnLabel()) && column.isSequence()) {
						final int value = Factory.getSequenceManager().getCurrentValue(connection,
								column.getSequenceName());
						responseValues.add(new ResponseValue(column.getColumnLabel(), new Integer(value),
								column.getColumnNumber()));
					}
				}
			}
		}
		return rowsAffected;
	}

	private int execWrite(final Connection connection, final Request request, final SqlStruct sqlStruct,
			final boolean doMain) throws SqlResourceException {
		int rowsAffected = 0;
		if (sqlStruct != null) {
			if (!doMain && sqlStruct.isClauseEmpty()) {
				// do not execute update on extension, which would affect all rows
			} else {
				try {
					final PreparedStatement statement = connection.prepareStatement(sqlStruct
							.getPreparedStatement());
					if (Config.logger.isDebugEnabled()) {
						Config.logger.debug("\n" + sqlStruct.getPreparedStatement() + "\n"
								+ sqlStruct.getStatement());
					}
					request.getLogger().addSql(sqlStruct.getStatement());
					for (int i = 0; i < sqlStruct.getPreparedValues().size(); i++) {
						final Object value = sqlStruct.getPreparedValues().get(i);
						if (value instanceof BinaryObject) {
							statement.setBytes(i + 1, ((BinaryObject) value).getBytes());
						} else {
							statement.setObject(i + 1, value);
						}
					}
					rowsAffected = statement.executeUpdate();
					statement.close();
				} catch (final SQLException exception) {
					throw new SqlResourceException(exception, sqlStruct.getStatement());
				}
			}
		}
		return rowsAffected;
	}

	private String getChildRowsName() {
		return metaData.getChild().getRowSetAlias();
	}
}