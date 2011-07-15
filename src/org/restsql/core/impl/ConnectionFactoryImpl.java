/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.restsql.core.Config;
import org.restsql.core.Factory.ConnectionFactory;

/**
 * Simple non-pooled connection factory that creates a new connection on every call. The caller must close the
 * connection. The factory uses the database properties in restsql's core properties for the JDBC connection.
 * 
 * @author Mark Sawers
 */
public class ConnectionFactoryImpl implements ConnectionFactory {
	private String url, user, password;

	public ConnectionFactoryImpl() {
		url = Config.properties.getProperty(Config.KEY_DATABASE_URL, Config.DEFAULT_DATABASE_URL);
		user = Config.properties.getProperty(Config.KEY_DATABASE_USER, Config.DEFAULT_DATABASE_USER);
		password = Config.properties.getProperty(Config.KEY_DATABASE_PASSWORD,
				Config.DEFAULT_DATABASE_PASSWORD);
	}

	public Connection getConnection(String defaultDatabase) throws SQLException {
		Connection connection = DriverManager.getConnection(url, user, password);
		if (defaultDatabase != null) {
			connection.setCatalog(defaultDatabase);
		}
		return connection;
	}
}
