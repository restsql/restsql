/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.security;

import org.restsql.core.Request;

/**
 * Authorizes restSQL requests.
 * 
 * @author Mark Sawers
 */
public interface Authorizer {

	/** Returns true if authorization is enabled. */
	public boolean isAuthorizationEnabled();
	
	/**
	 * Checks if user is assigned a role that may perform the request.
	 * 
	 * @param context security context, usually container-provided
	 * @param request SQL Resource request
	 * @return true if user is authorized (or authorization is disabled), false otherwise
	 */
	public boolean isAuthorized(SecurityContext context, Request request);

	/**
	 * Checks if user is assigned a role that may perform the request.
	 * 
	 * @param context security context, usually container-provided
	 * @param requestType request type
	 * @param sqlResource SQL resource name
	 * @return true if user is authorized (or authorization is disabled), false otherwise
	 */
	public boolean isAuthorized(SecurityContext context, Request.Type requestType, String sqlResource);

	/**
	 * Returns implementation configuration, e.g. data store location as well as a string representation of all roles
	 * and associated privileges.
	 */
	public String dumpConfig();

}
