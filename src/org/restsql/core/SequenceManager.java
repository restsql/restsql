/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.sql.Connection;

/**
 * Contains functions to manage sequences.
 * 
 * @author Mark Sawers
 */
public interface SequenceManager {

	/** Retrieves current sequence value. */
	public int getCurrentValue(final Connection connection, String sequenceName) throws SqlResourceException;

	/** Resets sequence to desired value. */
	public void setNextValue(final Connection connection, final String table, final String sequenceName,
			final int nextval, boolean printAction) throws SqlResourceException;

}
