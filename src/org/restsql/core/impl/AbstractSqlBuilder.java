/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.Request;
import org.restsql.core.Request.Type;
import org.restsql.core.RequestValue;
import org.restsql.core.RequestValue.Operator;
import org.restsql.core.SqlBuilder;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;

/**
 * Builds SQL for an operation on a SQL Resource.
 * 
 * @author Mark Sawers
 */

public abstract class AbstractSqlBuilder implements SqlBuilder {
	private static final int DEFAULT_DELETE_SIZE = 100;
	private static final int DEFAULT_INSERT_SIZE = 300;
	private static final int DEFAULT_SELECT_SIZE = 300;
	private static final int DEFAULT_UPDATE_SIZE = 300;

	// Public methods

	/** Creates select SQL. */
	@Override
	public SqlStruct buildSelectSql(final SqlResourceMetaData metaData, final String mainSql,
			final Request request) throws InvalidRequestException {
		final SqlStruct sql = new SqlStruct(mainSql.length(), DEFAULT_SELECT_SIZE);
		sql.getMain().append(mainSql);
		buildSelectSql(metaData, request.getResourceIdentifiers(), sql);
		buildSelectSql(metaData, request.getParameters(), sql);
		addOrderBy(metaData, sql);

		// Handle limit and offset
		if (request.getSelectLimit() != null) {
			// Call concrete database-specific class to get the limit clause
			sql.appendToBothClauses(buildSelectLimitSql(request.getSelectLimit().intValue(), request
					.getSelectOffset().intValue()));
		}

		sql.compileStatements();
		return sql;
	}

	/** Creates update, insert or delete SQL. */
	@Override
	public Map<String, SqlStruct> buildWriteSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException {
		Map<String, SqlStruct> sqls = null;
		switch (request.getType()) {
			case INSERT:
				sqls = buildInsertSql(metaData, request, doParent);
				break;
			case UPDATE:
				sqls = buildUpdateSql(metaData, request, doParent);
				break;
			case DELETE:
				sqls = buildDeleteSql(metaData, request, doParent);
				break;
			default:
				throw new InvalidRequestException("SELECT Request provided to SqlBuilder.buildWriteSql()");
		}
		return sqls;
	}

	/** Creates select SQL limit clause. Returns empty string if database does not support limit feature. */
	protected abstract String buildSelectLimitSql(final int limit, final int offset);

	/** Enables override for databases like PostgreSQL that need special handling for enumerations. */
	protected String buildPreparedParameterSql(final ColumnMetaData column) {
		return "?";
	}

	// Private helper methods

	/** Adds order by statement . */
	private void addOrderBy(final SqlResourceMetaData metaData, final SqlStruct sql) {
		boolean firstColumn = true;
		firstColumn = addOrderByColumn(metaData, sql, firstColumn, metaData.getParent());
		addOrderByColumn(metaData, sql, firstColumn, metaData.getChild());
	}

	/** Adds order by column list for the table's primary keys. */
	private boolean addOrderByColumn(final SqlResourceMetaData metaData, final SqlStruct sql,
			boolean firstColumn, final TableMetaData table) {
		if (table != null) {
			for (final ColumnMetaData column : table.getPrimaryKeys()) {
				if (firstColumn) {
					sql.appendToBothClauses(" ORDER BY ");
					firstColumn = false;
				} else {
					sql.appendToBothClauses(", ");
				}
				sql.appendToBothClauses(column.getQualifiedColumnName());
			}
		}
		return firstColumn;
	}

	private void appendToBoth(final SqlStruct sql, final boolean useMain, final String string) {
		if (useMain) {
			sql.appendToBothMains(string);
		} else {
			sql.appendToBothClauses(string);
		}
	}

	private void appendValue(final StringBuilder part, final StringBuilder preparedPart,
			final List<Object> preparedValues, final Object value, final boolean charOrDateTimeType,
			ColumnMetaData column) {
		if (value != null && charOrDateTimeType) {
			part.append('\'');
		}
		part.append(value);
		if (value != null && charOrDateTimeType) {
			part.append('\'');
		}
		preparedPart.append(buildPreparedParameterSql(column));
		preparedValues.add(value);
	}

