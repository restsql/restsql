/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.restsql.core.Factory;
import org.restsql.core.Factory.SqlResourceFactoryException;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.Request.Type;
import org.restsql.core.RequestLogger;
import org.restsql.core.RequestUtil;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.security.SecurityFactory;

/**
 * Contains core JAX-RS Resource of the service, processing SQL Resource CRUD requests. Also lists available resources.
 * 
 * @author Mark Sawers
 */
@Path("res")
public class ResResource {
	private static final String PARAM_DEFINITION = "_definition";
	private static final String PARAM_METADATA = "_metadata";

	@DELETE
	@Path("{resName}/{resId1}")
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, final String requestBody,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.DELETE, resName, new String[] { resId1 }, null,
				requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@DELETE
	@Path("{resName}/{resId1}/{resId2}")
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.DELETE, resName, new String[] { resId1, resId2 },
				null, requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@DELETE
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			@PathParam("resId3") final String resId3, final String requestBody,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.DELETE, resName, new String[] { resId1, resId2,
				resId3 }, null, requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@DELETE
	@Path("{resName}")
	public Response delete(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Type.DELETE, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), requestBody, requestMediaType,
				acceptMediaType, securityContext);
	}

	@GET
	@Path("{resName}/{resId1}")
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@Context final UriInfo uriInfo, @HeaderParam("Accept") String acceptMediaType,
			@Context final HttpServletRequest httpRequest, @Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.SELECT, resName, new String[] { resId1 },
				getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType, securityContext);
	}

	@GET
	@Path("{resName}/{resId1}/{resId2}")
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @Context final UriInfo uriInfo,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.SELECT, resName, new String[] { resId1, resId2 },
				getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType, securityContext);
	}

	@GET
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @PathParam("resId3") final String resId3,
			@Context final UriInfo uriInfo, @HeaderParam("Accept") String acceptMediaType,
			@Context final HttpServletRequest httpRequest, @Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.SELECT, resName, new String[] { resId1, resId2,
				resId3 }, getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType,
				securityContext);
	}

	@GET
	@Path("{resName}")
	public Response get(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		if (uriInfo.getQueryParameters().containsKey(PARAM_METADATA)) {
			return Response.ok("Work in progress").type(MediaType.TEXT_PLAIN_TYPE).build();
		} else if (uriInfo.getQueryParameters().containsKey(PARAM_DEFINITION)) {
			try {
				return Response.ok(Factory.getSqlResourceDefinition(resName))
						.type(MediaType.APPLICATION_XML_TYPE).build();
			} catch (final SqlResourceFactoryException exception) {
				return handleException(httpRequest, null, null, exception, null);
			}
		} else {
			return executeRequest(httpRequest, Request.Type.SELECT, resName, null, null,
					getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType,
					securityContext);
		}
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response getResources(@Context final UriInfo uriInfo) {
		final StringBuffer requestBody = new StringBuffer(500);
		requestBody.append("<html>\n<requestBody style=\"font-family:sans-serif\">\n");
		final String baseUri = uriInfo.getBaseUri().toString() + "res/";
		try {
			List<String> resNames = Factory.getSqlResourceNames();
			requestBody.append("<span style=\"font-weight:bold\">SQL Resources</span><br/>\n");
			if (resNames.size() > 0) {
				requestBody.append("<table>\n");
				for (final String resName : resNames) {
					requestBody.append("<tr><td>");
					requestBody.append(resName);
					requestBody.append("</td><td><a href=\"");
					requestBody.append(baseUri);
					requestBody.append(resName);
					requestBody.append("?_limit=10&amp;_offset=0\">query</a></td>");
					requestBody.append("<td><a href=\"");
					requestBody.append(baseUri);
					requestBody.append(resName);
					requestBody.append("?_definition\">definition</a></td>");
					requestBody.append("<td><a href=\"");
					requestBody.append(baseUri);
					requestBody.append(resName);
					requestBody.append("?_metadata\">metadata</a></td>");
					requestBody.append("</tr>\n");
				}
			} else {
				requestBody.append("No resource definition files found in ");
				requestBody.append(Factory.getSqlResourcesDir());
				requestBody
						.append(" ... please correct your <code>sqlresources.dir</code> property in your restsql.properties file");
			}
			requestBody.append("</table>\n</requestBody>\n</html>");
		} catch (SqlResourceFactoryException exception) {
			requestBody.append(exception.getMessage());
			requestBody
					.append(" ... please correct your <code>sqlresources.dir</code> property in your restsql.properties file");
		}
		return Response.ok(requestBody.toString()).build();
	}

	@POST
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response post(@PathParam("resName") final String resName,
			final MultivaluedMap<String, String> formParams,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequest(httpRequest, Type.INSERT, resName, null, null, getNameValuePairs(formParams),
				requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}/{resId1}")
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, final String requestBody,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.INSERT, resName, new String[] { resId1 }, null,
				requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}/{resId1}/{resId2}")
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.INSERT, resName, new String[] { resId1, resId2 },
				null, requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			@PathParam("resId3") final String resId3, final String requestBody,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.INSERT, resName, new String[] { resId1, resId2,
				resId3 }, null, requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}")
	public Response post(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Type.INSERT, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), requestBody, requestMediaType,
				acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response put(@PathParam("resName") final String resName,
			final MultivaluedMap<String, String> formParams, @Context final UriInfo uriInfo,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequest(httpRequest, Type.UPDATE, resName, null,
				getNameValuePairs(uriInfo.getQueryParameters()), getNameValuePairs(formParams), requestBody,
				requestMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			final MultivaluedMap<String, String> formParams,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1 },
				getNameValuePairs(formParams), requestBody, requestMediaType, acceptMediaType,
				securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}")
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1 }, null,
				requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, final MultivaluedMap<String, String> formParams,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1, resId2 },
				getNameValuePairs(formParams), requestBody, requestMediaType, acceptMediaType,
				securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}")
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, final String requestBody,
			@HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1, resId2 },
				null, requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @PathParam("resId3") final String resId3,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1, resId2,
				resId3 }, null, requestBody, requestMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}")
	public Response put(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String requestBody, @HeaderParam("Content-Type") String requestMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Type.UPDATE, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), requestBody, requestMediaType,
				acceptMediaType, securityContext);
	}

	// Private utils

	/** Processes the request. The central method of this resource class. */
	private Response executeRequest(HttpServletRequest httpRequest, final Request.Type requestType,
			final String resName, SqlResource sqlResource, final List<NameValuePair> resIds,
			final List<NameValuePair> params, final String requestBody, String requestMediaType,
			String acceptMediaType, SecurityContext securityContext) {

		// Determine the response media type and create http attributes structure
		String responseMediaType = RequestUtil
				.getResponseMediaType(params, requestMediaType, acceptMediaType);
		final HttpRequestAttributes httpAttributes = HttpRequestHelper.getHttpRequestAttributes(httpRequest,
				requestBody, requestMediaType, responseMediaType);

		// Create logger
		final RequestLogger requestLogger = Factory.getRequestLogger();
		requestLogger.setHttpRequestAttributes(httpAttributes);

		// Authorize
		if (!SecurityFactory.getAuthorizer().isAuthorized(new SecurityContextAdapter(securityContext),
				requestType, resName)) {
			Status status = Status.FORBIDDEN;
			requestLogger.log(status.getStatusCode());
			return Response.status(status).build();
		}

		try {
			String responseBody = null;
			if (sqlResource == null) {
				sqlResource = Factory.getSqlResource(resName);
			}

			// Execute request
			if (requestType.equals(Request.Type.SELECT)) {
				final Request request = Factory.getRequest(httpAttributes, requestType, resName, resIds,
						params, null, requestLogger);
				responseBody = sqlResource.read(request, responseMediaType);
			} else { // INSERT, UPDATE or DELETE
				final int rowsAffected;
				if (requestMediaType != null
						&& !requestMediaType.equals(MediaType.APPLICATION_FORM_URLENCODED)
						&& requestBody != null && requestBody.length() > 0) {
					// requestBody is not null, use request processor
					rowsAffected = Factory.getRequestDeserializer(requestMediaType).execWrite(httpAttributes,
							requestType, resIds, sqlResource, requestBody, requestLogger);
				} else {
					final Request request = Factory.getRequest(httpAttributes, requestType, resName, resIds,
							params, null, requestLogger);
					rowsAffected = sqlResource.write(request);
				}
				responseBody = Factory.getResponseSerializer(responseMediaType).serializeWrite(rowsAffected);
			}

			// Log response and send it
			requestLogger.log(responseBody);
			final CacheControl cacheControl = new CacheControl();
			cacheControl.setNoCache(true);
			return Response.ok(responseBody).type(responseMediaType).cacheControl(cacheControl).build();
		} catch (final SqlResourceException exception) {
			return handleException(httpRequest, requestBody, requestMediaType, exception, requestLogger);
		}
	}

	/**
	 * Pre-processes request, parsing resource ids into a collection, and then passing the request on to
	 * executeRequest().
	 */
	private Response executeRequestParseResIds(HttpServletRequest httpRequest,
			final Request.Type requestType, final String resName, final String[] resIdValues,
			final List<NameValuePair> params, final String requestBody, String requestMediaType,
			String acceptMediaType, SecurityContext securityContext) {
		final SqlResource sqlResource;
		final List<NameValuePair> resIds;
		try {
			sqlResource = Factory.getSqlResource(resName);
			resIds = RequestUtil.getResIds(sqlResource, resIdValues);
		} catch (final SqlResourceException exception) {
			return handleException(httpRequest, requestBody, requestMediaType, exception, null);
		}
		return executeRequest(httpRequest, requestType, resName, sqlResource, resIds, params, requestBody,
				requestMediaType, acceptMediaType, securityContext);
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

	/**
	 * Determines exception type, logs issue and returns appropriate http status with the exception message in the body.
	 */
	private Response handleException(final HttpServletRequest httpRequest, final String requestBody,
			String requestMediaType, final SqlResourceException exception, RequestLogger requestLogger) {
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
			final HttpRequestAttributes httpAttribs = HttpRequestHelper.getHttpRequestAttributes(httpRequest,
					requestBody, requestMediaType, requestMediaType);
			requestLogger.setHttpRequestAttributes(httpAttribs);
		}
		requestLogger.log(status.getStatusCode(), exception);
		return Response.status(status).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
	}
}
