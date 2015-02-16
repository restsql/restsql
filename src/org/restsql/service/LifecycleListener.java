/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.sql.SQLException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.service.monitoring.MonitoringFactory;
import org.restsql.service.monitoring.MonitoringManager;

/**
 * Loads properties file from servlet context (web.xml), or attempts to load from the system property and failing that
 * loads from the default. Also initializes the {@link MonitoringManager}.
 * 
 * @author Mark Sawers
 */
public class LifecycleListener implements ServletContextListener {
	private static final String KEY_STARTUP_LOGGING_CONSOLE_ENABLED = "org.restsql.startupLogging.consoleEnabled";
	
	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent event) {
		System.setProperty(KEY_STARTUP_LOGGING_CONSOLE_ENABLED, "true");

		String value = event.getServletContext().getInitParameter(Config.KEY_RESTSQL_PROPERTIES);
		if (value == null) {
			value = System.getProperty(Config.KEY_RESTSQL_PROPERTIES, Config.DEFAULT_RESTSQL_PROPERTIES);
		}
		System.setProperty(Config.KEY_RESTSQL_PROPERTIES, value);
		Config.loadAllProperties();

		try {
			MonitoringFactory.getMonitoringManager().init();
		} catch (Throwable throwable) {
			Config.logger.error(
					String.format("Error initializing monitoring manager [%s]",
							MonitoringFactory.getMonitoringManagerClass()), throwable);
		}
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			Factory.getConnectionFactory().destroy();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

}
