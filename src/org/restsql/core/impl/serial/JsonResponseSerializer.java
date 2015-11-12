/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.serial;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.ResponseSerializer;
import org.restsql.core.ResponseValue;
import org.restsql.core.SqlResource;
import org.restsql.core.WriteResponse;

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
	// TODO: move column get from metadata outside of result set while loop!!!
	@Override
	public String serializeReadFlat(final SqlResource sqlResource, final ResultSet resultSet)
			throws SQLException {
		final StringBuilder body = new StringBuilder(1000);
		int rowCount = 0;
		while (resultSet.next()) {
			if (rowCount > 0) {
				body.append(",");
			}
			rowCount++;
			body.append("\n\t\t{ ");
			final List<ColumnMetaData> columns = sqlResource.getMetaData().getAllReadColumns();
			boolean firstPair = true;
			for (ColumnMetaData column : columns) {
				if (!column.isNonqueriedForeignKey()) {
					Object value = column.getResultByNumber(resultSet);
					addAttribute(firstPair, body, column.getColumnLabel(), value);
					if (value != null) {
						firstPair = false;
					}
				}
			}
			body.append(" }");
		}
		return completeDoc(DocType.Read, sqlResource, null, body);
	}

	/**
	 * Converts hierarchical select results to a JSON array.
	 * 
	 * @param sqlResource SQL resource
	 * @param results results
	 * @return JSON string
	 */
	@Override
	public String serializeReadHierarchical(final SqlResource sqlResource,
			final List<Map<String, Object>> results) {
		final StringBuilder body = new StringBuilder(results.size() * 100);
		serializeReadRowsHierarchical(sqlResource, results, body, 1);
		return completeDoc(DocType.Read, sqlResource, null, body);
	}

	/**
	 * Converts write results to a JSON object.
	 * 
	 * @param response response
	 * @return XML doc
	 */
	@Override
	public String serializeWrite(final SqlResource sqlResource, final WriteResponse response) {
		StringBuilder body = null;
		if (response.getRows() != null) {
			body = new StringBuilder(response.getRows().size() * 100);
			serializeWriteRowsHierarchical(sqlResource, response.getRows(), body, 1);
		}
		return completeDoc(DocType.Write, sqlResource,
				new Object[] { "rowsAffected", response.getRowsAffected() }, body);
	}

	// Package level utils (for testability)

	void addAttribute(final boolean firstAttribute, final StringBuilder string, final String name,
			final Object value) {
		if (value != null) {
			if (!firstAttribute) {
				string.append(", ");
			}
			string.append(JsonUtil.quote(name));
			string.append(": ");
			if (value instanceof Number || value instanceof Boolean) {
				string.append(value);
			} else {
				string.append(JsonUtil.quote(value.toString()));
			}
		}
	}

	// Private utils

	private String completeDoc(final DocType docType, final SqlResource sqlResource,
			final Object[] attributes, final StringBuilder body) {
		int docLength = (body != null) ? body.length() + 250 : 250;
		StringBuilder doc = new StringBuilder(docLength);

		// Init doc
		if (docType == DocType.Read) {
			doc.append("{ \"");
			doc.append(sqlResource.getMetaData().getParent().getRowSetAlias());
			doc.append("\": [");
		} else { // DocType.Write
			doc.append("{ ");
		}

		// Add opening attributes
		if (attributes != null) {
			for (int i = 0; i < attributes.length; i += 2) {
				addAttribute(true, doc, String.valueOf(attributes[i]), attributes[i + 1]);
			}
		}

		// Close doc and insert the body, if non-empty
		if (body != null && body.length() > 0) {
			if (docType == DocType.Write) {
				if (attributes != null) {
					doc.append(",\n\t");
				}
				doc.append("\"");
				doc.append(sqlResource.getMetaData().getParent().getRowSetAlias());
				doc.append("\": [");
			}
			doc.append(body);
			doc.append("\n\t]\n}");
		} else {
			if (docType == DocType.Read) {
				doc.append("] }");
			} else { // DocType.Write
				doc.append(" }");
			}
		}

		return doc.toString();
	}

	/** One-level recursive method to serialize hierarchical results. */
	@SuppressWarnings("unchecked")
	private void serializeReadRowsHierarchical(final SqlResource sqlResource,
			final List<Map<String, Object>> rows, final StringBuilder body, final int level) {
		final int rowSize = rows.size();
		for (int i = 0; i < rowSize; i++) {
			final boolean lastRow = i == rowSize - 1;
			List<Map<String, Object>> childRows = null;
			final Map<String, Object> row = rows.get(i);
			if (level == 1) {
				body.append("\n\t\t{ ");
			} else {
				body.append("\n\t\t\t\t{ ");
			}

			// Do attribute columns
			boolean firstPair = true;
			for (final String columnLabel : row.keySet()) {
				final Object value = row.get(columnLabel);
				if (!(value instanceof List<?>)) {
					addAttribute(firstPair, body, columnLabel, value);
					if (value != null) {
						firstPair = false;
					}
				} else {
					childRows = (List<Map<String, Object>>) value;
				}
			}

			// Do embedded child object columns
			if (level == 1 && childRows.size() > 0) {
				body.append(",\n\t\t\t\"");
				body.append(sqlResource.getMetaData().getChild().getRowSetAlias());
				body.append("\": [");
				serializeReadRowsHierarchical(sqlResource, childRows, body, 2);
				body.append("\n\t\t\t]");
			}

			// Add line ending
			if (level == 1 && childRows.size() > 0) {
				body.append("\n\t\t}");
			} else {
				body.append(" }");
			}

			if (!lastRow) {
				body.append(",");
			}
		}
	}

	/** One-level recursive method to serialize hierarchical results. */
	@SuppressWarnings("unchecked")
	private void serializeWriteRowsHierarchical(final SqlResource sqlResource,
			final List<Set<ResponseValue>> rows, final StringBuilder body, final int level) {
		final int rowSize = rows.size();
		for (int i = 0; i < rowSize; i++) {
			final boolean lastRow = i == rowSize - 1;
			List<Set<ResponseValue>> childRows = null;
			final Set<ResponseValue> row = rows.get(i);
			if (level == 1) {
				body.append("\n\t\t{ ");
			} else {
				body.append("\n\t\t\t\t{ ");
			}

			// Do attribute columns
			boolean firstPair = true;
			for (final ResponseValue value : row) {
				if (!(value.getValue() instanceof List<?>)) {
					addAttribute(firstPair, body, value.getName(), value.getValue());
					if (value.getValue() != null) {
						firstPair = false;
					}
				} else {
					childRows = (List<Set<ResponseValue>>) value.getValue();
				}
			}

			// Do embedded child object columns
			if (level == 1 && childRows != null && childRows.size() > 0) {
				body.append(",\n\t\t\t\"");
				body.append(sqlResource.getMetaData().getChild().getRowSetAlias());
				body.append("\": [");
				serializeWriteRowsHierarchical(sqlResource, childRows, body, 2);
				body.append("\n\t\t\t]");
			}

			// Add line ending
			if (level == 1 && childRows != null && childRows.size() > 0) {
				body.append("\n\t\t}");
			} else {
				body.append(" }");
			}

			if (!lastRow) {
				body.append(",");
			}
		}
	}

	static enum DocType {
		Read, Write;
	}
}