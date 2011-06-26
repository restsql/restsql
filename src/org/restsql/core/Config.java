/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads properties from user-supplied file, defines property keys and defaults, and configures logging for framework.
 * 
 * @author Mark Sawers
 */
public class Config {

	public static final String DEFAULT_CONNECTION_FACTORY = "org.restsql.core.impl.ConnectionFactoryImpl";
	public static final String DEFAULT_DATABASE_CONFIG = "/resources/properties/default-database.properties";
	public static final String DEFAULT_JAVA_LOGGING_CONFIG = "resources/properties/default-logging.properties";
	public static final String DEFAULT_LOG4J_CONFIG = "resources/properties/default-log4j.properties";
	public static final String DEFAULT_LOGGING_DIR = "/var/log/restsql";
	public static final String DEFAULT_LOGGING_FACILITY = "log4j";
	public static final String DEFAULT_REQUEST_FACTORY = "org.restsql.core.impl.RequestFactoryImpl";
	public static final String DEFAULT_REQUEST_LOGGER_FACTORY = "org.restsql.core.impl.RequestLoggerFactoryImpl";
	public static final String DEFAULT_REQUEST_USE_XML_SCHEMA = "false";
	public static final String DEFAULT_RESPONSE_USE_XML_DIRECTIVE = "false";
	public static final String DEFAULT_RESPONSE_USE_XML_SCHEMA = "false";
	public static final String DEFAULT_RESTSQL_PROPERTIES = "/resources/properties/default-restsql.properties";
	public static final String DEFAULT_SQL_RESOURCE_FACTORY = "org.restsql.core.impl.SqlResourceFactoryImpl";
	public static final String DEFAULT_SQLRESOURCES_DIR = "/resources/xml/sqlresources";

	public static final String KEY_CONNECTION_FACTORY = "org.restsql.core.Factory.Connection";
	public static final String KEY_DATABASE_CONFIG = "database.config";
	public static final String KEY_JAVA_LOGGING_CONFIG = "java.util.logging.config.file";
	public static final String KEY_LOG4J_CONFIG = "log4j.configuration";
	public static final String KEY_LOGGING_DIR = "logging.dir";
	public static final String KEY_LOGGING_CONFIG = "logging.config";
	public static final String KEY_LOGGING_FACILITY = "logging.facility";
	public static final String KEY_REQUEST_FACTORY = "org.restsql.core.Factory.Request";
	public static final String KEY_REQUEST_LOGGER_FACTORY = "org.restsql.core.Factory.LoggerRequest";
	public static final String KEY_REQUEST_USE_XML_SCHEMA = "request.useXmlDirective";
	public static final String KEY_RESPONSE_USE_XML_DIRECTIVE = "response.useXmlDirective";
	public static final String KEY_RESPONSE_USE_XML_SCHEMA = "response.useXmlSchema";
	public static final String KEY_RESTSQL_PROPERTIES = "org.restsql.properties";
	public static final String KEY_SQL_RESOURCE_FACTORY = "org.restsql.core.Factory.SqlResource";
	public static final String KEY_SQLRESOURCES_DIR = "sqlresources.dir";
	public static final String KEY_TRIGGERS_CLASSPATH = "triggers.classpath";
	public static final String KEY_TRIGGERS_DEFINITION = "triggers.definition";

	/** The internal logger, for software troubleshooting **/
	public static Log logger;

	public static final String NAME_LOGGER_ACCESS = "org.restsql.access";
	public static final String NAME_LOGGER_ERROR = "org.restsql.error";
	public static final String NAME_LOGGER_INTERNAL = "org.restsql.internal";
	public static final String NAME_LOGGER_TRACE = "org.restsql.trace";

	/** Read-only version of configuration settings for other classes **/
	public static ImmutableProperties properties, loggingProperties;

	private static String loggingPropertiesFileContent;
	private static String loggingPropertiesFileName;
	private static String propertiesFileName;

	static {
		loadProperties();
	}

	// Public utils

	/** Returns string representation of all non-logging framework properties as name-value pairs. */
	public static String dumpConfig(final boolean includeDefaults) {
		final StringBuffer dump = new StringBuffer(1500);
		dump.append("Properties loaded from ");
		dump.append(propertiesFileName);
		dump.append(":\n");
		for (final Object key : properties.keySet()) {
			printProperty(dump, (String) key, properties.getProperty((String) key));
		}

		if (includeDefaults) {
			dump.append("\nProperties using defaults:\n");
			for (final Field field : Config.class.getFields()) {
				final String keyFieldName = field.getName();
				if (keyFieldName.startsWith("KEY")) {
					String keyFieldValue;
					try {
						keyFieldValue = (String) field.get(null);
						if (!properties.containsKey(keyFieldValue)) {
							try {
								final Field valueField = Config.class.getField("DEFAULT_"
										+ keyFieldName.substring(4));
								printProperty(dump, (String) field.get(null), (String) valueField.get(null));
							} catch (final Exception exception) {
							}
						}
					} catch (Exception exception) {
						logger.error("Error dumping config", exception); // this should never happen
					}
				}
			}
		}

		return dump.toString();
	}

