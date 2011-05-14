/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.restsql.core.Config;
import org.restsql.core.Factory.ConnectionFactory;

/**
 * Creates jdbc connections using database.xxx properties in the database.properties file.
 * 
 * @author Mark Sawers
 */
public class ConnectionFactoryImpl implements ConnectionFactory {
	public static final String KEY_DATABASE_USER = "database.user";
	public static final String KEY_DATABASE_URL = "database.url";
	public static final String KEY_DATABASE_PASSWORD = "database.password";
	public static final String DEFAULT_DATABASE_USER = "root";
	public static final String DEFAULT_DATABASE_URL = "jdbc:mysql://localhost:3306/";
	public static final String DEFAULT_DATABASE_PASSWORD = "root";

	private String url, user, password;
	private Properties properties = new Properties();

	public ConnectionFactoryImpl() {
		loadProperties();
		url = properties.getProperty(KEY_DATABASE_URL, DEFAULT_DATABASE_URL);
		user = properties.getProperty(KEY_DATABASE_USER, DEFAULT_DATABASE_USER);
		password = properties.getProperty(KEY_DATABASE_PASSWORD, DEFAULT_DATABASE_PASSWORD);
	}

	public Connection getConnection(String defaultDatabase) throws SQLException {
		Connection connection = DriverManager.getConnection(url, user, password);
		if (defaultDatabase != null) {
			connection.setCatalog(defaultDatabase);
		}
		return connection;
	}

	private void loadProperties() {
		String fileName = Config.properties.getProperty(Config.KEY_DATABASE_CONFIG,
				Config.DEFAULT_DATABASE_CONFIG);
		InputStream inputStream = null;
		try {
			final File file = new File(fileName);
			if (file.exists()) {
				inputStream = new FileInputStream(file);
			} else {
				inputStream = Config.class.getResourceAsStream(fileName);
			}
			properties.load(inputStream);
			if (Config.logger.isInfoEnabled()) {
				Config.logger.info("Loaded database properties from " + fileName);
			}
		} catch (final Exception exception) {
			Config.logger.error("Error loading database properties file " + fileName, exception);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException exception) {
					Config.logger.warn("Exception closing database properties file " + fileName, exception);
				}
			}
		}
	}
}
