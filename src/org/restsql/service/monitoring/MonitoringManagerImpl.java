/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service.monitoring;

import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.restsql.core.Config;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.ganglia.GangliaReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;

/**
 * Initializes performance and health monitoring.
 * 
 * @author Mark Sawers
 */
public class MonitoringManagerImpl implements MonitoringManager {

	/** Define metric and health check registries. */
	public final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
	public final MetricRegistry metricRegistry = new MetricRegistry();

	@Override
	public Set<Object> getApplicationSingletons() {
		Set<Object> singletons = new HashSet<Object>(1);
		singletons.add(new InstrumentedResourceMethodDispatchAdapter(metricRegistry));
		return singletons;
	}

	@Override
	public HealthCheckRegistry getHealthCheckRegistry() {
		return healthCheckRegistry;
	}

	@Override
	public MetricRegistry getMetricRegistry() {
		return metricRegistry;
	}

	@Override
	public void init() {
		initHealthCheck();
		initJmx();
		initGanglia();
		initGraphite();
	}

	@Override
	public Counter newCounter(@SuppressWarnings("rawtypes") final Class clazz, final String name) {
		return metricRegistry.counter(MetricRegistry.name(clazz, name));
	}

	@Override
	public Timer newTimer(@SuppressWarnings("rawtypes") final Class clazz, final String name) {
		return metricRegistry.timer(MetricRegistry.name(clazz, name));
	}

	/** Initializes Ganglia Reporter if configured. */
	protected void initGanglia() {
		if (Config.properties.containsKey(Config.KEY_MONITORING_GANGLIA_HOST)) {
			final String host = Config.properties.getProperty(Config.KEY_MONITORING_GANGLIA_HOST,
					"configureme");
			final String port = Config.properties.getProperty(Config.KEY_MONITORING_GANGLIA_PORT,
					Config.DEFAULT_MONITORING_GANGLIA_PORT);
			final String udpMode = Config.properties.getProperty(Config.KEY_MONITORING_GANGLIA_UDP_MODE,
					Config.DEFAULT_MONITORING_GANGLIA_UDP_MODE);
			final String ttl = Config.properties.getProperty(Config.KEY_MONITORING_GANGLIA_TTL,
					Config.DEFAULT_MONITORING_GANGLIA_TTL);
			final String frequency = Config.properties.getProperty(Config.KEY_MONITORING_GANGLIA_FREQUENCY,
					Config.DEFAULT_MONITORING_GANGLIA_FREQUENCY);
			final String message = String.format(
					"Ganglia reporter [host=%s, port=%s, udpMode=%s, ttl=%s, frequency=%s]", host, port,
					udpMode, ttl, frequency);

			try {
				final GMetric ganglia = new GMetric(
						host,
						Integer.parseInt(port),
						udpMode.equalsIgnoreCase(Config.DEFAULT_MONITORING_GANGLIA_UDP_MODE) ? UDPAddressingMode.UNICAST
								: UDPAddressingMode.MULTICAST, Integer.parseInt(ttl));
				final GangliaReporter reporter = GangliaReporter.forRegistry(metricRegistry)
						.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
						.build(ganglia);
				reporter.start(Integer.parseInt(frequency), TimeUnit.MINUTES);
				Config.logger.info(String.format("%s initialized %s", MonitoringManagerImpl.class.getName(), message));
			} catch (final Exception exception) {
				Config.logger.error(
						String.format("%s error initializing %s", MonitoringManagerImpl.class.getName(), message),
						exception);
				System.out.println(String.format(
						"ERROR: %s error initializing %s. See restsql internal log file for details.",
						MonitoringManagerImpl.class.getName(), message));
			}
		}
	}

	/**
	 * Initializes Graphite reporter if configured.
	 */
	protected void initGraphite() {
		if (Config.properties.containsKey(Config.KEY_MONITORING_GRAPHITE_HOST)) {
			final String host = Config.properties.getProperty(Config.KEY_MONITORING_GRAPHITE_HOST,
					"configureme");
			final String port = Config.properties.getProperty(Config.KEY_MONITORING_GRAPHITE_PORT,
					"configureme");
			final String prefix = Config.properties.getProperty(Config.KEY_MONITORING_GRAPHITE_PREFIX, "");
			final String frequency = Config.properties.getProperty(Config.KEY_MONITORING_GRAPHITE_FREQUENCY,
					Config.DEFAULT_MONITORING_GRAPHITE_FREQUNCY);
			final String message = String.format(
					"Graphite reporter [host=%s, port=%s, prefix=%s, frequency=%s]", host, port, prefix,
					frequency);
			try {
				final Graphite graphite = new Graphite(new InetSocketAddress(host, Integer.parseInt(port)));
				final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
						.prefixedWith(prefix).convertRatesTo(TimeUnit.SECONDS)
						.convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(graphite);
				reporter.start(Integer.parseInt(frequency), TimeUnit.MINUTES);
				Config.logger.info(String.format("%s initialized %s", MonitoringManagerImpl.class.getName(), message));
			} catch (final Exception exception) {
				Config.logger.error(
						String.format("%s error initializing %s", MonitoringManagerImpl.class.getName(), message),
						exception);
				System.out.println(String.format(
						"ERROR: %s error initializing %s. See restsql internal log file for details.",
						MonitoringManagerImpl.class.getName(), message));
			}
		}
	}

	/** Initializes health check. */
	protected void initHealthCheck() {
		healthCheckRegistry.register("database.connection", new DatabaseConnectionHealthCheck());
		if (healthCheckRegistry.runHealthCheck("database.connection").isHealthy()) {
			Config.logger.info(String.format(
					"%s Initialized database connection health check and first check is healthy",
					MonitoringManagerImpl.class.getName()));
		} else {
			String message = String.format(
					"%s Initialized database connection health check and first check is unhealthy! See restsql internal log file for details.",
					MonitoringManagerImpl.class.getName());
			Config.logger.error(message);
			System.out.println(String.format("ERROR: %s", message));
		}
	}

	/** Initializes JMX Reporter if configured. */
	protected void initJmx() {
		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
		reporter.start();
		Config.logger.info(String.format("%s Initialized JMX reporter", MonitoringManagerImpl.class.getName()));
	}
}