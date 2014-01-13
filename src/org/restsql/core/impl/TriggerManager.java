/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.restsql.core.Config;
import org.restsql.core.Request;
import org.restsql.core.SqlResourceException;
import org.restsql.core.Trigger;

/**
 * Manages trigger instantiation and execution. Triggers are defined in a properties file, whose location is referenced
 * in the restSQL properties file.
 * 
 * @author Mark Sawers
 */
public class TriggerManager {
	static final String TOKEN_WILDCARD = "*";
	private static final int DEFAULT_SUBLIST_SIZE = 5;

	// Map<sqlResource, List<Trigger>> -- trigger may repeat
	private static Map<String, List<Trigger>> triggers;

	/**
	 * Executes all trigger defined for the resource.
	 * 
	 * @param sqlResource SqlResource name
	 * @param request restSQL request
	 * @param before true if before execution; false if after execution
	 * @throws SqlResourceException if a trigger exception is thrown or database error occurs
	 */
	public static void executeTriggers(final String sqlResource, final Request request, final boolean before)
			throws SqlResourceException {

		// Load triggers if necessary
		if (triggers == null) {
			Properties definitions = getTriggerDefinitions();
			loadTriggers(definitions);
		}

		// Execute triggers scoped to this particulal SqlResource
		if (triggers.containsKey(sqlResource)) {
			for (final Trigger trigger : triggers.get(sqlResource)) {
				executeTrigger(request, trigger, before);
			}
		}

		// Execute triggers scoped to all SqlResources
		if (triggers.containsKey(TOKEN_WILDCARD)) {
			for (final Trigger trigger : triggers.get(TOKEN_WILDCARD)) {
				executeTrigger(request, trigger, before);
			}
		}
	}

	// Package-level utils, also for testing

	/**
	 * Adds trigger with the specified scope to the triggers data structure. Access is package-level to allow for
	 * testing.
	 */
	static void addTrigger(final Trigger trigger, final String sqlResources) {
		if (triggers == null) {
			triggers = new HashMap<String, List<Trigger>>();
		}

		if (sqlResources != null && sqlResources.length() > 0) {
			final StringTokenizer tokenizer = new StringTokenizer(sqlResources, ",");
			while (tokenizer.hasMoreTokens()) {
				final String sqlResource = tokenizer.nextToken();
				List<Trigger> subList = triggers.get(sqlResource);
				if (subList == null) {
					subList = new ArrayList<Trigger>(DEFAULT_SUBLIST_SIZE);
					triggers.put(sqlResource, subList);
				}
				subList.add(trigger);
			}
		}
	}

	/**
	 * Loads classes and creates objects and then populates the triggers data structure. Package level access for
	 * testability.
	 */
	@SuppressWarnings("unchecked")
	static void loadTriggers(final Properties definitions) {
		String triggersClasspath = Config.properties.getProperty(Config.KEY_TRIGGERS_CLASSPATH, null);
		if (Config.logger.isInfoEnabled()) {
			if (triggersClasspath != null) {
				Config.logger.info("Loading triggers from classpath " + triggersClasspath);
			} else {
				Config.logger.info("Loading triggers from system classpath");
			}
		}
		for (final String triggerClassName : definitions.stringPropertyNames()) {
			URLClassLoader classLoader = null;
			try {
				Class<Trigger> triggerClass;
				if (triggersClasspath != null) {
					File dir = new File(triggersClasspath);
					URL url = dir.toURI().toURL();
					URL[] urls = new URL[] { url };
					classLoader = new URLClassLoader(urls, Trigger.class.getClassLoader());
					triggerClass = (Class<Trigger>) classLoader.loadClass(triggerClassName);
				} else {
					triggerClass = (Class<Trigger>) Class.forName(triggerClassName);
				}

				final Trigger trigger = triggerClass.newInstance();
				addTrigger(trigger, definitions.getProperty(triggerClassName));
			} catch (final Exception exception) {
				Config.logger.error("Failed to load trigger " + triggerClassName, exception);
// If we were pure 1.7, then we could close the class loader
//			} finally {
//				if (classLoader != null) {
//					try {
//						classLoader.close();
//					} catch (Throwable t) {
//					}
//				}
			}
		}
		if (triggers == null) {
			triggers = new HashMap<String, List<Trigger>>();
		}
	}

	// Private utils

	/**
	 * Executes trigger method appropriate for the request.
	 */
	private static void executeTrigger(final Request request, final Trigger trigger, final boolean before)
			throws SqlResourceException {
		switch (request.getType()) {
			case SELECT:
				if (before) {
					trigger.beforeSelect(request);
				} else {
					trigger.afterSelect(request);
				}
				break;
			case INSERT:
				if (before) {
					trigger.beforeInsert(request);
				} else {
					trigger.afterInsert(request);
				}
				break;
			case UPDATE:
				if (before) {
					trigger.beforeUpdate(request);
				} else {
					trigger.afterUpdate(request);
				}
				break;
			case DELETE:
				if (before) {
					trigger.beforeDelete(request);
				} else {
					trigger.afterDelete(request);
				}
				break;
		}
	}

	/**
	 * Loads trigger definition properties file and then .
	 */
	private static Properties getTriggerDefinitions() {
		final Properties definitions = new Properties();
		final String fileName = Config.properties.getProperty(Config.KEY_TRIGGERS_DEFINITION, null);
		if (fileName != null) {
			InputStream inputStream = null;
			try {
				final File file = new File(fileName);
				if (file.exists()) {
					inputStream = new FileInputStream(file);
				} else {
					inputStream = Config.class.getResourceAsStream(fileName);
				}
				definitions.load(inputStream);
				if (Config.logger.isInfoEnabled()) {
					Config.logger.info("Loading trigger definitions from " + fileName);
				}
			} catch (final Exception exception) {
				Config.logger.error("Failed to load trigger definitions " + fileName, exception);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (Throwable t) {
					}
				}
			}

		}
		return definitions;
	}
}
