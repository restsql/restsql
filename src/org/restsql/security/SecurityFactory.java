/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.security;


import org.restsql.core.AbstractFactory;
import org.restsql.core.Config;

/**
 * Constructs security implementations. Use restsql properties to specify the implementation class names.
 * 
 * @author Mark Sawers
 */
public class SecurityFactory extends AbstractFactory {

	/** Returns authorizer singleton. */
	public static Authorizer getAuthorizer() {
		return (Authorizer) getInstance(Config.KEY_AUTHORIZER, Config.DEFAULT_AUTHORIZER);
	}

}
