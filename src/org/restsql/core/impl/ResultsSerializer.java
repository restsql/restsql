/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.Config;
import org.restsql.core.SqlResource;

public class ResultsSerializer {
	public static boolean useXmlDirective = Boolean.valueOf(Config.properties.getProperty(
			Config.KEY_RESPONSE_USE_XML_DIRECTIVE, Config.DEFAULT_RESPONSE_USE_XML_DIRECTIVE));
	public static boolean useXmlSchema = Boolean.valueOf(Config.properties.getProperty(
			Config.KEY_RESPONSE_USE_XML_SCHEMA, Config.DEFAULT_RESPONSE_USE_XML_SCHEMA));

	public static void appendNameValuePair(final StringBuffer string, final String name, final Object value) {
		if (value != null) {
			string.append(" ");
			string.append(name);
			string.append("=\"");
			string.append(value.toString());
			string.append('"');
		}
	}

	/**
	 * Converts select results to xml string.
	 * 
	 * @param sqlResource SQL resource
	 * @param results results
	 * @return XML string
	 * @todo escape illegal xml chars, e.g. quotes
	 */
	public static String serializeReadHierarchical(final SqlResource sqlResource, final List<Map<String, Object>> results) {
		final StringBuffer string = new StringBuffer(results.size() * 100);
		appendReadDocStart(string);
		serializeRows(sqlResource, results, string, 1);
		appendReadDocEnd(string);
		return string.toString();
	}
	
	/**
	 * Converts select results to xml string.
	 * 
	 * @param sqlResource SQL resource
	 * @param resultSet results
	 * @return XML string
	 * @todo escape illegal xml chars, e.g. quotes
	 */
	public static String serializeReadFlat(final SqlResource sqlResource, final ResultSet resultSet)
			throws SQLException {
		final StringBuffer string = new StringBuffer(1000);
		appendReadDocStart(string);
		while (resultSet.next()) {
			string.append("\n\t<");
			string.append(sqlResource.getParentTable().getTableAlias());
			for (final ColumnMetaData column : sqlResource.getMetaData().getAllReadColumns()) {
				if (!column.isNonqueriedForeignKey()) {
					appendNameValuePair(string, column.getColumnLabel(), SqlUtils.getObjectByColumnNumber(
							column, resultSet));
				}
			}
			string.append(" />");
		}
		appendReadDocEnd(string);
		return string.toString();
		
	}

	public static String serializeWrite(final int rowsAffected) {
		final StringBuffer string = new StringBuffer(250);
		if (useXmlDirective) {
			string.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		}
		if (useXmlSchema) {
			string
					.append("<writeResponse xmlns=\"http://restsql.org/schema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://restsql.org/schema Response.xsd\"");
		} else {
			string.append("<writeResponse");
		}
		appendNameValuePair(string, "rowsAffected", String.valueOf(rowsAffected));
		string.append(" />");
		return string.toString();
	}

	public static void setUseXmlDirective(final boolean use) {
		useXmlDirective = use;
	}

	public static void setUseXmlSchema(final boolean use) {
		useXmlSchema = use;
	}

	// Private utils

	static void appendReadDocEnd(final StringBuffer string) {
		string.append("\n</readResponse>");
	}

	static void appendReadDocStart(final StringBuffer string) {
		if (useXmlDirective) {
			string.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		}
		if (useXmlSchema) {
			string
					.append("<readResponse xmlns=\"http://restsql.org/schema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://restsql.org/schema Response.xsd \">");
		} else {
			string.append("<readResponse>");
		}
	}

	@SuppressWarnings("unchecked")
	private static void serializeRows(final SqlResource sqlResource, final List<Map<String, Object>> rows,
			final StringBuffer string, final int level) {
		boolean hierarchical = false;
		String tableAlias = sqlResource.getParentTable().getTableAlias();
		if (level == 2) {
			tableAlias = sqlResource.getChildTable().getTableAlias();
		}
		for (final Map<String, Object> row : rows) {
			if (level == 1) {
				string.append("\n\t<");
			} else {
				string.append("\n\t\t<");
			}
			string.append(tableAlias);

			// Do parent attribute columns
			for (final String columnLabel : row.keySet()) {
				final Object value = row.get(columnLabel);
				if (!(value instanceof List<?>)) {
					appendNameValuePair(string, columnLabel, value);
				}
			}

			// Do embedded child object columns
			for (final String columnLabel : row.keySet()) {
				final Object value = row.get(columnLabel);
				if (value instanceof List<?>) {
					hierarchical = true;
					string.append(">");
					final List<Map<String, Object>> childRows = (List<Map<String, Object>>) value;
					serializeRows(sqlResource, childRows, string, 2);
				}
			}

			if (hierarchical) {
				string.append("\n\t</");
				string.append(tableAlias);
				string.append(">");
			} else if (level == 2) {
				string.append(" />");
			} else {
				string.append(">");
			}
		}
	}
}
