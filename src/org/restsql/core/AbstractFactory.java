/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for generic factories.
 * 
 * @author Mark Sawers
 */
public abstract class AbstractFactory {

	private static final Map<String, FactoryHelper> helpers = new HashMap<String, FactoryHelper>();

	/** Returns singleton instance of the interface implementation, creating it on first access in a thread safe way. */
	public static Object getInstance(final String interfaceName, final String defaultImpl) {
		Object object = null;
		FactoryHelper helper = helpers.get(interfaceName);
		if (helper == null) {
			synchronized (helpers) {
				helper = helpers.get(interfaceName);
				if (helper == null) {
					object = newInstance(interfaceName, defaultImpl);
				}
			}
		}
		if (object == null) {
			object = helper.getInstance();
		}
		return object;
	}

	/** Returns new instance of the interface implementation. */
	public static Object newInstance(final String interfaceName, final String defaultImpl) {
		FactoryHelper helper = helpers.get(interfaceName);
		if (helper == null) {
			helper = new FactoryHelper(interfaceName, defaultImpl);
			helpers.put(interfaceName, helper);
		}
		return helper.newInstance();
	}

	/** Helps lookup implementation configuration, load the class and construct an instance. */
	public static class FactoryHelper {
		private String implName;
		private Object instance;
		private final String interfaceName;

		public FactoryHelper(final String interfaceName, final String defaultImpl) {
			this.interfaceName = interfaceName;
			this.implName = Config.properties.getProperty(interfaceName, defaultImpl);
		}

		public String getImplName() {
			return implName;
		}

		public Object getInstance() {
			if (instance == null) {
				instance = newInstance();
			}
			return instance;
		}

		public String getInterfaceName() {
			return interfaceName;
		}

		public Object newInstance() {
			try {
				instance = Class.forName(implName).newInstance();
				return instance;
			} catch (final Exception exception) {
				throw new RuntimeException("Error loading " + interfaceName + " implementation " + implName,
						exception);
			}
		}
	}

}
