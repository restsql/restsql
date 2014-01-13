/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.restsql.core.SequenceManager;
import org.restsql.core.SqlResourceException;

/**
 * @author Mark Sawers
 */
public abstract class AbstractSequenceManager implements SequenceManager {

	@Override
	public int getCurrentValue(final Connection connection, String sequenceName) throws SqlResourceException {
		final String sql = getCurrentValueSql(sequenceName);
		Statement statement = null;
		try {
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			resultSet.next();
			return resultSet.getInt(1);
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

	public abstract String getCurrentValueSql(String sequenceName);
}
