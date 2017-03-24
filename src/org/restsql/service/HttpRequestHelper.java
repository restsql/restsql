/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.restsql.core.Factory;
import org.restsql.core.Factory.SqlResourceFactoryException;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.RequestLogger;
import org.restsql.core.SqlResourceException;

/**
 * Contains service utilities.
 * 
 * @author Mark Sawers
 */
public class HttpRequestHelper {

	/** Builds HTML page with SQL Resources and actions for each. Used for /restsql/res and /restsql/conf. */
	public static StringBuffer buildSqlResourceListing() {
		final StringBuffer requestBody = new StringBuffer(500);
		requestBody
				.append("<!DOCTYPE html>\n<html><head><link rel=\"icon\" type=\"image/png\" href=\"../assets/favicon.ico\"/>");
		requestBody
				.append("<style>td{padding: 6px} table,tr,td {border-collapse: collapse} tr:nth-child(even){background-color: #f2f2f2} body {font-family:sans-serif}</style></head>\n<body>\n");
		try {
			final List<String> resNames = Factory.getSqlResourceNames();
			requestBody
					.append("<span style=\"font-weight:bold\"><a href=\"..\">restSQL</a> SQL Resources</span><hr/>\n");
			if (resNames.size() > 0) {
				requestBody.append("<table>\n");
				for (final String resName : resNames) {
					requestBody.append("<tr><td>");
					requestBody.append(resName);
					requestBody.append("</td>");

					// Query (JSON)
					requestBody.append("<td><a href=\"");
					requestBody.append("../res/");
					requestBody.append(resName);
					requestBody
							.append("?_output=application/json&_limit=10&amp;_offset=0\">query/json</a></td>");

					// Query (XML)
					requestBody.append("<td><a href=\"");
					requestBody.append("../res/");
					requestBody.append(resName);
					requestBody.append("?_limit=10&amp;_offset=0\">query/xml</a></td>");

					// Definition
					requestBody.append("<td><a href=\"");
					requestBody.append("../conf/definition/");
					requestBody.append(resName);
					requestBody.append("\">definition</a></td>");

					// MetaData
					requestBody.append("<td><a href=\"");
					requestBody.append("../conf/metadata/");
					requestBody.append(resName);
					requestBody.append("\">metadata</a></td>");

					// Documentation
					requestBody.append("<td><a href=\"");
					requestBody.append("../conf/documentation/");
					requestBody.append(resName);
					requestBody.append("\">documentation</a></td>");

					// Reload
					requestBody.append("<td><a href=\"");
					requestBody.append("../conf/reload/");
					requestBody.append(resName);
					requestBody.append("\">reload</a></td>");

					requestBody.append("</tr>\n");
				}
			} else {
				requestBody.append("No resource definition files found in ");
				requestBody.append(Factory.getSqlResourcesDir());
				requestBody
						.append(" ... please correct your <code>sqlresources.dir</code> property in your restsql.properties file");
			}
			requestBody.append("</table><p/>\n<a href=\"../swagger-ui/\">Swagger</a></body>\n</html>");
		} catch (final SqlResourceFactoryException exception) {
			requestBody.append(exception.getMessage());
			requestBody
					.append(" ... please correct your <code>sqlresources.dir</code> property in your restsql.properties file");
		}
		return requestBody;
	}

	/**
	 * Creates attributes helper object from http request with a request body.
	 * 
	 * @param httpRequest servlet request
	 * @param requestBody request body
	 * @param requestMediaType request body format, use internet media type e.g. application/xml
	 * @param responseMediaType request body format, use internet media type e.g. application/xml
	 */
	public static HttpRequestAttributes getHttpRequestAttributes(final HttpServletRequest httpRequest,
			final String requestBody, final String requestMediaType, final String responseMediaType) {
		String client, method, uri;
		client = httpRequest.getRemoteAddr();
		method = httpRequest.getMethod();
		if (httpRequest.getQueryString() == null) {
			uri = httpRequest.getRequestURI();
		} else {
			uri = httpRequest.getRequestURI() + "?" + httpRequest.getQueryString();
		}

		return Factory.getHttpRequestAttributes(client, method, uri, requestBody, requestMediaType,
				responseMediaType);
	}

	/**
	 * Converts form params into string. Jersey does not provide the string form of the request body when it generates a
	 * form param map.
	 */
	public static String getRequestBodyFromFormParams(final MultivaluedMap<String, String> formParams) {
		final StringBuilder string = new StringBuilder(300);
		for (final String key : formParams.keySet()) {
			if (string.length() > 0) {
				string.append('&');
			}
			string.append(key);
			string.append("=");
			string.append(formParams.get(key).get(0));
		}
		return string.toString();
	}

	/**
	 * Determines exception type, logs issue and returns appropriate http status with the exception message in the body.
	 */
	public static Response handleException(final HttpServletRequest httpRequest, final String requestBody,
			final String requestMediaType, final SqlResourceException exception, RequestLogger requestLogger) {
		Status status;
		if (exception instanceof SqlResourceFactoryException) {
			status = Status.NOT_FOUND;
		} else if (exception instanceof InvalidRequestException) {
			status = Status.BAD_REQUEST;
		} else { // exception instanceof SqlResourceException
			status = Status.INTERNAL_SERVER_ERROR;
		}
		if (requestLogger == null) {
			requestLogger = Factory.getRequestLogger();
			final HttpRequestAttributes httpAttribs = getHttpRequestAttributes(httpRequest, requestBody,
					requestMediaType, requestMediaType);
			requestLogger.setHttpRequestAttributes(httpAttribs);
		}
		requestLogger.log(status.getStatusCode(), exception);
		return Response.status(status).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
	}
}