/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.restsql.core.impl.MediaTypeParser;

/**
 * Contains utility methods to assist request processing.
 * 
 * @author Mark Sawers
 */
public class RequestUtil {
	private static Collection<String> supportedMediaTypes = new ArrayList<String>();

	// Note that the default media type, application/xml, must be the **LAST** one added!
	static {
		supportedMediaTypes.add("application/json");
		supportedMediaTypes.add("application/xml");
	}

	/** Returns name-value pairs, resourceId and value, for given resource and ordered value array. */
	public static List<NameValuePair> getResIds(final SqlResource sqlResource, final String[] values) {
		List<NameValuePair> resIds = null;
		if (values != null) {
			resIds = new ArrayList<NameValuePair>(values.length);
			for (final TableMetaData table : sqlResource.getMetaData().getTableMap().values()) {
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

	/**
	 * Determines content type from parameters, and removing the output param if present. If it is not present then uses
	 * the accept media type. The accept media type can be single mime-type or an Accept header string with multiple
	 * mime-types, some possibly with quality factors. If the accept media type is not present, uses the request media
	 * type, or if it is null or form url encoded, then returns the default media type,
	 * {@link HttpRequestAttributes#DEFAULT_MEDIA_TYPE}. Note: The content parameter value overrides the accept media
	 * type!
	 */
	public static String getResponseMediaType(final List<NameValuePair> params,
			final String requestMediaType, String acceptMediaType) {
		String responseMediaType = null;
		if (params != null) {
			int outputIndex = -1;
			for (int i = 0; i < params.size(); i++) {
				final NameValuePair param = params.get(i);
				if (param.getName().equalsIgnoreCase(Request.PARAM_NAME_OUTPUT)) {
					responseMediaType = convertToStandardInternetMediaType(param.getValue());
					outputIndex = i;
					break;
				}
			}

			if (outputIndex >= 0) {
				params.remove(outputIndex);
			}
		}
		if (responseMediaType == null) {
			if (acceptMediaType != null) {
				responseMediaType = MediaTypeParser.bestMatch(supportedMediaTypes, acceptMediaType);
			} else if (requestMediaType == null
					|| requestMediaType.equals("application/x-www-form-urlencoded")) {
				responseMediaType = HttpRequestAttributes.DEFAULT_MEDIA_TYPE;
			} else {
				responseMediaType = requestMediaType;
			}
		}
		return responseMediaType;
	}

	/** Converts short form of media type to the proper internet standard, e.g. json to application/json. */
	public static String convertToStandardInternetMediaType(String mediaType) {
		if (mediaType == null) {
			return null;
		} else if (mediaType.equalsIgnoreCase("xml")) {
			return "application/xml";
		} else if (mediaType.equalsIgnoreCase("json")) {
			return "application/json";
		} else {
			return mediaType;
		}
	}
}
