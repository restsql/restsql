/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restsql.core.RequestValue.Operator;
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
		supportedMediaTypes.add("application/x-www-form-urlencoded");
		supportedMediaTypes.add("application/json");
		supportedMediaTypes.add("application/xml");
	}

	/** Converts short form of media type to the proper internet standard, e.g. json to application/json. */
	public static String convertToStandardInternetMediaType(final String mediaType) {
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

	/**
	 * Returns name-value pairs, resourceId and value, for given resource and ordered value array.
	 * 
	 * @throws InvalidRequestException
	 */
	public static List<RequestValue> getResIds(final SqlResource sqlResource, final String[] values) {
		List<RequestValue> resIds = null;
		if (values != null) {
			resIds = new ArrayList<RequestValue>(values.length);
			for (final TableMetaData table : sqlResource.getMetaData().getTableMap().values()) {
				if (table.isParent()) {
					for (final ColumnMetaData column : table.getPrimaryKeys()) {
						for (final String value : values) {
							if (value != null) {
								final RequestValue resId = new RequestValue(column.getColumnLabel(), value,
										Operator.Equals);
								resIds.add(resId);
							}
						}
					}
				}
			}
		}
		return resIds;
	}

	/** Returns best match for Content-Type header or the original media type if it doesn't match. */
	public static String getRequestMediaType(final String contentMediaType) {
		String requestMediaType = null;
		if (contentMediaType != null && contentMediaType.length() > 0) {
			try {
				requestMediaType = MediaTypeParser.bestMatch(supportedMediaTypes, contentMediaType);
				if (requestMediaType == null || requestMediaType.length() == 0) {
					requestMediaType = contentMediaType;
				}
			} catch (Exception exception) {
				requestMediaType = contentMediaType;
			}
		}
		return requestMediaType;
	}

	/**
	 * Determines content type from parameters, and removing the output param if present. If it is not present then uses
	 * the accept media type. The accept media type can be single mime-type or an Accept header string with multiple
	 * mime-types, some possibly with quality factors. If the accept media type is not present, uses the request media
	 * type, or if it is null or form url encoded, then returns the default media type,
	 * {@link HttpRequestAttributes#DEFAULT_MEDIA_TYPE}. Note: The content parameter value overrides the accept media
	 * type!
	 */
	public static String getResponseMediaType(final List<RequestValue> params, final String requestMediaType,
			final String acceptMediaType) {
		String responseMediaType = null;
		if (params != null) {
			int outputIndex = -1;
			for (int i = 0; i < params.size(); i++) {
				final RequestValue param = params.get(i);
				if (param.getName().equalsIgnoreCase(Request.PARAM_NAME_OUTPUT)) {
					responseMediaType = convertToStandardInternetMediaType(param.getValue().toString());
					outputIndex = i;
					break;
				}
			}

			if (outputIndex >= 0) {
				params.remove(outputIndex);
			}
		}
		if (responseMediaType == null) {
			if (acceptMediaType != null && acceptMediaType.length() > 0) {
				responseMediaType = MediaTypeParser.bestMatch(supportedMediaTypes, acceptMediaType);
				if (responseMediaType == null || responseMediaType.length() == 0) {
					responseMediaType = acceptMediaType;
				}
			} else if (requestMediaType == null
					|| requestMediaType.equals("application/x-www-form-urlencoded")) {
				responseMediaType = HttpRequestAttributes.DEFAULT_MEDIA_TYPE;
			} else {
				responseMediaType = requestMediaType;
			}
		}
		return responseMediaType;
	}

	/**
	 * Checks for parameter list validity.
	 * 
	 * @throws InvalidRequestException if a parameter is included more than once unless there the operators
	 */
	public static void checkForInvalidMultipleParameters(List<RequestValue> params)
			throws InvalidRequestException {
		if (params != null && params.size() > 0) {
			// First organize them into a map by parameter name
			Map<String, List<RequestValue>> paramsByName = new HashMap<String, List<RequestValue>>(
					params.size());
			for (RequestValue param : params) {
				List<RequestValue> list = paramsByName.get(param.getName());
				if (list == null) {
					list = new ArrayList<RequestValue>(1);
					paramsByName.put(param.getName(), list);
				}
				list.add(param);

			}

			// Now iterate through each and look for invalid multiples
			for (List<RequestValue> list : paramsByName.values()) {
				if (list.size() > 1) {
					String name = list.get(0).getName();
					if (list.size() == 2) {
						Operator first = list.get(0).getOperator();
						Operator second = list.get(1).getOperator();
						if (first == Operator.Equals || second == Operator.Equals) {
							throw new InvalidRequestException("Parameter " + name + " found twice");
						} else if ((first == Operator.GreaterThan || first == Operator.GreaterThanOrEqualTo)
								&& (second == Operator.GreaterThan || second == Operator.GreaterThanOrEqualTo)) {
							throw new InvalidRequestException("Parameter " + name + " found twice with "
									+ first + " and " + second + " operators");
						} else if ((first == Operator.LessThan || first == Operator.LessThanOrEqualTo)
								&& (second == Operator.LessThan || second == Operator.LessThanOrEqualTo)) {
							throw new InvalidRequestException("Parameter " + name + " found twice with "
									+ first + " and " + second + " operators");
						}
					} else {
						throw new InvalidRequestException("Parameter " + name + " found " + list.size()
								+ " times");
					}
				}
			}
		}
	}
}
