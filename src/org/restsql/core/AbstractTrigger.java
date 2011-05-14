/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Contains no-op implementation of {@link Trigger} interface.
 * 
 * @author Mark Sawers
 */
public abstract class AbstractTrigger implements Trigger {

	@Override
	public void beforeSelect(Request request) throws InvalidRequestException, SqlResourceException {
	}

	@Override
	public void afterSelect(Request request) throws InvalidRequestException, SqlResourceException {
	}

	@Override
	public void afterInsert(Request request) throws InvalidRequestException, SqlResourceException {
	}

	@Override
	public void beforeInsert(Request request) throws InvalidRequestException, SqlResourceException {
	}

	@Override
	public void beforeUpdate(Request request) throws SqlResourceException {
	}

	@Override
	public void afterUpdate(Request request) throws SqlResourceException {
	}

	@Override
	public void afterDelete(Request request) throws SqlResourceException {
	}

	@Override
	public void beforeDelete(Request request) throws SqlResourceException {
	}
}

