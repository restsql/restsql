/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.tools;

import org.restsql.core.AbstractFactory;
import org.restsql.core.Config;

/**
 * Factory for tools objects.
 * 
 * @author Mark Sawers
 */
public class ToolsFactory extends AbstractFactory {

	/** Returns authorizer singleton. */
	public static ResourceDefinitionGenerator getResourceDefinitionGenerator() {
		return (ResourceDefinitionGenerator) getInstance(Config.KEY_RESOURCE_DEFINTION_GENERATOR, Config.DEFAULT_RESOURCE_DEFINITION_GENERATOR);
	}
}
