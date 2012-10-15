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
		Object value = null;
		if (column.getColumnType() == Types.DATE && column.getColumnTypeName().equals("YEAR")) {
			value = new Integer(resultSet.getInt(column.getColumnLabel()));
			if (resultSet.wasNull()) {
				value = null;
			}
		} else {
			value = resultSet.getObject(column.getColumnLabel());
		}
		return value;
	}

	static Object getObjectByColumnNumber(final ColumnMetaData column, final ResultSet resultSet)
			throws SQLException {
		Object value = null;
		if (column.getColumnType() == Types.DATE && column.getColumnTypeName().equals("YEAR")) {
			value = new Integer(resultSet.getInt(column.getColumnNumber()));
			if (resultSet.wasNull()) {
				value = null;
			}
		} else {
			value = resultSet.getObject(column.getColumnNumber());
		}
		return value;
	}
}
