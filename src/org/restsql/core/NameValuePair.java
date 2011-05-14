/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;


/**
 * Encapsulates name and value for collections of request identifiers and parameters.
 * 
 * @author Mark Sawers
 * @see Request
 */
public class NameValuePair {
	private String name, value;

	/** Creates object. */
	public NameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/** Returns name. */
	public String getName() {
		return name;
	}

	/** Returns value. */
	public String getValue() {
		return value;
	}
}