	private Map<String, SqlStruct> buildDeleteSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException {
		final Map<String, SqlStruct> sqls = new HashMap<String, SqlStruct>(metaData.getNumberTables());
		buildDeleteSqlPart(metaData, request.getResourceIdentifiers(), sqls, doParent);
		buildDeleteSqlPart(metaData, request.getParameters(), sqls, doParent);

		for (final String tableName : sqls.keySet()) {
			final SqlStruct sql = sqls.get(tableName);
			if (sql == null) {
				sqls.remove(tableName);
			} else {
				sql.compileStatements();
			}
		}

		if (sqls.size() == 0 && doParent) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private void buildDeleteSqlPart(final SqlResourceMetaData metaData,
			final List<RequestValue> requestParams, final Map<String, SqlStruct> sqls, final boolean doParent)
			throws InvalidRequestException {
		if (requestParams != null) {
			for (final RequestValue requestParam : requestParams) {
				final List<TableMetaData> tables = metaData.getWriteTables(Request.Type.DELETE, doParent);
				for (final TableMetaData table : tables) {
					final ColumnMetaData column = table.getColumns().get(requestParam.getName());
					if (column != null) {
						if (column.isReadOnly()) {
							throw new InvalidRequestException(InvalidRequestException.MESSAGE_READONLY_PARAM,
									column.getColumnLabel());
						}
						final String qualifiedTableName = column.getQualifiedTableName();
						SqlStruct sql = sqls.get(qualifiedTableName);
						if (sql == null) {
							// Create new sql holder
							sql = new SqlStruct(DEFAULT_DELETE_SIZE, DEFAULT_DELETE_SIZE / 2);
							sqls.put(qualifiedTableName, sql);
							sql.getMain().append("DELETE FROM ");
							sql.getMain().append(qualifiedTableName);
							sql.appendToBothClauses(" WHERE ");
						} else {
							sql.appendToBothClauses(" AND ");
						}
						setNameValue(Request.Type.DELETE, metaData, column, requestParam, true, sql, false);
					}
				}
			}
		}
	}

	/**
	 * Builds insert SQL.
	 * 
	 * @param params insert params
	 * @return map of sql struct, per table
	 * @throws InvalidRequestException if a database access error occurs
	 */
	private Map<String, SqlStruct> buildInsertSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException {

		final Map<String, SqlStruct> sqls = new HashMap<String, SqlStruct>(metaData.getNumberTables());

		// Iterate through the params and build the sql for each table
		for (final RequestValue param : request.getParameters()) {
			final List<TableMetaData> tables = metaData.getWriteTables(request.getType(), doParent);
			for (final TableMetaData table : tables) {
				final ColumnMetaData column = table.getColumns().get(param.getName());
				if (column != null) {
					if (column.isReadOnly()) {
						throw new InvalidRequestException(InvalidRequestException.MESSAGE_READONLY_PARAM,
								column.getColumnLabel());
					}
					final String qualifiedTableName = column.getQualifiedTableName();
					SqlStruct sql = sqls.get(qualifiedTableName);
					if (sql == null) {
						// Create new sql holder
						sql = new SqlStruct(DEFAULT_INSERT_SIZE, DEFAULT_INSERT_SIZE / 2);
						sqls.put(qualifiedTableName, sql);
						sql.getMain().append("INSERT INTO ");
						sql.getMain().append(qualifiedTableName);
						sql.getMain().append(" (");

						sql.appendToBothClauses(" VALUES (");
					} else {
						sql.getMain().append(',');
						sql.appendToBothClauses(",");
					}
					sql.getMain().append(column.getColumnName()); // since parameter may use column label

					// Begin quote the column value
					if (column.isCharOrDateTimeType() && param.getValue() != null) {
						sql.getClause().append('\'');
					}

					// Convert String to appropriate object
					column.normalizeValue(param);

					// Set the value in the printable clause, the ? in the prepared clause, and prepared clause value
					sql.getClause().append(param.getValue());
					sql.getPreparedClause().append(buildPreparedParameterSql(column));
					sql.getPreparedValues().add(param.getValue());

					// End quote the column value
					if (column.isCharOrDateTimeType() && param.getValue() != null) {
						sql.getClause().append('\'');
					}
				}
			}
		}

		for (final String tableName : sqls.keySet()) {
			final SqlStruct sql = sqls.get(tableName);
			if (sql == null) {
				sqls.remove(tableName);
			} else {
				sql.getMain().append(')');
				sql.appendToBothClauses(")");
				sql.compileStatements();
			}
		}

		if (sqls.size() == 0) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private void buildSelectSql(final SqlResourceMetaData metaData, final List<RequestValue> params,
			final SqlStruct sql) throws InvalidRequestException {
		if (params != null && params.size() > 0) {
			boolean validParamFound = false;
			for (final RequestValue param : params) {
				if (sql.getMain().indexOf("where ") > 0 || sql.getMain().indexOf("WHERE ") > 0
						|| sql.getClause().length() != 0) {
					sql.appendToBothClauses(" AND ");
				} else {
					sql.appendToBothClauses(" WHERE ");
				}

				for (final TableMetaData table : metaData.getTables()) {
					final ColumnMetaData column = table.getColumns().get(param.getName());
					if (column != null) {
						if (column.isReadOnly()) {
							throw new InvalidRequestException(InvalidRequestException.MESSAGE_READONLY_PARAM,
									column.getColumnLabel());
						}
						if (!column.isNonqueriedForeignKey()) {
							validParamFound = true;
							setNameValue(Request.Type.SELECT, metaData, column, param, true, sql, false);
						}
					}
				}
			}

			if (sql.getClause().length() > 0 && !validParamFound) {
				throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
			}
		}
	}

	private Map<String, SqlStruct> buildUpdateSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException {
		final Map<String, SqlStruct> sqls = new HashMap<String, SqlStruct>(metaData.getNumberTables());

		List<RequestValue> resIds;
		if (metaData.isHierarchical() && !doParent) {
			// Clone the list, since changing the request will affect the next child request
			resIds = new ArrayList<RequestValue>(request.getResourceIdentifiers().size());
			for (final RequestValue resId : request.getResourceIdentifiers()) {
				resIds.add(resId);
			}
		} else { // is flat or is hierarchical and executing the parent
			resIds = request.getResourceIdentifiers();
		}

		final List<TableMetaData> tables = metaData.getWriteTables(request.getType(), doParent);

		boolean validParamFound = false;
		for (final RequestValue param : request.getParameters()) {
			for (final TableMetaData table : tables) {
				final ColumnMetaData column = table.getColumns().get(param.getName());
				if (column != null) {
					if (column.isReadOnly()) {
						throw new InvalidRequestException(InvalidRequestException.MESSAGE_READONLY_PARAM,
								column.getColumnLabel());
					}
					if (column.isPrimaryKey()) {
						// Add this to the res Ids - assume resIds is non null
						resIds.add(param);
					} else if (!column.isNonqueriedForeignKey()) {
						SqlStruct sql = sqls.get(column.getQualifiedTableName());
						if (sql == null) {
							// Create new sql holder
							sql = new SqlStruct(DEFAULT_UPDATE_SIZE, DEFAULT_UPDATE_SIZE / 2, true);
							sqls.put(column.getQualifiedTableName(), sql);
							sql.appendToBothMains("UPDATE ");
							sql.appendToBothMains(column.getQualifiedTableName());
							sql.appendToBothMains(" SET ");
						} else {
							sql.appendToBothMains(",");
						}

						validParamFound = true;
						setNameValue(request.getType(), metaData, column, param, false, sql, true);
					}
				}
			}
		}

		if (!validParamFound) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		validParamFound = false;

		for (final String qualifiedTableName : sqls.keySet()) {
			final SqlStruct sql = sqls.get(qualifiedTableName);
			if (sql == null) {
				sqls.remove(qualifiedTableName);
			} else {
				// Iterate through the resourceIds and build the where clause sql for each table
				for (final RequestValue resId : resIds) {
					final TableMetaData table = metaData.getTableMap().get(qualifiedTableName);
					final ColumnMetaData column = table.getColumns().get(resId.getName());
					if (column != null) {
						if (sql.getClause().length() == 0) {
							sql.appendToBothClauses(" WHERE ");
						} else { // sql.getClause().length() > 0
							sql.appendToBothClauses(" AND ");
						}
						validParamFound = true;
						setNameValue(request.getType(), metaData, column, resId, true, sql, false);
					}
				}
				sql.compileStatements();
			}
		}

		if (!validParamFound) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private boolean containsWildcard(final Object value) {
		boolean contains = false;
		if (value != null && value instanceof String) {
			final int index = ((String) value).indexOf("%");
			contains = index > -1;
		}
		return contains;
	}

	/**
	 * Adds the SQL selector for the parameter pair with an appropriate operator (=, >, <, >=, <=, LIKE or IN).
	 * 
	 * @throws InvalidRequestException if unexpected operator is found (Escaped is only for internal use)
	 */
	private void setNameValue(final Type requestType, final SqlResourceMetaData metaData,
			final ColumnMetaData column, final RequestValue param, final boolean columnIsSelector,
			final SqlStruct sql, final boolean useMain) throws InvalidRequestException {

		// Convert String to Number object if required
		column.normalizeValue(param);

		// Append the name
		if (requestType == Request.Type.SELECT) {
			appendToBoth(sql, useMain, column.getQualifiedColumnName());
		} else {
			appendToBoth(sql, useMain, column.getColumnName());
		}

		// Append the operator
		if (columnIsSelector && param.getOperator() == Operator.Equals && containsWildcard(param.getValue())) {
			appendToBoth(sql, useMain, " LIKE ");
		} else if (!columnIsSelector && requestType == Request.Type.UPDATE
				&& param.getOperator() == Operator.IsNull) {
			appendToBoth(sql, useMain, " = ");
		} else {
			switch (param.getOperator()) {
				case Equals:
					appendToBoth(sql, useMain, " = ");
					break;
				case In:
					appendToBoth(sql, useMain, " IN ");
					break;
				case IsNull:
					appendToBoth(sql, useMain, " IS NULL");
					break;
				case IsNotNull:
					appendToBoth(sql, useMain, " IS NOT NULL");
					break;
				case LessThan:
					appendToBoth(sql, useMain, " < ");
					break;
				case LessThanOrEqualTo:
					appendToBoth(sql, useMain, " <= ");
					break;
				case GreaterThan:
					appendToBoth(sql, useMain, " > ");
					break;
				case GreaterThanOrEqualTo:
					appendToBoth(sql, useMain, " >= ");
					break;
				case NotEquals:
					appendToBoth(sql, useMain, " != ");
					break;
				default: // case Escaped
					throw new InvalidRequestException(
							"SqlBuilder.setNameValue() found unexpected operator of type "
									+ param.getOperator());
			}
		}

		// Append the value
		if (param.getOperator() == Operator.In) {
			appendToBoth(sql, useMain, "(");
			boolean firstValue = true;
			for (final Object value : param.getInValues()) {
				if (!firstValue) {
					appendToBoth(sql, useMain, ",");
				}
				appendValue(useMain ? sql.getMain() : sql.getClause(),
						useMain ? sql.getPreparedMain() : sql.getPreparedClause(), sql.getPreparedValues(),
						value, column.isCharOrDateTimeType(), column);
				firstValue = false;
			}
			appendToBoth(sql, useMain, ")");
		} else if ((param.getOperator() != Operator.IsNull && param.getOperator() != Operator.IsNotNull)
				|| (!columnIsSelector && requestType == Request.Type.UPDATE)) {
			appendValue(useMain ? sql.getMain() : sql.getClause(),
					useMain ? sql.getPreparedMain() : sql.getPreparedClause(), sql.getPreparedValues(),
					param.getValue(), column.isCharOrDateTimeType(), column);
		}
	}
}