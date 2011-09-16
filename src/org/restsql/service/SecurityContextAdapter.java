/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.security.Principal;

import org.restsql.security.SecurityContext;


/**
 * Adapts JAX-RS's SecurityContext to restSQL's SecurityContext .
 * 
 * @author Mark Sawers
 */
public class SecurityContextAdapter implements SecurityContext {
	private javax.ws.rs.core.SecurityContext jaxRsContext;

	public SecurityContextAdapter(javax.ws.rs.core.SecurityContext jaxRsContext) {
		this.jaxRsContext = jaxRsContext;
	}
	
	public Principal getUserPrincipal() {
		return jaxRsContext.getUserPrincipal();
	}

	public boolean isUserInRole(String roleName) {
		return jaxRsContext.isUserInRole(roleName);
	}
}
