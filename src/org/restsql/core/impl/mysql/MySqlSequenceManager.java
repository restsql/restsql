/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.restsql.core.SqlResourceException;
import org.restsql.core.impl.AbstractSequenceManager;

/**
 * Sequence utilities for MySQL.
 * 
 * @author Mark Sawers
 */
public class MySqlSequenceManager extends AbstractSequenceManager {

	@Override
	public String getCurrentValueSql(String sequenceName) {
		return "SELECT last_insert_id()";
	}

	@Override
	public void setNextValue(final Connection connection, final String table, final String sequenceName,
			final int nextval, boolean printAction) throws SqlResourceException {
		final String sql = "ALTER TABLE " + table + " AUTO_INCREMENT = " + nextval;
		Statement statement = null;
		try {
			statement = connection.createStatement();
			if (printAction) {
				System.out.println("\t[setUp] " + sql);
			}
			statement.executeUpdate(sql);
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
