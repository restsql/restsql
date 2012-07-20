/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.ResponseSerializer;
import org.restsql.core.SqlResource;

/**
 * Converts read/write results to a JSON string.
 * 
 * @author Mark Sawers
 */
public class JsonResponseSerializer implements ResponseSerializer {

	@Override
	public String getSupportedMediaType() {
		return "application/json";
	}
	
	/**
	 * Converts flat select results to a JSON array.
	 * 
	 * @param sqlResource SQL resource
	 * @param resultSet results
	 * @return JSON string
	 */
	public String serializeReadFlat(final SqlResource sqlResource, final ResultSet resultSet)
			throws SQLException {
		final StringBuilder string = new StringBuilder(1000);
		appendReadDocStart(sqlResource, string);
		int rowCount = 0;
		while (resultSet.next()) {
			if (rowCount > 0) {
				string.append(",");
			}
			rowCount++;
			string.append("\n\t\t{ ");
			final List<ColumnMetaData> columns = sqlResource.getMetaData().getAllReadColumns();
			final int columnSize = columns.size();
			for (int i = 0; i < columnSize; i++) {
				final ColumnMetaData column = columns.get(i);
				if (!column.isNonqueriedForeignKey()) {
					appendNameValuePair(i == 0, string, column.getColumnLabel(), SqlUtils
							.getObjectByColumnNumber(column, resultSet));
				}
			}
			string.append(" }");
		}
		appendReadDocEnd(string, rowCount == 0);
		return string.toString();

	}

	/**
	 * Converts hierarchical select results to a JSON array.
	 * 
	 * @param sqlResource SQL resource
	 * @param results results
	 * @return JSON string
	 */
	public String serializeReadHierarchical(final SqlResource sqlResource,
			final List<Map<String, Object>> results) {
		final StringBuilder string = new StringBuilder(results.size() * 100);
		appendReadDocStart(sqlResource, string);
		serializeHierarchicalRows(sqlResource, results, string, 1);
		appendReadDocEnd(string, results.size() == 0);
		return string.toString();
	}

	/**
	 * Converts write results to a JSON object.
	 * 
	 * @param rowsAffected rows affected
	 */
	public String serializeWrite(final int rowsAffected) {
		final StringBuilder string = new StringBuilder(250);
		string.append("{ ");
		appendNameValuePair(true, string, "rowsAffected", String.valueOf(rowsAffected));
		string.append(" }");
		return string.toString();
	}

	// Private utils

	private void appendNameValuePair(final boolean firstPair, final StringBuilder string, final String name,
			final Object value) {
		if (value != null) {
			if (!firstPair) {
				string.append(", ");
			}
			string.append(JsonUtil.quote(name));
			string.append(": ");
			string.append(JsonUtil.quote(value.toString()));
		}
	}

	private void appendReadDocEnd(final StringBuilder string, boolean emptyResults) {
		if (emptyResults) {
			string.append("] }");
		} else {
			string.append("\n\t]\n}");
		}
	}

	private void appendReadDocStart(final SqlResource sqlResource, final StringBuilder string) {
		string.append("{ \"");
		string.append(sqlResource.getMetaData().getParent().getTableAlias());
		string.append("s\": [");
	}

	/** One-level recursive method to serialize hierarchical results. */
	@SuppressWarnings("unchecked")
	private void serializeHierarchicalRows(final SqlResource sqlResource,
			final List<Map<String, Object>> rows, final StringBuilder string, final int level) {
		final int rowSize = rows.size();
		for (int i = 0; i < rowSize; i++) {
			final boolean lastRow = i == rowSize - 1;
			List<Map<String, Object>> childRows = null;
			final Map<String, Object> row = rows.get(i);
			if (level == 1) {
				string.append("\n\t\t{ ");
			} else {
				string.append("\n\t\t\t\t{ ");
			}

			// Do parent attribute columns
			int columnNumber = 0;
			for (final String columnLabel : row.keySet()) {
				columnNumber++;
				final Object value = row.get(columnLabel);
				if (!(value instanceof List<?>)) {
					appendNameValuePair(columnNumber == 1, string, columnLabel, value);
				} else {
					childRows = (List<Map<String, Object>>) value;
				}
			}

			// Do embedded child object columns
			if (level == 1 && childRows.size() > 0) {
				string.append(",\n\t\t\t\"");
				string.append(sqlResource.getMetaData().getChild().getTableAlias());
				string.append("s\": [");
				serializeHierarchicalRows(sqlResource, childRows, string, 2);
				string.append("\n\t\t\t]");
			}

			// Add line ending
			if (level == 1 && childRows.size() > 0) {
				string.append("\n\t\t}");
			} else {
				string.append(" }");
			}

			if (!lastRow) {
				string.append(",");
			}
		}
	}
}
