/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service.monitoring;


import org.restsql.core.AbstractFactory;
import org.restsql.core.Config;

/**
 * Constructs monitoring implementations. Use restsql properties to specify the implementation class names.
 * 
 * @author Mark Sawers
 */
public class MonitoringFactory extends AbstractFactory {
	
	/** Returns MonitoringManager singleton. */
	public static MonitoringManager getMonitoringManager() {
		return (MonitoringManager) getInstance(Config.KEY_MONITORING_MANAGER, Config.DEFAULT_MONITORING_MANAGER);
	}

	/** Returns configured MonitoringManager class name. */
	public static String getMonitoringManagerClass() {
		return Config.properties.getProperty(Config.KEY_MONITORING_MANAGER, Config.DEFAULT_MONITORING_MANAGER);
	}

}