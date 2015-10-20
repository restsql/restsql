/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.serial;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.restsql.core.ColumnMetaData;
import org.restsql.core.Config;
import org.restsql.core.ResponseSerializer;
import org.restsql.core.ResponseValue;
import org.restsql.core.SqlResource;
import org.restsql.core.WriteResponse;

/**
 * Converts read/write results to an XML document.
 * 
 * @author Mark Sawers
 */
public class XmlResponseSerializer implements ResponseSerializer {
	private static boolean useXmlDirective = Boolean.valueOf(Config.properties.getProperty(
			Config.KEY_RESPONSE_USE_XML_DIRECTIVE, Config.DEFAULT_RESPONSE_USE_XML_DIRECTIVE));
	private static boolean useXmlSchema = Boolean.valueOf(Config.properties.getProperty(
			Config.KEY_RESPONSE_USE_XML_SCHEMA, Config.DEFAULT_RESPONSE_USE_XML_SCHEMA));

	/** Overrides configuration. For test use only. */
	public static void setUseXmlDirective(final boolean use) {
		useXmlDirective = use;
	}

	/** Overrides configuration. For test use only. */
	public static void setUseXmlSchema(final boolean use) {
		useXmlSchema = use;
	}

	@Override
	public String getSupportedMediaType() {
		return "application/xml";
	}

	/**
	 * Converts flat select results to an XML document.
	 * 
	 * @param sqlResource SQL resource
	 * @param resultSet results
	 * @return XML doc
	 */
	@Override
	public String serializeReadFlat(final SqlResource sqlResource, final ResultSet resultSet)
			throws SQLException {
		StringBuilder body = null;
		while (resultSet.next()) {
			if (body == null) {
				body = new StringBuilder(1000);
			}
			body.append("\n\t<");
			body.append(sqlResource.getMetaData().getParent().getRowAlias());
			for (final ColumnMetaData column : sqlResource.getMetaData().getAllReadColumns()) {
				if (!column.isNonqueriedForeignKey()) {
					addAttribute(body, column.getColumnLabel(), column.getResultByNumber(resultSet));
				}
			}
			body.append(" />");
		}
		return completeDoc(DocType.Read, null, body);
	}

	/**
	 * Converts hierarchical select results to an XML document.
	 * 
	 * @param sqlResource SQL resource
	 * @param results results
	 * @return XML doc
	 */
	@Override
	public String serializeReadHierarchical(final SqlResource sqlResource,
			final List<Map<String, Object>> results) {
		final StringBuilder body = new StringBuilder(results.size() * 100);
		serializeReadRowsHierarchical(sqlResource, results, body, 1);
		return completeDoc(DocType.Read, null, body);
	}

	/**
	 * Converts write results to an XML document.
	 * 
	 * @param response response
	 * @return XML doc
	 */
	@Override
	public String serializeWrite(final SqlResource sqlResource, final WriteResponse response) {
		StringBuilder body = null;
		if (response.getRows() != null) {
			body = new StringBuilder(response.getRows().size() * 100);
			serializeWriteRows(sqlResource, response.getRows(), body, 1);
		}
		return completeDoc(DocType.Write,
				new String[] { "rowsAffected", String.valueOf(response.getRowsAffected()) }, body);
	}

	// Private utils

	private void addAttribute(final StringBuilder doc, final String name, final Object value) {
		if (value != null) {
			doc.append(" ");
			doc.append(name);
			doc.append("=\"");
			doc.append(StringEscapeUtils.escapeXml(value.toString()));
			doc.append('"');
		}
	}

