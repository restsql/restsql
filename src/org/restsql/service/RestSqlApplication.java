/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.restsql.core.Config;
import org.restsql.service.monitoring.MonitoringFactory;

/**
 * Identifies JAX-RS resources through code, since the declarative Jersey scanner does not work with JBoss AS. 
 * 
 * @author Mark Sawers
 */
public class RestSqlApplication extends Application {
	/** Initializes metrics. */
	public RestSqlApplication() {
	}

	/** Configures all the resource classes for the app. */
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(ConfResource.class);
		classes.add(LogResource.class);
		classes.add(ResResource.class);
		classes.add(WadlResource.class);
		classes.add(ToolsResource.class);
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		try {
			return MonitoringFactory.getMonitoringManager().getApplicationSingletons();
		} catch (Throwable throwable) {
			Config.logger.error(String.format(
					"Error getting application singletons from monitoring manager [%s]",
					MonitoringFactory.getMonitoringManagerClass()), throwable);
			return new HashSet<Object>(0);
		}
	}
}