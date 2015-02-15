/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service.monitoring;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

/** Enables Metrics AdminServlet to serve up metrics collected by restSQL service. */
public class MetricsServletContextListener extends MetricsServlet.ContextListener {
	@Override
	protected MetricRegistry getMetricRegistry() {
		return MonitoringFactory.getMonitoringManager().getMetricRegistry();
	}
}