	private String completeDoc(final DocType docType, final String[] attributes, final StringBuilder body) {
		int docLength = (body != null) ? body.length() + 250 : 250;
		StringBuilder doc = new StringBuilder(docLength);

		// Opening directive, if configured (default off)
		if (useXmlDirective) {
			doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		}

		// Init doc type element including the schema refs, if configured (default off)
		String elementName = (docType == DocType.Read) ? "<readResponse" : "<writeResponse";
		doc.append(elementName);
		if (useXmlSchema) {
			doc.append(" xmlns=\"http://restsql.org/schema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://restsql.org/schema Response.xsd \"");
		}

		// Add doc type element attributes
		if (attributes != null) {
			for (int i = 0; i < attributes.length; i += 2) {
				addAttribute(doc, attributes[i], attributes[i + 1]);
			}
		}

		// Cap the doc element and insert the body, if non-empty
		if (body != null && body.length() > 0) {
			doc.append(">");
			doc.append(body);
			if (docType == DocType.Read) {
				doc.append("\n</readResponse>");
			} else { // docType = DocType.Write
				doc.append("\n</writeResponse>");
			}
		} else {
			doc.append(" />");
		}

		return doc.toString();
	}

	/** One-level recursive method to serialize hierarchical results. */
	private void serializeReadRowsHierarchical(final SqlResource sqlResource,
			final List<Map<String, Object>> rows, final StringBuilder body, final int level) {
		String tableAlias = sqlResource.getMetaData().getParent().getRowAlias();
		if (level == 2) {
			tableAlias = sqlResource.getMetaData().getChild().getRowAlias();
		}
		for (final Map<String, Object> row : rows) {
			boolean hasChildren = false;

			// Start element
			if (level == 1) {
				body.append("\n\t<");
			} else {
				body.append("\n\t\t<");
			}
			body.append(tableAlias);

			// Do attribute columns
			for (final String columnLabel : row.keySet()) {
				final Object value = row.get(columnLabel);
				if (!(value instanceof List<?>)) {
					addAttribute(body, columnLabel, value);
				}
			}

			// Do embedded child object columns
			for (final String columnLabel : row.keySet()) {
				final Object value = row.get(columnLabel);
				if (value instanceof List<?> && ((List<?>) value).size() > 0) {
					hasChildren = true;
					body.append(">");
					@SuppressWarnings("unchecked")
					final List<Map<String, Object>> childRows = (List<Map<String, Object>>) value;
					serializeReadRowsHierarchical(sqlResource, childRows, body, 2);
				}
			}

			// Close element
			if (level == 1) {
				if (hasChildren) {
					// Enclose the children with the parent end element
					body.append("\n\t</");
					body.append(tableAlias);
					body.append(">");
				} else {
					// Close the parent
					body.append(" />");
				}
			} else { // level == 2
				// Close the child
				body.append(" />");
			}
		}
	}

	/** One-level recursive method to serialize hierarchical results. */
	private void serializeWriteRows(final SqlResource sqlResource, final List<Set<ResponseValue>> rows,
			final StringBuilder body, final int level) {
		String tableAlias = sqlResource.getMetaData().getParent().getRowAlias();
		if (level == 2) {
			tableAlias = sqlResource.getMetaData().getChild().getRowAlias();
		}
		for (final Set<ResponseValue> row : rows) {
			boolean hasChildren = false;

			// Start element
			if (level == 1) {
				body.append("\n\t<");
			} else {
				body.append("\n\t\t<");
			}
			body.append(tableAlias);

			// Do attribute columns
			for (final ResponseValue value : row) {
				if (!(value.getValue() instanceof List<?>)) {
					addAttribute(body, value.getName(), value.getValue());
				}
			}

			// Do embedded child object columns
			for (final ResponseValue value : row) {
				if (value.getValue() instanceof List<?> && ((List<?>) value.getValue()).size() > 0) {
					hasChildren = true;
					body.append(">"); // cap the parent element
					@SuppressWarnings("unchecked")
					final List<Set<ResponseValue>> childRows = (List<Set<ResponseValue>>) value.getValue();
					serializeWriteRows(sqlResource, childRows, body, 2);
				}
			}

			// Close element
			if (level == 1) {
				if (hasChildren) {
					// Enclose the children with the parent end element
					body.append("\n\t</");
					body.append(tableAlias);
					body.append(">");
				} else {
					// Close the parent
					body.append(" />");
				}
			} else { // level == 2
				// Close the child
				body.append(" />");
			}
		}
	}

	static enum DocType {
		Read, Write;
	}
}
