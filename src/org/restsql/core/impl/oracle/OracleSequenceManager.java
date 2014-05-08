/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.restsql.core.SqlResourceException;
import org.restsql.core.impl.AbstractSequenceManager;

/**
 * SequenceManager implementation for Oracle Database
 * 
 * @author Piotr Roznicki
 */


public class OracleSequenceManager extends AbstractSequenceManager {

	@Override
	public String getCurrentValueSql(String sequenceName) {
		return "SELECT " + sequenceName + ".CURRVAL FROM DUAL";
	}

	@Override
	public void setNextValue(final Connection connection, final String table, final String sequenceName,
			final int nextval, boolean printAction) throws SqlResourceException {

		final String sql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
		Statement statement = null;
		try {
			statement = connection.createStatement();
			if (printAction) {
				System.out.println("\t[setUp] " + sql);
			}
			statement.executeQuery(sql);
		} catch (final SQLException exception) {
			throw new SqlResourceException(exception, sql);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
				}
			}
		}
	}
}
