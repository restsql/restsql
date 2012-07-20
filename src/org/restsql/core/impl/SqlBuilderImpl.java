/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.SqlBuilder;
import org.restsql.core.SqlResourceMetaData;
import org.restsql.core.TableMetaData;
import org.restsql.core.Request.Type;

/**
 * Builds SQL for an operation on a SQL Resource.
 * 
 * @author Mark Sawers
 * @todo optimize - save sql or change to prepared statement?
 * @todo handle parameter string escaping
 */
public class SqlBuilderImpl implements SqlBuilder {
	private static final int DEFAULT_DELETE_SIZE = 100;
	private static final int DEFAULT_INSERT_SIZE = 300;
	private static final int DEFAULT_SELECT_SIZE = 300;
	private static final int DEFAULT_UPDATE_SIZE = 300;

	// Public methods

	/** Creates select SQL. */
	public String buildSelectSql(final SqlResourceMetaData metaData, final String mainSql,
			final List<NameValuePair> resourceIdentifiers, final List<NameValuePair> params)
			throws InvalidRequestException {
		final SqlStruct sql = new SqlStruct(mainSql.length(), DEFAULT_SELECT_SIZE);
		sql.getMain().append(mainSql);
		buildSelectSql(metaData, resourceIdentifiers, sql);
		buildSelectSql(metaData, params, sql);
		addOrderBy(metaData, sql);
		if (sql.getLimit() > -1) {
			if (sql.getOffset() >= 0) {
				sql.getClause().append(" LIMIT ");
				sql.getClause().append(sql.getLimit());
				sql.getClause().append(" OFFSET ");
				sql.getClause().append(sql.getOffset());
			} else {
				throw new InvalidRequestException(InvalidRequestException.MESSAGE_OFFSET_REQUIRED);
			}
		} else if (sql.getOffset() >= 0) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_LIMIT_REQUIRED);
		}
		sql.appendClauseToMain();
		return sql.getMain().toString();
	}

	/** Creates update, insert or delete SQL. */
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
					sql.getClause().append(" ORDER BY ");
					firstColumn = false;
				} else {
					sql.getClause().append(", ");
				}
				sql.getClause().append(column.getColumnName());
			}
		}
		return firstColumn;
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
				sql.appendClauseToMain();
			}
		}

		if (sqls.size() == 0 && doParent) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private void buildDeleteSqlPart(final SqlResourceMetaData metaData,
			final List<NameValuePair> nameValuePairs, final Map<String, SqlStruct> sqls,
			final boolean doParent) throws InvalidRequestException {
		if (nameValuePairs != null) {
			for (final NameValuePair nameValuePair : nameValuePairs) {
				final List<TableMetaData> tables = getWriteTables(Request.Type.DELETE, metaData, doParent);
				for (final TableMetaData table : tables) {
					final ColumnMetaData column = table.getColumns().get(nameValuePair.getName());
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
							sql.getClause().append(" WHERE ");
						} else {
							sql.getClause().append(" AND ");
						}
						setNameValue(Request.Type.DELETE, metaData, column, nameValuePair, sql.getClause());
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
		for (final NameValuePair param : request.getParameters()) {
			final List<TableMetaData> tables = getWriteTables(request.getType(), metaData, doParent);
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

						sql.getClause().append(" VALUES (");
					} else {
						sql.getMain().append(',');
						sql.getClause().append(',');
					}
					sql.getMain().append(column.getColumnName()); // since parameter may use column label
					if (column.isCharType() || column.isDateTimeType()) {
						sql.getClause().append('\'');
					}
					sql.getClause().append(param.getValue());
					if (column.isCharType() || column.isDateTimeType()) {
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
				sql.getClause().append(')');
				sql.appendClauseToMain();
			}
		}

		if (sqls.size() == 0) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private void buildSelectSql(final SqlResourceMetaData metaData, final List<NameValuePair> nameValues,
			final SqlStruct sql) throws InvalidRequestException {
		if (nameValues != null && nameValues.size() > 0) {
			boolean validParamFound = false;
			for (final NameValuePair param : nameValues) {
				if (param.getName().equalsIgnoreCase(Request.PARAM_NAME_LIMIT)) {
					try {
						sql.setLimit(Integer.valueOf(param.getValue()));
					} catch (final NumberFormatException exception) {
						throw new InvalidRequestException("Limit value " + param.getValue()
								+ " is not a number");
					}
				} else if (param.getName().equalsIgnoreCase(Request.PARAM_NAME_OFFSET)) {
					try {
						sql.setOffset(Integer.valueOf(param.getValue()));
					} catch (final NumberFormatException exception) {
						throw new InvalidRequestException("Offset value " + param.getValue()
								+ " is not a number");
					}
				} else {
					if (sql.getMain().indexOf("where ") > 0 || sql.getMain().indexOf("WHERE ") > 0
							|| sql.getClause().length() != 0) {
						sql.getClause().append(" AND ");
					} else {
						sql.getClause().append(" WHERE ");
					}
					for (final TableMetaData table : metaData.getTables()) {
						final ColumnMetaData column = table.getColumns().get(param.getName());
						if (column != null) {
							if (column.isReadOnly()) {
								throw new InvalidRequestException(InvalidRequestException.MESSAGE_READONLY_PARAM, column.getColumnLabel());
							}
							if (!column.isNonqueriedForeignKey()) {
								validParamFound = true;
								setNameValue(Request.Type.SELECT, metaData, column, param, sql.getClause());
							}
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

		List<NameValuePair> resIds;
		if (metaData.isHierarchical() && !doParent) {
			// Clone the list, since changing the request will affect the next child request
			resIds = new ArrayList<NameValuePair>(request.getResourceIdentifiers().size());
			for (final NameValuePair resId : request.getResourceIdentifiers()) {
				resIds.add(resId);
			}
		} else { // not hierachical or is hierarchical and executing the parent
			resIds = request.getResourceIdentifiers();
		}

		final List<TableMetaData> tables = getWriteTables(request.getType(), metaData, doParent);

		boolean validParamFound = false;
		for (final NameValuePair param : request.getParameters()) {
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
							sql = new SqlStruct(DEFAULT_UPDATE_SIZE, DEFAULT_UPDATE_SIZE / 2);
							sqls.put(column.getQualifiedTableName(), sql);
							sql.getMain().append("UPDATE ");
							sql.getMain().append(column.getQualifiedTableName());
							sql.getMain().append(" SET ");
						} else {
							sql.getMain().append(',');
						}

						validParamFound = true;
						setNameValue(request.getType(), metaData, column, param, sql.getMain());
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
				for (final NameValuePair resId : resIds) {
					final TableMetaData table = metaData.getTableMap().get(qualifiedTableName);
					final ColumnMetaData column = table.getColumns().get(resId.getName());
					if (column != null) {
						if (sql.getClause().length() == 0) {
							sql.getClause().append(" WHERE ");
						} else { // sql.getClause().length() > 0
							sql.getClause().append(" AND ");
						}
						validParamFound = true;
						setNameValue(request.getType(), metaData, column, resId, sql.getClause());
					}
				}
				sql.appendClauseToMain();
			}
		}

		if (!validParamFound) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private boolean containsWildcard(final String value) {
		boolean contains = false;
		final int index = value.indexOf("%");
		contains = index > -1;
		if (index > 0 && value.charAt(index - 1) == '\\') {
			contains = false; // wildcard escaped, literal value desired
		}
		return contains;
	}

	/**
	 * Determines the tables to use for write, possibly substituting the parent+, child+ or join table for query tables.
	 */
	private List<TableMetaData> getWriteTables(final Type requestType, final SqlResourceMetaData metaData,
			final boolean doParent) {
		List<TableMetaData> tables;
		if (metaData.isHierarchical()) {
			if (!doParent) { // child write
				if (metaData.hasJoinTable() && requestType != Type.UPDATE) {
					// Substitute join table for child if many to many hierarchical
					tables = metaData.getJoinList();
				} else {
					tables = metaData.getChildPlusExtTables();
				}
			} else { // parent write
				tables = metaData.getParentPlusExtTables();
			}
		} else {
			// Use all query tables
			tables = metaData.getTables();
		}
		return tables;
	}

	private void setNameValue(final Type requestType, final SqlResourceMetaData metaData,
			final ColumnMetaData column, final NameValuePair param, final StringBuffer sql) {
		if (requestType == Request.Type.SELECT) {
			if (metaData.hasMultipleDatabases()) {
				sql.append(column.getQualifiedTableName());
			} else {
				sql.append(column.getTableName());
			}
			sql.append('.');
		}
		sql.append(column.getColumnName()); // since parameter may use column label
		if (containsWildcard(param.getValue())) {
			sql.append(" LIKE ");
		} else {
			sql.append(" = ");
		}
		if (column.isCharType() || column.isDateTimeType()) {
			sql.append('\'');
		}
		sql.append(param.getValue());
		if (column.isCharType() || column.isDateTimeType()) {
			sql.append('\'');
		}
	}
}