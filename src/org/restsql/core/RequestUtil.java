/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains utility methods to assist request processing.
 * 
 * @author Mark Sawers
 */
public class RequestUtil {

	/** Returns name-value pairs, resourceId and value, for given resource and ordered value array. */
	public static List<NameValuePair> getResIds(final SqlResource sqlResource, final String[] values) {
		List<NameValuePair> resIds = null;
		if (values != null) {
			resIds = new ArrayList<NameValuePair>(values.length);
			for (final TableMetaData table : sqlResource.getTables().values()) {
				if (table.isParent()) {
					for (final ColumnMetaData column : table.getPrimaryKeys()) {
						for (final String value : values) {
							if (value != null) {
								final NameValuePair resId = new NameValuePair(column.getColumnLabel(), value);
								resIds.add(resId);
							}
						}
					}
				}
			}
		}
		return resIds;
	}
}
