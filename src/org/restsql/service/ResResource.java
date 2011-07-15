/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.restsql.core.Factory;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.RequestLogger;
import org.restsql.core.RequestUtil;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.Factory.SqlResourceFactoryException;
import org.restsql.core.Request.Type;
import org.restsql.core.impl.Serializer;

/**
 * Contains core JAX-RS Resource of the service, processing SQL Resource CRUD requests. Also lists available resources.
 * 
 * @author Mark Sawers
 */
@Path("res")
@Produces(MediaType.APPLICATION_XML)
public class ResResource {
	private static final String PARAM_METADATA = "_metadata";
	private static final String PARAM_DEFINITION = "_definition";

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response getResources(@Context final UriInfo uriInfo) {
		final StringBuffer body = new StringBuffer(500);
		body.append("<html>\n<body style=\"font-family:sans-serif\">\n");
		body.append("<span style=\"font-weight:bold\">SQL Resources</span><br/>\n");
		body.append("<table>\n");
		String baseUri = uriInfo.getBaseUri().toString() + "res/";
		for (String resName : Factory.getSqlResourceNames()) {
			body.append("<tr><td>");
			body.append(resName);
			body.append("</td><td><a href=\"");
			body.append(baseUri);
			body.append(resName);
			body.append("?_limit=10&amp;_offset=0\">query</a></td>");
			body.append("<td><a href=\"");
			body.append(baseUri);
			body.append(resName);
			body.append("?_definition\">definition</a></td>");
			body.append("<td><a href=\"");
			body.append(baseUri);
			body.append(resName);
			body.append("?_metadata\">metadata</a></td>");
			body.append("</tr>\n");
		}
		body.append("</table>\n</body>\n</html>");
		return Response.ok(body.toString()).build();
	}

