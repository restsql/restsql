/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.restsql.core.Config;
import org.restsql.core.Factory.ConnectionFactory;

/**
 * Simple pooled connection factory that use connection pool. The caller must close the
 * connection but it is ony released to pool. The factory uses the database property pool - poll name.
 * 
 * @author Piotr Roznicki
 */
public class ConnectionPoolFactoryImpl implements ConnectionFactory {
	private final String connectionName;
	private Context	initialContext = null;
  private DataSource dataSource	= null;

	public ConnectionPoolFactoryImpl() {
		connectionName = Config.properties.getProperty("database.pool","DEFAULT");
	}

	public Connection getConnection(String defaultDatabase) throws SQLException {
		java.sql.Connection connection = null;
		try {
			if (null == this.initialContext) {
				this.initialContext = new InitialContext();
				Context envCtxA = (Context) this.initialContext.lookup("java:comp/env");
				this.dataSource = (DataSource) envCtxA.lookup(connectionName);
			} else {
				if (null == this.dataSource) {
					Context envCtxA = (Context) this.initialContext.lookup("java:comp/env");
					this.dataSource = (DataSource) envCtxA.lookup(connectionName);
				}
			}
			connection = this.dataSource.getConnection();
		} catch (Exception e) {
			throw (new SQLException("Error getting connection from pool[" + connectionName + "]" + e.getMessage()));
		}
		if (defaultDatabase != null) {
			connection.setCatalog(defaultDatabase);
		}
		return connection;
	}

	public void destroy() throws SQLException {
	}
}
