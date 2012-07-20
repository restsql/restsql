/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.restsql.core.ColumnMetaData;

/**
 * Contains utilities to manage SQL and java.sql.ResultSets.
 * 
 * @author Mark Sawers
 */
class SqlUtils {

	static String removeWhitespaceFromSql(String sql) {
		sql.replaceAll("\\n", "");
		sql = sql.replaceAll("\\r", "");
		sql = sql.replaceFirst("\\s+", "");
		sql = sql.replaceFirst("\\t+", " ");
		sql = sql.replaceFirst("\\t+$", "");
		sql = sql.replaceAll("\\t", " ");
		return sql;
	}

	static Object getObjectByColumnLabel(final ColumnMetaData column, final ResultSet resultSet)
			throws SQLException {
		if (column.getColumnType() == Types.DATE && column.getColumnTypeName().equals("YEAR")) {
			return new Integer(resultSet.getInt(column.getColumnLabel()));
		} else {
			return resultSet.getObject(column.getColumnLabel());
		}
	}

	static Object getObjectByColumnNumber(final ColumnMetaData column, final ResultSet resultSet)
			throws SQLException {
		if (column.getColumnType() == Types.DATE && column.getColumnTypeName().equals("YEAR")) {
			return new Integer(resultSet.getInt(column.getColumnNumber()));
		} else {
			return resultSet.getObject(column.getColumnNumber());
		}
	}
}
