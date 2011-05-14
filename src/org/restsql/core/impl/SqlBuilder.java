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
import org.restsql.core.TableMetaData;
import org.restsql.core.Request.Type;

/**
 * Builds SQL for an operation on a SQL Resource. No caching.
 * 
 * @author Mark Sawers
 * @todo optimize - save sql or change to prepared statement?
 * @todo handle parameter string escaping
 */
public class SqlBuilder {
	private static final int DEFAULT_DELETE_SIZE = 100;
	private static final int DEFAULT_INSERT_SIZE = 300;
	private static final int DEFAULT_SELECT_SIZE = 300;
	private static final int DEFAULT_UPDATE_SIZE = 300;

	/**
	 * Builds select sql.
	 */
	static String buildSelectSql(final SqlResourceMetaData metaData, final String mainSql,
			final List<NameValuePair> resourceIdentifiers, final List<NameValuePair> params)
			throws InvalidRequestException {
		final SqlStruct sql = new SqlStruct(0, DEFAULT_SELECT_SIZE);
		buildSelectSql(metaData, mainSql, resourceIdentifiers, sql);
		buildSelectSql(metaData, mainSql, params, sql);
		if (sql.limit > -1) {
			if (sql.offset >= 0) {
				sql.clause.append(" LIMIT ");
				sql.clause.append(sql.offset);
				sql.clause.append(',');
				sql.clause.append(sql.limit);
			} else {
				throw new InvalidRequestException(InvalidRequestException.MESSAGE_OFFSET_REQUIRED);
			}
		} else if (sql.offset >= 0) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_LIMIT_REQUIRED);
		}
		return mainSql + sql.clause;
	}

	static Map<String, SqlStruct> buildWriteSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException {
		Map<String, SqlStruct> sqls = null;
		switch (request.getType()) {
			case INSERT:
				sqls = SqlBuilder.buildInsertSql(metaData, request, doParent);
				break;
			case UPDATE:
				sqls = SqlBuilder.buildUpdateSql(metaData, request, doParent);
				break;
			case DELETE:
				sqls = SqlBuilder.buildDeleteSql(metaData, request, doParent);
				break;
			default:
				throw new InvalidRequestException("SELECT Request provided to SqlBuilder.buildWriteSql()");
		}
		return sqls;
	}

	private static Map<String, SqlStruct> buildDeleteSql(final SqlResourceMetaData metaData,
			final Request request, final boolean doParent) throws InvalidRequestException {
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

	private static void buildDeleteSqlPart(final SqlResourceMetaData metaData,
			final List<NameValuePair> nameValuePairs, final Map<String, SqlStruct> sqls,
			final boolean doParent) {
		if (nameValuePairs != null) {
			for (final NameValuePair nameValuePair : nameValuePairs) {
				final List<TableMetaData> tables = getWriteTables(Request.Type.DELETE, metaData, doParent);
				for (final TableMetaData table : tables) {
					final ColumnMetaData column = table.getColumns().get(nameValuePair.getName());
					if (column != null) {
						final String qualifiedTableName = column.getQualifiedTableName();
						SqlStruct sql = sqls.get(qualifiedTableName);
						if (sql == null) {
							// Create new sql holder
							sql = new SqlStruct(DEFAULT_DELETE_SIZE, DEFAULT_DELETE_SIZE / 2);
							sqls.put(qualifiedTableName, sql);
							sql.main.append("DELETE FROM ");
							sql.main.append(qualifiedTableName);
							sql.clause.append(" WHERE ");
						} else {
							sql.clause.append(" AND ");
						}
						setNameValue(Request.Type.DELETE, metaData, column, nameValuePair, sql.clause);
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
	private static Map<String, SqlStruct> buildInsertSql(final SqlResourceMetaData metaData,
			final Request request, final boolean doParent) throws InvalidRequestException {
		final Map<String, SqlStruct> sqls = new HashMap<String, SqlStruct>(metaData.getNumberTables());

		// Iterate through the params and build the sql for each table
		for (final NameValuePair param : request.getParameters()) {
			final List<TableMetaData> tables = getWriteTables(request.getType(), metaData, doParent);
			for (final TableMetaData table : tables) {
				final ColumnMetaData column = table.getColumns().get(param.getName());
				if (column != null) {
					final String qualifiedTableName = column.getQualifiedTableName();
					SqlStruct sql = sqls.get(qualifiedTableName);
					if (sql == null) {
						// Create new sql holder
						sql = new SqlStruct(DEFAULT_INSERT_SIZE, DEFAULT_INSERT_SIZE / 2);
						sqls.put(qualifiedTableName, sql);
						sql.main.append("INSERT INTO ");
						sql.main.append(qualifiedTableName);
						sql.main.append(" (");

						sql.clause.append(" VALUES (");
					} else {
						sql.main.append(',');
						sql.clause.append(',');
					}
					sql.main.append(column.getColumnName()); // since parameter may use column label
					if (column.isCharType()) {
						sql.clause.append('\'');
					}
					sql.clause.append(param.getValue());
					if (column.isCharType()) {
						sql.clause.append('\'');
					}
				}
			}
		}

		for (final String tableName : sqls.keySet()) {
			final SqlStruct sql = sqls.get(tableName);
			if (sql == null) {
				sqls.remove(tableName);
			} else {
				sql.main.append(')');
				sql.clause.append(')');
				sql.appendClauseToMain();
			}
		}

		if (sqls.size() == 0) {
			throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
		}
		return sqls;
	}

	private static void buildSelectSql(final SqlResourceMetaData metaData, final String mainSql,
			final List<NameValuePair> nameValues, final SqlStruct sql) throws InvalidRequestException {
		if (nameValues != null) {
			boolean validParamFound = false;
			for (final NameValuePair param : nameValues) {
				if (param.getName().equalsIgnoreCase(Request.PARAM_NAME_LIMIT)) {
					try {
						sql.limit = Integer.valueOf(param.getValue());
					} catch (final NumberFormatException exception) {
						throw new InvalidRequestException("Limit value " + param.getValue() + " is not a number");
					}
				} else if (param.getName().equalsIgnoreCase(Request.PARAM_NAME_OFFSET)) {
					try {
						sql.offset = Integer.valueOf(param.getValue());
					} catch (final NumberFormatException exception) {
						throw new InvalidRequestException("Offset value " + param.getValue() + " is not a number");
					}
				} else {
					if (mainSql.indexOf("where ") > 0 || mainSql.indexOf("WHERE ") > 0
							|| sql.clause.length() != 0) {
						sql.clause.append(" AND ");
					} else {
						sql.clause.append(" WHERE ");
					}
					for (final TableMetaData table : metaData.getTables()) {
						final ColumnMetaData column = table.getColumns().get(param.getName());
						if (column != null && !column.isNonqueriedForeignKey()) {
							validParamFound = true;
							setNameValue(Request.Type.SELECT, metaData, column, param, sql.clause);
						}
					}
				}
			}
			if (sql.clause.length() > 0 && !validParamFound) {
				throw new InvalidRequestException(InvalidRequestException.MESSAGE_INVALID_PARAMS);
			}
		}
	}

	private static Map<String, SqlStruct> buildUpdateSql(final SqlResourceMetaData metaData,
			final Request request, final boolean doParent) throws InvalidRequestException {
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
					if (column.isPrimaryKey()) {
						// Add this to the res Ids - assume resIds is non null
						resIds.add(param);
					} else if (!column.isNonqueriedForeignKey()) {
						SqlStruct sql = sqls.get(column.getQualifiedTableName());
						if (sql == null) {
							// Create new sql holder
							sql = new SqlStruct(DEFAULT_UPDATE_SIZE, DEFAULT_UPDATE_SIZE / 2);
							sqls.put(column.getQualifiedTableName(), sql);
							sql.main.append("UPDATE ");
							sql.main.append(column.getQualifiedTableName());
							sql.main.append(" SET ");
						} else {
							sql.main.append(',');
						}

						validParamFound = true;
						setNameValue(request.getType(), metaData, column, param, sql.main);
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
						if (sql.clause.length() == 0) {
							sql.clause.append(" WHERE ");
						} else { // sql.clause.length() > 0
							sql.clause.append(" AND ");
						}
						validParamFound = true;
						setNameValue(request.getType(), metaData, column, resId, sql.clause);
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

	// Private utils

	private static boolean containsWildcard(final String value) {
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
	private static List<TableMetaData> getWriteTables(final Type requestType,
			final SqlResourceMetaData metaData, final boolean doParent) {
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

	private static void setNameValue(final Type requestType, final SqlResourceMetaData metaData,
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
		if (column.isCharType()) {
			sql.append('\'');
		}
		sql.append(param.getValue());
		if (column.isCharType()) {
			sql.append('\'');
		}
	}

	/**
	 * Helper struct for building sql.
	 * 
	 * @author Mark Sawers
	 */
	static class SqlStruct {
		StringBuffer clause;
		int limit = -1, offset = -1;
		StringBuffer main;

		SqlStruct(final int mainSize, final int clauseSize) {
			main = new StringBuffer(mainSize);
			clause = new StringBuffer(clauseSize);
		}

		public String getSql() {
			return main.toString();
		}

		public boolean isClauseEmpty() {
			return clause.length() == 0;
		}

		void appendClauseToMain() {
			main.append(clause);
		}
	}
}