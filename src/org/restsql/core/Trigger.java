/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Provides a mechanism for executing non-trivial domain validation or business logic on all CRUD operations. Throwing
 * {@link InvalidRequestException} from a <code>beforeXxx()</code> will stop the transaction and return HTTP 400 Bad
 * Request to the client. Throwing {@link SqlResourceException} from a <code>beforeXxx()</code> or
 * <code>afterXxx()</code> will return HTTP 500 Internal Server Error to the client.
 * 
 * @author Mark Sawers
 * @see AbstractTrigger
 */
public interface Trigger {
	public void beforeSelect(Request request) throws InvalidRequestException, SqlResourceException;

	public void beforeInsert(Request request) throws InvalidRequestException, SqlResourceException;

	public void beforeUpdate(Request request) throws InvalidRequestException, SqlResourceException;

	public void beforeDelete(Request request) throws InvalidRequestException, SqlResourceException;

	public void afterSelect(Request request) throws SqlResourceException;

	public void afterInsert(Request request) throws SqlResourceException;

	public void afterUpdate(Request request) throws SqlResourceException;

	public void afterDelete(Request request) throws SqlResourceException;
}
