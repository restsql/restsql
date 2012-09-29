/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

/**
 * Encapsulates name and value for collections of request identifiers and parameters. Also models the operator for
 * parameters.
 * 
 * @author Mark Sawers
 * @see Request
 */
public class NameValuePair {
	/**
	 * Parses operator from beginning of value (<, <=, > or >=). If an escaped comparison operator is found, it removes
	 * the escape character. If no operator is found, it returns Equal.
	 */
	public static Operator parseOperatorFromValue(final String value) {
		Operator operator = Operator.Equals;
		if (value.charAt(0) == '<') {
			if (value.charAt(1) == '=') {
				operator = Operator.LessThanOrEqualTo;
			} else {
				operator = Operator.LessThan;
			}
		} else if (value.charAt(0) == '>') {
			if (value.charAt(1) == '=') {
				operator = Operator.GreaterThanOrEqualTo;
			} else {
				operator = Operator.GreaterThan;
			}
		} else if (value.charAt(0) == '\\' && (value.charAt(1) == '<' || value.charAt(1) == '>')) {
			operator = Operator.Escaped;
		}
		return operator;
	}

	/** Returns value without leading operator. */
	public static String stripOperatorFromValue(final Operator operator, final String value) {
		switch (operator) {
			// Strip first char
			case Escaped:
			case LessThan:
			case GreaterThan:
				return value.substring(1);

				// Strip first two chars
			case LessThanOrEqualTo:
			case GreaterThanOrEqualTo:
				return value.substring(2);

			default: // case Equals
				return value;
		}
	}

	private final String name, value;
	private Operator operator;

	/** Creates object, parsing value for comparison operator. */
	public NameValuePair(final String name, final String value) {
		this.name = name;
		this.operator = parseOperatorFromValue(value);
		this.value = stripOperatorFromValue(this.operator, value);
		if (this.operator == Operator.Escaped) {
			this.operator = Operator.Equals;
		}
	}

	/** Creates object with equals operator. Used for request resource identifiers and body object attributes. */
	public NameValuePair(final String name, final String value, final Operator operator) {
		this.name = name;
		this.value = value;
		this.operator = Operator.Equals;
	}

	/** Returns name. */
	public String getName() {
		return name;
	}

	public Operator getOperator() {
		return operator;
	}

	/** Returns value. */
	public String getValue() {
		return value;
	}

	/** Returns string representation. */
	@Override
	public String toString() {
		return name + ": " + value;
	}

	/** Represents all operations for parameters. Note Escaped is for internal use only. */
	public static enum Operator {
		Equals, Escaped, LessThan, LessThanOrEqualTo, GreaterThan, GreaterThanOrEqualTo;
	}
}
