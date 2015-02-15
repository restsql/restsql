/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service.monitoring;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

/** Enables Metrics AdminServlet to serve up health check data provided by restSQL service. */
public class HealthCheckServletContextListener extends HealthCheckServlet.ContextListener {
	@Override
	protected HealthCheckRegistry getHealthCheckRegistry() {
		return MonitoringFactory.getMonitoringManager().getHealthCheckRegistry();
	}
}