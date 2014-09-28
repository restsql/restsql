/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.restsql.core.impl.ColumnMetaDataImpl;

/**
 * MySQL specific implementation.
 * 
 * @author Mark Sawers
 */
public class MySqlColumnMetaData extends ColumnMetaDataImpl {

	/**
	 * Compensates for the MySQL JDBC driver which returns Date object for YEAR type. Instead forces it to an INTEGER
	 * type. The documented Connection property to change this behavior does not work.
	 */
	@Override
	public int getColumnType(final int columnType, final String columnTypeName) {
		if (columnType == Types.DATE && columnTypeName.equals("YEAR")) {
			return Types.INTEGER;
		} else {
			return columnType;
		}
	}

	/**
	 * Compensates for the MySQL JDBC driver which returns Date object for YEAR type. Instead get the value as in int
	 * and create an Integer. The documented Connection property to change this behavior does not work.
	 */
	@Override
	public Object getResultByLabel(final ResultSet resultSet) throws SQLException {
		Object value = null;
		if (getColumnType() == Types.INTEGER) {
			// Help out MySQL JDBC driver which returns Date object for YEAR type
			value = new Integer(resultSet.getInt(getQualifiedColumnLabel()));
			if (resultSet.wasNull()) {
				value = null;
			}
		} else {
			value = super.getResultByLabel(resultSet);
		}
		return value;
	}

	/**
	 * Compensates for the MySQL JDBC driver which returns Date object for YEAR type. Instead get the value as in int
	 * and create an Integer. The documented Connection property to change this behavior does not work.
	 */
	@Override
	public Object getResultByNumber(final ResultSet resultSet) throws SQLException {
		Object value = null;
		if (getColumnType() == Types.INTEGER) {
			value = new Integer(resultSet.getInt(getColumnNumber()));
			if (resultSet.wasNull()) {
				value = null;
			}
		} else {
			value = super.getResultByNumber(resultSet);
		}
		return value;
	}

}
