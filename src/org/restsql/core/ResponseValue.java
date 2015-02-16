/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Represents a response column name, value and column number, which may be placed into a set ordered by column number.
 * In a hierarchical resource, the children set is placed into a column with the name of the child table (or alias) and
 * a column number at the max integer value.
 * 
 * @author Mark Sawers
 */
public class ResponseValue implements Comparable<ResponseValue> {
	private final String name;
	private final Object value;
	private final int columnNumber;

	public ResponseValue(final String name, final Object value, final int columnNumber) {
		this.name = name;
		this.value = value;
		this.columnNumber = columnNumber;
	}

	@Override
	public int compareTo(ResponseValue value) {
		if (columnNumber < value.getColumnNumber()) {
			return -1;
		} else if (columnNumber > value.getColumnNumber()) {
			return 1;
		} else {
			return 0;
		}
	}

	/** Returns true if the names, values and column numbers are equal. */
	@Override
	public boolean equals(Object o) {
		return ((ResponseValue) o).getName().equals(name) && ((ResponseValue) o).getValue().equals(value)
				&& ((ResponseValue) o).getColumnNumber() == columnNumber;
	}

	/** Returns column number in the select clause in the SQL Resource definition query. */
	public int getColumnNumber() {
		return columnNumber;
	}

	/** Returns name. */
	public String getName() {
		return name;
	}

	/** Returns value. */
	public Object getValue() {
		return value;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() + value.hashCode();
	}

	/** Returns string representation in the form <code>name: value</code>. */
	@Override
	public String toString() {
		return name + ": " + value;
	}
}