	/** Returns string representation of all logging properties as name-value pairs. */
	public static String dumpLoggingConfig() {
		final StringBuffer dump = new StringBuffer(1500);
		dump.append(loggingPropertiesFileName);
		dump.append(":\n\n");
		dump.append(loggingPropertiesFileContent);
		return dump.toString();
	}

	/** Loads all properties. */
	public static void loadProperties() {
		if (properties == null) {
			loadProperties(System.getProperty(KEY_RESTSQL_PROPERTIES, DEFAULT_RESTSQL_PROPERTIES));
		}
	}

	// Private utils

	private static void configureLogging() {
		final String facility = properties.getProperty(KEY_LOGGING_FACILITY, DEFAULT_LOGGING_FACILITY);
		final String config;
		if (facility.equals("java")) {
			if (System.getProperty(KEY_JAVA_LOGGING_CONFIG) == null) {
				String javaConfig = properties.getProperty(KEY_LOGGING_CONFIG, DEFAULT_JAVA_LOGGING_CONFIG);
				System.setProperty(KEY_JAVA_LOGGING_CONFIG, javaConfig);
				System.setProperty("org.apache.commons.logging.Log",
						"org.apache.commons.logging.impl.Jdk14Logger");
			}
			config = System.getProperty(KEY_JAVA_LOGGING_CONFIG);
			properties.backingProperties.setProperty(KEY_JAVA_LOGGING_CONFIG, config);
		} else { // log4j
			if (System.getProperty(KEY_LOG4J_CONFIG) == null) {
				String log4jConfig = properties.getProperty(KEY_LOGGING_CONFIG, DEFAULT_LOG4J_CONFIG);
				System.setProperty(KEY_LOG4J_CONFIG, log4jConfig);
				System.setProperty("org.apache.commons.logging.Log",
						"org.apache.commons.logging.impl.Log4JLogger");
			}
			config = System.getProperty(KEY_LOG4J_CONFIG);
			properties.backingProperties.setProperty(KEY_LOG4J_CONFIG, config);
		}
		logger = LogFactory.getLog(NAME_LOGGER_INTERNAL);
		loadLoggingProperties(config);
	}

	private static void loadLoggingProperties(final String fileName) {
		loggingPropertiesFileName = "/" + fileName;
		loggingProperties = new ImmutableProperties();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
				loggingPropertiesFileName);
		if (inputStream != null) {
			try {
				loggingProperties.backingProperties.load(inputStream);
				loggingPropertiesFileContent = loggingProperties.toString();
			} catch (final Exception exception) {
				logger.error("Error loading properties file", exception);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (final IOException ignored) {
					}
				}
			}
		} else {
			logger.warn("Current thread's class loader could not locate logging conf file "
					+ loggingPropertiesFileName);
		}
	}

	private static void loadProperties(final String fileName) {
		propertiesFileName = fileName;
		properties = new ImmutableProperties();
		InputStream inputStream = null;
		try {
			final File file = new File(fileName);
			if (file.exists()) {
				inputStream = new FileInputStream(file);
			} else {
				inputStream = Config.class.getResourceAsStream(fileName);
			}
			properties.backingProperties.load(inputStream);

			configureLogging();

			if (logger.isInfoEnabled()) {
				logger.info("Loaded restsql properties from " + fileName);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(dumpConfig(false));
			}
		} catch (final Exception exception) {
			exception.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (final IOException ignored) {
				}
			}
		}
	}

	private static void printProperty(final StringBuffer string, final String key, final String value) {
		string.append("\t");
		string.append(key);
		string.append(" = ");
		string.append(value);
		string.append("\n");
	}

	/** Wraps a java.util.Properties, exposing only the property getter. */
	public static class ImmutableProperties {
		Properties backingProperties = new Properties();

		public boolean containsKey(final String key) {
			return backingProperties.containsKey(key);
		}

		public String getProperty(final String key, final String defaultValue) {
			return backingProperties.getProperty(key, defaultValue);
		}

		private String getProperty(final String key) {
			return backingProperties.getProperty(key);
		}

		public String toString() {
			StringBuffer buffer = new StringBuffer(1000);
			for (Object key : backingProperties.keySet()) {
				printProperty(buffer, (String) key, backingProperties.getProperty((String) key));
			}
			return buffer.toString();
		}

		private Set<Object> keySet() {
			return backingProperties.keySet();
		}
	}
}
