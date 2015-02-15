/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service.monitoring;

import java.util.Set;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * Manages performance and health monitoring.
 * 
 * @author Mark Sawers
 */
public interface MonitoringManager {

	/** Used by application to get singletons for Jersey. */
	public abstract Set<Object> getApplicationSingletons();

	public abstract HealthCheckRegistry getHealthCheckRegistry();

	public abstract MetricRegistry getMetricRegistry();

	/** Initializes metrics. */
	public abstract void init();

	/** Creates new counter. */
	public abstract Counter newCounter(@SuppressWarnings("rawtypes") Class clazz, String name);

	/** Creates new timer. */
	public abstract Timer newTimer(@SuppressWarnings("rawtypes") Class clazz, String name);

}