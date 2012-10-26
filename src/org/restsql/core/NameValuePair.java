/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Encapsulates name and value for collections of request identifiers and parameters. Also models the operator for
 * parameters.
 * 
 * @author Mark Sawers
 * @see Request
 */
public class NameValuePair {

	/** Parses list of comma separated values from a string. */
	public static List<String> parseInValues(final String value) {
		List<String> list = new ArrayList<String>();
		String values = value.substring(1, value.length() - 1);
		StringTokenizer tokenizer = new StringTokenizer(values, ",");
		String lastValue = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (lastValue != null && lastValue.charAt(lastValue.length() - 1) == '\\') {
				// Was an escaped delimiter so strip escape and append this token to it
				String newValue = lastValue.substring(0, lastValue.length() - 1) + "," + token;
				list.set(list.size() - 1, newValue);
				lastValue = newValue;
			} else {
				list.add(token);
				lastValue = token;
			}
		}
		
		return list;
	}

	/**
	 * Parses operator from beginning of value (<, <=, > or >=) or enclosing brackets for the In operator. If an escaped
	 * comparison operator is found, it returns Escaped operator. If no operator is found, it returns Equal.
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
		} else if (value.charAt(0) == '(' && value.charAt(value.length() - 1) == ')') {
			operator = Operator.In;
		} else if (value.charAt(0) == '\\' && value.length() > 2
				&& (value.charAt(1) == '<' || value.charAt(1) == '>' || value.charAt(1) == '(')) {
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

			default: // case Equals, In
				return value;
		}
	}

	private List<String> inValues;
	private final String name, value;
	private Operator operator;

	/** Creates object, parsing value for comparison operator. */
	public NameValuePair(final String name, final String value) {
		this.name = name;
		this.operator = parseOperatorFromValue(value);
		this.value = stripOperatorFromValue(this.operator, value);
		if (this.operator == Operator.Escaped) {
			this.operator = Operator.Equals;
		} else if (this.operator == Operator.In) {
			this.inValues = parseInValues(this.value);
		}
	}

	/** Creates object with equals operator. Used for request resource identifiers and body object attributes. */
	public NameValuePair(final String name, final String value, final Operator operator) {
		this.name = name;
		this.value = value;
		this.operator = Operator.Equals;
	}

	/** Returns the In list values. **/
	public List<String> getInValues() {
		return inValues;
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
		Equals, Escaped, In, LessThan, LessThanOrEqualTo, GreaterThan, GreaterThanOrEqualTo;
	}
}
