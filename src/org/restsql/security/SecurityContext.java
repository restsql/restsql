/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.security;

import java.security.Principal;

/**
 * Provides user principal and role check method. Mapped to the JAX-RS SecurityContext by the
 * <code>org.restsql.service.SecurityContextAdapter</code>.
 * 
 * @author Mark Sawers
 */
public interface SecurityContext {

	/**
	 * Returns a boolean indicating whether the authenticated user is included in the specified logical "role".
	 * 
	 * @param roleName a String specifying the name of the role
	 * @return a boolean indicating whether the user making the request belongs to a given role; false if the user has not been authenticated
	 */
	public boolean isUserInRole(String roleName);
	
	/**
	 * Returns a java.security.Principal object containing the name of the current authenticated user. If the user has not been authenticated, the method returns null.
	 * 
	 * @return a java.security.Principal containing the name of the user making this request; null if the user has not been authenticated
	 */
	public Principal getUserPrincipal();
}