	@DELETE
	@Path("{resName}/{resId1}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.DELETE, resName, new String[] { resId1 }, null, body,
				requestLogger);
	}

	@DELETE
	@Path("{resName}/{resId1}/{resId2}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.DELETE, resName, new String[] { resId1, resId2 }, null, body,
				requestLogger);
	}

	@DELETE
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			@PathParam("resId3") final String resId3, final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.DELETE, resName, new String[] { resId1, resId2, resId3 }, null,
				body, requestLogger);
	}

	@DELETE
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response delete(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequest(Type.DELETE, resName, null, null, getNameValuePairs(uriInfo
				.getQueryParameters()), body, requestLogger);
	}

	@GET
	@Path("{resName}/{resId1}")
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request);
		return executeRequestParseResIds(Type.SELECT, resName, new String[] { resId1 }, null, null,
				requestLogger);
	}

	@GET
	@Path("{resName}/{resId1}/{resId2}")
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request);
		return executeRequestParseResIds(Type.SELECT, resName, new String[] { resId1, resId2 }, null, null,
				requestLogger);
	}

	@GET
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @PathParam("resId3") final String resId3,
			@Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request);
		return executeRequestParseResIds(Type.SELECT, resName, new String[] { resId1, resId2, resId3 }, null,
				null, requestLogger);
	}

	@GET
	@Path("{resName}")
	public Response get(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			@Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request);
		if (uriInfo.getQueryParameters().containsKey(PARAM_METADATA)) {
			return Response.ok("Work in progress").type(MediaType.TEXT_PLAIN_TYPE).build();
		} else if (uriInfo.getQueryParameters().containsKey(PARAM_DEFINITION)) {
			try {
				return Response.ok(Factory.getSqlResourceDefinition(resName)).type(
						MediaType.APPLICATION_XML_TYPE).build();
			} catch (SqlResourceFactoryException exception) {
				return handleException(exception, requestLogger);
			}
		} else {
			return executeRequest(Request.Type.SELECT, resName, null, null, getNameValuePairs(uriInfo
					.getQueryParameters()), null, requestLogger);
		}
	}

	@POST
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response post(@PathParam("resName") final String resName,
			final MultivaluedMap<String, String> formParams, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, formParams);
		return executeRequest(Type.INSERT, resName, null, null, getNameValuePairs(formParams), null,
				requestLogger);
	}

	@POST
	@Path("{resName}/{resId1}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.INSERT, resName, new String[] { resId1 }, null, body,
				requestLogger);
	}

	@POST
	@Path("{resName}/{resId1}/{resId2}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.INSERT, resName, new String[] { resId1, resId2 }, null, body,
				requestLogger);
	}

	@POST
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			@PathParam("resId3") final String resId3, final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.INSERT, resName, new String[] { resId1, resId2, resId3 }, null,
				body, requestLogger);
	}

	@POST
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response post(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequest(Type.INSERT, resName, null, null, getNameValuePairs(uriInfo
				.getQueryParameters()), body, requestLogger);
	}

	@PUT
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response put(@PathParam("resName") final String resName,
			final MultivaluedMap<String, String> formParams, @Context final UriInfo uriInfo,
			@Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, formParams);
		return executeRequest(Type.UPDATE, resName, null, getNameValuePairs(uriInfo.getQueryParameters()),
				getNameValuePairs(formParams), null, requestLogger);
	}

	@PUT
	@Path("{resName}/{resId1}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			final MultivaluedMap<String, String> formParams, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, formParams);
		return executeRequestParseResIds(Type.UPDATE, resName, new String[] { resId1 },
				getNameValuePairs(formParams), null, requestLogger);
	}

	@PUT
	@Path("{resName}/{resId1}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.UPDATE, resName, new String[] { resId1 }, null, body,
				requestLogger);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, final MultivaluedMap<String, String> formParams,
			@Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, formParams);
		return executeRequestParseResIds(Type.UPDATE, resName, new String[] { resId1, resId2 },
				getNameValuePairs(formParams), null, requestLogger);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.UPDATE, resName, new String[] { resId1, resId2 }, null, body,
				requestLogger);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @PathParam("resId3") final String resId3,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequestParseResIds(Type.UPDATE, resName, new String[] { resId1, resId2, resId3 }, null,
				body, requestLogger);
	}

	@PUT
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response put(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String body, @Context HttpServletRequest request) {
		RequestLogger requestLogger = Factory.getRequestLogger(request, body);
		return executeRequest(Type.UPDATE, resName, null, null, getNameValuePairs(uriInfo
				.getQueryParameters()), body, requestLogger);
	}

	// Private utils

	private Response executeRequestParseResIds(final Request.Type requestType, final String resName,
			final String[] resIdValues, final List<NameValuePair> params, final String body,
			RequestLogger requestLogger) {
		try {
			final SqlResource sqlResource = Factory.getSqlResource(resName);
			final List<NameValuePair> resIds = RequestUtil.getResIds(sqlResource, resIdValues);
			return executeRequest(requestType, resName, sqlResource, resIds, params, body, requestLogger);
		} catch (final SqlResourceException exception) {
			return handleException(exception, requestLogger);
		}
	}

	private Response executeRequest(final Request.Type requestType, final String resName,
			SqlResource sqlResource, final List<NameValuePair> resIds, final List<NameValuePair> params,
			final String body, RequestLogger requestLogger) {
		final int rowsAffected;
		try {
			String responseBody = null;
			if (sqlResource == null) {
				sqlResource = Factory.getSqlResource(resName);
			}

			if (requestType.equals(Request.Type.SELECT)) {
				final Request request = Factory.getRequest(requestType, resName, resIds, params, null,
						requestLogger);
				responseBody = sqlResource.readXml(request);
			} else { // INSERT, UPDATE or DELETE
				if (body == null || body.length() == 0) {
					final Request request = Factory.getRequest(requestType, resName, resIds, params, null,
							requestLogger);
					rowsAffected = sqlResource.write(request);
				} else { // body is not null, use xml request processor
					rowsAffected = XmlRequestProcessor.execWrite(requestType, resIds, sqlResource, body,
							requestLogger);
				}
				responseBody = Serializer.serializeWrite(rowsAffected);
			}

			requestLogger.log(responseBody);
			CacheControl cacheControl = new CacheControl();
			cacheControl.setNoCache(true);
			return Response.ok(responseBody).type(MediaType.APPLICATION_XML_TYPE).cacheControl(cacheControl)
					.build();
		} catch (final SqlResourceException exception) {
			return handleException(exception, requestLogger);
		}
	}

	private Response handleException(SqlResourceException exception, RequestLogger requestLogger) {
		Status status;
		if (exception instanceof SqlResourceFactoryException) {
			status = Status.NOT_FOUND;
		} else if (exception instanceof InvalidRequestException) {
			status = Status.BAD_REQUEST;
		} else { // exception instanceof SqlResourceException
			status = Status.INTERNAL_SERVER_ERROR;
		}
		requestLogger.log(status.getStatusCode(), exception);
		return Response.status(status).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
	}

	// Assumes no matrixing
	private List<NameValuePair> getNameValuePairs(final MultivaluedMap<String, String> formOrQueryParams) {
		final List<NameValuePair> params = new ArrayList<NameValuePair>(formOrQueryParams.size());
		for (final String key : formOrQueryParams.keySet()) {
			final NameValuePair param = new NameValuePair(key, formOrQueryParams.get(key).get(0));
			params.add(param);
		}
		return params;
	}
}
