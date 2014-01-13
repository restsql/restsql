/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a restSQL write response containing the rows affected. For inserts, it includes the resulting row, with
 * any auto-numbered column(s) values (using the column's sequence function). The response values are ordered by column
 * number in a set. Child rows are placed in value named with the child table (or alias), in a list of set structure.
 * The object may contain multiple responses for HTTP requests that include bodies with multiple write operations.
 * 
 * @author Mark Sawers
 */
public class WriteResponse {
	private List<Set<ResponseValue>> rows;

	private int rowsAffected;

	/** Constructs response with empty requestResults sized for 1. For framework use. */
	public WriteResponse() {
	}

	/** Adds request result. For framework use. */
	public void addRow(final Set<ResponseValue> values) {
		if (rows == null) {
			rows = new ArrayList<Set<ResponseValue>>();
		}
		rows.add(values);
	}

	/** Adds to rows affected. For framework use. */
	public void addRowsAffected(final int rowsAffected) {
		this.rowsAffected += rowsAffected;
	}

	/** Appends contents of response. For framework use. */
	public void addWriteResponse(final WriteResponse response) {
		if (response.getRows() != null) {
			if (rows == null) {
				rows = new ArrayList<Set<ResponseValue>>();
			}
			rows.addAll(response.getRows());
		}
		rowsAffected += response.getRowsAffected();
	}

	/** Returns inserted results, otherwise empty (non-null). */
	public List<Set<ResponseValue>> getRows() {
		return rows;
	}

	/** Returns number of rows affected. */
	public int getRowsAffected() {
		return rowsAffected;
	}
}