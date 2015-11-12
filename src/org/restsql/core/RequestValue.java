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
public class RequestValue {

	/** Parses list of comma separated values from a string. */
	public static List<String> parseInValues(final Object value) {
		final List<String> list = new ArrayList<String>();
		if (value instanceof String) {
			final String values = ((String) value).substring(1, ((String) value).length() - 1);
			final StringTokenizer tokenizer = new StringTokenizer(values, ",");
			String lastValue = null;
			while (tokenizer.hasMoreTokens()) {
				final String token = tokenizer.nextToken();
				if (lastValue != null && lastValue.charAt(lastValue.length() - 1) == '\\') {
					// Was an escaped delimiter so strip escape and append this token to it
					final String newValue = lastValue.substring(0, lastValue.length() - 1) + "," + token;
					list.set(list.size() - 1, newValue);
					lastValue = newValue;
				} else {
					list.add(token);
					lastValue = token;
				}
			}
		}

		return list;
	}

	/**
	 * Parses operator from beginning of value (<, <=, > or >=) or enclosing brackets for the In operator. If an escaped
	 * comparison operator is found, it returns Escaped operator. If no operator is found, it returns Equal.
	 */
	public static Operator parseOperatorFromValue(final Object value) {
		Operator operator;
		if (value == null) {
			operator = Operator.IsNull;
		} else {
			operator = Operator.Equals;
			if (value instanceof String) {
				final String stringValue = (String) value;
				if (stringValue.equalsIgnoreCase("null")) {
					operator = Operator.IsNull;
				} else if (stringValue.equalsIgnoreCase("!null")) {
					operator = Operator.IsNotNull;
				} else if (stringValue.equalsIgnoreCase("\\null")
						|| (stringValue.length() > 2 && stringValue.substring(0, 2).equals("\\!"))) {
					operator = Operator.Escaped;
				} else if (stringValue.length() > 0) {
					if (stringValue.charAt(0) == '!') {
						operator = Operator.NotEquals;
					} else if (stringValue.charAt(0) == '<') {
						if (stringValue.charAt(1) == '=') {
							operator = Operator.LessThanOrEqualTo;
						} else {
							operator = Operator.LessThan;
						}
					} else if (stringValue.charAt(0) == '>') {
						if (stringValue.charAt(1) == '=') {
							operator = Operator.GreaterThanOrEqualTo;
						} else {
							operator = Operator.GreaterThan;
						}
					} else if (stringValue.charAt(0) == '('
							&& stringValue.charAt(stringValue.length() - 1) == ')') {
						operator = Operator.In;
					} else if (stringValue.charAt(0) == '\\'
							&& stringValue.length() > 2
							&& (stringValue.charAt(1) == '<' || stringValue.charAt(1) == '>' || stringValue
									.charAt(1) == '(')) {
						operator = Operator.Escaped;
					}
				}
			}
		}
		return operator;
	}

	/** Returns value without leading operator. */
	public static Object stripOperatorFromValue(final Operator operator, final Object value) {
		if (value instanceof String) {
			switch (operator) {
			// Null it out
				case IsNull:
				case IsNotNull:
					return null;

					// Strip first char
				case Escaped:
				case LessThan:
				case GreaterThan:
				case NotEquals:
					return ((String) value).substring(1);

					// Strip first two chars
				case LessThanOrEqualTo:
				case GreaterThanOrEqualTo:
					return ((String) value).substring(2);

				default: // case Equals, In
					return value;
			}
		} else {
			return value;
		}
	}

	private List<String> inValues;
	private final String name;
	private Operator operator;
	private Object value; // this is non-final as Strings will be converted to Numeric object types per the column type

	/** Creates object, parsing value for comparison operator. */
	public RequestValue(final String name, final Object value) {
		this.name = name;
		operator = parseOperatorFromValue(value);
		this.value = stripOperatorFromValue(operator, value);
		if (operator == Operator.Escaped) {
			operator = Operator.Equals;
		} else if (operator == Operator.In) {
			inValues = parseInValues(this.value);
		}
	}

	/** Creates object with equals operator. Used for request resource identifiers and body object attributes. */
	public RequestValue(final String name, final Object value, final Operator operator) {
		this.name = name;
		this.value = value;
		this.operator = operator;
	}

	/** Returns true if the names, values and operators are equal. */
	@Override
	public boolean equals(final Object o) {
		return ((RequestValue) o).getName().equals(name) && ((RequestValue) o).getValue().equals(value)
				&& ((RequestValue) o).getOperator().equals(operator);
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
	public Object getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return name.hashCode() + value.hashCode();
	}

	/** Sets value. */
	public void setValue(final Object value) {
		this.value = value;
	}

	/** Returns string representation. */
	@Override
	public String toString() {
		return name + ": " + value;
	}

	/** Represents all operations for parameters. Note Escaped is for internal use only. */
	public static enum Operator {
		Equals, Escaped, GreaterThan, GreaterThanOrEqualTo, In, IsNull, IsNotNull, LessThan,
		LessThanOrEqualTo, NotEquals;
	}
}
