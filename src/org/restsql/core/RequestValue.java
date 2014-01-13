/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.sql.Types;
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
	public static List<String> parseInValues(final String value) {
		final List<String> list = new ArrayList<String>();
		final String values = value.substring(1, value.length() - 1);
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
	public RequestValue(final String name, final String value) {
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
	public RequestValue(final String name, final String value, final Operator operator) {
		this.name = name;
		this.value = value;
		this.operator = Operator.Equals;
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

	/**
	 * Builds response value from request value with column metadata, converting String value to Object if appropriate.
	 * 
	 * @param column column metadata
	 * @return response value
	 */
	public ResponseValue getResponseValue(final ColumnMetaData column) {
		Object value;
		try {
			switch (column.getColumnType()) {
				case Types.BOOLEAN:
					value = Boolean.valueOf(this.value);
					break;

				case Types.BIT:
				case Types.DATE:			// JDBC driver regards MySQL YEAR type as Date
				case Types.INTEGER:
				case Types.NUMERIC:
				case Types.SMALLINT:
				case Types.TINYINT:
					value = Integer.valueOf(this.value);
					break;

				case Types.BIGINT:
					value = Long.valueOf(this.value);
					break;

				case Types.DECIMAL:
				case Types.FLOAT:
				case Types.REAL:
					value = Float.valueOf(this.value);
					break;

				case Types.DOUBLE:
					value = Double.valueOf(this.value);
					break;

				default:
					value = this.value;
			}
		} catch (NumberFormatException e) {
			if (column.getColumnType() != Types.DATE) {
				Config.logger.info("Could not convert " + toString() + " to number");
			}
			value = this.value;
		}

		return new ResponseValue(this.name, value, column.getColumnNumber());
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
		Equals, Escaped, GreaterThan, GreaterThanOrEqualTo, In, LessThan, LessThanOrEqualTo;
	}
}
